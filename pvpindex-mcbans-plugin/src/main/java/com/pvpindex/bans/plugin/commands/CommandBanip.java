package com.pvpindex.bans.plugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.permission.Perms;

public class CommandBanip extends BaseCommand{
    public CommandBanip(){
        bePlayer = false;
        name = "banip";
        argLength = 1;
        usage = "ban an IP address";
        banning = true;
    }

    @Override
    public void execute() throws CommandException {
args.remove(0);
        sender.sendMessage(ChatColor.YELLOW + "IP ban is not supported in this version.");
    }

    @Override
    public boolean permission(CommandSender sender) {
        return Perms.BAN_IP.has(sender);
    }
}
