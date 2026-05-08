/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.legacy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pvpindex.bans.storage.StorageBackend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Queries mcbans.com for legacy ban records.
 *
 * <p>The mcbans.com v2 API endpoint {@code /api/v2/player/{uuid}} returns a
 * JSON object indicating whether the player is banned globally on that network.
 * Responses are cached locally as {@code is_legacy = 1} records so subsequent
 * join checks are fast and do not re-query the remote API.</p>
 *
 * <p>All network calls are best-effort: any failure silently resolves to
 * {@code Optional.empty()} so a mcbans.com outage never disrupts login.</p>
 */
public class LegacyBanImporter {

    private static final String API_BASE    = "https://www.mcbans.com/api/v2/player/";
    private static final int    TIMEOUT_SEC = 3;

    private final StorageBackend storage;
    private final Logger logger;
    private final HttpClient httpClient;

    public LegacyBanImporter(StorageBackend storage, Logger logger) {
        this.storage    = storage;
        this.logger     = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SEC))
                .build();
    }

    /**
     * Checks whether the player is banned on mcbans.com.
     *
     * <p>If the player is found to be banned, the record is stored locally
     * (marked {@code is_legacy = true, is_synced = true}) before being returned.
     * If the player is not banned or the API is unreachable, returns
     * {@code Optional.empty()}.</p>
     *
     * @param uuid       player UUID without dashes, lowercase
     * @param playerName last known username (used when storing the record)
     * @return ban reason string if banned, empty otherwise
     */
    public Optional<String> checkAndImport(String uuid, String playerName) {
        try {
            String url = API_BASE + uuid;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                // Player not found on mcbans.com - not banned
                return Optional.empty();
            }

            if (response.statusCode() != 200) {
                logger.fine("[MCBans-Legacy] mcbans.com returned HTTP "
                        + response.statusCode() + " for UUID " + uuid);
                return Optional.empty();
            }

            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();

            // Expected response fields: status ("banned"/"clean"), reason, admin, server
            String status = body.has("status") ? body.get("status").getAsString() : "clean";
            if (!"banned".equalsIgnoreCase(status)) {
                return Optional.empty();
            }

            String reason    = body.has("reason") ? body.get("reason").getAsString() : "Legacy ban (mcbans.com)";
            String adminName = body.has("admin")  ? body.get("admin").getAsString()  : "mcbans.com";

            // Store the legacy ban locally so we don't need to re-query on next join
            storage.insertLegacyBan(uuid, playerName, reason, adminName);

            return Optional.of(reason);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            logger.fine("[MCBans-Legacy] Could not reach mcbans.com: " + e.getMessage());
            return Optional.empty();
        }
    }
}
