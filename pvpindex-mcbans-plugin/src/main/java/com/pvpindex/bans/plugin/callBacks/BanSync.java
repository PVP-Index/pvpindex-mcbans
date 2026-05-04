/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.callBacks;

import com.pvpindex.bans.api.BanRecord;
import com.pvpindex.bans.api.BanRequest;
import com.pvpindex.bans.api.BanSyncPage;
import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.storage.BanDao;
import com.pvpindex.bans.storage.StorageBackend;
import com.pvpindex.bans.storage.LocalBan;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Periodic sync between the local SQLite cache and the PvPIndex API.
 *
 * <ol>
 *   <li>Upload any bans that were issued while the API was offline ({@code is_synced = 0}).</li>
 *   <li>Download a paginated delta of bans updated since the last successful sync.</li>
 * </ol>
 *
 * <p>If the API is unreachable at any point the sync exits silently so the plugin
 * remains functional with local data only.</p>
 */
public class BanSync extends Thread {

    private final MCBans plugin;
    private volatile boolean running = true;

    /** Epoch-second timestamp of the last successful sync cycle. */
    public volatile long lastSync = 0;

    /** {@code true} while a sync cycle is executing. */
    public volatile boolean syncRunning = false;

    public BanSync(MCBans plugin) {
        super("MCBans-BanSync");
        setDaemon(true);
        this.plugin = plugin;
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                long intervalSeconds = plugin.getConfigs().getSyncInterval() * 60L;
                Thread.sleep(intervalSeconds * 1000L);
                performSync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Stop the background thread on plugin disable. */
    public void stopSync() {
        running = false;
        interrupt();
    }

    /**
     * Perform one full sync cycle (upload unsynced, then download delta).
     * Safe to call manually (e.g. from {@code /mcbans sync}).
     */
    public void performSync() {
        if (syncRunning) {
            return;
        }
        syncRunning = true;
        try {
            uploadUnsynced();
            downloadDelta();
            lastSync = Instant.now().getEpochSecond();
        } finally {
            syncRunning = false;
        }
    }

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    /**
     * Push bans that were recorded while the API was down.
     * On success the local row is marked synced; failures are left for the next cycle.
     */
    private void uploadUnsynced() {
        StorageBackend dao = plugin.getBanDao();
        List<LocalBan> unsynced = dao.findUnsynced();
        for (LocalBan ban : unsynced) {
            if (!running) {
                return;
            }
            boolean ok;
            if (!ban.isActive()) {
                ok = plugin.getApiClient().unban(ban.uuid());
            } else {
                String expiresAt = ban.expiresAt() != null
                        ? Instant.ofEpochSecond(ban.expiresAt()).toString()
                        : null;
                BanRequest req = new BanRequest(
                        ban.uuid(), ban.playerName(), ban.type(),
                        ban.reason(), ban.adminUuid(), ban.adminName(), expiresAt);
                ok = plugin.getApiClient().ban(req).isPresent();
            }
            if (ok) {
                dao.markSynced(ban.uuid());
                plugin.debug("[BanSync] Uploaded offline ban for " + ban.playerName());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    /** Download paginated delta from the API and update the local cache. */
    private void downloadDelta() {
        StorageBackend dao = plugin.getBanDao();
        String lastSync = dao.getMeta("lastSyncAt").orElse(null);

        int page = 1;
        int total = 0;
        while (running) {
            Optional<BanSyncPage> pageOpt = plugin.getApiClient().getBans(lastSync, page);
            if (pageOpt.isEmpty()) {
                plugin.debug("[BanSync] API unavailable during delta download, will retry next cycle.");
                return;
            }
            BanSyncPage syncPage = pageOpt.get();
            for (BanRecord record : syncPage.records()) {
                if (record.playerUuid() == null) {
                    continue;
                }
                dao.upsertBan(BanDao.fromApiRecord(record));
                total++;
            }
            if (!syncPage.hasMore()) {
                break;
            }
            page++;
        }

        if (total > 0) {
            plugin.debug("[BanSync] Downloaded " + total + " ban record(s) from API.");
        }

        // Advance the cursor to now so next sync only fetches changes after this moment
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        dao.setMeta("lastSyncAt", now);
    }
}
