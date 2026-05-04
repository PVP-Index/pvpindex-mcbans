package com.pvpindex.bans.plugin.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the {@code /mcbans} command and all its sub-commands. */
class McbansCommandTest extends CommandTestBase {

    @Test
    void no_args_shows_root_help() {
        String out = run(new CommandMCBans(), adminPlayer());
        assertTrue(out.contains("MCBans"), "Expected help header, got: " + out);
    }

    @Test
    void banning_shows_banning_help() {
        String out = run(new CommandMCBans(), adminPlayer(), "banning");
        assertTrue(out.contains("/ban"), "Expected /ban entry in banning help: " + out);
    }

    @Test
    void user_shows_user_help() {
        String out = run(new CommandMCBans(), adminPlayer(), "user");
        assertTrue(out.contains("/lookup"), "Expected /lookup in user help: " + out);
    }

    @Test
    void download_sends_not_supported_message() {
        String out = run(new CommandMCBans(), adminPlayer(), "download");
        assertTrue(out.contains("not supported"), "Expected not-supported: " + out);
    }

    @Test
    void perms_no_sub_shows_perm_root() {
        String out = run(new CommandMCBans(), adminPlayer(), "perms");
        assertTrue(out.contains("mcbans.admin"), "Expected perm root listing: " + out);
    }

    @Test
    void perms_ban_shows_ban_permission_nodes() {
        String out = run(new CommandMCBans(), adminPlayer(), "perms", "ban");
        assertTrue(out.contains("mcbans.ban.global"), "Expected ban perm node: " + out);
    }

    @Test
    void perms_view_shows_view_permission_nodes() {
        String out = run(new CommandMCBans(), adminPlayer(), "perms", "view");
        assertTrue(out.contains("mcbans.view.alts"), "Expected view perm node: " + out);
    }

    @Test
    void perms_exempt_shows_exempt_permission_nodes() {
        String out = run(new CommandMCBans(), adminPlayer(), "perms", "exempt");
        assertTrue(out.contains("mcbans.kick.exempt"), "Expected exempt perm node: " + out);
    }

    @Test
    void perms_others_shows_lookup_permission_nodes() {
        String out = run(new CommandMCBans(), adminPlayer(), "perms", "others");
        assertTrue(out.contains("mcbans.lookup.player"), "Expected lookup perm node: " + out);
    }

    @Test
    void ping_with_admin_sends_not_supported() {
        String out = run(new CommandMCBans(), adminPlayer(), "ping");
        assertTrue(out.contains("not supported"), "Expected not-supported for ping: " + out);
    }

    @Test
    void ping_without_admin_sends_permission_denied() {
        String out = run(new CommandMCBans(), unprivilegedPlayer(), "ping");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void get_no_sub_shows_get_help() {
        String out = run(new CommandMCBans(), adminPlayer(), "get");
        assertTrue(out.contains("/mcbans get"), "Expected get help listing: " + out);
    }

    @Test
    void get_call_shows_callback_info() {
        String out = run(new CommandMCBans(), adminPlayer(), "get", "call");
        assertTrue(out.contains("Callback"), "Expected callback info message: " + out);
    }

    @Test
    void get_sync_returns_sync_timing_output() {
        String out = run(new CommandMCBans(), adminPlayer(), "get", "sync");
        assertFalse(out.isEmpty(), "Expected some sync timing output, got nothing");
    }

    @Test
    void reload_with_admin_reports_reload_progress() {
        String out = run(new CommandMCBans(), adminPlayer(), "reload");
        assertTrue(out.toLowerCase().contains("reload"),
                "Expected reload progress message: " + out);
    }

    @Test
    void reload_without_admin_sends_permission_denied() {
        String out = run(new CommandMCBans(), unprivilegedPlayer(), "reload");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }

    @Test
    void sync_with_admin_sends_triggering_message() {
        String out = run(new CommandMCBans(), adminPlayer(), "sync");
        assertTrue(out.contains("Triggering") || out.contains("sync"),
                "Expected sync trigger message: " + out);
    }

    @Test
    void sync_without_admin_sends_permission_denied() {
        String out = run(new CommandMCBans(), unprivilegedPlayer(), "sync");
        assertTrue(out.toLowerCase().contains("permission"),
                "Expected permission denied: " + out);
    }
}
