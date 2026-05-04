/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.permission.Perms;

public class CommandBanip extends BaseCommand{
    public CommandBanip(){
        bePlayer = false;
        name = "banip";
        argLength = 1;
        usage = "ban an IP address";
        banning = true;
    }

    @Override
    public void execute() throws CommandException {
args.remove(0);
        sender.sendMessage(ChatColor.YELLOW + "IP ban is not supported in this version.");
    }

    @Override
    public boolean permission(CommandSender sender) {
        return Perms.BAN_IP.has(sender);
    }
}
