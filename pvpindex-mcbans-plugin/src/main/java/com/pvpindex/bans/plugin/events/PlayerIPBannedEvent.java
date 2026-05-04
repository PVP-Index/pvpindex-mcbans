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

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerIPBannedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private String ip;
    private String sender;
    private String reason, senderUUID;

    public PlayerIPBannedEvent(String ip, String sender, String senderUUID, String reason) {
        this.ip = ip;
        this.sender = sender;
        this.reason = reason;
        this.senderUUID = senderUUID;
    }

    public UUID getSenderUUID() {
		return UUID.fromString(senderUUID);
	}
    
    public String getIP() {
        return this.ip;
    }

    public String getSenderName() {
        return this.sender;
    }

    public String getReason() {
        return this.reason;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
