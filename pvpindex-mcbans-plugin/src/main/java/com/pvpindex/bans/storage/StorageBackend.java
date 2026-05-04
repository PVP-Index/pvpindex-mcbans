package com.pvpindex.bans.storage;

import java.util.List;
import java.util.Optional;

/**
 * Backend-agnostic interface for local ban storage.
 *
 * <p>Implementations are provided for SQLite, YAML, MySQL, and PostgreSQL.
 * The active backend is selected via {@code storage.backend} in
 * {@code config.yml} and created by {@link StorageManager}.</p>
 *
 * <p>All operations are synchronous. Callers are responsible for
 * executing them off the main thread when required.</p>
 */
public interface StorageBackend {

    /**
     * Returns the first active, non-expired ban for a player,
     * or {@link Optional#empty()} if none exists.
     *
     * @param uuid player UUID without dashes, lowercase
     */
    Optional<LocalBan> findActiveBan(String uuid);

    /**
     * Returns every ban record that has not yet been pushed to the
     * PvPIndex API ({@code is_synced = 0}).
     */
    List<LocalBan> findUnsynced();

    /**
     * Insert or update a ban record.
     * Existing records are only overwritten when the incoming
     * {@code updated_at} is newer.
     *
     * @param ban the ban record to persist
     */
    void upsertBan(LocalBan ban);

    /**
     * Convenience method: insert a new ban that has not yet been
     * synced to the API (i.e. was issued while offline).
     */
    void insertOfflineBan(String uuid, String playerName, String type,
                          String reason, String adminUuid, String adminName,
                          Long expiresAt);

    /**
     * Mark the ban record for {@code uuid} as synced to the API.
     *
     * @param uuid player UUID without dashes, lowercase
     */
    void markSynced(String uuid);

    /**
     * Deactivate (unban) the active ban for {@code uuid}.
     *
     * @param uuid player UUID without dashes, lowercase
     */
    void deactivateBan(String uuid);

    /**
     * Retrieve a metadata value by key (e.g. the sync cursor
     * {@code lastSyncAt}).
     *
     * @param key metadata key
     * @return the stored value, or {@link Optional#empty()} if absent
     */
    Optional<String> getMeta(String key);

    /**
     * Persist a metadata key-value pair, overwriting any existing value.
     *
     * @param key   metadata key
     * @param value value to store
     */
    void setMeta(String key, String value);

    /**
     * Release any resources held by this backend (connections, file
     * handles, etc.). Called on plugin disable.
     */
    void close();
}
