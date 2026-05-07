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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.pvpindex.bans.plugin.BanType;
import com.pvpindex.bans.plugin.MCBans;
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

        String reason = config.getDefaultLocal();
        if (args.size() > 0){
            reason = config.resolveReason(Util.join(args, " "));
        }
        Ban banControl = new Ban(plugin, BanType.LOCAL.getActionName(), target, targetUUID, targetIP, senderName, senderUUID, reason, "", "", null, false);
        banControl.run();
    }

    @Override
    protected List<String> tabComplete(MCBans plugin, CommandSender sender, String cmd, String[] preArgs) {
        if (preArgs.length >= 2) {
            return presetsCompletion(plugin, preArgs[preArgs.length - 1]);
        }
        return null;
    }

    /** Returns all preset keys matching {@code partial} as "#key" suggestions.
     *  Shows all presets on empty input; filters case-insensitively on "#prefix".
     *  Returns an empty list (not null) when the user is typing a custom reason so
     *  Paper does not fall back to suggesting online player names. */
    static List<String> presetsCompletion(MCBans plugin, String partial) {
        if (!partial.isEmpty() && !partial.startsWith("#")) {
            return List.of(); // custom reason: suppress player-name fallback
        }
        String lprefix = partial.startsWith("#")
                ? partial.substring(1).toLowerCase(Locale.ROOT) : "";
        return plugin.getConfigs().getReasonPresetKeys().stream()
                .filter(k -> k.toLowerCase(Locale.ROOT).startsWith(lprefix))
                .sorted()
                .map(k -> "#" + k)
                .collect(Collectors.toList());
    }

    @Override
    public boolean permission(CommandSender sender) {
        return BanType.LOCAL.getPermission().has(sender);
    }
}
