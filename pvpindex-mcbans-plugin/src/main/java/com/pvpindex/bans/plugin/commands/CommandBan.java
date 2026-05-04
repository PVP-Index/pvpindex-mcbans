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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.pvpindex.bans.plugin.BanType;
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.request.Ban;
import com.pvpindex.bans.plugin.util.Util;

import static com.pvpindex.bans.plugin.I18n.localize;

public class CommandBan extends BaseCommand{
    public CommandBan(){
        bePlayer = false;
        name = "ban";
        argLength = 1;
        usage = "ban a player";
        banning = true;
    }

    @Override
    public void execute() throws CommandException {
        args.remove(0); //remove target

        // check BanType
        BanType type = BanType.LOCAL;

        String reason = null;
        Ban banControl = null;
        reason = config.getDefaultLocal();
        if (args.size() > 0){
            reason = Util.join(args, " ");
        }
        banControl = new Ban(plugin, type.getActionName(), target, targetUUID, targetIP, senderName, senderUUID, reason, "", "", null, false);
        banControl.run();
    }

    @Override
    public boolean permission(CommandSender sender) {
        return BanType.LOCAL.getPermission().has(sender);
    }
}
