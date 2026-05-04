package com.pvpindex.bans.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Opens the SQLite database and ensures the schema is up to date.
 *
 * <p>Call {@link #initialise()} on plugin enable; {@link #close()} on disable.</p>
 */
public class StorageManager {

    private static final String DB_FILE = "bans.db";

    private final File dataFolder;
    private final Logger logger;
    private Connection connection;

    public StorageManager(JavaPlugin plugin) {
        this.dataFolder = plugin.getDataFolder();
        this.logger     = plugin.getLogger();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Open the SQLite connection and create tables if they don't exist yet.
     */
    public void initialise() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        dataFolder.mkdirs();
        File dbFile = new File(dataFolder, DB_FILE);
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        createSchema();
        logger.info("[MCBans] SQLite storage initialised at " + dbFile.getPath());
    }

    /**
     * Close the SQLite connection gracefully.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning("[MCBans] Error closing SQLite connection: " + e.getMessage());
        }
    }

    /**
     * Returns the raw connection for use by {@link BanDao}.
     */
    public Connection getConnection() {
        return connection;
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            // Main ban table — one row per (uuid, ban lifecycle).
            // Unbans just flip is_active = 0; we keep the history row.
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

            // Index for the common offline-check query
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_bans_active ON player_bans (uuid, is_active)
                """);

            // Key/value store — used for sync cursors (e.g. lastSyncAt)
            st.execute("""
                CREATE TABLE IF NOT EXISTS meta (
                  key   TEXT NOT NULL PRIMARY KEY,
                  value TEXT
                )
                """);
        }
    }
}
