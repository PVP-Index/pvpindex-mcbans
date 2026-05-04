/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the {@code /unban} command. */
class UnbanCommandTest extends CommandTestBase {

    @Test
    void no_args_sends_format_error() {
        String out = run(new CommandUnban(), playerWith("mcbans.unban"));
        assertFalse(out.isEmpty(), "Expected error for missing target");
    }

    @Test
    void without_permission_sends_denied() {
        String out = run(new CommandUnban(), unprivilegedPlayer(), "TestTarget");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void with_permission_and_target_dispatches_without_immediate_error() {
        String out = run(new CommandUnban(), playerWith("mcbans.unban"), "TestTarget");
        assertTrue(out.isEmpty(),
                "Expected no immediate error for valid unban: " + out);
    }
}
