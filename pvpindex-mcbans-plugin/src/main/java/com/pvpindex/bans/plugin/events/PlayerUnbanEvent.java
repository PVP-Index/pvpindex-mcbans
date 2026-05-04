/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.events;

import java.util.UUID;

import com.pvpindex.bans.plugin.util.Util;
import com.pvpindex.bans.utils.IPTools;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerUnbanEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled = false;

    private String target,targetUUID;
    private String sender,senderUUID;

    public PlayerUnbanEvent(String target, String targetUUID, String sender, String senderUUID) {
        this.target = target;
        this.sender = sender;
        this.senderUUID = senderUUID;
        this.targetUUID = targetUUID;
    }
    
    public UUID getSenderUUID() {
		return UUID.fromString(senderUUID);
	}

    public String getTargetName() {
        return this.target;
    }
    
    @Deprecated
    public String getPlayerName() {
        return this.getTargetName();
    }

    public String getSenderName() {
        return this.sender;
    }

    public void setSenderName(String senderName) {
        this.sender = senderName;
    }
    
    public boolean isIPBan(){
        return IPTools.validBanIP(this.target);
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
