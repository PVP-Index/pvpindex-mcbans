/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.storage.backend;

import com.pvpindex.bans.storage.BanDao;
import com.pvpindex.bans.storage.StorageBackend;
import com.pvpindex.bans.storage.LocalBan;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SQLite-backed {@link StorageBackend}.
 *
 * <p>Stores bans in {@code plugins/MCBans/bans.db} using the embedded
 * SQLite JDBC driver that is already shaded into the plugin JAR.</p>
 */
public class SqliteBackend implements StorageBackend {

    private static final String DB_FILE = "bans.db";

    private final File dataFolder;
    private final Logger logger;
    private BanDao dao;

    public SqliteBackend(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger     = logger;
    }

    /**
     * Open the SQLite connection and create/migrate the schema.
     *
     * @throws SQLException           on JDBC errors
     * @throws ClassNotFoundException if the SQLite driver is missing
     */
    public void initialise() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        dataFolder.mkdirs();
        File dbFile = new File(dataFolder, DB_FILE);
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        createSchema(connection);
        dao = new BanDao(connection, logger, BanDao.Dialect.SQLITE);
        logger.info("SQLite storage initialised at " + dbFile.getPath());
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private void createSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_bans (
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
                CREATE INDEX IF NOT EXISTS idx_bans_active ON player_bans (uuid, is_active)
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS meta (
                  key   TEXT NOT NULL PRIMARY KEY,
                  value TEXT
                )
                """);
        }
    }

    // -------------------------------------------------------------------------
    // StorageBackend delegation
    // -------------------------------------------------------------------------

    @Override
    public Optional<LocalBan> findActiveBan(String uuid) {
        return dao.findActiveBan(uuid);
    }

    @Override
    public List<LocalBan> findUnsynced() {
        return dao.findUnsynced();
    }

    @Override
    public void upsertBan(LocalBan ban) {
        dao.upsertBan(ban);
    }

    @Override
    public void insertOfflineBan(String uuid, String playerName, String type, String reason,
                                 String adminUuid, String adminName, Long expiresAt) {
        dao.insertOfflineBan(uuid, playerName, type, reason, adminUuid, adminName, expiresAt);
    }

    @Override
    public void markSynced(String uuid) {
        dao.markSynced(uuid);
    }

    @Override
    public void deactivateBan(String uuid) {
        dao.deactivateBan(uuid);
    }

    @Override
    public Optional<String> getMeta(String key) {
        return dao.getMeta(key);
    }

    @Override
    public void setMeta(String key, String value) {
        dao.setMeta(key, value);
    }

    @Override
    public void close() {
        if (dao != null) {
            dao.close();
        }
    }
}
