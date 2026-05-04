package com.pvpindex.bans.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Thin HTTP client for the PvPIndex REST API.
 *
 * <ul>
 *   <li>All methods return {@code Optional.empty()} when the API is unreachable or returns a
 *       non-2xx status code, so callers can fall back to the local SQLite cache without crashing.</li>
 *   <li>Connect timeout: 3 s. Request timeout: 5 s.</li>
 *   <li>Thread-safe — the underlying {@link HttpClient} is shared.</li>
 * </ul>
 */
public class PvPIndexApiClient {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;
    private final Gson gson = new Gson();
    private final Logger logger;

    /** Duration after which a temp-ban "expires_at" field is omitted (permanent). */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    public PvPIndexApiClient(String baseUrl, String apiKey, Logger logger) {
        this.baseUrl = baseUrl.replaceAll("/$", ""); // strip trailing slash
        this.apiKey  = apiKey;
        this.logger  = logger;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Constructor with an injectable {@link HttpClient} \u2014 for unit testing only.
     * Allows tests to supply a pre-configured client (e.g. pointing at an embedded HTTP server)
     * without touching production network configuration.
     */
    PvPIndexApiClient(String baseUrl, String apiKey, Logger logger, HttpClient http) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey  = apiKey;
        this.logger  = logger;
        this.http    = http;
    }

    // -------------------------------------------------------------------------
    // Ban status
    // -------------------------------------------------------------------------

    /**
     * Check whether a player is banned on PvPIndex.
     *
     * @param minecraftUuid the player UUID, with or without dashes
     * @return empty if API is down or player not found; a {@link BanStatusResponse} otherwise
     */
    public Optional<BanStatusResponse> getBanStatus(String minecraftUuid) {
        String uuid = stripDashes(minecraftUuid);
        HttpRequest req = buildGet("/plugin/players/" + uuid + "/ban-status");
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 404) {
                // Player does not exist in PvPIndex yet — not banned
                return Optional.of(new BanStatusResponse(false, null));
            }
            if (!is2xx(resp)) {
                logger.warning("[PvPIndex] ban-status returned " + resp.statusCode());
                return Optional.empty();
            }
            JsonObject body = JsonParser.parseString(resp.body()).getAsJsonObject();
            boolean banned = body.get("banned").getAsBoolean();
            BanRecord record = null;
            if (banned && body.has("ban")) {
                record = parseBanRecord(body.getAsJsonObject("ban"));
            }
            return Optional.of(new BanStatusResponse(banned, record));
        } catch (IOException | InterruptedException e) {
            logger.warning("[PvPIndex] ban-status request failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Ban / Unban
    // -------------------------------------------------------------------------

    /**
     * Submit a ban to PvPIndex.
     *
     * @return the newly created {@link BanRecord}, or empty if the API is down
     */
    public Optional<BanRecord> ban(BanRequest banRequest) {
        String uuid = stripDashes(banRequest.playerUuid());
        String body = gson.toJson(banRequest);
        HttpRequest req = buildPost("/plugin/players/" + uuid + "/ban", body);
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (!is2xx(resp)) {
                logger.warning("[PvPIndex] ban returned " + resp.statusCode() + ": " + resp.body());
                return Optional.empty();
            }
            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            return Optional.of(parseBanRecord(json.getAsJsonObject("ban")));
        } catch (IOException | InterruptedException e) {
            logger.warning("[PvPIndex] ban request failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Unban a player on PvPIndex.
     *
     * @return {@code true} if the unban succeeded, {@code false} if the API is down or returned an error
     */
    public boolean unban(String minecraftUuid) {
        String uuid = stripDashes(minecraftUuid);
        HttpRequest req = buildDelete("/plugin/players/" + uuid + "/ban");
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return is2xx(resp) || resp.statusCode() == 404; // 404 = already not banned → treat as success
        } catch (IOException | InterruptedException e) {
            logger.warning("[PvPIndex] unban request failed: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Delta sync
    // -------------------------------------------------------------------------

    /**
     * Fetch one page of the bans delta.
     *
     * @param updatedSince ISO-8601 timestamp (may be null for full sync)
     * @param page         1-based page number
     * @return empty if API down; a {@link BanSyncPage} otherwise
     */
    public Optional<BanSyncPage> getBans(String updatedSince, int page) {
        String path = "/plugin/bans?page=" + page
                + (updatedSince != null ? "&updated_since=" + updatedSince : "");
        HttpRequest req = buildGet(path);
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (!is2xx(resp)) {
                logger.warning("[PvPIndex] /plugin/bans returned " + resp.statusCode());
                return Optional.empty();
            }
            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            List<BanRecord> records = new ArrayList<>();
            JsonArray data = json.getAsJsonArray("data");
            if (data != null) {
                for (JsonElement el : data) {
                    records.add(parseBanRecord(el.getAsJsonObject()));
                }
            }
            JsonObject meta = json.has("meta") ? json.getAsJsonObject("meta") : null;
            int lastPage = meta != null && meta.has("last_page") ? meta.get("last_page").getAsInt() : 1;
            return Optional.of(new BanSyncPage(records, page, lastPage));
        } catch (IOException | InterruptedException e) {
            logger.warning("[PvPIndex] /plugin/bans request failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpRequest buildGet(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private HttpRequest buildPost(String path, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest buildDelete(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .DELETE()
                .build();
    }

    private static boolean is2xx(HttpResponse<?> resp) {
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }

    private static String stripDashes(String uuid) {
        return uuid == null ? null : uuid.toLowerCase().replace("-", "");
    }

    private BanRecord parseBanRecord(JsonObject obj) {
        String id           = getStringOrNull(obj, "id");
        String playerUuid   = getStringOrNull(obj, "player_uuid");
        String playerName   = getStringOrNull(obj, "player_username");
        String type         = getStringOrNull(obj, "type");
        String reason       = getStringOrNull(obj, "reason");
        String adminUuid    = getStringOrNull(obj, "admin_uuid");
        String adminName    = getStringOrNull(obj, "admin_name");
        String expiresAt    = getStringOrNull(obj, "expires_at");
        boolean isActive    = obj.has("is_active") && obj.get("is_active").getAsBoolean();
        String updatedAt    = getStringOrNull(obj, "updated_at");
        return new BanRecord(
                id, playerUuid, playerName, type, reason, adminUuid, adminName, expiresAt, isActive, updatedAt);
    }

    private static String getStringOrNull(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }
}
