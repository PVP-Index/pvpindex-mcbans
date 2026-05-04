package com.pvpindex.bans.plugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.pvpindex.bans.plugin.permission.Perms;
import com.pvpindex.bans.plugin.util.Util;

import static com.pvpindex.bans.plugin.I18n.localize;

public class CommandBanlookup extends BaseCommand{
    public CommandBanlookup(){
        bePlayer = false;
        name = "banlookup";
        argLength = 1;
        usage = "lookup a player's ban history";
        banning = false;
    }

    @Override
    public void execute() {
target = args.remove(0);
        if (!Util.isInteger(target) || Integer.parseInt(target) < 0){
            Util.message(sender, localize("formatError"));
            return;
        }
        sender.sendMessage(ChatColor.YELLOW + "Ban lookup is not supported in this version.");
    }

    @Override
    public boolean permission(CommandSender sender) {
        return Perms.LOOKUP_BAN.has(sender);
    }
}