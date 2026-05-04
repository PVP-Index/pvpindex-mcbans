/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BanDao} using an in-memory SQLite database.
 *
 * <p>No Bukkit or MockBukkit required — pure Java 21 + sqlite-jdbc.</p>
 */
class BanDaoTest {

    private Connection connection;
    private BanDao dao;

    private static final String UUID_ALICE = "aaaabbbbccccdddd1111222233334444";
    private static final String UUID_BOB   = "bbbbaaaaccccdddd5555666677778888";

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE player_bans (
                  uuid        TEXT NOT NULL PRIMARY KEY,
                  player_name TEXT,
                  type        TEXT NOT NULL,
                  reason      TEXT NOT NULL,
                  admin_uuid  TEXT,
                  admin_name  TEXT,
                  expires_at  INTEGER,
                  is_active   INTEGER NOT NULL DEFAULT 1,
                  is_synced   INTEGER NOT NULL DEFAULT 0,
                  created_at  INTEGER NOT NULL,
                  updated_at  INTEGER NOT NULL
                )
                """);
            st.execute("""
                CREATE TABLE meta (
                  key   TEXT NOT NULL PRIMARY KEY,
                  value TEXT
                )
                """);
        }
        dao = new BanDao(connection, Logger.getLogger("test"));
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    // -------------------------------------------------------------------------
    // findActiveBan
    // -------------------------------------------------------------------------

    @Test
    void findActiveBan_returnsEmpty_whenNoBanExists() {
        Optional<LocalBan> result = dao.findActiveBan(UUID_ALICE);
        assertFalse(result.isPresent());
    }

    @Test
    void findActiveBan_returnsBan_whenActiveBanPresent() {
        insertBan(UUID_ALICE, "local", true, null);
        Optional<LocalBan> result = dao.findActiveBan(UUID_ALICE);
        assertTrue(result.isPresent());
        assertEquals(UUID_ALICE, result.get().uuid());
        assertEquals("local", result.get().type());
    }

    @Test
    void findActiveBan_returnsEmpty_whenBanIsInactive() {
        insertBan(UUID_ALICE, "global", false, null);
        Optional<LocalBan> result = dao.findActiveBan(UUID_ALICE);
        assertFalse(result.isPresent());
    }

    @Test
    void findActiveBan_autoExpires_tempBanPastExpiry() {
        long pastExpiry = Instant.now().getEpochSecond() - 3600; // 1 hour ago
        insertBan(UUID_ALICE, "temp", true, pastExpiry);

        Optional<LocalBan> result = dao.findActiveBan(UUID_ALICE);
        assertFalse(result.isPresent(), "Expired temp ban should be auto-deactivated");

        // Verify the row was actually deactivated in DB
        Optional<LocalBan> afterExpiry = dao.findActiveBan(UUID_ALICE);
        assertFalse(afterExpiry.isPresent());
    }

    @Test
    void findActiveBan_keepsTempBan_whenNotYetExpired() {
        long futureExpiry = Instant.now().getEpochSecond() + 3600; // 1 hour from now
        insertBan(UUID_ALICE, "temp", true, futureExpiry);

        Optional<LocalBan> result = dao.findActiveBan(UUID_ALICE);
        assertTrue(result.isPresent());
        assertEquals("temp", result.get().type());
    }

    // -------------------------------------------------------------------------
    // insertOfflineBan / deactivateBan
    // -------------------------------------------------------------------------

    @Test
    void insertOfflineBan_createsUnsyncedRow() {
        dao.insertOfflineBan(UUID_ALICE, "Alice", "global", "Griefing", null, "Admin", null);

        Optional<LocalBan> ban = dao.findActiveBan(UUID_ALICE);
        assertTrue(ban.isPresent());
        assertFalse(ban.get().isSynced());
        assertEquals("Alice", ban.get().playerName());
        assertEquals("Griefing", ban.get().reason());
    }

    @Test
    void deactivateBan_flipsIsActiveToFalse() {
        insertBan(UUID_ALICE, "local", true, null);
        dao.deactivateBan(UUID_ALICE);

        Optional<LocalBan> ban = dao.findActiveBan(UUID_ALICE);
        assertFalse(ban.isPresent(), "Deactivated ban should not appear in findActiveBan");
    }

    // -------------------------------------------------------------------------
    // markSynced / findUnsynced
    // -------------------------------------------------------------------------

    @Test
    void findUnsynced_returnsUnsyncedBans() {
        dao.insertOfflineBan(UUID_ALICE, "Alice", "local", "Test", null, null, null);
        dao.insertOfflineBan(UUID_BOB,   "Bob",   "global", "Grief", null, "Admin", null);

        List<LocalBan> unsynced = dao.findUnsynced();
        assertEquals(2, unsynced.size());
    }

    @Test
    void markSynced_removesFromUnsyncedList() {
        dao.insertOfflineBan(UUID_ALICE, "Alice", "local", "Test", null, null, null);
        dao.markSynced(UUID_ALICE);

        List<LocalBan> unsynced = dao.findUnsynced();
        assertTrue(unsynced.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getMeta / setMeta
    // -------------------------------------------------------------------------

    @Test
    void getMeta_returnsEmpty_whenKeyAbsent() {
        assertFalse(dao.getMeta("missing").isPresent());
    }

    @Test
    void setAndGetMeta_roundTrips() {
        dao.setMeta("lastSyncAt", "2026-01-01T00:00:00Z");
        Optional<String> result = dao.getMeta("lastSyncAt");
        assertTrue(result.isPresent());
        assertEquals("2026-01-01T00:00:00Z", result.get());
    }

    // -------------------------------------------------------------------------
    // upsertBan
    // -------------------------------------------------------------------------

    @Test
    void upsertBan_insertsNewRow() {
        long now = Instant.now().getEpochSecond();
        LocalBan ban = new LocalBan(UUID_ALICE, "Alice", "local", "Grief",
                null, null, null, true, true, now, now);
        dao.upsertBan(ban);

        Optional<LocalBan> found = dao.findActiveBan(UUID_ALICE);
        assertTrue(found.isPresent());
        assertNotNull(found.get());
    }

    @Test
    void upsertBan_updatesExistingRowWhenNewer() {
        long earlier = Instant.now().getEpochSecond() - 60;
        LocalBan old = new LocalBan(UUID_ALICE, "Alice", "local", "Old reason",
                null, null, null, true, true, earlier, earlier);
        dao.upsertBan(old);

        long later = Instant.now().getEpochSecond();
        LocalBan updated = new LocalBan(UUID_ALICE, "Alice", "global", "New reason",
                null, null, null, true, true, earlier, later);
        dao.upsertBan(updated);

        Optional<LocalBan> found = dao.findActiveBan(UUID_ALICE);
        assertTrue(found.isPresent());
        assertEquals("New reason", found.get().reason());
        assertEquals("global", found.get().type());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void insertBan(String uuid, String type, boolean isActive, Long expiresAt) {
        long now = Instant.now().getEpochSecond();
        dao.upsertBan(new LocalBan(uuid, "Player", type, "Test reason",
                null, null, expiresAt, isActive, true, now, now));
    }
}
