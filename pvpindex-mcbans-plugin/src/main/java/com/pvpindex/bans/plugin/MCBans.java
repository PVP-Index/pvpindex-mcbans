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
import com.pvpindex.bans.plugin.api.MCBansAPI;
import com.pvpindex.bans.storage.StorageBackend;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * Plugin entry point - lifecycle is delegated to {@link Bootstrap};
 * shared state is held in {@link Registry}.
 */
public class MCBans extends JavaPlugin {

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {
        Bootstrap.onEnable(this);
    }

    @Override
    public void onDisable() {
        Bootstrap.onDisable();
    }

    // -------------------------------------------------------------------------
    // Command dispatch
    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return Registry.getCommandHandler().onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Registry.getCommandHandler().onTabComplete(sender, command, alias, args);
    }

    // -------------------------------------------------------------------------
    // Static accessors (legacy API - delegate to Registry)
    // -------------------------------------------------------------------------

    public static MCBans getInstance() {
        return Registry.getPlugin();
    }

    public static String getPrefix() {
        return Registry.getConfig().getPrefix();
    }

    /** Legacy flag: broadcast ban announcements to all online players. */
    public static boolean announceAll = false;

    public static UUID fromString(String uuid) {
        return UUID.fromString(uuid.replaceAll(
                "(?ism)([a-z0-9]{8})([a-z0-9]{4})([a-z0-9]{4})([a-z0-9]{4})([a-z0-9]{12})",
                "$1-$2-$3-$4-$5"));
    }

    public static Player getPlayer(Plugin plugin, UUID uuid) {
        return plugin.getServer().getPlayer(uuid);
    }

    public static Player getPlayer(Plugin plugin, String target) {
        return plugin.getServer().getPlayerExact(target);
    }

    // -------------------------------------------------------------------------
    // Instance accessors (delegate to Registry)
    // -------------------------------------------------------------------------

    public StorageBackend getBanDao() {
        return Registry.getStorage();
    }

    public StorageBackend getStorage() {
        return Registry.getStorage();
    }

    public PvPIndexApiClient getApiClient() {
        return Registry.getApiClient();
    }

    public ConfigurationManager getConfigs() {
        return Registry.getConfig();
    }

    public ActionLog getLog() {
        return Registry.getLog();
    }

    public DebugLogger getDebugLogger() {
        return Registry.getDebugLogger();
    }

    public void debug(String message) {
        DebugLogger dl = Registry.getDebugLogger();
        if (dl != null) {
            dl.debug(message);
        }
    }

    public MCBansAPI getAPI(Plugin plugin) {
        return MCBansAPI.getHandle(this, plugin);
    }

    /**
     * Replaces the API client - for unit testing only.
     */
    void setApiClientForTesting(PvPIndexApiClient client) {
        Registry.setApiClient(client);
    }
}
