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
 * Result of an AntVPN player verification check.
 *
 * @param allowed   {@code true} if the player is permitted to join (no VPN detected or
 *                  VPN protection is disabled)
 * @param sessionId the session ID returned by AntVPN, or {@code null} on timeout/error
 * @param isAttack  whether AntVPN reports an active attack pattern for this IP
 */
public record VpnCheckResult(
        boolean allowed,
        String  sessionId,
        boolean isAttack
) {
    /** Convenience factory: player passed the check. */
    public static VpnCheckResult pass() {
        return new VpnCheckResult(true, null, false);
    }

    /** Convenience factory: player failed the check (VPN detected). */
    public static VpnCheckResult fail(String sessionId, boolean isAttack) {
        return new VpnCheckResult(false, sessionId, isAttack);
    }
}
