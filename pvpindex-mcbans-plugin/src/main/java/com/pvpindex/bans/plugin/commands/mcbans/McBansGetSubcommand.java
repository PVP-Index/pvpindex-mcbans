package com.pvpindex.bans.plugin.commands.mcbans;

import com.pvpindex.bans.plugin.ConfigurationManager;
import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.plugin.Registry;
import com.pvpindex.bans.plugin.exception.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles {@code /mcbans get [call|sync]}.
 */
public class McBansGetSubcommand extends McBansSubcommand {

    public McBansGetSubcommand(
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
            rootGet();
            return;
        }
        switch (args.remove(0).toLowerCase()) {
            case "call":
                getCall();
                break;
            case "sync":
                getSync();
                break;
            default:
                rootGet();
                break;
        }
    }

    private void rootGet() {
        send(ChatColor.WHITE + "/mcbans get call" + ChatColor.BLUE
                + " Time until callback thread sends data.");
        send(ChatColor.WHITE + "/mcbans get sync" + ChatColor.BLUE
                + " Time until next sync.");
    }

    private void getCall() {
        send(ChatColor.GOLD + "Callback-based sync is not used in this version.");
    }

    private void getSync() {
        if (config.isEnableAutoSync()) {
            long syncInterval = 60 * config.getSyncInterval();
            if (syncInterval < (60 * 5)) {
                syncInterval = (60 * 5);
            }
            final String remainStr = timeRemain(
                    (Registry.getBanSync().lastSync + syncInterval) - (System.currentTimeMillis() / 1000));
            if (remainStr != null) {
                send(ChatColor.GOLD + remainStr + " until next sync.");
            } else {
                send(ChatColor.GOLD + "Ban sync is in progress...");
            }
        } else {
            send(ChatColor.RED + "Auto sync is disabled by config.yml!");
        }
    }

    /**
     * Formats a duration in seconds into a human-readable string.
     *
     * @return formatted string, or {@code null} if {@code remain} is &lt;= 0.
     */
    String timeRemain(long remain) {
        if (remain <= 0) {
            return null;
        }
        try {
            String format = "";
            long timeRemaining = remain;
            long sec = timeRemaining % 60;
            long min = (timeRemaining / 60) % 60;
            long hours = (timeRemaining / (60 * 60)) % 24;
            long days = (timeRemaining / (60 * 60 * 24)) % 7;
            long weeks = (timeRemaining / (60 * 60 * 24 * 7));
            if (sec != 0) {
                format = sec + " seconds";
            }
            if (min != 0) {
                format = min + " minutes " + format;
            }
            if (hours != 0) {
                format = hours + " hours " + format;
            }
            if (days != 0) {
                format = days + " days " + format;
            }
            if (weeks != 0) {
                format = weeks + " weeks " + format;
            }
            return format;
        } catch (ArithmeticException e) {
            if (config.isDebug()) {
                e.printStackTrace();
            }
            return "error";
        }
    }
}
