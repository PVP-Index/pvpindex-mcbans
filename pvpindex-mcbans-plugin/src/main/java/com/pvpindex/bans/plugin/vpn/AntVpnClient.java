/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.vpn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * WebSocket client for the AntVPN realtime API.
 *
 * <p>Connects to {@code wss://api.antivpn.io/connect} using a JWT Bearer token
 * in the HTTP upgrade header. Sends VERIFY requests for each joining player and
 * resolves the associated {@link CompletableFuture} when the response arrives.</p>
 *
 * <p>Reconnects automatically using exponential backoff (1 s to 30 s) when the
 * connection drops. The server sends a WebSocket PING every 54 seconds; Java's
 * built-in WebSocket client handles PONG responses automatically.</p>
 */
public class AntVpnClient implements WebSocket.Listener {

    private static final String WS_URL        = "wss://api.antivpn.io/connect";
    private static final int    BASE_DELAY_MS = 1_000;
    private static final int    MAX_DELAY_MS  = 30_000;

    private final String jwtToken;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;

    /** Pending VERIFY requests keyed by transactionalId. */
    private final Map<String, CompletableFuture<VpnCheckResult>> pendingChecks
            = new ConcurrentHashMap<>();

    /** Buffer for accumulating partial WebSocket text frames. */
    private final StringBuilder frameBuffer = new StringBuilder();

    private volatile WebSocket webSocket;
    private volatile boolean protectionEnabled = true;
    private volatile boolean isAttackMode      = false;
    private volatile boolean connected          = false;
    private final AtomicBoolean shutdown        = new AtomicBoolean(false);

    private int reconnectAttempt = 0;

    public AntVpnClient(String jwtToken, Logger logger) {
        this.jwtToken  = jwtToken;
        this.logger    = logger;
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "MCBans-AntVPN");
            t.setDaemon(true);
            return t;
        });
        pool.setRemoveOnCancelPolicy(true);
        this.scheduler = pool;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Open the WebSocket connection. Non-blocking; connection happens asynchronously. */
    public void connect() {
        if (shutdown.get()) {
            return;
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + jwtToken)
                .buildAsync(URI.create(WS_URL), this)
                .whenComplete((ws, ex) -> {
                    if (ex != null) {
                        logger.warning("[MCBans-VPN] WebSocket connect failed: " + ex.getMessage());
                        scheduleReconnect();
                    }
                    // onOpen() will set connected = true and reset backoff
                });
    }

    /** Gracefully close the WebSocket and stop the scheduler. */
    public void disconnect() {
        shutdown.set(true);
        scheduler.shutdownNow();
        WebSocket ws = this.webSocket;
        if (ws != null && !ws.isOutputClosed()) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "Plugin disabled").join();
        }
        // Fail all pending futures so callers don't block indefinitely
        pendingChecks.values().forEach(f -> f.complete(VpnCheckResult.pass()));
        pendingChecks.clear();
    }

    /** Whether the WebSocket is currently connected. */
    public boolean isConnected() {
        return connected;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sends a VERIFY request to AntVPN and returns a future that resolves when
     * the server responds (or when the caller's timeout elapses).
     *
     * @param playerName last known username
     * @param playerUuid player UUID string (with or without dashes)
     * @param ipAddress  IP address to check
     * @return a future resolving to the check result
     */
    public CompletableFuture<VpnCheckResult> checkPlayer(String playerName,
                                                          String playerUuid,
                                                          String ipAddress) {
        if (!connected || webSocket == null || webSocket.isOutputClosed()) {
            return CompletableFuture.completedFuture(VpnCheckResult.pass());
        }

        if (!protectionEnabled) {
            return CompletableFuture.completedFuture(VpnCheckResult.pass());
        }

        String txId = UUID.randomUUID().toString();
        CompletableFuture<VpnCheckResult> future = new CompletableFuture<>();
        pendingChecks.put(txId, future);

        JsonObject payload = new JsonObject();
        payload.addProperty("type",            "VERIFY");
        payload.addProperty("transactionalId", txId);
        payload.addProperty("username",        playerName);
        payload.addProperty("userId",          playerUuid.replace("-", ""));
        payload.addProperty("address",         ipAddress);

        webSocket.sendText(payload.toString(), true)
                .whenComplete((ws, ex) -> {
                    if (ex != null) {
                        logger.warning("[MCBans-VPN] sendText failed for " + playerName + ": " + ex.getMessage());
                        pendingChecks.remove(txId);
                        future.complete(VpnCheckResult.pass());
                    }
                });

        return future;
    }

    // -------------------------------------------------------------------------
    // WebSocket.Listener callbacks
    // -------------------------------------------------------------------------

    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket   = webSocket;
        this.connected   = true;
        this.reconnectAttempt = 0;
        logger.info("[MCBans-VPN] AntVPN WebSocket connected.");
        webSocket.request(1);
    }

    @Override
    public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        frameBuffer.append(data);
        if (last) {
            String message = frameBuffer.toString();
            frameBuffer.setLength(0);
            handleMessage(message);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletableFuture<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        connected = false;
        logger.info("[MCBans-VPN] AntVPN WebSocket closed (code=" + statusCode
                + ", reason=" + reason + ").");
        scheduleReconnect();
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        connected = false;
        logger.warning("[MCBans-VPN] AntVPN WebSocket error: " + error.getMessage());
        scheduleReconnect();
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    private void handleMessage(String raw) {
        try {
            JsonObject msg = JsonParser.parseString(raw).getAsJsonObject();
            String type = msg.has("type") ? msg.get("type").getAsString() : "";

            switch (type) {
                case "SETTINGS" -> handleSettings(msg);
                case "VERIFY"   -> handleVerify(msg);
                case "PONG"     -> { /* keep-alive acknowledged */ }
                default         -> logger.fine("[MCBans-VPN] Unknown message type: " + type);
            }
        } catch (Exception e) {
            logger.warning("[MCBans-VPN] Failed to parse AntVPN message: " + e.getMessage());
        }
    }

    private void handleSettings(JsonObject msg) {
        JsonElement enabledEl = msg.get("enabled");
        if (enabledEl != null) {
            protectionEnabled = enabledEl.getAsInt() == 1;
        }
        String shield = msg.has("shieldMode") ? msg.get("shieldMode").getAsString() : "";
        isAttackMode = "true".equalsIgnoreCase(shield) || "1".equals(shield);
        logger.info("[MCBans-VPN] AntVPN settings received: enabled=" + protectionEnabled
                + ", attackMode=" + isAttackMode);
    }

    private void handleVerify(JsonObject msg) {
        if (!msg.has("transactionalId")) {
            return;
        }
        String txId = msg.get("transactionalId").getAsString();
        CompletableFuture<VpnCheckResult> future = pendingChecks.remove(txId);
        if (future == null) {
            return; // Response for an unknown or already-timed-out request
        }

        boolean valid     = msg.has("valid")     && msg.get("valid").getAsBoolean();
        boolean isAttack  = msg.has("is_attack") && msg.get("is_attack").getAsBoolean();
        String  sessionId = msg.has("sessionId") ? msg.get("sessionId").getAsString() : null;

        if (valid) {
            // AntVPN says the IP is clean
            future.complete(VpnCheckResult.pass());
        } else {
            future.complete(VpnCheckResult.fail(sessionId, isAttack));
        }
    }

    // -------------------------------------------------------------------------
    // Reconnection
    // -------------------------------------------------------------------------

    private void scheduleReconnect() {
        if (shutdown.get()) {
            return;
        }
        // Fail pending futures before reconnecting
        pendingChecks.values().forEach(f -> f.complete(VpnCheckResult.pass()));
        pendingChecks.clear();

        long delayMs = Math.min((long) BASE_DELAY_MS * (1L << reconnectAttempt), MAX_DELAY_MS);
        reconnectAttempt++;
        logger.info("[MCBans-VPN] Reconnecting to AntVPN in " + delayMs + " ms "
                + "(attempt " + reconnectAttempt + ").");
        scheduler.schedule(this::connect, delayMs, TimeUnit.MILLISECONDS);
    }
}
