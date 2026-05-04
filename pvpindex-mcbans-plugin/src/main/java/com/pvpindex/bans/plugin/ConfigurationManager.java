package com.pvpindex.bans.plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;

import com.pvpindex.bans.plugin.util.FileStructure;
import com.pvpindex.bans.plugin.util.Util;


public class ConfigurationManager {
    /* Current config.yml File Version! */
    private final int latestVersion = 2;

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

        // check API key
        if (conf.getString("pvpindex.apiKey", "").trim().isEmpty()){
            isValidKey = false;
            if (initialLoad){
                log.warning("No API key configured — ban sync is disabled.");
                log.warning("Set pvpindex.apiKey in config.yml. Get a key at: https://pvpindex.com/apply");
            }else{
                log.warning("API key missing or invalid — please check config.yml.");
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

            plugin.reloadConfig();
            conf = plugin.getConfig();

            log.info("Generated fresh config.yml.");
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
}
