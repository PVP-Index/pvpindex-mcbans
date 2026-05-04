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
import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.plugin.Registry;
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.permission.Perms;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static com.pvpindex.bans.plugin.I18n.localize;

/**
 * Handles {@code /mcbans sync [all]}.
 */
public class McBansSyncSubcommand extends McBansSubcommand {

    public McBansSyncSubcommand(
            MCBans plugin,
            CommandSender sender,
            Player senderPlayer,
            ConfigurationManager config,
            List<String> args) {
        super(plugin, sender, senderPlayer, config, args);
    }

    @Override
    public void execute() throws CommandException {
        if (!Perms.ADMIN.has(sender)) {
            throw new CommandException(ChatColor.RED + localize("permissionDenied"));
        }
        if (args.isEmpty()) {
            rootSync();
        } else {
            switch (args.remove(0).toLowerCase()) {
                case "all":
                    syncAll();
                    break;
                default:
                    rootSync();
                    break;
            }
        }
    }

    private void rootSync() {
        send(ChatColor.GREEN + "Triggering ban sync...");
        new Thread(() -> Registry.getBanSync().performSync(), "MCBans-ManualSync").start();
    }

    private void syncAll() {
        send(ChatColor.GREEN + "Triggering full ban sync...");
        new Thread(() -> Registry.getBanSync().performSync(), "MCBans-FullSync").start();
    }
}
