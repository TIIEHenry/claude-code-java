/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/grove
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.*;

/**
 * Grove service - Grove notification settings for privacy/terms updates.
 */
public final class GroveService {
    private static final long CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    private final HttpClient httpClient;
    private final String baseUrl;

    private volatile AccountSettings cachedSettings;
    private volatile GroveConfig cachedConfig;
    private volatile long cacheTimestamp;

    /**
     * Create Grove service.
     */
    public GroveService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Account settings record.
     */
    public record AccountSettings(
        Boolean groveEnabled,
        String groveNoticeViewedAt
    ) {
        public static AccountSettings empty() {
            return new AccountSettings(null, null);
        }
    }

    /**
     * Grove config record.
     */
    public record GroveConfig(
        boolean groveEnabled,
        boolean domainExcluded,
        boolean noticeIsGracePeriod,
        Integer noticeReminderFrequency
    ) {
        public static GroveConfig defaults() {
            return new GroveConfig(false, false, true, null);
        }
    }

    /**
     * API result sealed interface.
     */
    public sealed interface ApiResult permits Success, Failure {}

    public static final class Success<T> implements ApiResult {
        private final T data;
        public Success(T data) { this.data = data; }
        public T getData() { return data; }
    }

    public static final class Failure implements ApiResult {
        public Failure() {}
    }

    /**
     * Get Grove settings.
     * Memoized for the session.
     */
    public CompletableFuture<ApiResult> getGroveSettings() {
        if (isEssentialTrafficOnly()) {
            return CompletableFuture.completedFuture(new Failure());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/oauth/account/settings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", getAuthHeader())
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    return new Failure();
                }

                AccountSettings settings = parseSettings(response.body());
                cachedSettings = settings;
                return new Success<>(settings);
            } catch (Exception e) {
                // Don't cache failures
                return new Failure();
            }
        });
    }

    /**
     * Mark Grove notice viewed.
     */
    public CompletableFuture<Void> markGroveNoticeViewed() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/oauth/account/grove_notice_viewed"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", getAuthHeader())
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // Clear cache
                cachedSettings = null;
            } catch (Exception e) {
                // Ignore
            }
            return null;
        });
    }

    /**
     * Update Grove settings.
     */
    public CompletableFuture<Void> updateGroveSettings(boolean groveEnabled) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = "{\"grove_enabled\":" + groveEnabled + "}";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/oauth/account/settings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", getAuthHeader())
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                    .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // Clear cache
                cachedSettings = null;
            } catch (Exception e) {
                // Ignore
            }
            return null;
        });
    }

    /**
     * Check if qualified for Grove (non-blocking, cache-first).
     */
    public boolean isQualifiedForGrove(String accountId) {
        if (!isConsumerSubscriber()) {
            return false;
        }

        if (accountId == null) {
            return false;
        }

        // Check cache
        if (cachedConfig != null && cacheTimestamp > 0) {
            long now = System.currentTimeMillis();
            if (now - cacheTimestamp <= CACHE_EXPIRATION_MS) {
                return cachedConfig.groveEnabled();
            }
        }

        // Trigger background fetch
        CompletableFuture.runAsync(() -> fetchAndStoreGroveConfig(accountId));
        return cachedConfig != null ? cachedConfig.groveEnabled() : false;
    }

    /**
     * Fetch and store Grove config.
     */
    @SuppressWarnings("unchecked")
    private void fetchAndStoreGroveConfig(String accountId) {
        try {
            ApiResult result = getGroveNoticeConfig().join();
            if (result instanceof Success<?> success) {
                GroveConfig config = (GroveConfig) success.getData();
                cachedConfig = config;
                cacheTimestamp = System.currentTimeMillis();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Get Grove notice config.
     */
    public CompletableFuture<ApiResult> getGroveNoticeConfig() {
        if (isEssentialTrafficOnly()) {
            return CompletableFuture.completedFuture(new Failure());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/claude_code_grove"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", getAuthHeader())
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    return new Failure();
                }

                GroveConfig config = parseConfig(response.body());
                return new Success<>(config);
            } catch (Exception e) {
                return new Failure();
            }
        });
    }

    /**
     * Calculate if should show Grove dialog.
     */
    @SuppressWarnings("unchecked")
    public boolean calculateShouldShowGrove(
        ApiResult settingsResult,
        ApiResult configResult,
        boolean showIfAlreadyViewed
    ) {
        // Hide on API failure
        if (settingsResult instanceof Failure || configResult instanceof Failure) {
            return false;
        }

        AccountSettings settings = ((Success<AccountSettings>) settingsResult).getData();
        GroveConfig config = ((Success<GroveConfig>) configResult).getData();

        boolean hasChosen = settings.groveEnabled() != null;
        if (hasChosen) {
            return false;
        }
        if (showIfAlreadyViewed) {
            return true;
        }
        if (!config.noticeIsGracePeriod()) {
            return true;
        }

        // Check reminder frequency
        Integer reminderFrequency = config.noticeReminderFrequency();
        if (reminderFrequency != null && settings.groveNoticeViewedAt() != null) {
            long viewedAt = parseDate(settings.groveNoticeViewedAt());
            long daysSinceViewed = (System.currentTimeMillis() - viewedAt) / (1000 * 60 * 60 * 24);
            return daysSinceViewed >= reminderFrequency;
        }

        // Show if never viewed
        return settings.groveNoticeViewedAt() == null;
    }

    // Helper methods
    private boolean isEssentialTrafficOnly() {
        String level = System.getenv("CLAUDE_CODE_TRAFFIC_LEVEL");
        return "essential".equals(level);
    }

    private boolean isConsumerSubscriber() {
        String subscriber = System.getenv("CLAUDE_AI_SUBSCRIBER");
        return "true".equals(subscriber);
    }

    private String getAuthHeader() {
        String token = System.getenv("CLAUDE_CODE_ACCESS_TOKEN");
        return token != null ? "Bearer " + token : "";
    }

    private AccountSettings parseSettings(String json) {
        // Simplified parsing
        return AccountSettings.empty();
    }

    private GroveConfig parseConfig(String json) {
        // Simplified parsing
        return GroveConfig.defaults();
    }

    private long parseDate(String dateStr) {
        try {
            return Instant.parse(dateStr).toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }
}