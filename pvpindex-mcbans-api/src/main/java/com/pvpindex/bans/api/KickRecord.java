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
 * A single kick record as returned by the PvPIndex API.
 * Only created when {@code kicks.public: true} is set in config.yml.
 */
public record KickRecord(
        String id,
        String playerUuid,
        String playerUsername,
        String reason,
        String adminUuid,
        String adminName,
        String serverId,
        String createdAt    // ISO-8601 string
) {}
