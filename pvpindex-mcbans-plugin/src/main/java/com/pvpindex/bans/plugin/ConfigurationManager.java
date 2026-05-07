/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;

import com.pvpindex.bans.plugin.util.FileStructure;
import com.pvpindex.bans.plugin.util.Util;


public class ConfigurationManager {
    /* Current config.yml File Version! */
    private final int latestVersion = 3;

    private final MCBans plugin;
    private final ActionLog log;

    //private YamlConfiguration conf;
    private FileConfiguration conf;
    private File pluginDir;
    
    private boolean isValidKey = false;

    /**
     * Constructor
     */
    public ConfigurationManager(final MCBans plugin, final ActionLog actionLog){
        this.plugin = plugin;
        this.log = actionLog;

        this.pluginDir = this.plugin.getDataFolder();
    }

    /**
     * Load config.yml
     */
    public void loadConfig(final boolean initialLoad) throws Exception{
        // create directories
        FileStructure.createDir(pluginDir);

        // get config.yml path
        File file = new File(pluginDir, "config.yml");
        if (!file.exists()){
            FileStructure.extractResource("/config.yml", pluginDir, false, false);
            log.info("config.yml not found - created default.");
        }

        plugin.reloadConfig();
        conf = plugin.getConfig();

        checkver(conf.getInt("ConfigVersion", 1));
        validatePresets();

        // check API key
        if (conf.getString("pvpindex.apiKey", "").trim().isEmpty()){
            isValidKey = false;
            if (initialLoad){
                log.warning("No API key configured - ban sync is disabled.");
                log.warning("Set pvpindex.apiKey in config.yml. Get a key at: https://pvpindex.com/apply");
            }else{
                log.warning("API key missing or invalid - please check config.yml.");
            }
        }else{
            isValidKey = true;
        }

        // check log enable
        if (isEnableLog()){
            if (!new File(getLogFile()).exists()){
                try{
                    new File(getLogFile()).createNewFile();
                } catch (IOException ex){
                    log.warning("Could not create log file! " + getLogFile());
                }
            }
        }

        // check isEnabledAutoSync
        if (!initialLoad && isEnableAutoSync()){
            Registry.getBanSync().performSync(); // force run auto-sync
        }
    }

    /**
     * Check configuration file version
     */
    private void checkver(final int ver){
        // compare configuration file version
        if (ver < latestVersion){
            // first, rename old configuration
            final String destName = "oldconfig-v" + ver + ".yml";
            String srcPath = new File(pluginDir, "config.yml").getPath();
            String destPath = new File(pluginDir, destName).getPath();
            try{
                FileStructure.copyTransfer(srcPath, destPath);
                log.info("Migrated outdated config.yml (v" + ver + ") to " + destName + ".");
            }catch(Exception ex){
                log.warning("Failed to back up old config.yml.");
            }

            // force copy config.yml and languages
            FileStructure.extractResource("/config.yml", pluginDir, true, false);
            // Also re-extract language files so updated URLs/messages take effect
            I18n.extractLanguageFiles(true);

            plugin.reloadConfig();
            conf = plugin.getConfig();

            log.info("Generated fresh config.yml and re-extracted language files.");
        }
    }
    
    public boolean isValidApiKey(){
        return isValidKey;
    }

    /* ***** Begin Configuration Getters *********************** */
    public String getPrefix(){
        return Util.color(conf.getString("prefix", "[PvPIndex MCBans]"));
    }

    // PvPIndex API settings
    public String getApiUrl(){
        return conf.getString("pvpindex.apiUrl", "https://api.pvpindex.com");
    }
    public String getApiKey(){
        return conf.getString("pvpindex.apiKey", "").trim();
    }
    /** Sync interval in minutes (default 60). */
    public int getSyncInterval(){
        return conf.getInt("pvpindex.syncInterval", 60);
    }
    public String getLanguage(){
        return conf.getString("language", "default");
    }
    public String getPermission(){
        return conf.getString("permission", "SuperPerms");
    }

    public String getDefaultLocal(){
        return conf.getString("defaultLocal", "You have been banned!");
    }
    public String getDefaultTemp(){
        return conf.getString("defaultTemp", "You have been temporarily banned!");
    }
    public String getDefaultKick(){
        return conf.getString("defaultKick", "You have been kicked!");
    }

    public boolean isDebug(){
        return conf.getBoolean("isDebug", false);
    }
    public boolean isEnableLog(){
        return conf.getBoolean("logEnable", false);
    }
    public String getLogFile(){
        return conf.getString("logFile", "plugins/MCBans/actions.log");
    }

    public boolean isEnableMaxAlts(){
        return conf.getBoolean("enableMaxAlts", false);
    }
    public int getMaxAlts(){
        return conf.getInt("maxAlts", 2);
    }

    public String getAffectedWorlds(){
        return conf.getString("affectedWorlds", "*");
    }
    public int getBackDaysAgo(){
        return conf.getInt("backDaysAgo", 20);
    }

    public boolean isEnableAutoSync(){
        return conf.getBoolean("enableAutoSync", true);
    }

    // ─── Storage settings ───────────────────────────────────────────────────

    /**
     * Storage backend type: {@code sqlite} (default), {@code yaml},
     * {@code mysql}, or {@code postgresql}.
     */
    public String getStorageBackend() {
        return conf.getString("storage.backend", "sqlite").trim().toLowerCase();
    }

    /**
     * Whether to enable syncing with the PvPIndex API.
     * Falls back to the legacy {@code enableAutoSync} key if not present.
     */
    public boolean isEnableSync() {
        if (conf.contains("storage.sync.enabled")) {
            return conf.getBoolean("storage.sync.enabled", true);
        }
        return isEnableAutoSync();
    }

    public String getDbHost() {
        String backend = getStorageBackend();
        if ("postgresql".equals(backend) || "postgres".equals(backend)) {
            return conf.getString("storage.postgresql.host", "localhost");
        }
        return conf.getString("storage.mysql.host", "localhost");
    }

    public int getDbPort() {
        String backend = getStorageBackend();
        if ("postgresql".equals(backend) || "postgres".equals(backend)) {
            return conf.getInt("storage.postgresql.port", 5432);
        }
        return conf.getInt("storage.mysql.port", 3306);
    }

    public String getDbName() {
        String backend = getStorageBackend();
        if ("postgresql".equals(backend) || "postgres".equals(backend)) {
            return conf.getString("storage.postgresql.database", "mcbans");
        }
        return conf.getString("storage.mysql.database", "mcbans");
    }

    public String getDbUser() {
        String backend = getStorageBackend();
        if ("postgresql".equals(backend) || "postgres".equals(backend)) {
            return conf.getString("storage.postgresql.username", "mcbans");
        }
        return conf.getString("storage.mysql.username", "mcbans");
    }

    public String getDbPassword() {
        String backend = getStorageBackend();
        if ("postgresql".equals(backend) || "postgres".equals(backend)) {
            return conf.getString("storage.postgresql.password", "");
        }
        return conf.getString("storage.mysql.password", "");
    }

    public int getDbPoolSize() {
        String backend = getStorageBackend();
        if ("postgresql".equals(backend) || "postgres".equals(backend)) {
            return conf.getInt("storage.postgresql.pool-size", 5);
        }
        return conf.getInt("storage.mysql.pool-size", 5);
    }

    public boolean isDbUseSSL() {
        return conf.getBoolean("storage.mysql.useSSL", false);
    }

    public boolean isSendJoinMessage(){
        return conf.getBoolean("onJoinMCBansMessage", false);
    }
    public boolean isSendDetailPrevBans(){
        return conf.getBoolean("sendDetailPrevBansOnJoin", false);
    }
    public double getMinRep(){
        return conf.getDouble("minRep", 3.0D);
    }
    public int getCallBackInterval(){
        return conf.getInt("callBackInterval", 15);
    }
    public boolean isEncryption(){ return conf.getBoolean("encryption", false); }
    /*
    public boolean isSendPreviousBans(){
        return conf.getBoolean("sendPreviousBans", true);
    }
    */
    public int getTimeoutInSec(){
        return conf.getInt("timeout", 10);
    }
    public boolean isFailsafe(){
        return conf.getBoolean("failsafe", false);
    }

    // ─── Kick message templates ──────────────────────────────────────────────

    /**
     * Returns the kick message template for the given ban type.
     * {@code type} should be one of {@code "global"}, {@code "local"}, or {@code "temp"}.
     * Supports placeholders: {@code {reason}}, {@code {admin}}, {@code {expires}},
     * {@code {appeal_url}}.
     */
    public String getKickMessage(String type) {
        String template = conf.getString("kick-message." + type, null);
        if (template != null) {
            return template;
        }
        return switch (type) {
            case "temp"  -> "&cYou are &ltemporarily banned&r&c from this server.\n&fReason: &7{reason}\n&fBanned by: &7{admin}\n&fExpires: &7{expires}";
            case "local" -> "&cYou are &llocally banned&r&c from this server.\n&fReason: &7{reason}\n&fBanned by: &7{admin}";
            default      -> "&cYou are &lbanned&r&c from this server.\n&fReason: &7{reason}\n&fBanned by: &7{admin}";
        };
    }

    /** Returns the failsafe kick message (shown when API is unreachable and failsafe=true). */
    public String getKickFailsafeMessage() {
        return conf.getString("kick-message.failsafe",
                "&c[PvPIndex MCBans] &rUnable to verify your ban status. Please try again shortly.");
    }

    /** Returns the appeal URL appended to kick messages, or an empty string if not configured. */
    public String getKickAppealUrl() {
        return conf.getString("kick-message.appeal-url", "").trim();
    }

    // ─── Reason presets ─────────────────────────────────────────────────────

    /**
     * Resolves a raw reason string.  If {@code raw} starts with {@code #}, the
     * remainder is looked up as a preset key and the configured reason text is
     * returned.  If the key is unknown the original {@code raw} value is
     * returned unchanged so commands degrade gracefully.
     */
    public String resolveReason(String raw) {
        if (raw == null || !raw.startsWith("#")) {
            return raw;
        }
        String key = raw.substring(1).toLowerCase(Locale.ROOT);
        if (conf.isConfigurationSection("reason-presets." + key)) {
            String preset = conf.getString("reason-presets." + key + ".reason", null);
            return preset != null ? preset : raw;
        }
        String preset = conf.getString("reason-presets." + key, null);
        return preset != null ? preset : raw;
    }

    /**
     * Returns the {@code default-duration} value for a reason preset (e.g.
     * {@code "7d"}), or {@code null} if the preset does not define one.
     */
    public String getPresetDefaultDuration(String key) {
        String normKey = key.toLowerCase(Locale.ROOT);
        if (!conf.isConfigurationSection("reason-presets." + normKey)) {
            return null;
        }
        return conf.getString("reason-presets." + normKey + ".default-duration", null);
    }

    /**
     * Returns all defined reason-preset keys for tab completion.
     */
    public List<String> getReasonPresetKeys() {
        if (!conf.isConfigurationSection("reason-presets")) {
            return Collections.emptyList();
        }
        return new ArrayList<>(conf.getConfigurationSection("reason-presets").getKeys(false));
    }

    // ─── Preset validation ───────────────────────────────────────────────────

    private void validatePresets() {
        List<String> keys = getReasonPresetKeys();
        if (keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String resolved = resolveReason("#" + key);
            if (resolved.equals("#" + key)) {
                log.warning("Reason preset '" + key + "' has no 'reason' field - it will be ignored.");
            }
            String dur = getPresetDefaultDuration(key);
            if (dur != null && !dur.matches("\\d+(s|m|h|d|w)")) {
                log.warning("Reason preset '" + key
                        + "' has an invalid default-duration '" + dur
                        + "' (expected format e.g. 7d, 30m, 2h).");
            }
        }
        log.info("Loaded " + keys.size() + " reason preset(s): " + String.join(", ", keys));
    }
}
