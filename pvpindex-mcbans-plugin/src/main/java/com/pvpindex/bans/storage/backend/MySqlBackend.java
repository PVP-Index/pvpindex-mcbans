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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * MySQL / MariaDB-backed {@link StorageBackend} using HikariCP for connection pooling.
 *
 * <p>The schema uses {@code ON DUPLICATE KEY UPDATE} for upserts, which is
 * required for MySQL compatibility (PostgreSQL and SQLite use {@code ON CONFLICT}).</p>
 */
public class MySqlBackend implements StorageBackend {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;
    private final boolean useSSL;
    private final Logger logger;

    private HikariDataSource dataSource;
    private BanDao dao;

    public MySqlBackend(String host, int port, String database,
                        String username, String password, int poolSize,
                        boolean useSSL, Logger logger) {
        this.host     = host;
        this.port     = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.useSSL   = useSSL;
        this.logger   = logger;
    }

    /**
     * Open the HikariCP pool and create/migrate the schema.
     *
     * @throws SQLException on JDBC / schema errors
     */
    public void initialise() throws SQLException {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC&characterEncoding=utf8");
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(poolSize);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(10_000);
        cfg.setPoolName("MCBans-MySQL");

        dataSource = new HikariDataSource(cfg);

        try (Connection connection = dataSource.getConnection()) {
            createSchema(connection);
        }

        dao = new BanDao(dataSource.getConnection(), logger, BanDao.Dialect.MYSQL);
        logger.info("MySQL storage initialised at " + host + ":" + port + "/" + database);
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private void createSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_bans (
                  uuid        VARCHAR(36)  NOT NULL,
                  player_name VARCHAR(16),
                  type        VARCHAR(10)  NOT NULL,
                  reason      VARCHAR(500) NOT NULL,
                  admin_uuid  VARCHAR(36),
                  admin_name  VARCHAR(16),
                  expires_at  BIGINT,
                  is_active   TINYINT(1)   NOT NULL DEFAULT 1,
                  is_synced   TINYINT(1)   NOT NULL DEFAULT 0,
                  is_legacy   TINYINT(1)   NOT NULL DEFAULT 0,
                  created_at  BIGINT       NOT NULL,
                  updated_at  BIGINT       NOT NULL,
                  PRIMARY KEY (uuid)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """);
            // Migration: add is_legacy column if upgrading from older schema
            try {
                st.execute("ALTER TABLE player_bans ADD COLUMN is_legacy TINYINT(1) NOT NULL DEFAULT 0");
            } catch (java.sql.SQLException ignored) {
                // Column already exists - expected for fresh installs
            }
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_bans_active ON player_bans (uuid, is_active)
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS meta (
                  `key`   VARCHAR(64)  NOT NULL,
                  value   VARCHAR(255),
                  PRIMARY KEY (`key`)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
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
    public void insertLegacyBan(String uuid, String playerName, String reason, String adminName) {
        dao.insertLegacyBan(uuid, playerName, reason, adminName);
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
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
