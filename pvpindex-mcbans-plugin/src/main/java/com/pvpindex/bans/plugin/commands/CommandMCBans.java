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


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.plugin.commands.mcbans.McBansDownloadSubcommand;
import com.pvpindex.bans.plugin.commands.mcbans.McBansGetSubcommand;
import com.pvpindex.bans.plugin.commands.mcbans.McBansHelpSubcommand;
import com.pvpindex.bans.plugin.commands.mcbans.McBansPermsSubcommand;
import com.pvpindex.bans.plugin.commands.mcbans.McBansPingSubcommand;
import com.pvpindex.bans.plugin.commands.mcbans.McBansPresetsSubcommand;
import com.pvpindex.bans.plugin.commands.mcbans.McBansReloadSubcommand;
import com.pvpindex.bans.plugin.commands.mcbans.McBansStaffSubcommand;
import com.pvpindex.bans.plugin.commands.mcbans.McBansSyncSubcommand;
import org.bukkit.command.CommandSender;

import com.pvpindex.bans.plugin.exception.CommandException;


public class CommandMCBans extends BaseCommand {
    public CommandMCBans() {
        bePlayer = false;
        name = "mcbans";
        argLength = 0;
        usage = "show information";
        banning = false;
    }

    @Override
    public void execute() throws CommandException {
        if (args.isEmpty()) {
            new McBansHelpSubcommand(plugin, sender, senderPlayer, config, args).execute();
            return;
        }

        final String first = args.get(0).toLowerCase();
        switch (first) {
            case "banning":
            case "user":
                new McBansHelpSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            case "download":
                args.remove(0);
                new McBansDownloadSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            case "perms":
                args.remove(0);
                new McBansPermsSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            case "ping":
                args.remove(0);
                new McBansPingSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            case "sync":
                args.remove(0);
                new McBansSyncSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            case "get":
                args.remove(0);
                new McBansGetSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            case "reload":
                args.remove(0);
                new McBansReloadSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            case "presets":
                args.remove(0);
                new McBansPresetsSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            case "staff":
                args.remove(0);
                new McBansStaffSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
            default:
                new McBansHelpSubcommand(plugin, sender, senderPlayer, config, args).execute();
                break;
        }
    }

    @Override
    public boolean permission(CommandSender sender) {
        return true;
    }

    @Override
    protected List<String> tabComplete(MCBans plugin, CommandSender sender, String cmd, String[] preArgs) {
        List<String> options = new ArrayList<>();
        if (preArgs.length == 1) {
            options.add("banning");
            options.add("user");
            options.add("perms");
            options.add("download");
            options.add("ping");
            options.add("sync");
            options.add("get");
            options.add("reload");
            options.add("presets");
            if (sender != null && false) {
                options.add("staff");
            }
        } else if (preArgs.length == 2) {
            switch (preArgs[0].toLowerCase()) {
                case "perms":
                    options.add("ban");
                    options.add("view");
                    options.add("exempt");
                    options.add("others");
                    break;
                case "sync":
                    options.add("all");
                    break;
                case "get":
                    options.add("call");
                    options.add("sync");
                    break;
                case "staff":
                    if (sender != null && false) {
                        options.add("perms");
                        options.add("debug");
                        options.add("verify");
                    }
                    break;
                default:
                    break;
            }
        }
        return options.stream()
                .filter(p -> p.startsWith(preArgs[preArgs.length - 1]))
                .collect(Collectors.toList());
    }
}
