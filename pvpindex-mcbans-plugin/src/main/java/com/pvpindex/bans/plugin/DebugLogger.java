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

/**
 * Centralised debug-logging helper.
 *
 * <p>All verbose/debug output produced by the plugin flows through this class
 * so that the {@code isDebug} flag has a single enforcement point and debug
 * calls can be easily found, enabled, or disabled without touching every
 * call-site.</p>
 */
public class DebugLogger {

    private final ActionLog log;
    private final ConfigurationManager config;

    public DebugLogger(ActionLog log, ConfigurationManager config) {
        this.log    = log;
        this.config = config;
    }

    /**
     * Emit {@code message} to the server log when {@code isDebug} is {@code true}.
     *
     * @param message the debug message
     */
    public void debug(String message) {
        if (config.isDebug()) {
            log.info(message);
        }
    }

    /**
     * Emit a formatted debug message using {@link String#format} when
     * {@code isDebug} is {@code true}.
     *
     * @param format  the format string (same syntax as {@link String#format})
     * @param args    format arguments
     */
    public void debug(String format, Object... args) {
        if (config.isDebug()) {
            log.info(String.format(format, args));
        }
    }
}
