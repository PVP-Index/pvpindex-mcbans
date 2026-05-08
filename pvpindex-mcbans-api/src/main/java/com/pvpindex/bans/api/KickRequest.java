/*
 * This file is part of PvPIndex MCBans.
 *
 * Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.api;

/**
 * Request body sent to {@code POST /plugin/players/{uuid}/kick}.
 *
 * @param playerUuid      the kicked player's UUID (dashes are stripped server-side)
 * @param playerUsername  last-known username (may be null)
 * @param reason          kick reason
 * @param adminUuid       UUID of the staff member who issued the kick (may be null for console)
 * @param adminName       display name of the staff member (may be null for console)
 */
public record KickRequest(
        String playerUuid,
        String playerUsername,
        String reason,
        String adminUuid,
        String adminName
) {}
