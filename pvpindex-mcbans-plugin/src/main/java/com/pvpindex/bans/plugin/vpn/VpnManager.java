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

import com.pvpindex.bans.plugin.util.Util;
import com.pvpindex.bans.storage.StorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Manages VPN detection using the AntVPN realtime WebSocket API.
 *
 * <p>Call {@link #start()} on plugin enable and {@link #shutdown()} on
 * plugin disable.  During player login, call
 * {@link #checkPlayer(AsyncPlayerPreLoginEvent)} to obtain a
 * {@link CompletableFuture} that resolves to the action to take.</p>
 */
public class VpnManager {

    private static final String BYPASS_PERMISSION = "mcbans.vpn.bypass";

    private final VpnConfiguration config;
    private final StorageBackend   storage;
    private final Logger           logger;
    private AntVpnClient           client;

    public VpnManager(VpnConfiguration config, StorageBackend storage, Logger logger) {
        this.config  = config;
        this.storage = storage;
        this.logger  = logger;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Start the AntVPN WebSocket client (if enabled and an API token is configured). */
    public void start() {
        if (!config.isEnabled()) {
            return;
        }
        String token = config.getApiToken();
        if (token.isEmpty()) {
            logger.warning("[MCBans-VPN] AntVPN is enabled but no api-token is set in antivpn.yml.");
            return;
        }
        client = new AntVpnClient(token, logger);
        client.connect();
        logger.info("[MCBans-VPN] AntVPN integration started (action=" + config.getAction() + ").");
    }

    /** Stop the AntVPN WebSocket client and release resources. */
    public void shutdown() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    /** Whether the VPN check is active. */
    public boolean isEnabled() {
        return config.isEnabled() && client != null && client.isConnected();
    }

    // -------------------------------------------------------------------------
    // Check
    // -------------------------------------------------------------------------

    /**
     * Asynchronously checks whether the joining player is using a VPN.
     *
     * <p>Returns immediately with {@link VpnCheckResult#pass()} when VPN
     * protection is inactive (disabled, no token, or client not yet connected).
     * Callers should apply a timeout using
     * {@link CompletableFuture#orTimeout(long, java.util.concurrent.TimeUnit)}.</p>
     *
     * @param event the pre-login event for the joining player
     * @return future resolving to the VPN check result
     */
    public CompletableFuture<VpnCheckResult> checkPlayer(AsyncPlayerPreLoginEvent event) {
        if (!config.isEnabled() || client == null) {
            return CompletableFuture.completedFuture(VpnCheckResult.pass());
        }

        // Check bypass permission (online players only - pre-login players are not yet on the server)
        if (config.isBypassPermissionEnabled()) {
            Player online = Bukkit.getPlayer(event.getUniqueId());
            if (online != null && online.hasPermission(BYPASS_PERMISSION)) {
                return CompletableFuture.completedFuture(VpnCheckResult.pass());
            }
        }

        String uuid = event.getUniqueId().toString().replace("-", "").toLowerCase();
        String ip   = event.getAddress().getHostAddress();

        return client.checkPlayer(event.getName(), uuid, ip);
    }

    // -------------------------------------------------------------------------
    // Action handling
    // -------------------------------------------------------------------------

    /**
     * Applies the configured VPN action to a player that failed the VPN check.
     *
     * @param event  the pre-login event
     * @param result the failed VPN check result
     */
    public void handleDetection(AsyncPlayerPreLoginEvent event, VpnCheckResult result) {
        String name = event.getName();
        String uuid = event.getUniqueId().toString().replace("-", "").toLowerCase();
        String ip   = event.getAddress().getHostAddress();
        VpnAction action = config.getAction();

        logger.info("[MCBans-VPN] VPN detected for " + name + " (" + ip
                + ") sessionId=" + result.sessionId()
                + " attack=" + result.isAttack()
                + " -> action=" + action);

        switch (action) {
            case WARN -> {
                // Notify admins in-game; player is still allowed to join
                String notice = Util.color("&c[PvPIndex MCBans] &rVPN detected for &e" + name
                        + " &r(" + ip + "). Player was allowed in (action=WARN).");
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("mcbans.admin"))
                        .forEach(p -> p.sendMessage(notice));
            }
            case KICK -> {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        Util.color(config.getKickMessage()));
            }
            case BAN -> {
                // Store a local VPN ban (synced via BanSync if sync-bans=true)
                long expiresAt = config.getBanDurationSeconds() > 0
                        ? Instant.now().getEpochSecond() + config.getBanDurationSeconds()
                        : 0L;
                storage.insertOfflineBan(
                        uuid,
                        name,
                        "local",
                        config.getBanReason(),
                        null,
                        "AntVPN",
                        expiresAt > 0 ? expiresAt : null);

                if (!config.isSyncBans()) {
                    // Mark already synced to skip the API push
                    storage.markSynced(uuid);
                }

                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        Util.color(config.getKickMessage()));
            }
            default -> {
                // Should not occur, but fail-safe: allow the player in
                logger.warning("[MCBans-VPN] Unhandled VPN action: " + action);
            }
        }
    }

    /** Exposes configuration for external readers. */
    public VpnConfiguration getConfiguration() {
        return config;
    }
}
