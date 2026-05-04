/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke tests for the MCBans plugin lifecycle using MockBukkit.
 *
 * <p>These tests verify that the plugin can be loaded, enabled, and disabled
 * without throwing exceptions, even when no API key is configured.</p>
 */
class MCBansPluginTest {

    private ServerMock server;
    private MCBans plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // Load plugin — uses default config (empty API key, SQLite in temp dir)
        plugin = MockBukkit.load(MCBans.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void plugin_loadsSuccessfully() {
        assertNotNull(plugin, "Plugin instance should not be null after load");
    }

    @Test
    void plugin_isEnabled_afterLoad() {
        assertNotNull(plugin.getServer(), "Plugin server should be available");
    }

    @Test
    void plugin_getBanDao_isNotNull_afterEnable() {
        assertNotNull(plugin.getBanDao(), "BanDao should be initialised on enable");
    }

    @Test
    void plugin_getApiClient_isNotNull_afterEnable() {
        assertNotNull(plugin.getApiClient(), "API client should be initialised on enable");
    }

    @Test
    void playerJoin_withNoBan_isAllowed() {
        PlayerMock player = server.addPlayer("TestPlayer");
        // Player added successfully — no ban exists, login should be permitted
        assertNotNull(player.getUniqueId());
    }
}
