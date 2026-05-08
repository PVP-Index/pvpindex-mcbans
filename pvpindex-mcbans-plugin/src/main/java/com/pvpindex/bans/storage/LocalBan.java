/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.storage;

/**
 * Immutable local ban record stored in the local storage backend.
 *
 * @param uuid        Minecraft UUID without dashes, lowercase
 * @param playerName  Last known username
 * @param type        "global", "local", "temp", or "vpn"
 * @param reason      Ban reason (max 500 chars)
 * @param adminUuid   Admin UUID without dashes (nullable)
 * @param adminName   Admin display name (nullable)
 * @param expiresAt   Unix epoch seconds or null for permanent bans
 * @param isActive    Whether the ban is currently active
 * @param isSynced    Whether the ban has been pushed to the PvPIndex API
 * @param isLegacy    Whether this ban was imported from a legacy source (e.g. mcbans.com)
 * @param createdAt   Unix epoch seconds
 * @param updatedAt   Unix epoch seconds
 */
public record LocalBan(
        String uuid,
        String playerName,
        String type,
        String reason,
        String adminUuid,
        String adminName,
        Long   expiresAt,
        boolean isActive,
        boolean isSynced,
        boolean isLegacy,
        long    createdAt,
        long    updatedAt
) {}
