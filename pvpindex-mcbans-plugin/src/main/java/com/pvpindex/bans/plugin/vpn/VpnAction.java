/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.vpn;

/**
 * Action to take when a player is detected as using a VPN or proxy.
 */
public enum VpnAction {
    /** Log a warning to console and notify online admins; allow the player to join. */
    WARN,
    /** Disconnect the player with a configurable kick message. */
    KICK,
    /** Ban the player locally and disconnect them. The ban is synced via the normal BanSync flow. */
    BAN
}
