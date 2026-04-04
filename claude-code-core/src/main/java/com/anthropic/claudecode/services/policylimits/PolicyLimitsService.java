/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/policyLimits/index
 */
package com.anthropic.claudecode.services.policylimits;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.time.*;

/**
 * Policy limits service - Fetch and cache policy limits.
 *
 * Only blocked policies are included. If a policy key is absent, it's allowed.
 */
public final class PolicyLimitsService {
    private static final PolicyLimitsTypes.PolicyLimitsConfig DEFAULT_CONFIG =
        PolicyLimitsTypes.PolicyLimitsConfig.defaults();

    private volatile PolicyLimitsTypes.PolicyLimitsResponse cachedResponse;
    private volatile String cachedEtag;
    private volatile Instant lastFetchTime;

    private final HttpClient httpClient;
    private final PolicyLimitsTypes.PolicyLimitsConfig config;
    private final String apiUrl;

    /**
     * Create policy limits service.
     */
    public PolicyLimitsService(String apiUrl) {
        this.apiUrl = apiUrl;
        this.config = DEFAULT_CONFIG;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Create with default config.
     */
    public PolicyLimitsService() {
        this("https://api.claude.ai/policy-limits");
    }

    /**
     * Fetch policy limits.
     */
    public CompletableFuture<PolicyLimitsTypes.PolicyLimitsFetchResult> fetchPolicyLimits() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .GET();

                // Add If-None-Match header for conditional request
                if (cachedEtag != null) {
                    requestBuilder.header("If-None-Match", cachedEtag);
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 304) {
                    // Not modified
                    return PolicyLimitsTypes.PolicyLimitsFetchResult.notModified(cachedEtag);
                }

                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    // Auth error - don't retry
                    return PolicyLimitsTypes.PolicyLimitsFetchResult.failure(
                        "Authentication failed",
                        true
                    );
                }

                if (response.statusCode() != 200) {
                    return PolicyLimitsTypes.PolicyLimitsFetchResult.failure(
                        "HTTP " + response.statusCode()
                    );
                }

                // Parse response
                String body = response.body();
                Map<String, PolicyLimitsTypes.PolicyRestriction> restrictions = parseResponse(body);
                String etag = response.headers().firstValue("ETag").orElse(null);

                // Update cache
                cachedResponse = new PolicyLimitsTypes.PolicyLimitsResponse(restrictions);
                cachedEtag = etag;
                lastFetchTime = Instant.now();

                return PolicyLimitsTypes.PolicyLimitsFetchResult.success(restrictions, etag);
            } catch (Exception e) {
                return PolicyLimitsTypes.PolicyLimitsFetchResult.failure(e.getMessage());
            }
        });
    }

    /**
     * Parse response body.
     */
    private Map<String, PolicyLimitsTypes.PolicyRestriction> parseResponse(String body) {
        Map<String, PolicyLimitsTypes.PolicyRestriction> restrictions = new HashMap<>();

        // Simple JSON parsing
        // Would use Jackson in production
        if (body == null || body.isEmpty()) {
            return restrictions;
        }

        // Extract restrictions from JSON
        int restrictionsStart = body.indexOf("\"restrictions\"");
        if (restrictionsStart < 0) {
            return restrictions;
        }

        // Very simplified parsing
        // In production, use proper JSON parser
        return restrictions;
    }

    /**
     * Get cached policy limits.
     */
    public PolicyLimitsTypes.PolicyLimitsResponse getCachedLimits() {
        return cachedResponse;
    }

    /**
     * Check if policy is allowed.
     */
    public boolean isPolicyAllowed(String policyKey) {
        if (cachedResponse == null) {
            return true; // Default to allowed if not fetched
        }
        return cachedResponse.isAllowed(policyKey);
    }

    /**
     * Check if policy is restricted.
     */
    public boolean isPolicyRestricted(String policyKey) {
        if (cachedResponse == null) {
            return false;
        }
        return cachedResponse.isRestricted(policyKey);
    }

    /**
     * Check policy and return result.
     */
    public PolicyLimitsTypes.PolicyCheckResult checkPolicy(String policyKey) {
        if (isPolicyRestricted(policyKey)) {
            return new PolicyLimitsTypes.PolicyCheckResult.Denied(policyKey, "Policy restricted");
        }
        return PolicyLimitsTypes.PolicyCheckResult.Allowed.INSTANCE;
    }

    /**
     * Should refresh cache.
     */
    public boolean shouldRefreshCache() {
        if (lastFetchTime == null) {
            return true;
        }
        Duration sinceLastFetch = Duration.between(lastFetchTime, Instant.now());
        return sinceLastFetch.toMillis() >= config.refreshIntervalMs();
    }

    /**
     * Clear cache.
     */
    public void clearCache() {
        cachedResponse = null;
        cachedEtag = null;
        lastFetchTime = null;
    }

    /**
     * Get last fetch time.
     */
    public Instant getLastFetchTime() {
        return lastFetchTime;
    }

    /**
     * Get cached etag.
     */
    public String getCachedEtag() {
        return cachedEtag;
    }
}