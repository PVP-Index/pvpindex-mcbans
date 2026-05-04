package com.pvpindex.bans.storage;

/**
 * Immutable local ban record stored in SQLite.
 *
 * @param uuid        Minecraft UUID without dashes, lowercase
 * @param playerName  Last known username
 * @param type        "global", "local", or "temp"
 * @param reason      Ban reason (max 500 chars)
 * @param adminUuid   Admin UUID without dashes (nullable)
 * @param adminName   Admin display name (nullable)
 * @param expiresAt   Unix epoch seconds or null for permanent bans
 * @param isActive    Whether the ban is currently active
 * @param isSynced    Whether the ban has been pushed to the PvPIndex API
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
        long    createdAt,
        long    updatedAt
) {}
