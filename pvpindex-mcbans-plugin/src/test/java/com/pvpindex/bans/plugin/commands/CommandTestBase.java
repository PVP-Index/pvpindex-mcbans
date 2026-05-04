package com.pvpindex.bans.plugin.commands;

import java.util.ArrayList;
import java.util.List;

import com.pvpindex.bans.plugin.MCBans;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock;

/**
 * Shared setup and helpers for per-command integration tests.
 *
 * <p>Each concrete test class extends this to get a fresh {@link MockBukkit}
 * environment for every test method, along with common helper utilities.</p>
 */
abstract class CommandTestBase {

    protected ServerMock server;
    protected MCBans plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MCBans.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Returns a new player with each of the supplied permission nodes granted. */
    protected PlayerMock playerWith(String... perms) {
        PlayerMock p = server.addPlayer();
        for (String perm : perms) {
            p.addAttachment(plugin, perm, true);
        }
        return p;
    }

    /** Returns a new player with mcbans.admin permission. */
    protected PlayerMock adminPlayer() {
        return playerWith("mcbans.admin");
    }

    /** Returns a new player with no permissions. */
    protected PlayerMock unprivilegedPlayer() {
        return server.addPlayer();
    }

    /** Drains all pending messages from a player, stripping colour codes. */
    protected List<String> allMessages(PlayerMock player) {
        List<String> msgs = new ArrayList<>();
        String msg;
        while ((msg = player.nextMessage()) != null) {
            msgs.add(ChatColor.stripColor(msg));
        }
        return msgs;
    }

    /** All pending messages joined with newlines (stripped of colour codes). */
    protected String messages(PlayerMock player) {
        return String.join("\n", allMessages(player));
    }

    /**
     * Runs a command with the given args and returns all messages sent to the player.
     *
     * <p>Uses {@code cmd.name} as the command label so that BaseCommand routing
     * is exercised with the command's own registered name. Ticks the scheduler
     * 11 times to flush messages dispatched via runTaskLater(plugin, 10).</p>
     */
    protected String run(BaseCommand cmd, PlayerMock sender, String... args) {
        cmd.run(plugin, sender, cmd.name, args);
        ((BukkitSchedulerMock) server.getScheduler()).performTicks(11);
        return messages(sender);
    }
}
