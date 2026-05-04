package com.pvpindex.bans.plugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.pvpindex.bans.plugin.permission.Perms;

public class CommandAltlookup extends BaseCommand{
    public CommandAltlookup(){
        bePlayer = false;
        name = "altlookup";
        argLength = 1;
        usage = "lookup a player's alternate accounts";
        banning = true;
    }

    @Override
    public void execute() {
        args.remove(0);
        sender.sendMessage(ChatColor.YELLOW + "Alt lookup is not supported in this version.");
    }

    @Override
    public boolean permission(CommandSender sender) {
        return Perms.LOOKUP_ALT.has(sender);
    }
}
