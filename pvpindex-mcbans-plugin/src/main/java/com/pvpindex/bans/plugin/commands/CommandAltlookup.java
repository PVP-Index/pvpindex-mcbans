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
import com.pvpindex.bans.plugin.permission.Perms;

public class CommandAltlookup extends BaseCommand{
    public CommandAltlookup(){
        bePlayer = false;
        name = "altlookup";
        argLength = 1;
        usage = "lookup a player's alternate accounts";
        banning = true;
    }

    @Override
    public void execute() {
        args.remove(0);
        sender.sendMessage(ChatColor.YELLOW + "Alt lookup is not supported in this version.");
    }

    @Override
    public boolean permission(CommandSender sender) {
        return Perms.LOOKUP_ALT.has(sender);
    }
}
