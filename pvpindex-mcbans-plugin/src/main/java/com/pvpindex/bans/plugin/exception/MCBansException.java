/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.exception;

public class MCBansException extends Exception{
    private static final long serialVersionUID = -1420944571331163458L;

    public MCBansException(final String message){
        super(message);
    }

    public MCBansException(final Throwable cause){
        super(cause);
    }

    public MCBansException(final String message, final Throwable cause){
        super(message, cause);
    }
}
