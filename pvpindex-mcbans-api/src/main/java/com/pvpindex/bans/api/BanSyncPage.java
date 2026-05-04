/*
 * This file is part of PvPIndex MCBans.
 *
 * Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.api;

import java.util.List;

/**
 * One page of results from {@code GET /plugin/bans}.
 */
public record BanSyncPage(List<BanRecord> records, int currentPage, int lastPage) {
    public boolean hasMore() {
        return currentPage < lastPage;
    }
}
