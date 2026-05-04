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

public class CommandException extends MCBansException{
    private static final long serialVersionUID = 7018784682407110223L;

    public CommandException(final String message){
        super(message);
    }

    public CommandException(final Throwable cause){
        super(cause);
    }

    public CommandException(final String message, final Throwable cause){
        super(message, cause);
    }
}
