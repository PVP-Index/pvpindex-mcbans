package com.pvpindex.bans.plugin.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the {@code /ban} command. */
class BanCommandTest extends CommandTestBase {

    @Test
    void no_args_sends_format_error() {
        String out = run(new CommandBan(), playerWith("mcbans.ban.local"));
        assertFalse(out.isEmpty(), "Expected error message for missing target");
    }

    @Test
    void without_permission_sends_denied() {
        String out = run(new CommandBan(), unprivilegedPlayer(), "TestTarget");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void with_permission_and_target_dispatches_without_immediate_error() {
        String out = run(new CommandBan(), playerWith("mcbans.ban.local"), "TestTarget");
        assertTrue(out.isEmpty(), "Expected no immediate error for valid ban: " + out);
    }

    @Test
    void with_permission_target_and_reason_dispatches_without_immediate_error() {
        String out = run(new CommandBan(), playerWith("mcbans.ban.local"),
                "TestTarget", "cheating");
        assertTrue(out.isEmpty(),
                "Expected no immediate error for ban with reason: " + out);
    }
}
