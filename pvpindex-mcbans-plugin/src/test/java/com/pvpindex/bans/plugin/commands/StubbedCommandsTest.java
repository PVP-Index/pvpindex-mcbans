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

/** Tests for stubbed / not-yet-implemented commands. */
class StubbedCommandsTest extends CommandTestBase {

    @Test
    void altlookup_with_permission_sends_not_supported() {
        String out = run(new CommandAltlookup(),
                playerWith("mcbans.lookup.alt"), "TestTarget");
        assertTrue(out.contains("not supported"),
                "Expected not-supported message: " + out);
    }

    @Test
    void altlookup_without_permission_sends_denied() {
        String out = run(new CommandAltlookup(), unprivilegedPlayer(), "TestTarget");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void altlookup_no_args_sends_format_error() {
        String out = run(new CommandAltlookup(), playerWith("mcbans.lookup.alt"));
        assertFalse(out.isEmpty(), "Expected error for missing target");
    }

    @Test
    void banip_with_permission_sends_not_supported() {
        String out = run(new CommandBanip(),
                playerWith("mcbans.ban.ip"), "TestTarget");
        assertTrue(out.contains("not supported"),
                "Expected not-supported message: " + out);
    }

    @Test
    void banip_without_permission_sends_denied() {
        String out = run(new CommandBanip(), unprivilegedPlayer(), "TestTarget");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void banip_no_args_sends_format_error() {
        String out = run(new CommandBanip(), playerWith("mcbans.ban.ip"));
        assertFalse(out.isEmpty(), "Expected error for missing target");
    }

    @Test
    void banlookup_with_valid_integer_id_sends_not_supported() {
        String out = run(new CommandBanlookup(),
                playerWith("mcbans.lookup.ban"), "123");
        assertTrue(out.contains("not supported"),
                "Expected not-supported message: " + out);
    }

    @Test
    void banlookup_with_non_integer_id_sends_format_error() {
        String out = run(new CommandBanlookup(),
                playerWith("mcbans.lookup.ban"), "notanumber");
        assertFalse(out.isEmpty(), "Expected format error for non-integer ban ID");
        assertFalse(out.contains("not supported"),
                "Should not reach not-supported with invalid ID: " + out);
    }

    @Test
    void banlookup_without_permission_sends_denied() {
        String out = run(new CommandBanlookup(), unprivilegedPlayer(), "123");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void lookup_with_permission_sends_not_supported() {
        String out = run(new CommandLookup(),
                playerWith("mcbans.lookup.player"), "TestTarget");
        assertTrue(out.contains("not supported"),
                "Expected not-supported message: " + out);
    }

    @Test
    void lookup_without_permission_sends_denied() {
        String out = run(new CommandLookup(), unprivilegedPlayer(), "TestTarget");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void lookup_no_args_sends_format_error() {
        String out = run(new CommandLookup(), playerWith("mcbans.lookup.player"));
        assertFalse(out.isEmpty(), "Expected error for missing target");
    }

    @Test
    void mcbsettings_with_admin_sends_not_supported() {
        String out = run(new CommandMCBansSettings(), adminPlayer());
        assertTrue(out.contains("not supported"),
                "Expected not-supported message: " + out);
    }

    @Test
    void mcbsettings_without_permission_sends_denied() {
        String out = run(new CommandMCBansSettings(), unprivilegedPlayer());
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void previous_with_permission_sends_not_supported() {
        String out = run(new CommandPrevious(),
                playerWith("mcbans.view.previous"), "TestTarget");
        assertTrue(out.contains("not supported"),
                "Expected not-supported message: " + out);
    }

    @Test
    void previous_without_permission_sends_denied() {
        String out = run(new CommandPrevious(), unprivilegedPlayer(), "TestTarget");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void previous_no_args_sends_format_error() {
        String out = run(new CommandPrevious(), playerWith("mcbans.view.previous"));
        assertFalse(out.isEmpty(), "Expected error for missing target");
    }
}
