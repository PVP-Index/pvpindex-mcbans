/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.bukkitListeners;

import com.pvpindex.bans.api.BanStatusResponse;
import com.pvpindex.bans.plugin.ActionLog;
import com.pvpindex.bans.plugin.ConfigurationManager;
import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.plugin.util.Util;
import com.pvpindex.bans.storage.BanDao;
import com.pvpindex.bans.storage.StorageBackend;
import com.pvpindex.bans.storage.LocalBan;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles AsyncPlayerPreLoginEvent: checks whether the joining player is banned.
 *
 * Flow:
 *  1. Ask the PvPIndex API (up to 3 s timeout).
 *  2. If the API is unreachable, fall back to the local SQLite cache.
 *  3. If banned, deny login with a human-readable kick message.
 */
public class PlayerListener implements Listener {

    private static final long API_TIMEOUT_MS = 3_000;

    private final MCBans plugin;
    private final ActionLog log;
    private final ConfigurationManager config;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "MCBans-LoginCheck");
        t.setDaemon(true);
        return t;
    });

    public PlayerListener(MCBans plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLog();
        this.config = plugin.getConfigs();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString().replace("-", "").toLowerCase();

        Optional<BanStatusResponse> apiResult;
        try {
            apiResult = CompletableFuture
                    .supplyAsync(() -> plugin.getApiClient().getBanStatus(uuid), executor)
                    .get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warning("API timed out for " + event.getName() + " - falling back to local cache.");
            apiResult = Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.warning("API error for " + event.getName() + ": " + e.getMessage());
            apiResult = Optional.empty();
        }

        if (apiResult.isPresent()) {
            BanStatusResponse status = apiResult.get();
            if (status.banned() && status.ban() != null) {
                plugin.getBanDao().upsertBan(BanDao.fromApiRecord(status.ban()));
                denyLogin(event, status.ban().type(), status.ban().reason(),
                        status.ban().adminName(), status.ban().expiresAt());
            }
        } else {
            // API unavailable - fall back to local storage cache
            StorageBackend store = plugin.getBanDao();
            Optional<LocalBan> localBan = store.findActiveBan(uuid);
            if (localBan.isPresent()) {
                LocalBan ban = localBan.get();
                denyLogin(event, ban.type(), ban.reason(), ban.adminName(), null);
            } else if (config.isFailsafe()) {
                // Issue #118: failsafe=true - deny login when API is unreachable
                // and there is no cached ban record, to prevent unverified access.
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        Util.color(config.getKickFailsafeMessage()));
                log.warning("Denied login for " + event.getName()
                        + " (failsafe=true, API unreachable)");
            }
        }
    }

    private void denyLogin(AsyncPlayerPreLoginEvent event, String type, String reason,
                           String adminName, String expiresAt) {
        String banType  = type != null ? type : "local";
        String admin    = adminName != null ? adminName : "Server";
        String expiry   = expiresAt != null ? expiresAt : "permanent";
        String appealUrl = config.getKickAppealUrl();

        String message = config.getKickMessage(banType)
                .replace("{reason}",     reason != null ? reason : "")
                .replace("{admin}",      admin)
                .replace("{expires}",    expiry)
                .replace("{appeal_url}", appealUrl);

        if (!appealUrl.isEmpty()) {
            message += "\n&fAppeal at: &b" + appealUrl;
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Util.color(message));
        log.info("Denied login for " + event.getName()
                + " (type=" + banType + ", reason=" + reason + ")");
    }
}
