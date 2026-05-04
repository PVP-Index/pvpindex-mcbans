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
