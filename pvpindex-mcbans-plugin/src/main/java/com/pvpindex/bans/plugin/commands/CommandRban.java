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

import static com.pvpindex.bans.plugin.I18n.localize;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.pvpindex.bans.plugin.BanType;
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.permission.Perms;
import com.pvpindex.bans.plugin.request.Ban;
import com.pvpindex.bans.plugin.util.Util;

public class CommandRban extends BaseCommand{
    public CommandRban(){
        bePlayer = false;
        name = "rban";
        argLength = 1;
        usage = "ban a player and rollback their block changes";
        banning = true;
    }

    @Override
    public void execute() throws CommandException {
        args.remove(0); // remove target

        // check BanType
        BanType type = BanType.LOCAL;
        if (args.size() > 0) {
            if (args.get(0).equalsIgnoreCase("g")){
                type = BanType.GLOBAL;
            }else if (args.get(0).equalsIgnoreCase("t")){
                type = BanType.TEMP;
            }
        }
        if (type != BanType.LOCAL){
            args.remove(0);
        }

        // check permission
        if (!type.getPermission().has(sender)){
            throw new CommandException(ChatColor.RED + localize("permissionDenied"));
        }

        String reason = null;
        Ban banControl = null;
		switch (type){
            case LOCAL:
                reason = config.getDefaultLocal();
                if (args.size() > 0){
                    reason = config.resolveReason(Util.join(args, " "));
                }
                banControl = new Ban(plugin, type.getActionName(), target, targetUUID, targetIP, senderName, senderUUID, reason, "", "", null, true);
                break;

            case GLOBAL:
                if (args.size() == 0){
                    Util.message(sender, ChatColor.RED + localize("formatError"));
                    return;
                }
                reason = config.resolveReason(Util.join(args, " "));
                banControl = new Ban(plugin, type.getActionName(), target, targetUUID, targetIP, senderName, senderUUID, reason, "", "", null, true);
                break;

            case TEMP:
                if (args.size() <= 2){
                    Util.message(sender, ChatColor.RED + localize("formatError"));
                    return;
                }
                String measure = "";
                String duration = args.remove(0);
                if(!duration.matches("(?sim)([0-9]+)(minute(s|)|m|hour(s|)|h|day(s|)|d|week(s|)|w)")){
                	measure = args.remove(0);
                }else{
                	try {
                		Pattern regex = Pattern.compile("([0-9]+)(minute(s|)|m|hour(s|)|h|day(s|)|d|week(s|)|w)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                		Matcher regexMatcher = regex.matcher(duration);
                		if (regexMatcher.find()) {
                			duration = regexMatcher.group(1);
                			measure = regexMatcher.group(2);
                		} 
                	} catch (PatternSyntaxException ex) {}
                }
                reason = config.getDefaultTemp();
                if (args.size() > 0){
                    reason = config.resolveReason(Util.join(args, " "));
                }
                banControl = new Ban(plugin, type.getActionName(), target, targetUUID, targetIP, senderName, senderUUID, reason, duration, measure, null, true);
                break;
        }

        // Start
        if (banControl == null){
            Util.message(sender, ChatColor.RED + "Internal error! Please report console logs!");
            throw new RuntimeException("Undefined BanType: " + type.name());
        }
        banControl.run();
    }

    @Override
    public boolean permission(CommandSender sender) {
        return Perms.BAN_ROLLBACK.has(sender);
    }
}
