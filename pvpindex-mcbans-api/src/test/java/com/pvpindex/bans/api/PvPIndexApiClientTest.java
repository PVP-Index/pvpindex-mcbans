/*
 * This file is part of PvPIndex MCBans.
 *
 * Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PvPIndexApiClient}.
 *
 * <p>Uses an embedded {@link HttpServer} so the client makes real HTTP calls against
 * a local loopback server — no mocking framework, no real network traffic.</p>
 *
 * <p>Covers the following original MCBans issues:</p>
 * <ul>
 *   <li><b>#120</b> — {@code ConnectException} / connection refused must not crash the plugin;
 *       the method must return {@code Optional.empty()} gracefully.</li>
 *   <li><b>#120</b> — HTTP 5xx from the API must also return {@code Optional.empty()}.</li>
 *   <li>HTTP 404 (player unknown) must return {@code BanStatusResponse(false, null)}.</li>
 *   <li>HTTP 200 with {@code banned=false} must return a clean {@code BanStatusResponse}.</li>
 *   <li>HTTP 200 with {@code banned=true} must parse the ban record correctly.</li>
 * </ul>
 */
class PvPIndexApiClientTest {

    private static final Logger LOG = Logger.getLogger("api-client-test");
    private static final String TEST_UUID = "aaaabbbbccccdddd1111222233334444";

    private HttpServer httpServer;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();
        int port = httpServer.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;
    }

    @AfterEach
    void stopServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a client pointing at the embedded server, with a 2 s connect timeout. */
    private PvPIndexApiClient client() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        return new PvPIndexApiClient(baseUrl, "test-key", LOG, http);
    }

    /** Registers an HTTP handler that always returns the given status + body. */
    private void respond(int status, String body) {
        httpServer.createContext("/", (HttpExchange ex) -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    // =========================================================================
    // Issue #120 — graceful error handling (connection refused / network error)
    // =========================================================================

    @Nested
    class ConnectionErrors {

        /**
         * Regression for issue #120: when {@code api.mcbans.com} is unreachable
         * (connection refused), the plugin must not throw and must return empty.
         */
        @Test
        void connection_refused_returns_empty() {
            // Point at a port where nothing is listening
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(500))
                    .build();
            PvPIndexApiClient offlineClient =
                    new PvPIndexApiClient("http://127.0.0.1:1", "key", LOG, http);

            Optional<BanStatusResponse> result = offlineClient.getBanStatus(TEST_UUID);

            assertFalse(result.isPresent(),
                    "Issue #120: ConnectException must be caught and return Optional.empty()");
        }

        @Test
        void http_500_internal_server_error_returns_empty() {
            respond(500, "{\"error\":\"Internal Server Error\"}");
            Optional<BanStatusResponse> result = client().getBanStatus(TEST_UUID);
            assertFalse(result.isPresent(),
                    "HTTP 500 must return Optional.empty() so fallback logic is triggered");
        }

        @Test
        void http_503_service_unavailable_returns_empty() {
            respond(503, "{\"error\":\"Service Unavailable\"}");
            Optional<BanStatusResponse> result = client().getBanStatus(TEST_UUID);
            assertFalse(result.isPresent(),
                    "HTTP 503 must return Optional.empty()");
        }
    }

    // =========================================================================
    // Normal API responses
    // =========================================================================

    @Nested
    class BanStatusResponses {

        @Test
        void http_404_returns_not_banned() {
            respond(404, "");
            Optional<BanStatusResponse> result = client().getBanStatus(TEST_UUID);

            assertTrue(result.isPresent(), "HTTP 404 should return a present Optional");
            assertFalse(result.get().banned(),
                    "HTTP 404 means player is unknown — not banned");
        }

        @Test
        void http_200_not_banned_returns_correct_response() {
            respond(200, "{\"banned\":false}");
            Optional<BanStatusResponse> result = client().getBanStatus(TEST_UUID);

            assertTrue(result.isPresent());
            assertFalse(result.get().banned());
        }

        @Test
        void http_200_banned_returns_ban_record() {
            String json = """
                    {
                      "banned": true,
                      "ban": {
                        "id": "ban-1",
                        "player_uuid": "%s",
                        "player_username": "TestPlayer",
                        "type": "global",
                        "reason": "X-raying",
                        "admin_uuid": null,
                        "admin_name": "AdminUser",
                        "expires_at": null,
                        "is_active": true,
                        "updated_at": "2025-01-01T00:00:00Z"
                      }
                    }
                    """.formatted(TEST_UUID);
            respond(200, json);

            Optional<BanStatusResponse> result = client().getBanStatus(TEST_UUID);

            assertTrue(result.isPresent(), "Response should be present for banned player");
            assertTrue(result.get().banned(), "banned flag should be true");
            BanRecord ban = result.get().ban();
            assertNotNull(ban, "ban record must not be null when banned=true");
            assertEquals("global", ban.type());
            assertEquals("X-raying", ban.reason());
            assertEquals("AdminUser", ban.adminName());
        }

        @Test
        void http_200_temp_ban_parses_expires_at() {
            String expiry = "2099-12-31T23:59:59Z";
            String json = """
                    {
                      "banned": true,
                      "ban": {
                        "id": "ban-temp",
                        "player_uuid": "%s",
                        "player_username": "TestPlayer",
                        "type": "temp",
                        "reason": "Ban evasion",
                        "admin_uuid": null,
                        "admin_name": "Staff",
                        "expires_at": "%s",
                        "is_active": true,
                        "updated_at": "2025-06-01T00:00:00Z"
                      }
                    }
                    """.formatted(TEST_UUID, expiry);
            respond(200, json);

            Optional<BanStatusResponse> result = client().getBanStatus(TEST_UUID);

            assertTrue(result.isPresent());
            BanRecord ban = result.get().ban();
            assertEquals("temp", ban.type());
            assertEquals(expiry, ban.expiresAt(),
                    "expiresAt must be parsed from the JSON response");
        }

        @Test
        void uuid_with_dashes_is_stripped_for_request() throws Exception {
            // Track which path was requested
            String[] requestedPath = {null};
            httpServer.createContext("/plugin/players/", (HttpExchange ex) -> {
                requestedPath[0] = ex.getRequestURI().getPath();
                byte[] body = "{\"banned\":false}".getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200, body.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(body);
                }
            });

            String uuidWithDashes = "aaaabbbb-cccc-dddd-1111-222233334444";
            client().getBanStatus(uuidWithDashes);

            assertNotNull(requestedPath[0], "Server should have received a request");
            // Extract the UUID segment: path is /plugin/players/{uuid}/ban-status
            String[] parts = requestedPath[0].split("/");
            String uuidSegment = null;
            for (int i = 0; i < parts.length - 1; i++) {
                if ("players".equals(parts[i])) {
                    uuidSegment = parts[i + 1];
                    break;
                }
            }
            assertNotNull(uuidSegment, "Could not find UUID segment in path: " + requestedPath[0]);
            assertFalse(uuidSegment.contains("-"),
                    "UUID in the API path must have dashes stripped; got UUID segment: " + uuidSegment);
        }
    }

    // =========================================================================
    // Authorization header
    // =========================================================================

    @Test
    void request_includes_api_key_in_authorization_header() throws Exception {
        String[] authHeader = {null};
        httpServer.createContext("/", (HttpExchange ex) -> {
            authHeader[0] = ex.getRequestHeaders().getFirst("Authorization");
            byte[] body = "{\"banned\":false}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        PvPIndexApiClient c = new PvPIndexApiClient(baseUrl, "my-secret-key", LOG, http);
        c.getBanStatus(TEST_UUID);

        assertNotNull(authHeader[0], "Authorization header must be present");
        assertTrue(authHeader[0].contains("my-secret-key"),
                "Authorization header must include the API key; got: " + authHeader[0]);
    }
}
