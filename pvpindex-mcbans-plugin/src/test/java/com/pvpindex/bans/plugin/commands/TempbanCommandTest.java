package com.pvpindex.bans.plugin.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the {@code /tempban} command. */
class TempbanCommandTest extends CommandTestBase {

    @Test
    void no_args_sends_format_error() {
        String out = run(new CommandTempban(), playerWith("mcbans.ban.temp"));
        assertFalse(out.isEmpty(), "Expected error for missing target");
    }

    @Test
    void target_only_sends_format_error() {
        String out = run(new CommandTempban(), playerWith("mcbans.ban.temp"),
                "TestTarget");
        assertFalse(out.isEmpty(), "Expected error when duration is missing");
    }

    @Test
    void without_permission_sends_denied() {
        String out = run(new CommandTempban(), unprivilegedPlayer(),
                "TestTarget", "1day");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void with_combined_duration_dispatches_without_immediate_error() {
        String out = run(new CommandTempban(), playerWith("mcbans.ban.temp"),
                "TestTarget", "1day");
        assertTrue(out.isEmpty(),
                "Expected no immediate error for combined duration: " + out);
    }

    @Test
    void with_duration_and_reason_dispatches_without_immediate_error() {
        String out = run(new CommandTempban(), playerWith("mcbans.ban.temp"),
                "TestTarget", "7days", "ban evasion");
        assertTrue(out.isEmpty(),
                "Expected no immediate error for tempban with reason: " + out);
    }
}
