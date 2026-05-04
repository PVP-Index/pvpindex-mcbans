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

import static com.pvpindex.bans.plugin.I18n.localize;

public class CommandMCBansSettings extends BaseCommand {
	public CommandMCBansSettings(){
        bePlayer = false;
        name = "mcbs";
        argLength = 0;
        usage = "mcbs <setting> <value>";
        banning = true;
    }
	@Override
	public void execute() throws CommandException {
		if (!this.permission(sender)){
            throw new CommandException(ChatColor.RED + localize("permissionDenied"));
        }
		sender.sendMessage(ChatColor.YELLOW + "Server settings command is not supported in this version.");
	}

	@Override
	public boolean permission(CommandSender sender) {
		return Perms.ADMIN.has(sender);
	}

}
