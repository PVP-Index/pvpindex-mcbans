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

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Reads VPN/anti-cheat settings from {@code antivpn.yml} in the plugin data folder.
 *
 * <p>All settings are isolated in this dedicated config file so VPN configuration
 * does not clutter the main {@code config.yml}.</p>
 */
public class VpnConfiguration {

    private final File dataFolder;
    private final Logger logger;
    private FileConfiguration config;

    public VpnConfiguration(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger     = logger;
    }

    /**
     * Load (or create) {@code antivpn.yml} from the plugin data folder.
     * On first run the bundled resource default is extracted.
     */
    public void load() {
        File file = new File(dataFolder, "antivpn.yml");

        if (!file.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/antivpn.yml")) {
                if (in != null) {
                    dataFolder.mkdirs();
                    java.nio.file.Files.copy(in, file.toPath());
                } else {
                    logger.warning("[MCBans-VPN] antivpn.yml resource not found in jar - using defaults.");
                }
            } catch (IOException e) {
                logger.warning("[MCBans-VPN] Could not extract antivpn.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        // Merge any missing defaults from the bundled resource
        try (InputStream in = getClass().getResourceAsStream("/antivpn.yml")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
            }
        } catch (IOException ignored) {
            // Ignore - defaults already applied above or file already exists
        }
    }

    /** Whether the AntVPN integration is enabled. */
    public boolean isEnabled() {
        return config != null && config.getBoolean("antivpn.enabled", false);
    }

    /** The AntVPN API JWT token. */
    public String getApiToken() {
        return config != null ? config.getString("antivpn.api-token", "").trim() : "";
    }

    /** Action to take on detection: WARN, KICK, or BAN. */
    public VpnAction getAction() {
        if (config == null) {
            return VpnAction.KICK;
        }
        String raw = config.getString("antivpn.action", "kick").trim().toUpperCase();
        try {
            return VpnAction.valueOf(raw);
        } catch (IllegalArgumentException e) {
            logger.warning("[MCBans-VPN] Unknown action '" + raw + "' in antivpn.yml - defaulting to KICK.");
            return VpnAction.KICK;
        }
    }

    /** Ban duration in seconds for VPN bans; 0 = permanent. */
    public long getBanDurationSeconds() {
        return config != null ? config.getLong("antivpn.ban-duration-seconds", 0L) : 0L;
    }

    /** Whether VPN bans should be synced to the PvPIndex API. */
    public boolean isSyncBans() {
        return config != null && config.getBoolean("antivpn.sync-bans", false);
    }

    /** Kick message shown to detected VPN users. Supports & colour codes. */
    public String getKickMessage() {
        if (config == null) {
            return "&c[PvPIndex MCBans] &rVPN or proxy usage is not allowed on this server.";
        }
        return config.getString("antivpn.kick-message",
                "&c[PvPIndex MCBans] &rVPN or proxy usage is not allowed on this server.");
    }

    /** Ban reason stored in the local ban record for VPN bans. */
    public String getBanReason() {
        if (config == null) {
            return "VPN or proxy usage detected";
        }
        return config.getString("antivpn.ban-reason", "VPN or proxy usage detected");
    }

    /** Timeout in milliseconds to wait for an AntVPN response before allowing the player in. */
    public long getTimeoutMs() {
        return config != null ? config.getLong("antivpn.timeout-ms", 3000L) : 3000L;
    }

    /** Whether to bypass VPN checks for players with the {@code mcbans.vpn.bypass} permission. */
    public boolean isBypassPermissionEnabled() {
        return config != null && config.getBoolean("antivpn.bypass-permission", true);
    }

    /** Whether to skip the VPN check when AntVPN indicates an active attack (shield mode). */
    public boolean isSkipOnAttack() {
        return config != null && config.getBoolean("antivpn.skip-on-attack", false);
    }
}
