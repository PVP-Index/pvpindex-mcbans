package com.pvpindex.bans.plugin.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the {@code /globalban} command. */
class GlobalbanCommandTest extends CommandTestBase {

    @Test
    void no_args_sends_format_error() {
        String out = run(new CommandGlobalban(), playerWith("mcbans.ban.global"));
        assertFalse(out.isEmpty(), "Expected error for missing target");
    }

    @Test
    void target_only_sends_format_error() {
        String out = run(new CommandGlobalban(), playerWith("mcbans.ban.global"),
                "TestTarget");
        assertFalse(out.isEmpty(), "Expected error when reason is omitted");
    }

    @Test
    void without_permission_sends_denied() {
        String out = run(new CommandGlobalban(), unprivilegedPlayer(),
                "TestTarget", "hacking");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void with_permission_and_reason_dispatches_without_immediate_error() {
        String out = run(new CommandGlobalban(), playerWith("mcbans.ban.global"),
                "TestTarget", "hacking");
        assertTrue(out.isEmpty(),
                "Expected no immediate error for valid globalban: " + out);
    }
}
