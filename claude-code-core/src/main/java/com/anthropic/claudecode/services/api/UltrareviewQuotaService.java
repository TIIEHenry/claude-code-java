/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/ultrareviewQuota
 */
package com.anthropic.claudecode.services.api;

import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.time.*;

/**
 * Ultrareview quota service - Peek ultrareview quota for display and nudge decisions.
 */
public final class UltrareviewQuotaService {
    private final java.net.http.HttpClient httpClient;
    private final String baseUrl;

    /**
     * Create ultrareview quota service.
     */
    public UltrareviewQuotaService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Ultrareview quota response record.
     */
    public record UltrareviewQuotaResponse(
        int reviewsUsed,
        int reviewsLimit,
        int reviewsRemaining,
        boolean isOverage
    ) {
        public static UltrareviewQuotaResponse empty() {
            return new UltrareviewQuotaResponse(0, 0, 0, false);
        }
    }

    /**
     * Fetch ultrareview quota.
     * Returns null when not a subscriber or endpoint errors.
     */
    public CompletableFuture<UltrareviewQuotaResponse> fetchUltrareviewQuota() {
        if (!isClaudeAISubscriber()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/ultrareview/quota"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", getAuthHeader())
                    .header("x-organization-uuid", getOrgUUID())
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    return null;
                }

                return parseResponse(response.body());
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Check if quota is exceeded.
     */
    public CompletableFuture<Boolean> isQuotaExceeded() {
        return fetchUltrareviewQuota().thenApply(quota -> {
            if (quota == null) {
                return false;
            }
            return quota.reviewsRemaining() <= 0 || quota.isOverage();
        });
    }

    /**
     * Check if quota is low (below threshold).
     */
    public CompletableFuture<Boolean> isQuotaLow(int threshold) {
        return fetchUltrareviewQuota().thenApply(quota -> {
            if (quota == null) {
                return false;
            }
            return quota.reviewsRemaining() < threshold && quota.reviewsRemaining() > 0;
        });
    }

    // Helper methods
    private boolean isClaudeAISubscriber() {
        String subscriber = System.getenv("CLAUDE_AI_SUBSCRIBER");
        return "true".equals(subscriber);
    }

    private String getAuthHeader() {
        String token = System.getenv("CLAUDE_CODE_ACCESS_TOKEN");
        return token != null ? "Bearer " + token : "";
    }

    private String getOrgUUID() {
        return System.getenv("CLAUDE_CODE_ORG_UUID");
    }

    private UltrareviewQuotaResponse parseResponse(String json) {
        // Simplified parsing
        return UltrareviewQuotaResponse.empty();
    }
}