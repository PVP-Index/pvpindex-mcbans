/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.commands.mcbans;

import com.pvpindex.bans.plugin.ConfigurationManager;
import com.pvpindex.bans.plugin.I18n;
import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.plugin.Registry;
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.permission.Perms;
import com.pvpindex.bans.plugin.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

import static com.pvpindex.bans.plugin.I18n.localize;

/**
 * Handles {@code /mcbans staff [perms|debug|verify]}.
 *
 * <p>Note: the staff command is intentionally restricted (the {@code senderPlayer != null}
 * guard is preserved from the original implementation).</p>
 */
public class McBansStaffSubcommand extends McBansSubcommand {

    public McBansStaffSubcommand(
            MCBans plugin,
            CommandSender sender,
            Player senderPlayer,
            ConfigurationManager config,
            List<String> args) {
        super(plugin, sender, senderPlayer, config, args);
    }

    @Override
    public void execute() throws CommandException {
        // Preserve original behaviour: staff command is only active for in-game players
        // and the second condition is intentionally hard-coded to false (feature flag).
        if (senderPlayer == null || !false) { // NOSONAR intentional feature flag
            return;
        }
        if (args.isEmpty()) {
            rootStaff();
            return;
        }
        switch (args.remove(0).toLowerCase()) {
            case "perms":
                staffPerms();
                break;
            case "debug":
                staffDebug();
                break;
            case "verify":
                staffVerify();
                break;
            default:
                rootStaff();
                break;
        }
    }

    private void rootStaff() {
        send("&6-=== Server Settings ===-");
        send("&6Valid API Key: &e" + config.isValidApiKey()
                + "&6 Permissions: &e" + config.getPermission());
        send("&6AutoSync: &e" + config.isEnableAutoSync()
                + "&6 Sync Interval: &e" + config.getSyncInterval() + "m");
        send("&6Max Alts: &e" + config.isEnableMaxAlts() + " (" + config.getMaxAlts() + ")");
        send("&6isDebug: &e" + config.isDebug() + "&6 Log: &e" + config.isEnableLog());

        send("&6-=== Server Status ===-");
        send("&6MCBans Plugin: &e" + plugin.getDescription().getVersion());
        send("&6Name: &e" + Bukkit.getServer().getName()
                + "&6 IP: &e" + Bukkit.getServer().getIp() + ":" + Bukkit.getServer().getPort());
        send("&6OnlineMode: &e" + Bukkit.getOnlineMode());

        send("&6-=== Online Players ===-");
        send("&6mcbans.admin: &e" + Util.join(Perms.ADMIN.getPlayerNames(), ", "));
        send("&6mcbans.ban.global: &e" + Util.join(Perms.BAN_GLOBAL.getPlayerNames(), ", "));
    }

    private void staffPerms() {
        send("&6-=== All Online Players Perms ===-");
        for (Perms perm : Perms.values()) {
            send("&6" + perm.getNode() + ": &e" + Util.join(perm.getPlayerNames(), ", "));
        }
    }

    private void staffDebug() {
        send("&6-=== Debug Information ===-");
        send("&6Spigot Version: &e" + Bukkit.getVersion());
        send("&6Build: &e" + Bukkit.getBukkitVersion());
        send("&6last_sync: &e" + Registry.getBanSync().lastSync + " &6Sync Running: &e" + Registry.getBanSync().syncRunning);
    }

    private void staffVerify() {
        Util.message(Bukkit.getConsoleSender(),
                ChatColor.AQUA + senderPlayer.getName() + " is an MCBans staff member.");
        Set<Player> players = Perms.VIEW_STAFF.getPlayers();
        players.addAll(Perms.ADMIN.getPlayers());
        players.addAll(Perms.BAN_GLOBAL.getPlayers());
        for (Player p : players) {
            Util.message(p, ChatColor.AQUA + localize("isMCBansMod", I18n.PLAYER, senderPlayer.getName()));
        }
    }
}
