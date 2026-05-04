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

import com.pvpindex.bans.storage.StorageBackend;
import com.pvpindex.bans.storage.LocalBan;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * YAML-backed {@link StorageBackend}.
 *
 * <p>All bans are stored in {@code plugins/MCBans/bans.yml} using Bukkit's
 * {@link YamlConfiguration} API.  Each ban occupies a section keyed by the
 * player UUID; a {@code meta} section holds sync-cursor values.</p>
 *
 * <p>This backend is suitable for small servers that prefer human-readable
 * storage without a database engine.  For high-throughput servers consider
 * SQLite or MySQL instead.</p>
 *
 * <pre>{@code
 * bans:
 *   aaaabbbbccccdddd1111222233334444:
 *     player_name: "Steve"
 *     type: "local"
 *     reason: "Cheating"
 *     admin_uuid: null
 *     admin_name: "Console"
 *     expires_at: 0          # 0 = no expiry
 *     is_active: true
 *     is_synced: false
 *     created_at: 1700000000
 *     updated_at: 1700000000
 * meta:
 *   lastSyncAt: "2024-01-01T00:00:00Z"
 * }</pre>
 */
public class YamlBackend implements StorageBackend {

    private static final String BANS_FILE = "bans.yml";

    private final File dataFolder;
    private final Logger logger;
    private File bansFile;
    private YamlConfiguration config;

    public YamlBackend(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger     = logger;
    }

    /**
     * Load (or create) {@code bans.yml}.
     */
    public void initialise() {
        dataFolder.mkdirs();
        bansFile = new File(dataFolder, BANS_FILE);
        if (!bansFile.exists()) {
            try {
                bansFile.createNewFile();
            } catch (IOException e) {
                logger.warning("Could not create bans.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(bansFile);
        logger.info("YAML storage initialised at " + bansFile.getPath());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String banKey(String uuid) {
        return "bans." + normalise(uuid);
    }

    private static String normalise(String uuid) {
        return uuid == null ? null : uuid.toLowerCase().replace("-", "");
    }

    private void save() {
        try {
            config.save(bansFile);
        } catch (IOException e) {
            logger.warning("[MCBans-YAML] Failed to save bans.yml: " + e.getMessage());
        }
    }

    private LocalBan fromSection(String uuid) {
        String key = banKey(uuid);
        if (!config.contains(key)) {
            return null;
        }
        long expiresAtRaw = config.getLong(key + ".expires_at", 0L);
        Long expiresAt    = expiresAtRaw > 0 ? expiresAtRaw : null;
        return new LocalBan(
                uuid,
                config.getString(key + ".player_name"),
                config.getString(key + ".type", "local"),
                config.getString(key + ".reason", ""),
                config.getString(key + ".admin_uuid"),
                config.getString(key + ".admin_name"),
                expiresAt,
                config.getBoolean(key + ".is_active", true),
                config.getBoolean(key + ".is_synced", false),
                config.getLong(key + ".created_at", 0L),
                config.getLong(key + ".updated_at", 0L)
        );
    }

    private void writeBan(LocalBan ban) {
        String key = banKey(ban.uuid());
        config.set(key + ".player_name", ban.playerName());
        config.set(key + ".type",        ban.type());
        config.set(key + ".reason",      ban.reason());
        config.set(key + ".admin_uuid",  ban.adminUuid());
        config.set(key + ".admin_name",  ban.adminName());
        config.set(key + ".expires_at",  ban.expiresAt() != null ? ban.expiresAt() : 0L);
        config.set(key + ".is_active",   ban.isActive());
        config.set(key + ".is_synced",   ban.isSynced());
        config.set(key + ".created_at",  ban.createdAt());
        config.set(key + ".updated_at",  ban.updatedAt());
    }

    // -------------------------------------------------------------------------
    // StorageBackend
    // -------------------------------------------------------------------------

    @Override
    public Optional<LocalBan> findActiveBan(String uuid) {
        String normUuid = normalise(uuid);
        LocalBan ban = fromSection(normUuid);
        if (ban == null || !ban.isActive()) {
            return Optional.empty();
        }
        // Auto-expire temp bans
        if (ban.expiresAt() != null && ban.expiresAt() < Instant.now().getEpochSecond()) {
            deactivateBan(uuid);
            return Optional.empty();
        }
        return Optional.of(ban);
    }

    @Override
    public List<LocalBan> findUnsynced() {
        List<LocalBan> result = new ArrayList<>();
        if (!config.contains("bans")) {
            return result;
        }
        for (String uuid : config.getConfigurationSection("bans").getKeys(false)) {
            LocalBan ban = fromSection(uuid);
            if (ban != null && !ban.isSynced()) {
                result.add(ban);
            }
        }
        return result;
    }

    @Override
    public void upsertBan(LocalBan ban) {
        String normUuid = normalise(ban.uuid());
        LocalBan existing = fromSection(normUuid);
        if (existing != null && existing.updatedAt() >= ban.updatedAt()) {
            return; // existing record is newer — skip
        }
        writeBan(new LocalBan(
                normUuid,
                ban.playerName(),
                ban.type(),
                ban.reason(),
                ban.adminUuid(),
                ban.adminName(),
                ban.expiresAt(),
                ban.isActive(),
                ban.isSynced(),
                ban.createdAt() > 0 ? ban.createdAt() : Instant.now().getEpochSecond(),
                ban.updatedAt() > 0 ? ban.updatedAt() : Instant.now().getEpochSecond()
        ));
        save();
    }

    @Override
    public void insertOfflineBan(String uuid, String playerName, String type, String reason,
                                 String adminUuid, String adminName, Long expiresAt) {
        long now = Instant.now().getEpochSecond();
        upsertBan(new LocalBan(normalise(uuid), playerName, type, reason,
                adminUuid, adminName, expiresAt, true, false, now, now));
    }

    @Override
    public void markSynced(String uuid) {
        String key = banKey(normalise(uuid));
        if (config.contains(key)) {
            config.set(key + ".is_synced", true);
            save();
        }
    }

    @Override
    public void deactivateBan(String uuid) {
        String key = banKey(normalise(uuid));
        if (config.contains(key) && config.getBoolean(key + ".is_active", false)) {
            config.set(key + ".is_active",  false);
            config.set(key + ".updated_at", Instant.now().getEpochSecond());
            save();
        }
    }

    @Override
    public Optional<String> getMeta(String key) {
        String value = config.getString("meta." + key);
        return value != null ? Optional.of(value) : Optional.empty();
    }

    @Override
    public void setMeta(String key, String value) {
        config.set("meta." + key, value);
        save();
    }

    @Override
    public void close() {
        // Nothing to close; YamlConfiguration is file-based with no open handles
    }
}
