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
 * PostgreSQL-backed {@link StorageBackend} using HikariCP for connection pooling.
 *
 * <p>PostgreSQL 9.5+ supports {@code ON CONFLICT … DO UPDATE} (the same upsert
 * syntax as SQLite), so {@link BanDao.Dialect#SQLITE} is used here.</p>
 */
public class PostgresBackend implements StorageBackend {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;
    private final Logger logger;

    private HikariDataSource dataSource;
    private BanDao dao;

    public PostgresBackend(String host, int port, String database,
                           String username, String password, int poolSize,
                           Logger logger) {
        this.host     = host;
        this.port     = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.logger   = logger;
    }

    /**
     * Open the HikariCP pool and create/migrate the schema.
     *
     * @throws SQLException on JDBC / schema errors
     */
    public void initialise() throws SQLException {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(poolSize);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(10_000);
        cfg.setPoolName("MCBans-PostgreSQL");

        dataSource = new HikariDataSource(cfg);

        try (Connection connection = dataSource.getConnection()) {
            createSchema(connection);
        }

        dao = new BanDao(dataSource.getConnection(), logger, BanDao.Dialect.SQLITE);
        logger.info("PostgreSQL storage initialised at " + host + ":" + port + "/" + database);
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private void createSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_bans (
                  uuid        VARCHAR(36)  NOT NULL PRIMARY KEY,
                  player_name VARCHAR(16),
                  type        VARCHAR(10)  NOT NULL,
                  reason      VARCHAR(500) NOT NULL,
                  admin_uuid  VARCHAR(36),
                  admin_name  VARCHAR(16),
                  expires_at  BIGINT,
                  is_active   SMALLINT     NOT NULL DEFAULT 1,
                  is_synced   SMALLINT     NOT NULL DEFAULT 0,
                  created_at  BIGINT       NOT NULL,
                  updated_at  BIGINT       NOT NULL
                )
                """);
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_bans_active ON player_bans (uuid, is_active)
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS meta (
                  key   VARCHAR(64)  NOT NULL PRIMARY KEY,
                  value VARCHAR(255)
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
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
