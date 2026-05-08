/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin;

import com.pvpindex.bans.api.BanRecord;
import com.pvpindex.bans.api.BanStatusResponse;
import com.pvpindex.bans.api.PvPIndexApiClient;
import com.pvpindex.bans.storage.LocalBan;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for {@link com.pvpindex.bans.plugin.bukkitListeners.PlayerListener}.
 *
 * <p>These tests cover the login-check flow, particularly:</p>
 * <ul>
 *   <li><b>#118</b> - {@code failsafe=true} must deny login when the API is unreachable
 *       and no local ban cache entry exists for the player.</li>
 *   <li><b>#120</b> - when the API returns an error / IOException, the plugin gracefully
 *       falls back to the local SQLite cache (no crash, login allowed if not cached).</li>
 *   <li>Normal ban/unban flow: API says banned → login denied with reason in message.</li>
 *   <li>SQLite fallback: API down, local ban present → login still denied.</li>
 * </ul>
 *
 * <p>The real PvPIndex API client is replaced by a {@link StubApiClient} for each test,
 * injected via {@link MCBans#setApiClientForTesting(PvPIndexApiClient)}.</p>
 */
class PlayerListenerTest {

    private static final UUID PLAYER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String PLAYER_NAME = "TestPlayer";

    private ServerMock server;
    private MCBans plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MCBans.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Fires AsyncPlayerPreLoginEvent from a background thread (required for async events in MockBukkit). */
    private AsyncPlayerPreLoginEvent fireLoginEvent(UUID uuid, String name) throws Exception {
        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent(
                name, InetAddress.getByName("127.0.0.1"), uuid);
        CompletableFuture.runAsync(() -> server.getPluginManager().callEvent(event))
                .get(10, TimeUnit.SECONDS);
        return event;
    }

    /** Builds a minimal BanRecord for use in stubs. */
    private BanRecord globalBan(String reason) {
        return new BanRecord(
                "ban-1", uuidStr(PLAYER_UUID), PLAYER_NAME,
                "global", reason,
                null, "AdminPlayer",
                null, true,
                "2025-01-01T00:00:00Z");
    }

    private BanRecord tempBan(String reason, String expiresAt) {
        return new BanRecord(
                "ban-2", uuidStr(PLAYER_UUID), PLAYER_NAME,
                "temp", reason,
                null, "AdminPlayer",
                expiresAt, true,
                "2025-01-01T00:00:00Z");
    }

    private static String uuidStr(UUID uuid) {
        return uuid.toString().replace("-", "").toLowerCase();
    }

    // =========================================================================
    // Issue #120 - API down: graceful fallback, no crash
    // =========================================================================

    @Nested
    class ApiUnavailable {

        @Test
        void api_down_no_local_ban_allows_login() throws Exception {
            // failsafe=false (default): when API is down and no local ban → allow
            plugin.setApiClientForTesting(new StubApiClient(Optional.empty()));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult(),
                    "Player should be allowed to join when API is down and no local ban (failsafe=false)");
        }

        @Test
        void api_down_local_ban_present_denies_login() throws Exception {
            // Store a local ban in SQLite first
            plugin.getBanDao().insertOfflineBan(
                    uuidStr(PLAYER_UUID), PLAYER_NAME,
                    "local", "Griefing",
                    null, "Admin", null);

            // API is down → falls back to SQLite
            plugin.setApiClientForTesting(new StubApiClient(Optional.empty()));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            assertNotEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult(),
                    "Player with local ban should be denied even when API is down");
        }
    }

    // =========================================================================
    // Issue #118 - failsafe config does nothing (now fixed)
    // =========================================================================

    @Nested
    class FailsafeConfig {

        /**
         * Reproduces issue #118: when failsafe=true and the API is down,
         * a player with no cached ban should be DENIED (not allowed in).
         *
         * <p>Before the fix, {@code PlayerListener} never read {@code config.isFailsafe()},
         * so this test would have failed (login would have been ALLOWED).</p>
         */
        @Test
        void failsafe_true_denies_login_when_api_down() throws Exception {
            // Enable failsafe via config override
            plugin.getConfig().set("failsafe", true);

            // API is unreachable, no local ban
            plugin.setApiClientForTesting(new StubApiClient(Optional.empty()));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            assertNotEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult(),
                    "Issue #118: failsafe=true must deny login when API is down, even without a local ban");
        }

        @Test
        void failsafe_false_allows_login_when_api_down() throws Exception {
            // Default: failsafe=false - allow players when API is unreachable
            plugin.getConfig().set("failsafe", false);
            plugin.setApiClientForTesting(new StubApiClient(Optional.empty()));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult(),
                    "failsafe=false should allow login when API is down and no local ban");
        }

        @Test
        void failsafe_true_still_denies_known_banned_player() throws Exception {
            // Even without failsafe, a locally-cached ban should block
            plugin.getConfig().set("failsafe", true);
            plugin.getBanDao().insertOfflineBan(
                    uuidStr(PLAYER_UUID), PLAYER_NAME,
                    "global", "Cheating", null, "Admin", null);
            plugin.setApiClientForTesting(new StubApiClient(Optional.empty()));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            assertNotEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult(),
                    "Locally-banned player must be denied regardless of failsafe setting");
        }
    }

    // =========================================================================
    // Normal API-present flows
    // =========================================================================

    @Nested
    class ApiPresent {

        @Test
        void api_says_not_banned_allows_login() throws Exception {
            plugin.setApiClientForTesting(
                    new StubApiClient(Optional.of(new BanStatusResponse(false, null))));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult(),
                    "Player should be allowed when API reports no ban");
        }

        @Test
        void api_says_banned_denies_login() throws Exception {
            plugin.setApiClientForTesting(
                    new StubApiClient(Optional.of(
                            new BanStatusResponse(true, globalBan("X-raying")))));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            assertNotEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult(),
                    "Globally banned player must be denied login");
        }

        @Test
        void api_says_banned_kick_message_contains_reason() throws Exception {
            plugin.setApiClientForTesting(
                    new StubApiClient(Optional.of(
                            new BanStatusResponse(true, globalBan("Fly hacking")))));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            String msg = event.getKickMessage();
            assertNotEquals("", msg, "Kick message should not be empty");
            // The reason should appear somewhere in the kick message
            assertEquals(true, msg.contains("Fly hacking"),
                    "Kick message should contain the ban reason; got: " + msg);
        }

        @Test
        void api_says_temp_banned_kick_message_says_temporarily() throws Exception {
            plugin.setApiClientForTesting(
                    new StubApiClient(Optional.of(
                            new BanStatusResponse(true,
                                    tempBan("Ban evasion", "2099-12-31T23:59:59Z")))));

            AsyncPlayerPreLoginEvent event = fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            String msg = event.getKickMessage();
            assertEquals(true, msg.contains("temporarily"),
                    "Kick message for temp ban should say 'temporarily'; got: " + msg);
        }

        @Test
        void api_says_banned_caches_ban_in_sqlite() throws Exception {
            plugin.setApiClientForTesting(
                    new StubApiClient(Optional.of(
                            new BanStatusResponse(true, globalBan("Griefing")))));

            fireLoginEvent(PLAYER_UUID, PLAYER_NAME);

            // The ban should now be in the local SQLite cache
            Optional<LocalBan> cached = plugin.getBanDao().findActiveBan(uuidStr(PLAYER_UUID));
            assertEquals(true, cached.isPresent(),
                    "Ban returned by API should be upserted into local SQLite cache");
            assertEquals("Griefing", cached.get().reason());
        }
    }

    // =========================================================================
    // Stub API client
    // =========================================================================

    /**
     * Stub implementation of {@link PvPIndexApiClient} for tests.
     *
     * <p>Overrides {@code getBanStatus()} to return a configurable value without
     * making any real HTTP calls.</p>
     */
    static final class StubApiClient extends PvPIndexApiClient {

        private final Optional<BanStatusResponse> response;

        StubApiClient(Optional<BanStatusResponse> response) {
            super("http://stub.invalid", "stub-key", Logger.getLogger("stub"));
            this.response = response;
        }

        @Override
        public Optional<BanStatusResponse> getBanStatus(String uuid) {
            return response;
        }
    }
}
