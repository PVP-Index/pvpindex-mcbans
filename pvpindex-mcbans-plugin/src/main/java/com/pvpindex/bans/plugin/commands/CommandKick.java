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

import com.pvpindex.bans.plugin.request.Kick;
import org.bukkit.command.CommandSender;

import com.pvpindex.bans.plugin.permission.Perms;
import com.pvpindex.bans.plugin.util.Util;

public class CommandKick extends BaseCommand{
    public CommandKick(){
        bePlayer = false;
        name = "kick";
        argLength = 1;
        usage = "kick a player from the server";
        banning = true;
    }

    @Override
    public void execute() {
        args.remove(0); // remove target
        
        // build reason
        String reason = config.getDefaultKick();
        if (args.size() > 0){
            reason = Util.join(args, " ");
        }

        // Start
        Kick kickPlayer = new Kick(plugin, target, targetUUID, senderName, senderUUID, reason, false);
        kickPlayer.run();
    }

    @Override
    public boolean permission(CommandSender sender) {
        return Perms.KICK.has(sender);
    }
}
