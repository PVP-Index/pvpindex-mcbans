/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin;

import com.pvpindex.bans.api.PvPIndexApiClient;
import com.pvpindex.bans.plugin.callBacks.BanSync;
import com.pvpindex.bans.plugin.commands.MCBansCommandHandler;
import com.pvpindex.bans.storage.StorageBackend;
import com.pvpindex.bans.storage.StorageManager;

/**
 * Central registry that holds every shared plugin component.
 *
 * <p>Components are written once by {@link Bootstrap#onEnable(MCBans)} and
 * cleared by {@link Bootstrap#onDisable()}.  All reads are via the static
 * getters below so the rest of the codebase never needs to pass a
 * {@code MCBans} reference around.</p>
 */
public final class Registry {

    private static MCBans plugin;
    private static ConfigurationManager config;
    private static ActionLog log;
    private static DebugLogger debugLogger;
    private static StorageManager storageManager;
    private static StorageBackend storage;
    private static PvPIndexApiClient apiClient;
    private static MCBansCommandHandler commandHandler;
    private static BanSync bansync;

    /** Legacy flag: broadcast ban announcements to all online players. */
    public static boolean announceAll = false;

    private Registry() {
    }

    // -------------------------------------------------------------------------
    // Package-private mutators — called only from Bootstrap
    // -------------------------------------------------------------------------

    static void init(
            MCBans p,
            ConfigurationManager c,
            ActionLog l,
            DebugLogger d,
            StorageManager sm,
            StorageBackend s,
            PvPIndexApiClient a,
            MCBansCommandHandler ch,
            BanSync bs) {
        plugin = p;
        config = c;
        log = l;
        debugLogger = d;
        storageManager = sm;
        storage = s;
        apiClient = a;
        commandHandler = ch;
        bansync = bs;
    }

    static void setApiClient(PvPIndexApiClient a) {
        apiClient = a;
    }

    static void setPlugin(MCBans p) {
        plugin = p;
    }

    static void setConfig(ConfigurationManager c) {
        config = c;
    }

    static void setLog(ActionLog l) {
        log = l;
    }

    static void clear() {
        plugin = null;
        config = null;
        log = null;
        debugLogger = null;
        storageManager = null;
        storage = null;
        apiClient = null;
        commandHandler = null;
        bansync = null;
        announceAll = false;
    }

    // -------------------------------------------------------------------------
    // Public getters
    // -------------------------------------------------------------------------

    public static MCBans getPlugin() {
        return plugin;
    }

    public static ConfigurationManager getConfig() {
        return config;
    }

    public static ActionLog getLog() {
        return log;
    }

    public static DebugLogger getDebugLogger() {
        return debugLogger;
    }

    public static StorageManager getStorageManager() {
        return storageManager;
    }

    public static StorageBackend getStorage() {
        return storage;
    }

    public static PvPIndexApiClient getApiClient() {
        return apiClient;
    }

    public static MCBansCommandHandler getCommandHandler() {
        return commandHandler;
    }

    public static BanSync getBanSync() {
        return bansync;
    }
}
