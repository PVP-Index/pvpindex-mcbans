package com.pvpindex.bans.plugin.commands;


import org.bukkit.command.CommandSender;

import com.pvpindex.bans.plugin.BanType;
import com.pvpindex.bans.plugin.exception.CommandException;
import com.pvpindex.bans.plugin.request.Ban;

public class CommandUnban extends BaseCommand{
    public CommandUnban(){
        bePlayer = false;
        name = "unban";
        argLength = 1;
        usage = "unban a player/uuid or IP";
        banning = true;
    }

    @Override
    public void execute() throws CommandException {
    	args.remove(0); // remove target

        //String target = args.get(0).trim(); already fetched in BaseCommand
        
        // Start
        Ban banControl = new Ban(plugin, BanType.UNBAN.getActionName(), target, targetUUID, "", senderName, senderUUID, "", "", "", null, false);
        banControl.run();
    }

    @Override
    public boolean permission(CommandSender sender) {
        return BanType.UNBAN.getPermission().has(sender);
    }
}
