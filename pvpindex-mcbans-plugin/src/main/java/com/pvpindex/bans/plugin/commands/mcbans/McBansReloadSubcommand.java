package com.pvpindex.bans.plugin.commands.mcbans;

import com.pvpindex.bans.plugin.ConfigurationManager;
import com.pvpindex.bans.plugin.I18n;
import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.permission.Perms;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static com.pvpindex.bans.plugin.I18n.localize;

/**
 * Handles {@code /mcbans reload}.
 */
public class McBansReloadSubcommand extends McBansSubcommand {

    public McBansReloadSubcommand(
            MCBans plugin,
            CommandSender sender,
            Player senderPlayer,
            ConfigurationManager config,
            List<String> args) {
        super(plugin, sender, senderPlayer, config, args);
    }

    @Override
    public void execute() throws CommandException {
        if (!Perms.ADMIN.has(sender)) {
            throw new CommandException(ChatColor.RED + localize("permissionDenied"));
        }
        send(ChatColor.AQUA + "Reloading configuration...");
        try {
            config.loadConfig(false);
            send(ChatColor.GREEN + "Reload complete.");
        } catch (Exception ex) {
            send(ChatColor.RED + "An error occurred while trying to load the config file.");
        }
        send(ChatColor.AQUA + "Reloading language file...");
        try {
            I18n.extractLanguageFiles(false);
            I18n.setCurrentLanguage(config.getLanguage());
            send(ChatColor.GREEN + "Reload complete.");
        } catch (Exception ex) {
            send(ChatColor.RED + "An error occurred while trying to load the language file.");
        }
    }
}
