/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.placeholder;

import com.pvpindex.bans.storage.LocalBan;
import com.pvpindex.bans.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link McBansPlaceholderExpansion}.
 *
 * <p>The expansion is exercised by calling
 * {@link McBansPlaceholderExpansion#onRequest(org.bukkit.OfflinePlayer, String)}
 * directly with a stub {@link StorageBackend}, avoiding any dependency on a
 * live PlaceholderAPI instance.</p>
 */
class McBansPlaceholderExpansionTest {

    private static final long NOW = System.currentTimeMillis() / 1000L;

    // -------------------------------------------------------------------------
    // Stub storage backend - returns a fixed ban or empty
    // -------------------------------------------------------------------------

    /**
     * Minimal StorageBackend stub.  All mutation methods are no-ops;
     * {@code findActiveBan} returns the pre-configured ban (or empty).
     */
    private static final class StubStorage implements StorageBackend {

        private final LocalBan ban;

        StubStorage(final LocalBan ban) {
            this.ban = ban;
        }

        @Override
        public Optional<LocalBan> findActiveBan(final String uuid) {
            return Optional.ofNullable(ban);
        }

        @Override
        public List<LocalBan> findUnsynced() {
            return List.of();
        }

        @Override
        public void upsertBan(final LocalBan b) {}

        @Override
        public void insertOfflineBan(
                final String u, final String p, final String t,
                final String r, final String au, final String an,
                final Long e) {}

        @Override
        public void markSynced(final String uuid) {}

        @Override
        public void deactivateBan(final String uuid) {}

        @Override
        public Optional<String> getMeta(final String key) {
            return Optional.empty();
        }

        @Override
        public void setMeta(final String key, final String value) {}

        @Override
        public void close() {}
    }

    private static StorageBackend storageWith(final LocalBan ban) {
        return new StubStorage(ban);
    }

    private static LocalBan activeBan(final String type, final String reason, final String adminName) {
        return new LocalBan(
                "abc123", "TestPlayer", type, reason,
                "adminuuid", adminName,
                null, true, true, false, NOW - 100, NOW - 100
        );
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void getIdentifier_returns_mcbans() {
        McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(null));
        assertEquals("mcbans", exp.getIdentifier());
    }

    @Test
    void persist_returns_true() {
        McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(null));
        assertTrue(exp.persist());
    }

    // -------------------------------------------------------------------------
    // %mcbans_banned%
    // -------------------------------------------------------------------------

    @Nested
    class Banned {

        @BeforeEach
        void setUp() {
            MockBukkit.mock();
        }

        @AfterEach
        void tearDown() {
            MockBukkit.unmock();
        }

        @Test
        void banned_is_false_when_no_active_ban() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(null));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Alice", UUID.randomUUID());
            assertEquals("false", exp.onRequest(player, "banned"));
        }

        @Test
        void banned_is_true_when_active_ban_exists() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(
                    storageWith(activeBan("local", "griefing", "Admin")));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Bob", UUID.randomUUID());
            assertEquals("true", exp.onRequest(player, "banned"));
        }
    }

    // -------------------------------------------------------------------------
    // %mcbans_ban_type%
    // -------------------------------------------------------------------------

    @Nested
    class BanType {

        @BeforeEach
        void setUp() {
            MockBukkit.mock();
        }

        @AfterEach
        void tearDown() {
            MockBukkit.unmock();
        }

        @Test
        void ban_type_is_none_when_not_banned() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(null));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Alice", UUID.randomUUID());
            assertEquals("none", exp.onRequest(player, "ban_type"));
        }

        @Test
        void ban_type_reflects_local_ban() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(
                    storageWith(activeBan("local", "hacking", "Staff")));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Bob", UUID.randomUUID());
            assertEquals("local", exp.onRequest(player, "ban_type"));
        }

        @Test
        void ban_type_reflects_global_ban() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(
                    storageWith(activeBan("global", "cheating", "Mod")));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Charlie", UUID.randomUUID());
            assertEquals("global", exp.onRequest(player, "ban_type"));
        }
    }

    // -------------------------------------------------------------------------
    // %mcbans_ban_reason%
    // -------------------------------------------------------------------------

    @Nested
    class BanReason {

        @BeforeEach
        void setUp() {
            MockBukkit.mock();
        }

        @AfterEach
        void tearDown() {
            MockBukkit.unmock();
        }

        @Test
        void ban_reason_is_empty_when_not_banned() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(null));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Alice", UUID.randomUUID());
            assertEquals("", exp.onRequest(player, "ban_reason"));
        }

        @Test
        void ban_reason_returns_reason_text() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(
                    storageWith(activeBan("local", "spam in chat", "Admin")));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Bob", UUID.randomUUID());
            assertEquals("spam in chat", exp.onRequest(player, "ban_reason"));
        }
    }

    // -------------------------------------------------------------------------
    // %mcbans_ban_admin%
    // -------------------------------------------------------------------------

    @Nested
    class BanAdmin {

        @BeforeEach
        void setUp() {
            MockBukkit.mock();
        }

        @AfterEach
        void tearDown() {
            MockBukkit.unmock();
        }

        @Test
        void ban_admin_is_empty_when_not_banned() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(null));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Alice", UUID.randomUUID());
            assertEquals("", exp.onRequest(player, "ban_admin"));
        }

        @Test
        void ban_admin_returns_admin_name() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(
                    storageWith(activeBan("global", "reason", "SeniorMod")));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Bob", UUID.randomUUID());
            assertEquals("SeniorMod", exp.onRequest(player, "ban_admin"));
        }

        @Test
        void ban_admin_handles_null_admin_name() {
            LocalBan banWithNullAdmin = new LocalBan(
                    "abc123", "TestPlayer", "local", "reason",
                    null, null, null, true, true, false, NOW, NOW);
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(banWithNullAdmin));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Alice", UUID.randomUUID());
            assertEquals("", exp.onRequest(player, "ban_admin"));
        }
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Nested
    class EdgeCases {

        @BeforeEach
        void setUp() {
            MockBukkit.mock();
        }

        @AfterEach
        void tearDown() {
            MockBukkit.unmock();
        }

        @Test
        void unknown_placeholder_returns_null() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(null));
            PlayerMock player = new PlayerMock(MockBukkit.getMock(), "Alice", UUID.randomUUID());
            assertNull(exp.onRequest(player, "unknown_identifier_xyz"));
        }

        @Test
        void null_player_returns_empty_string() {
            McBansPlaceholderExpansion exp = new McBansPlaceholderExpansion(storageWith(null));
            assertEquals("", exp.onRequest(null, "banned"));
        }
    }
}
