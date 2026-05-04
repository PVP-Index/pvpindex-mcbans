package com.pvpindex.bans.plugin.request;

import com.pvpindex.bans.api.BanRequest;
import com.pvpindex.bans.plugin.ActionLog;
import com.pvpindex.bans.plugin.I18n;
import com.pvpindex.bans.plugin.MCBans;
import com.pvpindex.bans.plugin.events.PlayerBanEvent;
import com.pvpindex.bans.plugin.events.PlayerGlobalBanEvent;
import com.pvpindex.bans.plugin.events.PlayerLocalBanEvent;
import com.pvpindex.bans.plugin.events.PlayerTempBanEvent;
import com.pvpindex.bans.plugin.events.PlayerUnbanEvent;
import com.pvpindex.bans.plugin.events.PlayerUnbannedEvent;
import com.pvpindex.bans.plugin.util.Util;
import com.pvpindex.bans.utils.TimeTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.pvpindex.bans.plugin.I18n.localize;

/**
 * Executes ban/unban actions against the PvPIndex API.
 *
 * <p>Every action is written to the local SQLite cache immediately, regardless of
 * whether the API call succeeded, so behaviour is consistent when the API is down.</p>
 */
public class Ban {

    private final MCBans plugin;
    private final ActionLog log;

    private String playerName;
    private String playerIP;
    private String senderName;
    private String reason;
    private String action;
    private String duration;
    private String measure;
    private String playerUUID;
    private String senderUUID;
    private boolean rollback;

    private static final Map<String, Integer> ACTION_IDS = new HashMap<>();
    static {
        ACTION_IDS.put("globalBan", 0);
        ACTION_IDS.put("localBan",  1);
        ACTION_IDS.put("tempBan",   2);
        ACTION_IDS.put("unBan",     3);
    }

    public Ban(MCBans plugin, String action, String playerName, String playerUUID, String playerIP,
               String senderName, String senderUUID, String reason, String duration, String measure,
               Object ignored, boolean rollback) {
        this(plugin, action, playerName, playerIP, senderName, reason, duration, measure, ignored, rollback);
        this.playerUUID = playerUUID;
        this.senderUUID = senderUUID;
    }

    public Ban(MCBans plugin, String action, String playerName, String playerIP,
               String senderName, String reason, String duration, String measure,
               Object ignored, boolean rollback) {
        this.plugin     = plugin;
        this.log        = plugin.getLog();
        this.playerName = playerName;
        this.playerIP   = playerIP;
        this.senderName = senderName;
        this.reason     = reason;
        this.rollback   = rollback;
        this.duration   = duration;
        this.measure    = measure;
        this.action     = action;
    }

    public Ban(MCBans plugin, String action, String playerName, String playerIP,
               String senderName, String reason, String duration, String measure) {
        this(plugin, action, playerName, playerIP, senderName, reason, duration, measure, null, false);
    }

    // -------------------------------------------------------------------------
    // Dispatch
    // -------------------------------------------------------------------------

    public void run() {
        try {
            if (!ACTION_IDS.containsKey(action)) {
                err();
                return;
            }
            int actionId = ACTION_IDS.get(action);

            // Fire cancellable BanEvent
            if (actionId != 3) {
                PlayerBanEvent banEvent = new PlayerBanEvent(playerName, playerUUID, playerIP,
                        senderName, senderUUID, reason, actionId, duration, measure);
                plugin.getServer().getPluginManager().callEvent(banEvent);
                if (banEvent.isCancelled()) {
                    return;
                }
                senderName = banEvent.getSenderName();
                reason     = banEvent.getReason();
                actionId   = banEvent.getActionID();
                duration   = banEvent.getDuration();
                measure    = banEvent.getMeasure();
            }

            switch (actionId) {
                case 0 -> globalBan();
                case 1 -> localBan();
                case 2 -> tempBan();
                case 3 -> unBan();
                default -> err();
            }
        } catch (Exception e) {
            e.printStackTrace();
            err();
        }
    }

    // -------------------------------------------------------------------------
    // Global ban
    // -------------------------------------------------------------------------

    private void globalBan() {
        new Thread(() -> {
            String uuid = normaliseUUID(playerUUID);
            // Always write locally first (offline safety)
            plugin.getBanDao().insertOfflineBan(uuid, playerName, "global", reason,
                    normaliseUUID(senderUUID), senderName, null);

            // Try pushing to API
            boolean apiOk = plugin.getApiClient()
                    .ban(new BanRequest(uuid, playerName, "global", reason,
                            normaliseUUID(senderUUID), senderName, null))
                    .isPresent();
            if (apiOk) {
                plugin.getBanDao().markSynced(uuid);
            }

            kick(localize("globalBanPlayer", I18n.REASON, reason, I18n.SENDER, senderName));
            Util.broadcastMessage(ChatColor.RED + localize("globalBanSuccess",
                    I18n.PLAYER, playerName, I18n.SENDER, senderName, I18n.REASON, reason));
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getServer().getPluginManager()
                            .callEvent(new PlayerGlobalBanEvent(
                                    playerName, playerUUID, playerIP, senderName, senderUUID, reason)));
            log.info(senderName + " globally banned " + playerName + " for: " + reason);
        }, "MCBans-GlobalBan").start();
    }

    // -------------------------------------------------------------------------
    // Local ban
    // -------------------------------------------------------------------------

    private void localBan() {
        new Thread(() -> {
            String uuid = normaliseUUID(playerUUID);
            plugin.getBanDao().insertOfflineBan(uuid, playerName, "local", reason,
                    normaliseUUID(senderUUID), senderName, null);

            boolean apiOk = plugin.getApiClient()
                    .ban(new BanRequest(uuid, playerName, "local", reason,
                            normaliseUUID(senderUUID), senderName, null))
                    .isPresent();
            if (apiOk) {
                plugin.getBanDao().markSynced(uuid);
            }

            kick(localize("localBanPlayer", I18n.REASON, reason, I18n.SENDER, senderName));
            Util.broadcastMessage(ChatColor.RED + localize("localBanSuccess",
                    I18n.PLAYER, playerName, I18n.SENDER, senderName, I18n.REASON, reason));
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getServer().getPluginManager()
                            .callEvent(new PlayerLocalBanEvent(
                                    playerName, playerUUID, playerIP, senderName, senderUUID, reason)));
            log.info(senderName + " locally banned " + playerName + " for: " + reason);
        }, "MCBans-LocalBan").start();
    }

    // -------------------------------------------------------------------------
    // Temp ban
    // -------------------------------------------------------------------------

    private void tempBan() {
        new Thread(() -> {
            String uuid = normaliseUUID(playerUUID);
            long expiresMs     = TimeTools.convertStringToDate(duration + " " + measure);
            long expiresEpoch   = expiresMs / 1000;
            String expiresIso   = Instant.ofEpochSecond(expiresEpoch).toString();

            plugin.getBanDao().insertOfflineBan(uuid, playerName, "temp", reason,
                    normaliseUUID(senderUUID), senderName, expiresEpoch);

            boolean apiOk = plugin.getApiClient()
                    .ban(new BanRequest(uuid, playerName, "temp", reason,
                            normaliseUUID(senderUUID), senderName, expiresIso))
                    .isPresent();
            if (apiOk) {
                plugin.getBanDao().markSynced(uuid);
            }

            String durationDisplay = duration + " " + measure;
            kick(localize("tempBanPlayer", I18n.REASON, reason, I18n.SENDER, senderName));
            Util.broadcastMessage(ChatColor.RED + localize("tempBanSuccess",
                    I18n.PLAYER, playerName, I18n.SENDER, senderName,
                    I18n.REASON, reason, "%DURATION%", durationDisplay));
            PlayerTempBanEvent tempBanEvent = new PlayerTempBanEvent(
                    playerName, playerUUID, playerIP, senderName, senderUUID, reason, duration, measure);
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getServer().getPluginManager().callEvent(tempBanEvent));
            log.info(senderName + " temp-banned " + playerName + " for " + durationDisplay + ": " + reason);
        }, "MCBans-TempBan").start();
    }

    // -------------------------------------------------------------------------
    // Unban
    // -------------------------------------------------------------------------

    private void unBan() {
        PlayerUnbanEvent unBanEvent = new PlayerUnbanEvent(playerName, playerUUID, senderName, senderUUID);
        plugin.getServer().getPluginManager().callEvent(unBanEvent);
        if (unBanEvent.isCancelled()) {
            return;
        }
        senderName = unBanEvent.getSenderName();

        new Thread(() -> {
            String uuid = normaliseUUID(playerUUID);
            plugin.getBanDao().deactivateBan(uuid);

            boolean apiOk = plugin.getApiClient().unban(uuid);
            if (!apiOk) {
                plugin.getBanDao().insertOfflineBan(uuid, playerName, "local", "UNBAN",
                        normaliseUUID(senderUUID), senderName, null);
                // Mark as unban-pending (is_active=false) so BanSync uploads the unban
                plugin.getBanDao().deactivateBan(uuid);
            }

            Util.broadcastMessage(ChatColor.GREEN + localize("unBanSuccess",
                    I18n.PLAYER, playerName, I18n.SENDER, senderName));
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    plugin.getServer().getPluginManager()
                            .callEvent(new PlayerUnbannedEvent(playerName, playerUUID, senderName, senderUUID)), 1);
            log.info(senderName + " unbanned " + playerName + "!");
        }, "MCBans-Unban").start();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public void kickPlayer(String playerName, String playerUUID, String kickReason) {
        kick(kickReason);
    }

    private void kick(String msg) {
        Player target = playerUUID != null
                ? MCBans.getPlayer(plugin, MCBans.fromString(playerUUID))
                : MCBans.getPlayer(plugin, playerName);
        if (target != null) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,
                    () -> target.kickPlayer(msg), 1);
        }
    }

    private void err() {
        Util.message(senderName, ChatColor.RED + "An error occurred processing the ban command.");
    }

    private static String normaliseUUID(String uuid) {
        return uuid == null ? null : uuid.toLowerCase().replace("-", "");
    }
}
