package com.pvpindex.bans.plugin.commands.mcbans;

import com.pvpindex.bans.plugin.ConfigurationManager;
import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.plugin.exception.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles {@code /mcbans perms [ban|view|exempt|others]}.
 */
public class McBansPermsSubcommand extends McBansSubcommand {

    public McBansPermsSubcommand(
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
            rootPerms();
            return;
        }
        switch (args.remove(0).toLowerCase()) {
            case "ban":
                banPerms();
                break;
            case "view":
                viewPerms();
                break;
            case "exempt":
                exemptPerms();
                break;
            case "others":
                otherPerms();
                break;
            default:
                rootPerms();
                break;
        }
    }

    private void rootPerms() {
        send("&f/mcbans perms ban" + ChatColor.BLUE + " Banning/kick permissions");
        send("&f/mcbans perms exempt" + ChatColor.BLUE + " Exemptions from kick/ban");
        send("&f/mcbans perms view" + ChatColor.BLUE + " On connect bans/bans/alts");
        send("&f/mcbans perms others" + ChatColor.BLUE + " Lookups");
        send(ChatColor.GOLD + "mcbans.admin" + ChatColor.BLUE
                + " Grants complete MCBans admin permission");
    }

    private void banPerms() {
        send(ChatColor.GOLD + "mcbans.ban.global" + ChatColor.BLUE + " Grants global ban permissions");
        send(ChatColor.GOLD + "mcbans.ban.local" + ChatColor.BLUE + " Grants local ban permissions");
        send(ChatColor.GOLD + "mcbans.ban.temp" + ChatColor.BLUE + " Grants temporary ban permissions");
        send(ChatColor.GOLD + "mcbans.ban.rollback" + ChatColor.BLUE + " Grants rollback ban permissions");
        send(ChatColor.GOLD + "mcbans.ban.ip" + ChatColor.BLUE + " Grants IP ban permissions");
        send(ChatColor.GOLD + "mcbans.unban" + ChatColor.BLUE + " Grants unban permissions");
        send(ChatColor.GOLD + "mcbans.kick" + ChatColor.BLUE + " Grants kick permissions");
    }

    private void viewPerms() {
        send(ChatColor.GOLD + "mcbans.view.alts" + ChatColor.BLUE
                + " View players alts on connect {premium only}");
        send(ChatColor.GOLD + "mcbans.view.bans" + ChatColor.BLUE
                + " View players bans on connect");
        send(ChatColor.GOLD + "mcbans.view.staff" + ChatColor.BLUE
                + " View if player is MCBans Staff on connect");
        send(ChatColor.GOLD + "mcbans.view.previous" + ChatColor.BLUE
                + " View players previous names on connect");
        send(ChatColor.GOLD + "mcbans.announce" + ChatColor.BLUE
                + " View if the player is banned/kicked");
    }

    private void exemptPerms() {
        send(ChatColor.GOLD + "mcbans.kick.exempt" + ChatColor.BLUE
                + " Player cannot be kicked at all");
        send(ChatColor.GOLD + "mcbans.ban.exempt" + ChatColor.BLUE
                + " Player cannot be banned at all");
    }

    private void otherPerms() {
        send(ChatColor.GOLD + "mcbans.lookup.player" + ChatColor.BLUE
                + " Grants lookup player command");
        send(ChatColor.GOLD + "mcbans.lookup.ban" + ChatColor.BLUE
                + " Grants lookup ban command");
        send(ChatColor.GOLD + "mcbans.lookup.alt" + ChatColor.BLUE
                + " Grants lookup alternate accounts command");
    }
}
