/*
 * This file is part of PvPIndex MCBans.
 *
 * Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.api;

/**
 * Response from {@code GET /plugin/players/{uuid}/ban-status}.
 */
public record BanStatusResponse(boolean banned, BanRecord ban) {}
