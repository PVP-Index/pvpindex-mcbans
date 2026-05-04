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

import com.pvpindex.bans.plugin.MCBans;
import org.bukkit.command.CommandSender;

import com.pvpindex.bans.plugin.BanType;
import com.pvpindex.bans.plugin.request.Ban;
import com.pvpindex.bans.plugin.util.Util;

import java.util.List;
import java.util.stream.Collectors;


public class CommandGlobalban extends BaseCommand{
    public CommandGlobalban(){
        bePlayer = false;
        name = "globalban";
        argLength = 2;
        usage = "global ban a player";
        banning = true;
    }

    @Override
    public void execute() {
        args.remove(0); // remove target

        if(args.size()<0){
            Util.message(sender, "A reason is required for a global ban.");
            return;
        }
        // build reason
        String reason = Util.join(args, " ");
        
        // Start
        Ban banControl = new Ban(plugin, BanType.GLOBAL.getActionName(), target, targetUUID, targetIP, senderName, senderUUID, reason, "", "", null, false);
        banControl.run();
    }

    @Override
    protected List<String> tabComplete(MCBans plugin, CommandSender sender, String cmd, String[] preArgs) {
        if(preArgs.length==1){
            return plugin.getServer().getOnlinePlayers().stream().filter(player->player.getName().startsWith(preArgs[0])).map(player->player.getName()).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public boolean permission(CommandSender sender) {
        return BanType.GLOBAL.getPermission().has(sender);
    }
}
