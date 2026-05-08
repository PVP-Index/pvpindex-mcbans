/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.permission;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.pvpindex.bans.plugin.ActionLog;
import com.pvpindex.bans.plugin.MCBans;

public class PermissionHandler {
    public enum PermType {
        VAULT,
        PEX,
        SUPERPERMS,
        OPS,
        ;
    }

    // instance
    private static PermissionHandler instance;

    private final MCBans plugin;
    private final ActionLog log;
    private PermType permType = null;

    // permission plugin
    private net.milkbowl.vault.permission.Permission vaultPermission = null; // not import package
    // PermissionsEx removed - not available on Paper 1.21

    /**
     * Constructor
     * @param plugin MCBans instance
     */
    private PermissionHandler(final MCBans plugin){
        this.plugin = plugin;
        this.log = plugin.getLog();
        instance = this;
    }

    /**
     * Setup and Select permission controller
     * @param silent false if send message to console
     */
    public void setupPermissions(final boolean silent){
        final String selected = plugin.getConfigs().getPermission().trim();
        boolean found = true;

        if ("vault".equalsIgnoreCase(selected)){
            if (setupVaultPermission()){
                permType = PermType.VAULT;
            }else{
                log.warning("Selected Vault for permission control, but MCBans did not find this plugin!");
            }
        }
        else if ("pex".equalsIgnoreCase(selected) || "permissionsex".equalsIgnoreCase(selected)){
            if (setupPEXPermission()){
                permType = PermType.PEX;
            }else{
                log.warning("Selected PermissionsEx for permission control, but MCBans did not find this plugin!");
            }
        }
        else if ("superperms".equalsIgnoreCase(selected)){
            permType = PermType.SUPERPERMS;
        }
        else if ("ops".equalsIgnoreCase(selected)){
            permType = PermType.OPS;
        }
        else{
            found = false;
        }

        // Invalid configuration, Use default SuperPerms
        if (permType == null){
            permType = PermType.SUPERPERMS;
            if (!found) log.warning("Valid permissions name not selected!");
        }

        // Display result
        if (!silent){
            log.info("Using " + getPermTypeString() + " for permission control.");
        }
    }
    public void setupPermissions(){
        this.setupPermissions(false);
    }

    /**
     * Check permissible has the permission.
     * @param permissible check target Sender, Player etc.
     * @param permission check permission node.
     * @return true if permissible has that permission.
     */
    public boolean has(final Permissible permissible, final String permission){
        // Console / Rcon has all permission, return true
        if (!(permissible instanceof Player)){
            return true;
        }
        
        Player player = (Player) permissible;

        // Switch by using permission controller
        switch (permType){
            // Vault
            case VAULT:
                return vaultPermission.has(player, permission);

            // PEX (not supported - fallthrough to SuperPerms)
            case PEX:
                return player.hasPermission(new org.bukkit.permissions.Permission(permission));

            // SuperPerms
            case SUPERPERMS:
                return player.hasPermission(new org.bukkit.permissions.Permission(permission));

            // Ops
            case OPS:
                return player.isOp();

            // Other Types, forgot to add here
            default:
                log.warning("Plugin author forgot to add integration for this permission plugin. Please report this!");
                return false;
        }
    }

    /**
     * Check player has the permission in specific world. (working only Vault or Pex)
     * @param playerName check target player name. maybe needs online.
     * @param permission check permission node.
     * @param worldName check target world name.
     * @return true if player has that permission in specific world.
     */
    public boolean has(final String playerName, final String permission, final String worldName){
        // Switch by using permission controller
        switch (permType){
            // Vault
            case VAULT:
                return vaultPermission.playerHas(worldName, plugin.getServer().getPlayer(playerName), permission);

            // PEX (not supported - fallthrough to SuperPerms)
            case PEX: {
                Player player = plugin.getServer().getPlayer(playerName);
                if (player == null) return false;
                return player.hasPermission(permission);
            }

            // SuperPerms
            case SUPERPERMS: {
                // NOTE: SuperPerms has not Cross-World permission system, So this check is not working properly.
                Player player = plugin.getServer().getPlayer(playerName);
                if (player == null) return false;
                else return player.hasPermission(permission);
            }

            // Ops
            case OPS:{
                Player player = plugin.getServer().getPlayer(playerName);
                if (player == null) return false;
                else return player.isOp();
            }

            // Other Types, forgot add here
            default:
                log.warning("Plugin author forgot to add integration for this permission plugin. Please report this!");
                return false;
        }
    }

    /**
     * Get using permission controller name
     * @return string controller name
     */
    public String getPermTypeString(){
        // Switch by using permission controller
        switch (permType){
            case VAULT:
                return "Vault: " + Bukkit.getServer().getServicesManager().getRegistration(Permission.class).getProvider().getName();

            case PEX:
                return "PermissionsEx";

            case OPS:
                return "OPs";

            case SUPERPERMS:
                return "SuperPerms";

            default:
                return "Unknown! Please report this!";
        }
    }

    // ** setup controller plugins *****
    /**
     * Setup Vault plugin
     * @return boolean true if success
     */
    private boolean setupVaultPermission(){
        Plugin vault = plugin.getServer().getPluginManager().getPlugin("Vault");
        if (vault == null) vault = plugin.getServer().getPluginManager().getPlugin("vault");
        if (vault == null) return false;
        try{
            RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null){
                vaultPermission = permissionProvider.getProvider();
            }

        }catch (Exception ex){
            log.warning("Unexpected error trying to setup Vault permissions!");
            ex.printStackTrace();
        }

        return (vaultPermission != null);
    }

    /**
     * Setup PermissionsEx plugin
     * @return boolean true if success
     */
    private boolean setupPEXPermission(){
        // PermissionsEx is not available on Paper 1.21
        return false;
    }
    // ** end **************************

    /**
     * Get singleton instance
     * @return PermissionHandler instance
     */
    public static PermissionHandler getInstance(){
        if (instance == null){
            synchronized (PermissionHandler.class){
                if (instance == null){
                    instance = new PermissionHandler(MCBans.getInstance());
                }
            }
        }
        return instance;
    }
}
