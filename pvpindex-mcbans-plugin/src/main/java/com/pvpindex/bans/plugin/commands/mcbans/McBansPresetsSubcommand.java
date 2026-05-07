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
import com.pvpindex.bans.plugin.permission.Perms;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static com.pvpindex.bans.plugin.I18n.localize;

/**
 * Handles {@code /mcbans presets}.
 *
 * <p>Lists all reason presets defined in {@code config.yml} so staff can
 * quickly see what {@code #key} shortcuts are available when banning.</p>
 */
public class McBansPresetsSubcommand extends McBansSubcommand {

    public McBansPresetsSubcommand(
            MCBans plugin,
            CommandSender sender,
            Player senderPlayer,
            ConfigurationManager config,
            List<String> args) {
        super(plugin, sender, senderPlayer, config, args);
    }

    @Override
    public void execute() throws CommandException {
        if (!Perms.BAN_LOCAL.has(sender)) {
            throw new CommandException(ChatColor.RED + localize("permissionDenied"));
        }

        List<String> keys = config.getReasonPresetKeys();
        if (keys.isEmpty()) {
            send("&eNo reason presets are defined. Add them under &6reason-presets&e in config.yml.");
            return;
        }

        send("&b--- Reason Presets (" + keys.size() + " defined) ---");
        for (String key : keys) {
            String reason = config.resolveReason("#" + key);
            String durSuffix = "";
            String defaultDur = config.getPresetDefaultDuration(key);
            if (defaultDur != null) {
                durSuffix = " &7[default: " + defaultDur + "]";
            }
            if (reason.startsWith("#")) {
                // Misconfigured - no reason field
                send("  &c#" + key + " &7\u2192 &4(missing reason field)" + durSuffix);
            } else {
                send("  &f#" + key + " &7\u2192 &e" + reason + durSuffix);
            }
        }
        send("&7Use &f#key&7 as the reason in /ban, /gban, /tban, /rban.");
    }
}
