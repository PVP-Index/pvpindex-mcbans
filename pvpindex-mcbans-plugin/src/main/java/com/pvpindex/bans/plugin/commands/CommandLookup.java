package com.pvpindex.bans.plugin.commands;


import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.pvpindex.bans.plugin.permission.Perms;

public class CommandLookup extends BaseCommand{
    public CommandLookup(){
        bePlayer = false;
        name = "lookup";
        argLength = 1;
        usage = "lookup a player's ban history";
        banning = true;
    }

    @Override
    public void execute() {
args.remove(0);
        sender.sendMessage(ChatColor.YELLOW + "Player lookup is not supported in this version.");
    }

    @Override
    public boolean permission(CommandSender sender) {
        return Perms.LOOKUP_PLAYER.has(sender);
    }
}
