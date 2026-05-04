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
import com.pvpindex.bans.plugin.bukkitListeners.PlayerListener;
import com.pvpindex.bans.plugin.callBacks.BanSync;
import com.pvpindex.bans.plugin.commands.BaseCommand;
import com.pvpindex.bans.plugin.commands.CommandAltlookup;
import com.pvpindex.bans.plugin.commands.CommandBan;
import com.pvpindex.bans.plugin.commands.CommandBanip;
import com.pvpindex.bans.plugin.commands.CommandBanlookup;
import com.pvpindex.bans.plugin.commands.CommandGlobalban;
import com.pvpindex.bans.plugin.commands.CommandKick;
import com.pvpindex.bans.plugin.commands.CommandLookup;
import com.pvpindex.bans.plugin.commands.CommandMCBans;
import com.pvpindex.bans.plugin.commands.CommandMCBansSettings;
import com.pvpindex.bans.plugin.commands.CommandPrevious;
import com.pvpindex.bans.plugin.commands.CommandRban;
import com.pvpindex.bans.plugin.commands.CommandTempban;
import com.pvpindex.bans.plugin.commands.CommandUnban;
import com.pvpindex.bans.plugin.commands.MCBansCommandHandler;
import com.pvpindex.bans.plugin.permission.Perms;
import com.pvpindex.bans.plugin.placeholder.McBansPlaceholderExpansion;
import com.pvpindex.bans.storage.StorageBackend;
import com.pvpindex.bans.storage.StorageManager;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles plugin startup and shutdown logic.
 *
 * <p>{@link MCBans#onEnable()} and {@link MCBans#onDisable()} are the sole
 * callers.  All constructed components are stored in {@link Registry} so the
 * rest of the codebase can retrieve them without a direct {@code MCBans}
 * reference.</p>
 */
public final class Bootstrap {

    private Bootstrap() {
    }

    // -------------------------------------------------------------------------
    // Startup
    // -------------------------------------------------------------------------

    public static void onEnable(MCBans plugin) {
        // Register plugin instance early — MCBans.getInstance() is used during init
        Registry.setPlugin(plugin);

        ActionLog log = new ActionLog(plugin);
        Registry.setLog(log);

        // Configuration
        ConfigurationManager config = new ConfigurationManager(plugin, log);
        try {
            config.loadConfig(true);
        } catch (Exception ex) {
            log.warning("Failed to load config.yml: " + ex.getMessage());
            ex.printStackTrace();
        }
        // Register config early so MCBans.getPrefix() works during the rest of init
        Registry.setConfig(config);

        DebugLogger debugLogger = new DebugLogger(log, config);

        PluginManager pm = plugin.getServer().getPluginManager();
        if (!pm.isPluginEnabled(plugin)) {
            return;
        }

        // Storage (configured backend: sqlite / yaml / mysql / postgresql)
        StorageManager storageManager = new StorageManager(plugin.getDataFolder(), plugin.getLogger(), config);
        StorageBackend storage;
        try {
            storageManager.initialise();
            storage = storageManager.getStorage();
        } catch (Exception e) {
            log.severe("Storage backend failed to initialise: " + e.getMessage());
            pm.disablePlugin(plugin);
            return;
        }

        // HTTP API client
        PvPIndexApiClient apiClient = new PvPIndexApiClient(
                config.getApiUrl(),
                config.getApiKey(),
                plugin.getLogger());

        // Language
        I18n.init(config.getLanguage());

        // Events
        pm.registerEvents(new PlayerListener(plugin), plugin);

        // Permissions
        Perms.setupPermissionHandler();

        // Commands
        MCBansCommandHandler commandHandler = new MCBansCommandHandler(plugin);
        registerCommands(commandHandler);

        // Background ban-sync thread
        BanSync bansync = new BanSync(plugin);
        bansync.start();

        // Store all components before registering PAPI (which may call getStorage())
        Registry.init(plugin, config, log, debugLogger, storageManager, storage, apiClient, commandHandler, bansync);

        // Perform an initial sync on startup — must come after Registry.init() so
        // plugin.getBanDao() / plugin.getApiClient() resolve correctly.
        new Thread(() -> bansync.performSync(), "MCBans-InitialSync").start();

        // PlaceholderAPI integration (soft-depend — only register if PAPI is present)
        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new McBansPlaceholderExpansion(storage).register();
            log.info("PlaceholderAPI expansion registered.");
        }

        final PluginDescriptionFile pdf = plugin.getDescription();
        log.info("v" + pdf.getVersion() + " enabled.");
    }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    public static void onDisable() {
        BanSync bansync = Registry.getBanSync();
        if (bansync != null) {
            bansync.stopSync();
        }

        StorageManager storageManager = Registry.getStorageManager();
        if (storageManager != null) {
            storageManager.close();
        }

        MCBans plugin = Registry.getPlugin();
        if (plugin != null) {
            plugin.getServer().getScheduler().cancelTasks(plugin);
        }

        ActionLog log = Registry.getLog();
        if (plugin != null && log != null) {
            final PluginDescriptionFile pdf = plugin.getDescription();
            log.info("v" + pdf.getVersion() + " disabled.");
        }

        Registry.clear();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void registerCommands(MCBansCommandHandler commandHandler) {
        List<BaseCommand> cmds = new ArrayList<>();
        cmds.add(new CommandBan());
        cmds.add(new CommandGlobalban());
        cmds.add(new CommandTempban());
        cmds.add(new CommandRban());
        cmds.add(new CommandBanip());
        cmds.add(new CommandUnban());
        cmds.add(new CommandKick());
        cmds.add(new CommandLookup());
        cmds.add(new CommandBanlookup());
        cmds.add(new CommandAltlookup());
        cmds.add(new CommandMCBans());
        cmds.add(new CommandPrevious());
        cmds.add(new CommandMCBansSettings());
        for (BaseCommand cmd : cmds) {
            commandHandler.registerCommand(cmd);
        }
    }
}
