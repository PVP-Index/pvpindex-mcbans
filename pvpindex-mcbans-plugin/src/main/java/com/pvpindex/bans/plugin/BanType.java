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

import com.pvpindex.bans.plugin.permission.Perms;

public enum BanType {
    GLOBAL  ("globalBan", Perms.BAN_GLOBAL),
    LOCAL   ("localBan", Perms.BAN_LOCAL),
    TEMP    ("tempBan", Perms.BAN_TEMP),

    UNBAN   ("unBan", Perms.UNBAN),
    ;

    final private String actionName;
    final private Perms permission;

    private BanType(final String actionName, final Perms permission){
        this.actionName = actionName;
        this.permission = permission;
    }

    public String getActionName(){
        return this.actionName;
    }

    public Perms getPermission(){
        return this.permission;
    }
}
