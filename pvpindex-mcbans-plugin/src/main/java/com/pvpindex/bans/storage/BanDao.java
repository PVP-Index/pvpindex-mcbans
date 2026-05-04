package com.pvpindex.bans.storage;

import com.pvpindex.bans.api.BanRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SQL-based implementation of {@link StorageBackend}.
 *
 * <p>The default dialect ({@link Dialect#SQLITE}) is fully compatible with
 * PostgreSQL 9.5+ since both support {@code ON CONFLICT} upsert syntax.
 * Use {@link Dialect#MYSQL} for MySQL / MariaDB servers, which require
 * {@code ON DUPLICATE KEY UPDATE} instead.</p>
 *
 * <p>The database schema is created/migrated by the owning backend class
 * (e.g. {@link backend.SqliteBackend}).  Pass the {@link Connection} obtained
 * from that backend.</p>
 */
public class BanDao implements StorageBackend {

    /**
     * SQL dialect controlling upsert syntax.
     * <ul>
     *   <li>{@code SQLITE}  — {@code ON CONFLICT … DO UPDATE} (also valid for PostgreSQL)</li>
     *   <li>{@code MYSQL}   — {@code ON DUPLICATE KEY UPDATE}</li>
     * </ul>
     */
    public enum Dialect {
        SQLITE,
        MYSQL
    }

    private final Connection connection;
    private final Logger logger;
    private final Dialect dialect;

    /** Constructs a DAO using the SQLite/PostgreSQL dialect. */
    public BanDao(Connection connection, Logger logger) {
        this(connection, logger, Dialect.SQLITE);
    }

    public BanDao(Connection connection, Logger logger, Dialect dialect) {
        this.connection = connection;
        this.logger     = logger;
        this.dialect    = dialect;
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    /**
     * Returns the first active, non-expired ban for a player, or {@link Optional#empty()}.
     */
    @Override
    public Optional<LocalBan> findActiveBan(String uuid) {
        String sql = "SELECT * FROM player_bans WHERE uuid = ? AND is_active = 1 LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalise(uuid));
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            }
            LocalBan ban = fromRow(rs);
            // Auto-expire temp bans locally
            if (ban.expiresAt() != null && ban.expiresAt() < Instant.now().getEpochSecond()) {
                deactivateBan(uuid);
                return Optional.empty();
            }
            return Optional.of(ban);
        } catch (SQLException e) {
            logger.warning("[MCBans-SQLite] findActiveBan error: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns all bans that have not yet been synced to the API ({@code is_synced = 0}).
     */
    @Override
    public List<LocalBan> findUnsynced() {
        List<LocalBan> list = new ArrayList<>();
        String sql = "SELECT * FROM player_bans WHERE is_synced = 0";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(fromRow(rs));
            }
        } catch (SQLException e) {
            logger.warning("[MCBans-SQLite] findUnsynced error: " + e.getMessage());
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    /**
     * Insert or replace a ban record.  Existing row with the same UUID is replaced only
     * when the incoming record is newer (by {@code updated_at} epoch second).
     */
    @Override
    public void upsertBan(LocalBan ban) {
        final String sql;
        if (dialect == Dialect.MYSQL) {
            sql = "INSERT INTO player_bans"
                + " (uuid, player_name, type, reason, admin_uuid, admin_name,"
                + "  expires_at, is_active, is_synced, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE"
                + "  player_name = IF(VALUES(updated_at) > updated_at, VALUES(player_name), player_name),"
                + "  type        = IF(VALUES(updated_at) > updated_at, VALUES(type),        type),"
                + "  reason      = IF(VALUES(updated_at) > updated_at, VALUES(reason),      reason),"
                + "  admin_uuid  = IF(VALUES(updated_at) > updated_at, VALUES(admin_uuid),  admin_uuid),"
                + "  admin_name  = IF(VALUES(updated_at) > updated_at, VALUES(admin_name),  admin_name),"
                + "  expires_at  = IF(VALUES(updated_at) > updated_at, VALUES(expires_at),  expires_at),"
                + "  is_active   = IF(VALUES(updated_at) > updated_at, VALUES(is_active),   is_active),"
                + "  is_synced   = IF(VALUES(updated_at) > updated_at, VALUES(is_synced),   is_synced),"
                + "  updated_at  = IF(VALUES(updated_at) > updated_at, VALUES(updated_at),  updated_at)";
        } else {
            sql = """
                INSERT INTO player_bans
                  (uuid, player_name, type, reason, admin_uuid, admin_name,
                   expires_at, is_active, is_synced, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                  player_name = excluded.player_name,
                  type        = excluded.type,
                  reason      = excluded.reason,
                  admin_uuid  = excluded.admin_uuid,
                  admin_name  = excluded.admin_name,
                  expires_at  = excluded.expires_at,
                  is_active   = excluded.is_active,
                  is_synced   = excluded.is_synced,
                  updated_at  = excluded.updated_at
                WHERE excluded.updated_at > player_bans.updated_at
                """;
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            long now = Instant.now().getEpochSecond();
            ps.setString(1, normalise(ban.uuid()));
            ps.setString(2, ban.playerName());
            ps.setString(3, ban.type());
            ps.setString(4, ban.reason());
            ps.setString(5, ban.adminUuid());
            ps.setString(6, ban.adminName());
            ps.setObject(7, ban.expiresAt());       // Long or null
            ps.setInt(8, ban.isActive() ? 1 : 0);
            ps.setInt(9, ban.isSynced() ? 1 : 0);
            ps.setLong(10, ban.createdAt() > 0 ? ban.createdAt() : now);
            ps.setLong(11, ban.updatedAt() > 0 ? ban.updatedAt() : now);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[MCBans-SQLite] upsertBan error: " + e.getMessage());
        }
    }

    /** Convenience — creates a ban marked as un-synced (offline ban). */
    @Override
    public void insertOfflineBan(String uuid, String playerName, String type, String reason,
                                 String adminUuid, String adminName, Long expiresAt) {
        long now = Instant.now().getEpochSecond();
        upsertBan(new LocalBan(
                normalise(uuid), playerName, type, reason, adminUuid, adminName,
                expiresAt, true, false, now, now));
    }

    /** Mark a ban as synced to the API. */
    @Override
    public void markSynced(String uuid) {
        String sql = "UPDATE player_bans SET is_synced = 1 WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalise(uuid));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[MCBans-SQLite] markSynced error: " + e.getMessage());
        }
    }

    /** Deactivate the active ban for a player (unban). */
    @Override
    public void deactivateBan(String uuid) {
        String sql = "UPDATE player_bans SET is_active = 0, updated_at = ? WHERE uuid = ? AND is_active = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Instant.now().getEpochSecond());
            ps.setString(2, normalise(uuid));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[MCBans-SQLite] deactivateBan error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Meta (sync cursor)
    // -------------------------------------------------------------------------

    @Override
    public Optional<String> getMeta(String key) {
        String sql = "SELECT value FROM meta WHERE key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(rs.getString("value")) : Optional.empty();
        } catch (SQLException e) {
            logger.warning("[MCBans-SQLite] getMeta error: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void setMeta(String key, String value) {
        final String sql;
        if (dialect == Dialect.MYSQL) {
            sql = "INSERT INTO meta (key, value) VALUES (?, ?)"
                + " ON DUPLICATE KEY UPDATE value = VALUES(value)";
        } else {
            sql = "INSERT INTO meta (key, value) VALUES (?, ?)"
                + " ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[MCBans-SQLite] setMeta error: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning("[MCBans-BanDao] Error closing connection: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Factory — build a LocalBan from a BanRecord received from the API
    // -------------------------------------------------------------------------

    public static LocalBan fromApiRecord(BanRecord r) {
        long updatedAt = r.updatedAt() != null
                ? parseIsoEpoch(r.updatedAt())
                : Instant.now().getEpochSecond();
        Long expiresAt = r.expiresAt() != null ? parseIsoEpoch(r.expiresAt()) : null;
        return new LocalBan(
                r.playerUuid() != null ? r.playerUuid().replace("-", "").toLowerCase() : null,
                r.playerUsername(),
                r.type(),
                r.reason(),
                r.adminUuid(),
                r.adminName(),
                expiresAt,
                r.isActive(),
                true,          // downloaded from API → already synced
                updatedAt,
                updatedAt
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static LocalBan fromRow(ResultSet rs) throws SQLException {
        Long expiresAt = rs.getObject("expires_at") != null ? rs.getLong("expires_at") : null;
        return new LocalBan(
                rs.getString("uuid"),
                rs.getString("player_name"),
                rs.getString("type"),
                rs.getString("reason"),
                rs.getString("admin_uuid"),
                rs.getString("admin_name"),
                expiresAt,
                rs.getInt("is_active") == 1,
                rs.getInt("is_synced") == 1,
                rs.getLong("created_at"),
                rs.getLong("updated_at")
        );
    }

    private static String normalise(String uuid) {
        return uuid == null ? null : uuid.toLowerCase().replace("-", "");
    }

    /** Parse ISO-8601 string to Unix epoch seconds (best-effort). */
    private static long parseIsoEpoch(String iso) {
        try {
            return Instant.parse(iso).getEpochSecond();
        } catch (Exception e) {
            return Instant.now().getEpochSecond();
        }
    }
}
