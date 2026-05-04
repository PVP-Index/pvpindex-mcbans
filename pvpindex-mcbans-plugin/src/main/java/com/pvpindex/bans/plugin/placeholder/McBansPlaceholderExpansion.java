/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.placeholder;

import com.pvpindex.bans.storage.LocalBan;
import com.pvpindex.bans.storage.StorageBackend;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * PlaceholderAPI expansion for MCBans.
 *
 * <p>Registers the {@code %mcbans_<identifier>%} namespace.
 * Only registered when PlaceholderAPI is present (soft-depend).</p>
 *
 * <p>Supported placeholders:</p>
 * <ul>
 *   <li>{@code %mcbans_banned%} — {@code "true"} / {@code "false"} — whether
 *       the player has an active local ban on this server.</li>
 *   <li>{@code %mcbans_ban_type%} — {@code "global"} / {@code "local"} /
 *       {@code "temp"} / {@code "none"}.</li>
 *   <li>{@code %mcbans_ban_reason%} — the ban reason, or {@code ""} when not
 *       banned.</li>
 *   <li>{@code %mcbans_ban_admin%} — the banning admin's display name, or
 *       {@code ""} when not banned.</li>
 * </ul>
 */
public final class McBansPlaceholderExpansion extends PlaceholderExpansion {

    private final StorageBackend storage;

    public McBansPlaceholderExpansion(final StorageBackend storage) {
        this.storage = storage;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mcbans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "PvPIndex";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    /** Expansion must not be unregistered when the providing plugin is reloaded. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(final OfflinePlayer player, final @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        String uuid = player.getUniqueId().toString().replace("-", "").toLowerCase();
        Optional<LocalBan> banOpt = storage.findActiveBan(uuid);

        switch (identifier) {
            case "banned":
                return banOpt.isPresent() ? "true" : "false";

            case "ban_type":
                return banOpt.map(LocalBan::type).orElse("none");

            case "ban_reason":
                return banOpt.map(LocalBan::reason).orElse("");

            case "ban_admin":
                return banOpt.map(b -> b.adminName() != null ? b.adminName() : "").orElse("");

            default:
                return null; // unknown placeholder — let PAPI handle gracefully
        }
    }
}
