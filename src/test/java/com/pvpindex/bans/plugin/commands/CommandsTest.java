package com.pvpindex.bans.plugin.commands;

import java.util.ArrayList;
import java.util.List;

import com.pvpindex.bans.plugin.MCBans;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for every command and subcommand.
 *
 * <p>Synchronous behaviour is verified directly: argument validation,
 * permission checks, and expected output messages. Async ban/kick worker
 * threads are started but not awaited; their database/API work is outside
 * the scope of these tests.</p>
 */
class CommandsTest {

    private ServerMock server;
    private MCBans plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MCBans.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Returns a new player with each of the supplied permission nodes granted. */
    private PlayerMock playerWith(String... perms) {
        PlayerMock p = server.addPlayer();
        for (String perm : perms) {
            p.addAttachment(plugin, perm, true);
        }
        return p;
    }

    /** Returns a new player with mcbans.admin permission. */
    private PlayerMock adminPlayer() {
        return playerWith("mcbans.admin");
    }

    /** Returns a new player with no permissions. */
    private PlayerMock unprivilegedPlayer() {
        return server.addPlayer();
    }

    /** Drains all pending messages from a player, stripping colour codes. */
    private List<String> allMessages(PlayerMock player) {
        List<String> msgs = new ArrayList<>();
        String msg;
        while ((msg = player.nextMessage()) != null) {
            msgs.add(ChatColor.stripColor(msg));
        }
        return msgs;
    }

    /** All pending messages joined with newlines (stripped of colour codes). */
    private String messages(PlayerMock player) {
        return String.join("\n", allMessages(player));
    }

    /**
     * Runs a command with the given args and returns all messages sent to the player.
     *
     * <p>Uses {@code cmd.name} as the command label so that BaseCommand routing
     * is exercised with the command's own registered name. Ticks the scheduler
     * 11 times to flush messages dispatched via runTaskLater(plugin, 10).</p>
     */
    private String run(BaseCommand cmd, PlayerMock sender, String... args) {
        cmd.run(plugin, sender, cmd.name, args);
        ((BukkitSchedulerMock) server.getScheduler()).performTicks(11);
        return messages(sender);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // /mcbans and its subcommands
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class McbansCommand {

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
            // May say "until next sync", "in progress", or "Auto sync is disabled"
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

    // ──────────────────────────────────────────────────────────────────────────
    // /ban
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class BanCommand {

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

    // ──────────────────────────────────────────────────────────────────────────
    // /globalban
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class GlobalbanCommand {

        @Test
        void no_args_sends_format_error() {
            String out = run(new CommandGlobalban(), playerWith("mcbans.ban.global"));
            assertFalse(out.isEmpty(), "Expected error for missing target");
        }

        @Test
        void target_only_sends_format_error() {
            // argLength=2 requires at least two arguments
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

    // ──────────────────────────────────────────────────────────────────────────
    // /kick
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class KickCommand {

        @Test
        void no_args_sends_format_error() {
            String out = run(new CommandKick(), playerWith("mcbans.kick"));
            assertFalse(out.isEmpty(), "Expected error for missing target");
        }

        @Test
        void without_permission_sends_denied() {
            String out = run(new CommandKick(), unprivilegedPlayer(), "TestTarget");
            assertTrue(out.toLowerCase().contains("permission"),
                    "Expected permission denied: " + out);
        }

        @Test
        void with_permission_and_target_dispatches_without_immediate_error() {
            server.addPlayer("TestTarget"); // must be online for kick to proceed
            String out = run(new CommandKick(), playerWith("mcbans.kick"), "TestTarget");
            assertTrue(out.isEmpty(),
                    "Expected no immediate error for valid kick: " + out);
        }

        @Test
        void with_custom_reason_dispatches_without_immediate_error() {
            server.addPlayer("TestTarget"); // must be online for kick to proceed
            String out = run(new CommandKick(), playerWith("mcbans.kick"),
                    "TestTarget", "spamming");
            assertTrue(out.isEmpty(),
                    "Expected no immediate error for kick with reason: " + out);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // /tempban
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class TempbanCommand {

        @Test
        void no_args_sends_format_error() {
            String out = run(new CommandTempban(), playerWith("mcbans.ban.temp"));
            assertFalse(out.isEmpty(), "Expected error for missing target");
        }

        @Test
        void target_only_sends_format_error() {
            // argLength=2 so a single argument fails the size check
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
            // Duration token like "1day" is parsed as combined numeric+unit
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

    // ──────────────────────────────────────────────────────────────────────────
    // /unban
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class UnbanCommand {

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

    // ──────────────────────────────────────────────────────────────────────────
    // /rban
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class RbanCommand {

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
            // Default type is LOCAL when no flag is given
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
            // Has rollback but NOT ban.global — sub-check inside execute() fires
            String out = run(new CommandRban(),
                    playerWith("mcbans.ban.rollback"),
                    "TestTarget", "g", "exploiting");
            assertTrue(out.toLowerCase().contains("permission"),
                    "Expected permission denied for rban global without perm: " + out);
        }

        @Test
        void temp_type_with_valid_duration_dispatches_without_immediate_error() {
            // /rban TestTarget t 1 day reason  →  duration=1, measure=day
            String out = run(new CommandRban(),
                    playerWith("mcbans.ban.rollback", "mcbans.ban.temp"),
                    "TestTarget", "t", "1", "day", "reason");
            assertTrue(out.isEmpty(), "Expected no error for rban temp: " + out);
        }

        @Test
        void temp_type_with_insufficient_args_sends_format_error() {
            // Temp requires duration + measure + reason (>2 items after removing target & flag)
            String out = run(new CommandRban(),
                    playerWith("mcbans.ban.rollback", "mcbans.ban.temp"),
                    "TestTarget", "t");
            assertFalse(out.isEmpty(), "Expected format error when temp args are missing");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Stubbed / not-yet-implemented commands
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class StubbedCommands {

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
}
