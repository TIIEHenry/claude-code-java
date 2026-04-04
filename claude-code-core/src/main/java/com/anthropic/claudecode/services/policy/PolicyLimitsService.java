/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/policyLimits
 */
package com.anthropic.claudecode.services.policy;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Policy limits service for enterprise policy restrictions.
 */
public final class PolicyLimitsService {
    private PolicyLimitsService() {}

    // Constants
    private static final String CACHE_FILENAME = "policy-limits.json";
    private static final int FETCH_TIMEOUT_MS = 10000;
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long POLLING_INTERVAL_MS = 60 * 60 * 1000; // 1 hour

    // Background polling state
    private static ScheduledFuture<?> pollingTask = null;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Loading promise
    private static CompletableFuture<Void> loadingPromise = null;

    // Session cache
    private static final AtomicReference<Map<String, PolicyRestriction>> sessionCache = new AtomicReference<>(null);

    // Policies that deny on miss in essential-traffic mode
    private static final Set<String> ESSENTIAL_TRAFFIC_DENY_ON_MISS = Set.of("allow_product_feedback");

    /**
     * Policy restriction record.
     */
    public record PolicyRestriction(
        String policy,
        boolean allowed,
        String reason
    ) {}

    /**
     * Policy limits response.
     */
    public record PolicyLimitsResponse(
        Map<String, PolicyRestriction> restrictions
    ) {}

    /**
     * Policy limits fetch result.
     */
    public record PolicyLimitsFetchResult(
        boolean success,
        Map<String, PolicyRestriction> restrictions,
        String error,
        boolean skipRetry
    ) {}

    /**
     * Initialize loading promise.
     */
    public static void initializeLoadingPromise() {
        if (loadingPromise != null) return;

        if (isEligible()) {
            loadingPromise = new CompletableFuture<>();
            // Timeout after 30 seconds
            scheduler.schedule(() -> {
                if (loadingPromise != null && !loadingPromise.isDone()) {
                    loadingPromise.complete(null);
                }
            }, 30, TimeUnit.SECONDS);
        }
    }

    /**
     * Check if user is eligible for policy limits.
     */
    public static boolean isEligible() {
        // Check for first-party provider
        String provider = getApiProvider();
        if (!"firstParty".equals(provider)) {
            return false;
        }

        // Check for custom base URL
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (baseUrl != null && !isFirstPartyUrl(baseUrl)) {
            return false;
        }

        // Check for API key
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return true;
        }

        // Check for OAuth tokens with enterprise/team subscription
        String subscriptionType = System.getenv("CLAUDE_SUBSCRIPTION_TYPE");
        return "enterprise".equals(subscriptionType) || "team".equals(subscriptionType);
    }

    /**
     * Wait for policy limits to load.
     */
    public static CompletableFuture<Void> waitForLoad() {
        if (loadingPromise != null) {
            return loadingPromise;
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Check if a specific policy is allowed.
     * Fails open - returns true if policy is unknown or unavailable.
     */
    public static boolean isPolicyAllowed(String policy) {
        Map<String, PolicyRestriction> restrictions = getRestrictionsFromCache();

        if (restrictions == null) {
            // In essential-traffic mode, deny certain policies on cache miss
            if (isEssentialTrafficOnly() && ESSENTIAL_TRAFFIC_DENY_ON_MISS.contains(policy)) {
                return false;
            }
            return true; // fail open
        }

        PolicyRestriction restriction = restrictions.get(policy);
        if (restriction == null) {
            return true; // unknown policy = allowed
        }

        return restriction.allowed();
    }

    /**
     * Get restrictions from cache.
     */
    public static Map<String, PolicyRestriction> getRestrictionsFromCache() {
        if (!isEligible()) {
            return null;
        }

        Map<String, PolicyRestriction> cached = sessionCache.get();
        if (cached != null) {
            return cached;
        }

        // Load from file
        try {
            Path cachePath = getCachePath();
            if (Files.exists(cachePath)) {
                String content = Files.readString(cachePath);
                // Parse JSON - simplified
                // In real implementation, use Jackson
                cached = new HashMap<>();
                sessionCache.set(cached);
                return cached;
            }
        } catch (IOException ignored) {}

        return null;
    }

    /**
     * Load policy limits during initialization.
     */
    public static CompletableFuture<Void> loadPolicyLimits() {
        if (isEligible() && loadingPromise == null) {
            loadingPromise = new CompletableFuture<>();
        }

        return CompletableFuture.runAsync(() -> {
            try {
                fetchAndLoadPolicyLimits();
                if (isEligible()) {
                    startBackgroundPolling();
                }
            } finally {
                if (loadingPromise != null) {
                    loadingPromise.complete(null);
                }
            }
        });
    }

    /**
     * Refresh policy limits after auth change.
     */
    public static CompletableFuture<Void> refreshPolicyLimits() {
        return clearCache().thenRun(() -> {
            if (isEligible()) {
                fetchAndLoadPolicyLimits();
            }
        });
    }

    /**
     * Clear all policy limits cache.
     */
    public static CompletableFuture<Void> clearCache() {
        stopBackgroundPolling();
        sessionCache.set(null);
        loadingPromise = null;

        try {
            Files.deleteIfExists(getCachePath());
        } catch (IOException ignored) {}

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Start background polling.
     */
    public static void startBackgroundPolling() {
        if (pollingTask != null) return;
        if (!isEligible()) return;

        pollingTask = scheduler.scheduleAtFixedRate(
            () -> pollPolicyLimits(),
            POLLING_INTERVAL_MS,
            POLLING_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stop background polling.
     */
    public static void stopBackgroundPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
        }
    }

    // Private helpers

    private static void fetchAndLoadPolicyLimits() {
        Map<String, PolicyRestriction> cachedRestrictions = getRestrictionsFromCache();

        try {
            PolicyLimitsFetchResult result = fetchWithRetry(cachedRestrictions);

            if (!result.success()) {
                if (cachedRestrictions != null) {
                    sessionCache.set(cachedRestrictions);
                }
                return;
            }

            Map<String, PolicyRestriction> newRestrictions = result.restrictions();
            if (newRestrictions != null) {
                sessionCache.set(newRestrictions);
                saveCachedRestrictions(newRestrictions);
            }
        } catch (Exception e) {
            if (cachedRestrictions != null) {
                sessionCache.set(cachedRestrictions);
            }
        }
    }

    private static PolicyLimitsFetchResult fetchWithRetry(Map<String, PolicyRestriction> cached) {
        for (int attempt = 1; attempt <= DEFAULT_MAX_RETRIES + 1; attempt++) {
            PolicyLimitsFetchResult result = fetchPolicyLimits(cached);

            if (result.success() || result.skipRetry()) {
                return result;
            }

            if (attempt <= DEFAULT_MAX_RETRIES) {
                long delayMs = getRetryDelay(attempt);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return new PolicyLimitsFetchResult(false, null, "Max retries exceeded", false);
    }

    private static PolicyLimitsFetchResult fetchPolicyLimits(Map<String, PolicyRestriction> cached) {
        try {
            // Build HTTP request to policy endpoint
            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = "https://api.anthropic.com";
            }

            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return new PolicyLimitsFetchResult(false, null, "No API key", true);
            }

            String endpoint = baseUrl + "/v1/organizations/policy-limits";

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(endpoint))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse JSON response
                Map<String, PolicyRestriction> restrictions = parsePolicyResponse(response.body());
                return new PolicyLimitsFetchResult(true, restrictions, null, false);
            } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                return new PolicyLimitsFetchResult(false, null, "Auth error", true);
            } else {
                return new PolicyLimitsFetchResult(false, null, "HTTP " + response.statusCode(), false);
            }
        } catch (Exception e) {
            return new PolicyLimitsFetchResult(false, null, e.getMessage(), false);
        }
    }

    /**
     * Parse policy response JSON.
     */
    private static Map<String, PolicyRestriction> parsePolicyResponse(String json) {
        Map<String, PolicyRestriction> restrictions = new HashMap<>();
        try {
            // Simple JSON parsing without external library
            // Find restrictions object
            int restrictionsStart = json.indexOf("\"restrictions\"");
            if (restrictionsStart < 0) return restrictions;

            int objStart = json.indexOf("{", restrictionsStart);
            if (objStart < 0) return restrictions;

            // Find matching closing brace
            int depth = 1;
            int objEnd = objStart + 1;
            while (objEnd < json.length() && depth > 0) {
                char c = json.charAt(objEnd);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                objEnd++;
            }

            String restrictionsObj = json.substring(objStart, objEnd);

            // Parse individual restrictions
            int i = 0;
            while (i < restrictionsObj.length()) {
                // Find policy key
                int keyStart = restrictionsObj.indexOf("\"", i);
                if (keyStart < 0) break;
                int keyEnd = restrictionsObj.indexOf("\"", keyStart + 1);
                if (keyEnd < 0) break;

                String policy = restrictionsObj.substring(keyStart + 1, keyEnd);

                // Find value object
                int valueStart = restrictionsObj.indexOf("{", keyEnd);
                if (valueStart < 0) break;

                depth = 1;
                int valueEnd = valueStart + 1;
                while (valueEnd < restrictionsObj.length() && depth > 0) {
                    char c = restrictionsObj.charAt(valueEnd);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    valueEnd++;
                }

                String valueObj = restrictionsObj.substring(valueStart, valueEnd);

                // Parse allowed and reason
                boolean allowed = valueObj.contains("\"allowed\":true") ||
                                  !valueObj.contains("\"allowed\":false");

                String reason = null;
                int reasonStart = valueObj.indexOf("\"reason\"");
                if (reasonStart >= 0) {
                    int reasonValStart = valueObj.indexOf("\"", reasonStart + 8);
                    if (reasonValStart >= 0) {
                        int reasonValEnd = valueObj.indexOf("\"", reasonValStart + 1);
                        if (reasonValEnd >= 0) {
                            reason = valueObj.substring(reasonValStart + 1, reasonValEnd);
                        }
                    }
                }

                restrictions.put(policy, new PolicyRestriction(policy, allowed, reason));
                i = valueEnd;
            }
        } catch (Exception e) {
            // Return empty map on parse error
        }
        return restrictions;
    }

    private static void saveCachedRestrictions(Map<String, PolicyRestriction> restrictions) {
        try {
            Path path = getCachePath();
            Files.createDirectories(path.getParent());
            // In real implementation, would write JSON
            Files.writeString(path, "{}");
        } catch (IOException ignored) {}
    }

    private static void pollPolicyLimits() {
        if (!isEligible()) return;

        Map<String, PolicyRestriction> previous = sessionCache.get();
        fetchAndLoadPolicyLimits();

        Map<String, PolicyRestriction> current = sessionCache.get();
        // Could notify listeners if changed
    }

    private static Path getCachePath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".claude", CACHE_FILENAME);
    }

    private static String getApiProvider() {
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_BEDROCK"))) return "bedrock";
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_VERTEX"))) return "vertex";
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_FOUNDRY"))) return "foundry";
        return "firstParty";
    }

    private static boolean isFirstPartyUrl(String url) {
        if (url == null) return true;
        return url.contains("anthropic.com");
    }

    private static boolean isEssentialTrafficOnly() {
        String privacy = System.getenv("CLAUDE_CODE_PRIVACY_LEVEL");
        return "essential-traffic".equals(privacy);
    }

    private static long getRetryDelay(int attempt) {
        return 500L * (long) Math.pow(2, attempt - 1);
    }

    private static boolean isEnvTruthy(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }
}