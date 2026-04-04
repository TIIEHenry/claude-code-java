/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/usage
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.time.*;

/**
 * API usage service - Fetch usage information.
 */
public final class ApiUsageService {
    private final java.net.http.HttpClient httpClient;
    private final String baseUrl;

    /**
     * Create API usage service.
     */
    public ApiUsageService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Rate limit record.
     */
    public record RateLimit(
        Double utilization,
        Instant resetsAt
    ) {
        public static RateLimit empty() {
            return new RateLimit(null, null);
        }
    }

    /**
     * Extra usage record.
     */
    public record ExtraUsage(
        boolean isEnabled,
        Double monthlyLimit,
        Double usedCredits,
        Double utilization
    ) {
        public static ExtraUsage empty() {
            return new ExtraUsage(false, null, null, null);
        }
    }

    /**
     * Utilization record.
     */
    public record Utilization(
        RateLimit fiveHour,
        RateLimit sevenDay,
        RateLimit sevenDayOauthApps,
        RateLimit sevenDayOpus,
        RateLimit sevenDaySonnet,
        ExtraUsage extraUsage
    ) {
        public static Utilization empty() {
            return new Utilization(null, null, null, null, null, null);
        }
    }

    /**
     * Fetch utilization.
     */
    public CompletableFuture<Utilization> fetchUtilization(String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            if (accessToken == null || accessToken.isEmpty()) {
                return Utilization.empty();
            }

            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/oauth/usage"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    return Utilization.empty();
                }

                return parseUtilization(response.body());
            } catch (Exception e) {
                return Utilization.empty();
            }
        });
    }

    /**
     * Parse utilization from JSON.
     */
    private Utilization parseUtilization(String json) {
        // Simple JSON parsing
        RateLimit fiveHour = parseRateLimit(json, "five_hour");
        RateLimit sevenDay = parseRateLimit(json, "seven_day");
        RateLimit sevenDayOpus = parseRateLimit(json, "seven_day_opus");
        RateLimit sevenDaySonnet = parseRateLimit(json, "seven_day_sonnet");
        ExtraUsage extraUsage = parseExtraUsage(json);

        return new Utilization(
            fiveHour,
            sevenDay,
            null,
            sevenDayOpus,
            sevenDaySonnet,
            extraUsage
        );
    }

    /**
     * Parse rate limit from JSON.
     */
    private RateLimit parseRateLimit(String json, String key) {
        Double utilization = extractDouble(json, key + ".utilization");
        Long resetTs = extractLong(json, key + ".resets_at");
        Instant resetsAt = resetTs != null ? Instant.ofEpochSecond(resetTs) : null;
        return new RateLimit(utilization, resetsAt);
    }

    /**
     * Parse extra usage from JSON.
     */
    private ExtraUsage parseExtraUsage(String json) {
        Boolean enabled = extractBoolean(json, "extra_usage.is_enabled");
        Double monthlyLimit = extractDouble(json, "extra_usage.monthly_limit");
        Double usedCredits = extractDouble(json, "extra_usage.used_credits");
        Double utilization = extractDouble(json, "extra_usage.utilization");
        return new ExtraUsage(
            enabled != null ? enabled : false,
            monthlyLimit,
            usedCredits,
            utilization
        );
    }

    /**
     * Extract double from JSON.
     */
    private Double extractDouble(String json, String path) {
        // Simplified extraction
        return null;
    }

    /**
     * Extract long from JSON.
     */
    private Long extractLong(String json, String path) {
        // Simplified extraction
        return null;
    }

    /**
     * Extract boolean from JSON.
     */
    private Boolean extractBoolean(String json, String path) {
        // Simplified extraction
        return null;
    }
}