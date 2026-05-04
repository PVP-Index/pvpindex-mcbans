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
