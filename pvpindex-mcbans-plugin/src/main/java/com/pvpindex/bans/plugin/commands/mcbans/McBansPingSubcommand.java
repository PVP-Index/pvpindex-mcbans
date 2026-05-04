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
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.permission.Perms;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static com.pvpindex.bans.plugin.I18n.localize;

/**
 * Handles {@code /mcbans ping}.
 */
public class McBansPingSubcommand extends McBansSubcommand {

    public McBansPingSubcommand(
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
        send(ChatColor.YELLOW + "Ping not supported in this version.");
    }
}
