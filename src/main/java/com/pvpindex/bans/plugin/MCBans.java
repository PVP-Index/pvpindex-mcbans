package com.pvpindex.bans.plugin;

import com.pvpindex.bans.api.PvPIndexApiClient;
import com.pvpindex.bans.plugin.bukkitListeners.PlayerListener;
import com.pvpindex.bans.plugin.callBacks.BanSync;
import com.pvpindex.bans.plugin.commands.BaseCommand;
import com.pvpindex.bans.plugin.commands.CommandAltlookup;
import com.pvpindex.bans.plugin.commands.CommandBan;
import com.pvpindex.bans.plugin.commands.CommandBanip;
import com.pvpindex.bans.plugin.commands.CommandBanlookup;
import com.pvpindex.bans.plugin.commands.CommandGlobalban;
import com.pvpindex.bans.plugin.commands.CommandKick;
import com.pvpindex.bans.plugin.commands.CommandLookup;
import com.pvpindex.bans.plugin.commands.CommandMCBans;
import com.pvpindex.bans.plugin.commands.CommandMCBansSettings;
import com.pvpindex.bans.plugin.commands.CommandPrevious;
import com.pvpindex.bans.plugin.commands.CommandRban;
import com.pvpindex.bans.plugin.commands.CommandTempban;
import com.pvpindex.bans.plugin.commands.CommandUnban;
import com.pvpindex.bans.plugin.commands.MCBansCommandHandler;
import com.pvpindex.bans.plugin.permission.Perms;
import com.pvpindex.bans.storage.BanDao;
import com.pvpindex.bans.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.pvpindex.bans.plugin.api.MCBansAPI;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MCBans extends JavaPlugin {

    private static MCBans instance;

    // Infrastructure
    private StorageManager storageManager;
    private BanDao banDao;
    private PvPIndexApiClient apiClient;

    // Sync
    public BanSync bansync = null;
    public long lastSync = 0;
    public boolean syncRunning = false;

    // Misc
    private ActionLog log;
    private ConfigurationManager config;
    private MCBansCommandHandler commandHandler;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {
        instance = this;
        log = new ActionLog(this);

        // Load config
        config = new ConfigurationManager(this);
        try {
            config.loadConfig(true);
        } catch (Exception ex) {
            log.warning("An error occurred while loading the config file.");
            ex.printStackTrace();
        }

        PluginManager pm = getServer().getPluginManager();
        if (!pm.isPluginEnabled(this)) {
            return;
        }

        // SQLite storage
        storageManager = new StorageManager(this);
        try {
            storageManager.initialise();
        } catch (SQLException | ClassNotFoundException e) {
            log.severe("Failed to open SQLite database: " + e.getMessage());
            pm.disablePlugin(this);
            return;
        }
        banDao = new BanDao(storageManager.getConnection(), getLogger());

        // HTTP API client
        apiClient = new PvPIndexApiClient(
                config.getApiUrl(),
                config.getApiKey(),
                getLogger());

        // Language
        log.info("Loading language file: " + config.getLanguage());
        I18n.init(config.getLanguage());

        // Events
        pm.registerEvents(new PlayerListener(this), this);

        // Permissions
        Perms.setupPermissionHandler();

        // Commands
        commandHandler = new MCBansCommandHandler(this);
        registerCommands();

        // Background ban-sync thread
        bansync = new BanSync(this);
        bansync.start();

        // Perform an initial sync on startup
        new Thread(() -> bansync.performSync(), "MCBans-InitialSync").start();

        final PluginDescriptionFile pdf = this.getDescription();
        log.info(pdf.getName() + " version " + pdf.getVersion() + " is enabled!");
    }

    @Override
    public void onDisable() {
        if (bansync != null) {
            bansync.stopSync();
        }

        if (storageManager != null) {
            storageManager.close();
        }

        getServer().getScheduler().cancelTasks(this);
        instance = null;

        final PluginDescriptionFile pdf = this.getDescription();
        log.info(pdf.getName() + " version " + pdf.getVersion() + " is disabled!");
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandHandler.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return commandHandler.onTabComplete(sender, command, alias, args);
    }

    private void registerCommands() {
        List<BaseCommand> cmds = new ArrayList<>();
        cmds.add(new CommandBan());
        cmds.add(new CommandGlobalban());
        cmds.add(new CommandTempban());
        cmds.add(new CommandRban());
        cmds.add(new CommandBanip());
        cmds.add(new CommandUnban());
        cmds.add(new CommandKick());
        cmds.add(new CommandLookup());
        cmds.add(new CommandBanlookup());
        cmds.add(new CommandAltlookup());
        cmds.add(new CommandMCBans());
        cmds.add(new CommandPrevious());
        cmds.add(new CommandMCBansSettings());
        for (BaseCommand cmd : cmds) {
            commandHandler.registerCommand(cmd);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void debug(String message) {
        if (config.isDebug()) {
            log.info(message);
        }
    }

    public ConfigurationManager getConfigs() {
        return config;
    }

    public ActionLog getLog() {
        return log;
    }

    public BanDao getBanDao() {
        return banDao;
    }

    public PvPIndexApiClient getApiClient() {
        return apiClient;
    }

    /**
     * Replaces the API client — for unit testing only.
     * Allows injecting a stub without modifying config or network.
     */
    void setApiClientForTesting(PvPIndexApiClient client) {
        this.apiClient = client;
    }

    public MCBansAPI getAPI(Plugin plugin) {
        return MCBansAPI.getHandle(this, plugin);
    }

    /** Legacy compatibility flag: broadcast bans to all online players. */
    public static boolean announceAll = false;

    public static String getPrefix() {
        return instance.config.getPrefix();
    }

    public static MCBans getInstance() {
        return instance;
    }

    public static UUID fromString(String uuid) {
        return UUID.fromString(uuid.replaceAll(
                "(?ism)([a-z0-9]{8})([a-z0-9]{4})([a-z0-9]{4})([a-z0-9]{4})([a-z0-9]{12})",
                "$1-$2-$3-$4-$5"));
    }

    public static Player getPlayer(Plugin plugin, UUID uuid) {
        return plugin.getServer().getPlayer(uuid);
    }

    public static Player getPlayer(Plugin plugin, String target) {
        return plugin.getServer().getPlayerExact(target);
    }
}
