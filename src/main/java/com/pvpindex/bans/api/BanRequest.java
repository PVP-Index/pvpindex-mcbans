package com.pvpindex.bans.api;

/**
 * Request DTO for submitting a ban to the PvPIndex API.
 */
public record BanRequest(
        String playerUuid,
        String playerUsername,
        String type,
        String reason,
        String adminUuid,
        String adminName,
        String expiresAt       // ISO-8601 or null for non-temp bans
) {}
