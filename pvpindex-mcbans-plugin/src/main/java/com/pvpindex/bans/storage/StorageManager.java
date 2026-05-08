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

import com.pvpindex.bans.plugin.ConfigurationManager;
import com.pvpindex.bans.storage.backend.MySqlBackend;
import com.pvpindex.bans.storage.backend.PostgresBackend;
import com.pvpindex.bans.storage.backend.SqliteBackend;
import com.pvpindex.bans.storage.backend.YamlBackend;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * Factory that selects and initialises the correct {@link StorageBackend}
 * based on the {@code storage.backend} key in {@code config.yml}.
 *
 * <p>Supported values (case-insensitive):
 * <ul>
 *   <li>{@code sqlite} (default) - embedded SQLite database</li>
 *   <li>{@code yaml}             - flat YAML file</li>
 *   <li>{@code mysql}            - MySQL / MariaDB via HikariCP</li>
 *   <li>{@code postgresql} / {@code postgres} - PostgreSQL via HikariCP</li>
 * </ul>
 * </p>
 */
public class StorageManager {

    private final File dataFolder;
    private final Logger logger;
    private final ConfigurationManager config;
    private StorageBackend storage;

    public StorageManager(JavaPlugin plugin) {
        this.dataFolder = plugin.getDataFolder();
        this.logger     = plugin.getLogger();
        this.config     = null; // will not be used in this path
    }

    /**
     * Constructor used at runtime - takes the already-loaded ConfigurationManager
     * so the factory can read storage settings.
     */
    public StorageManager(File dataFolder, Logger logger, ConfigurationManager config) {
        this.dataFolder = dataFolder;
        this.logger     = logger;
        this.config     = config;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Create and initialise the configured backend.
     *
     * @throws Exception if the backend cannot be opened (JDBC error, missing driver, …)
     */
    public void initialise() throws Exception {
        String backendType = config != null
                ? config.getStorageBackend()
                : "sqlite";

        switch (backendType.toLowerCase()) {
            case "yaml" -> {
                YamlBackend yaml = new YamlBackend(dataFolder, logger);
                yaml.initialise();
                storage = yaml;
            }
            case "mysql" -> {
                MySqlBackend mysql = new MySqlBackend(
                        config.getDbHost(),
                        config.getDbPort(),
                        config.getDbName(),
                        config.getDbUser(),
                        config.getDbPassword(),
                        config.getDbPoolSize(),
                        config.isDbUseSSL(),
                        logger);
                mysql.initialise();
                storage = mysql;
            }
            case "postgresql", "postgres" -> {
                PostgresBackend pg = new PostgresBackend(
                        config.getDbHost(),
                        config.getDbPort(),
                        config.getDbName(),
                        config.getDbUser(),
                        config.getDbPassword(),
                        config.getDbPoolSize(),
                        logger);
                pg.initialise();
                storage = pg;
            }
            default -> {
                // sqlite (default)
                SqliteBackend sqlite = new SqliteBackend(dataFolder, logger);
                sqlite.initialise();
                storage = sqlite;
            }
        }
    }

    /**
     * Close the active backend gracefully on plugin disable.
     */
    public void close() {
        if (storage != null) {
            storage.close();
        }
    }

    /**
     * Returns the active {@link StorageBackend}.
     * Must be called after {@link #initialise()}.
     */
    public StorageBackend getStorage() {
        return storage;
    }
}

