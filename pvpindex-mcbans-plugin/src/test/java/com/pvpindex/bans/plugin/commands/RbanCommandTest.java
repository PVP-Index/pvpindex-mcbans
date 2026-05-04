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

/** Tests for the {@code /rban} command. */
class RbanCommandTest extends CommandTestBase {

    @Test
    void no_args_sends_format_error() {
        String out = run(new CommandRban(), playerWith("mcbans.ban.rollback"));
        assertFalse(out.isEmpty(), "Expected error for missing target");
    }

    @Test
    void without_permission_sends_denied() {
        String out = run(new CommandRban(), unprivilegedPlayer(), "TestTarget");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void local_type_dispatches_without_immediate_error() {
        String out = run(new CommandRban(),
                playerWith("mcbans.ban.rollback", "mcbans.ban.local"),
                "TestTarget");
        assertTrue(out.isEmpty(), "Expected no error for rban local: " + out);
    }

    @Test
    void global_type_with_reason_dispatches_without_immediate_error() {
        String out = run(new CommandRban(),
                playerWith("mcbans.ban.rollback", "mcbans.ban.global"),
                "TestTarget", "g", "exploiting");
        assertTrue(out.isEmpty(), "Expected no error for rban global: " + out);
    }

    @Test
    void global_type_without_global_permission_sends_denied() {
        String out = run(new CommandRban(),
                playerWith("mcbans.ban.rollback"),
                "TestTarget", "g", "exploiting");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied for rban global without perm: " + out);
    }

    @Test
    void temp_type_with_valid_duration_dispatches_without_immediate_error() {
        String out = run(new CommandRban(),
                playerWith("mcbans.ban.rollback", "mcbans.ban.temp"),
                "TestTarget", "t", "1", "day", "reason");
        assertTrue(out.isEmpty(), "Expected no error for rban temp: " + out);
    }

    @Test
    void temp_type_with_insufficient_args_sends_format_error() {
        String out = run(new CommandRban(),
                playerWith("mcbans.ban.rollback", "mcbans.ban.temp"),
                "TestTarget", "t");
        assertFalse(out.isEmpty(), "Expected format error when temp args are missing");
    }
}
