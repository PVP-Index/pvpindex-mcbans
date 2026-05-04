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
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles {@code /mcbans}, {@code /mcbans banning}, and {@code /mcbans user}.
 *
 * <p>When {@code args} is empty, shows the root help index.  When the first
 * element is {@code "banning"} or {@code "user"} it shows the corresponding
 * section (the element is consumed inside {@link #execute()}).</p>
 */
public class McBansHelpSubcommand extends McBansSubcommand {

    public McBansHelpSubcommand(
            MCBans plugin,
            CommandSender sender,
            Player senderPlayer,
            ConfigurationManager config,
            List<String> args) {
        super(plugin, sender, senderPlayer, config, args);
    }

    @Override
    public void execute() throws CommandException {
        if (args.isEmpty()) {
            rootMCBans();
            return;
        }
        switch (args.remove(0).toLowerCase()) {
            case "banning":
                banningHelp();
                break;
            case "user":
                userHelp();
                break;
            default:
                rootMCBans();
                break;
        }
    }

    private void rootMCBans() {
        send("&bMCBans &3" + plugin.getDescription().getVersion()
                + "&b Help &f|| &b<> &f= required, &b[] &f= optional");
        send("&f/mcbans banning" + ChatColor.BLUE + " Help with banning/unbanning commands");
        send("&f/mcbans user" + ChatColor.BLUE + " Help with user management commands");
        send("&f/mcbans perms" + ChatColor.BLUE + " Permission list for MCBans");
        send("&f/mcbans get" + ChatColor.BLUE + " Get time till next API call");
        send("&f/mcbans ping" + ChatColor.BLUE + " Check overall response time from API");
        send("&f/mcbans sync" + ChatColor.BLUE + " Force a sync to occur with MCBans API");
        send("&f/mcbans reload" + ChatColor.BLUE + " Reload settings and language files");
    }

    private void banningHelp() {
        send("&f------------------------------------------");
        send("&f/ban <name|uuid> [reason]" + ChatColor.BLUE + " Local ban a player");
        send("&f/ban <name|uuid> -g <reason>" + ChatColor.BLUE + " Global ban a player");
        send("&f/ban <name|uuid> -t <time> <m, h, d, w> <reason>"
                + ChatColor.BLUE + " Temporarily ban a player");
        send("&f/tban <name|uuid> <time> <m(minute), h(hour), d(day), w(week)> [reason]"
                + ChatColor.BLUE + " Temp ban a player");
        send("&f/gban <name|uuid> <reason>" + ChatColor.BLUE + " Global ban a player");
        send("&f/rban <name|uuid> [reason]" + ChatColor.BLUE + " Rollback and local ban a player");
        send("&f/rban <name|uuid> -g <reason>"
                + ChatColor.BLUE + " Rollback and global ban a player");
        send("&f/rban <name|uuid> -t <time> <m, h, d, w> <reason>"
                + ChatColor.BLUE + " Rollback and temporarily ban a player");
        send("&f/banip <ip> [reason]" + ChatColor.BLUE + " Bans an IP address");
        send("&f/unban <name|ip|uuid>" + ChatColor.BLUE + " Bans an IP address");
    }

    private void userHelp() {
        send("&f------------------------------------------");
        send("&f/lookup <name|uuid>" + ChatColor.BLUE + " Lookup the player ban information");
        send("&f/banlookup <banID>" + ChatColor.BLUE + " Lookup the player ban information");
        send("&f/altlookup <name>" + ChatColor.BLUE + " Lookup the alt account information");
        send("&f/kick <name> [reason]" + ChatColor.BLUE + " Kick player from the server");
    }
}
