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
 * A single ban record as returned by the PvPIndex API.
 */
public record BanRecord(
        String id,
        String playerUuid,
        String playerUsername,
        String type,
        String reason,
        String adminUuid,
        String adminName,
        String expiresAt,      // ISO-8601 string or null
        boolean isActive,
        String updatedAt       // ISO-8601 string
) {}
