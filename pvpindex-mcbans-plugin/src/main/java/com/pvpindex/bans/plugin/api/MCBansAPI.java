/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.api;

import java.util.HashMap;

import com.pvpindex.bans.plugin.BanType;
import com.pvpindex.bans.plugin.MCBans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.pvpindex.bans.plugin.request.Ban;
import com.pvpindex.bans.plugin.request.Kick;
import com.pvpindex.bans.plugin.util.Util;

public class MCBansAPI {
    private final MCBans plugin;
    private final String pname;

    private MCBansAPI(final MCBans plugin, final String pname) {
        plugin.getLog().info("MCBans API linked with: " + pname);
        this.plugin = plugin;
        this.pname = pname;
    }

    private void ban(BanType type, String targetName, String targetUUID, String senderName, String senderUUID, String reason, String duration, String measure){
        // check null
        if (targetName == null || senderName == null){
            return;
        }

        String targetIP = "";
        if (type != BanType.UNBAN){
            final Player target = Bukkit.getPlayerExact(targetName);
            targetIP = (target != null) ? target.getAddress().getAddress().getHostAddress() : "";
        }

        Ban banControl = new Ban(plugin, type.getActionName(), targetName, targetUUID, targetIP, senderName, senderUUID, reason, duration, measure, null, false);
        banControl.run();
    }

    /**
     * Add Locally BAN
     * @param targetName BAN target player's name
     * @param senderName BAN issued admin's name
     * @param reason BAN reason
     */
    public void localBan(String targetName, String targetUUID, String senderName, String senderUUID, String reason){
        plugin.getLog().info("Plugin " + pname + " tried to local ban player " + targetName);

        reason = (reason == null || reason.isEmpty()) ? plugin.getConfigs().getDefaultLocal() : reason;
        this.ban(BanType.LOCAL, targetName, targetUUID, senderName, senderUUID, reason, "", "");
    }

    /**
     * Add Globally BAN
     * @param targetName BAN target player's name
     * @param senderName BAN issued admin's name
     * @param reason BAN reason
     */
    public void globalBan(String targetName, String targetUUID, String senderName, String senderUUID, String reason){
        plugin.getLog().info("Plugin " + pname + " tried to global ban player " + targetName);
        if (reason == null || reason.isEmpty()) return;
        this.ban(BanType.GLOBAL, targetName, targetUUID, senderName, senderUUID, reason, "", "");
    }

    /**
     * Add Temporary BAN
     * @param targetName BAN target player's name
     * @param targetUUID BAN target player's UUID
     * @param senderName BAN issued admin's name
     * @param senderUUID BAN issued admin's UUID
     * @param reason BAN reason
     * @param duration Banning length duration (intValue)
     * @param measure Banning length measure (m(minute), h(hour), d(day), w(week))
     */
    public void tempBan(String targetName, String targetUUID, String senderName, String senderUUID, String reason, String duration, String measure){
        plugin.getLog().info("Plugin " + pname + " tried to temp ban player " + targetName);

        reason = (reason == null || reason.isEmpty()) ? plugin.getConfigs().getDefaultTemp() : reason;
        duration = (duration == null) ? "" : duration;
        measure = (measure == null) ? "" : measure;
        this.ban(BanType.TEMP, targetName, targetUUID, senderName, senderUUID, reason, duration, measure);
    }

    /**
     * Remove BAN
     * @param targetName UnBan target player's name
     * @param senderName UnBan issued admin's name
     * @param senderUUID UnBan issued admin's UUID
     */
    public void unBan(String targetName, String targetUUID, String senderName, String senderUUID){
        plugin.getLog().info("Plugin " + pname + " tried to unban player " + targetName);
        if (targetName == null || senderName == null){
            plugin.getLog().info("Invalid usage (null): unBan");
            return;
        }
        if (!Util.isValidName(targetName)){
            plugin.getLog().info("The target you are trying to unban is not a valid name format!");
            return;
        }

        this.ban(BanType.UNBAN, targetName, targetUUID, senderName, senderUUID, "", "", "");
    }
    
    /**
     * IP Ban — not supported in this version.
     */
    public void ipBan(String ip, String senderName, String senderUUID, String reason){
        plugin.getLog().info("ipBan is not supported in this version.");
    }

    /**
     * IP Ban — not supported in this version.
     */
    public void ipBan(String ip, String senderName, String reason){
        this.ipBan(ip, senderName, "", reason);
    }

    /**
     * Kick Player
     * @param targetName Kick target player's name
     * @param senderName Kick issued admin's name
     * @param reason Kick reason
     */
    public void kick(String targetName, String targetUUID, String senderName, String senderUUID, String reason){
        plugin.getLog().info("Plugin " + pname + " tried to kick player " + targetName);
        reason = (reason == null || reason.isEmpty()) ? plugin.getConfigs().getDefaultKick() : reason;

        // Start
        Kick kickPlayer = new Kick(plugin, targetName, targetUUID, senderName, senderUUID, reason, true);
        kickPlayer.run();
    }

    /**
     * Lookup Player — not supported in this version.
     */
    public void lookupPlayer(String targetName, String targetUUID, String senderName, String senderUUID){
        plugin.getLog().info("lookupPlayer is not supported in this version.");
    }

    /**
     * Lookup Ban — not supported in this version.
     */
    public void lookupBan(int banID){
        plugin.getLog().info("lookupBan is not supported in this version.");
    }
    
    /**
     * Lookup Alt Accounts — not supported in this version.
     */
    public void lookupAlt(String playerName){
        plugin.getLog().info("lookupAlt is not supported in this version.");
    }

    /**
     * Get MCBans plugin version
     * @return plugin version
     */
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    private static HashMap<Plugin, MCBansAPI> apiHandles = new HashMap<>();
    public static MCBansAPI getHandle(final MCBans plugin, final Plugin otherPlugin){
        if (otherPlugin == null) return null;

        MCBansAPI api = apiHandles.get(otherPlugin);

        if (api == null){
            // get new api
            api = new MCBansAPI(plugin, otherPlugin.getName());

            apiHandles.put(otherPlugin, api);
        }

        return api;
    }
}
