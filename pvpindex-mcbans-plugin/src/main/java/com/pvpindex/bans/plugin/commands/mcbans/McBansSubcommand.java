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
import com.pvpindex.bans.plugin.util.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Abstract base for all /mcbans sub-command handlers.
 *
 * <p>Each subclass receives the execution context from {@code CommandMCBans.execute()}
 * and is responsible for handling its own argument routing.</p>
 */
public abstract class McBansSubcommand {

    protected final MCBans plugin;
    protected final CommandSender sender;
    protected final Player senderPlayer;
    protected final ConfigurationManager config;
    protected final List<String> args;

    protected McBansSubcommand(
            MCBans plugin,
            CommandSender sender,
            Player senderPlayer,
            ConfigurationManager config,
            List<String> args) {
        this.plugin = plugin;
        this.sender = sender;
        this.senderPlayer = senderPlayer;
        this.config = config;
        this.args = args;
    }

    /**
     * Execute this sub-command. May throw {@link CommandException} to surface an
     * error message to the sender.
     */
    public abstract void execute() throws CommandException;

    /**
     * Convenience: send a colour-formatted message to the originating sender.
     */
    protected void send(String msg) {
        Util.message(sender, Util.color(msg));
    }
}
