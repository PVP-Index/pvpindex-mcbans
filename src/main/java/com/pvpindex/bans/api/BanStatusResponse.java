package com.pvpindex.bans.api;

/**
 * Response from {@code GET /plugin/players/{uuid}/ban-status}.
 */
public record BanStatusResponse(boolean banned, BanRecord ban) {}
