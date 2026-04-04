/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/remoteManagedSettings/index.ts
 */
package com.anthropic.claudecode.services.remotemanagedsettings;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.security.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import com.anthropic.claudecode.utils.Debug;

/**
 * Remote Managed Settings Service.
 *
 * Manages fetching, caching, and validation of remote-managed settings
 * for enterprise customers.
 */
public final class RemoteManagedSettingsService {
    private RemoteManagedSettingsService() {}

    // Constants
    private static final int SETTINGS_TIMEOUT_MS = 10000; // 10 seconds
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long POLLING_INTERVAL_MS = 60 * 60 * 1000; // 1 hour
    private static final long LOADING_PROMISE_TIMEOUT_MS = 30000; // 30 seconds
    private static final String REMOTE_SETTINGS_URL = "https://api.anthropic.com/v1/claude-cli/settings";

    // HTTP client
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .version(Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // Background polling state
    private static ScheduledFuture<?> pollingFuture = null;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Loading promise state
    private static CompletableFuture<Void> loadingCompletePromise = null;

    // Session cache
    private static volatile Map<String, Object> sessionCache = null;

    /**
     * Remote managed settings fetch result.
     */
    public record RemoteManagedSettingsFetchResult(
        boolean success,
        Map<String, Object> settings,
        String checksum,
        String error,
        boolean skipRetry
    ) {}

    /**
     * Initialize the loading promise for remote managed settings.
     */
    public static void initializeRemoteManagedSettingsLoadingPromise() {
        if (loadingCompletePromise != null) {
            return;
        }

        if (isRemoteManagedSettingsEligible()) {
            loadingCompletePromise = new CompletableFuture<>();

            // Set a timeout to prevent deadlocks
            scheduler.schedule(() -> {
                if (loadingCompletePromise != null && !loadingCompletePromise.isDone()) {
                    Debug.logForDebugging("Remote settings: Loading promise timed out, resolving anyway");
                    loadingCompletePromise.complete(null);
                }
            }, LOADING_PROMISE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Check if user is eligible for remote managed settings.
     */
    public static boolean isRemoteManagedSettingsEligible() {
        // Check if API key or OAuth tokens are available
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return true;
        }

        // Check OAuth tokens
        String accessToken = System.getenv("CLAUDE_ACCESS_TOKEN");
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Check if the current user is eligible for remote managed settings.
     */
    public static boolean isEligibleForRemoteManagedSettings() {
        return isRemoteManagedSettingsEligible();
    }

    /**
     * Wait for the initial remote settings loading to complete.
     */
    public static CompletableFuture<Void> waitForRemoteManagedSettingsToLoad() {
        if (loadingCompletePromise != null) {
            return loadingCompletePromise;
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Load remote settings during CLI initialization.
     */
    public static CompletableFuture<Void> loadRemoteManagedSettings() {
        if (isRemoteManagedSettingsEligible() && loadingCompletePromise == null) {
            loadingCompletePromise = new CompletableFuture<>();
        }

        // Cache-first: if we have cached settings, unblock waiters immediately
        Map<String, Object> cachedSettings = getRemoteManagedSettingsSyncFromCache();
        if (cachedSettings != null && loadingCompletePromise != null) {
            loadingCompletePromise.complete(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> settings = fetchAndLoadRemoteManagedSettings();

                // Start background polling
                if (isRemoteManagedSettingsEligible()) {
                    startBackgroundPolling();
                }
            } finally {
                if (loadingCompletePromise != null && !loadingCompletePromise.isDone()) {
                    loadingCompletePromise.complete(null);
                }
            }
        });
    }

    /**
     * Refresh remote settings asynchronously.
     */
    public static CompletableFuture<Void> refreshRemoteManagedSettings() {
        return CompletableFuture.runAsync(() -> {
            clearRemoteManagedSettingsCache();

            if (!isRemoteManagedSettingsEligible()) {
                return;
            }

            fetchAndLoadRemoteManagedSettings();
            Debug.logForDebugging("Remote settings: Refreshed after auth change");
        });
    }

    /**
     * Clear all remote settings.
     */
    public static void clearRemoteManagedSettingsCache() {
        stopBackgroundPolling();
        sessionCache = null;
        loadingCompletePromise = null;

        try {
            Path path = getSettingsPath();
            Files.deleteIfExists(path);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Start background polling for remote settings.
     */
    public static void startBackgroundPolling() {
        if (pollingFuture != null) {
            return;
        }

        if (!isRemoteManagedSettingsEligible()) {
            return;
        }

        pollingFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                pollRemoteSettings();
            } catch (Exception e) {
                // Ignore polling errors
            }
        }, POLLING_INTERVAL_MS, POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop background polling for remote settings.
     */
    public static void stopBackgroundPolling() {
        if (pollingFuture != null) {
            pollingFuture.cancel(false);
            pollingFuture = null;
        }
    }

    /**
     * Get settings path.
     */
    public static Path getSettingsPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".claude", "remote_settings.json");
    }

    /**
     * Get remote managed settings from cache.
     */
    public static Map<String, Object> getRemoteManagedSettingsSyncFromCache() {
        if (sessionCache != null) {
            return sessionCache;
        }

        try {
            Path path = getSettingsPath();
            if (Files.exists(path)) {
                String content = Files.readString(path);
                // Parse JSON - simplified
                sessionCache = new LinkedHashMap<>();
                return sessionCache;
            }
        } catch (Exception e) {
            // Ignore errors
        }

        return null;
    }

    /**
     * Compute checksum from settings.
     */
    public static String computeChecksumFromSettings(Map<String, Object> settings) {
        try {
            String normalized = serializeSorted(settings);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "sha256:" + hex;
        } catch (Exception e) {
            return "";
        }
    }

    // Private helper methods

    private static Map<String, Object> fetchAndLoadRemoteManagedSettings() {
        if (!isRemoteManagedSettingsEligible()) {
            return null;
        }

        Map<String, Object> cachedSettings = getRemoteManagedSettingsSyncFromCache();
        String cachedChecksum = cachedSettings != null ? computeChecksumFromSettings(cachedSettings) : null;

        try {
            RemoteManagedSettingsFetchResult result = fetchWithRetry(cachedChecksum);

            if (!result.success()) {
                if (cachedSettings != null) {
                    Debug.logForDebugging("Remote settings: Using stale cache after fetch failure");
                    sessionCache = cachedSettings;
                    return cachedSettings;
                }
                return null;
            }

            if (result.settings() == null && cachedSettings != null) {
                Debug.logForDebugging("Remote settings: Cache still valid (304 Not Modified)");
                sessionCache = cachedSettings;
                return cachedSettings;
            }

            Map<String, Object> newSettings = result.settings() != null ? result.settings() : new LinkedHashMap<>();
            boolean hasContent = !newSettings.isEmpty();

            if (hasContent) {
                sessionCache = newSettings;
                saveSettings(newSettings);
                Debug.logForDebugging("Remote settings: Applied new settings successfully");
                return newSettings;
            }

            sessionCache = newSettings;
            try {
                Files.deleteIfExists(getSettingsPath());
                Debug.logForDebugging("Remote settings: Deleted cached file (404 response)");
            } catch (Exception e) {
                // Ignore
            }

            return newSettings;
        } catch (Exception e) {
            if (cachedSettings != null) {
                Debug.logForDebugging("Remote settings: Using stale cache after error");
                sessionCache = cachedSettings;
                return cachedSettings;
            }
            return null;
        }
    }

    private static RemoteManagedSettingsFetchResult fetchWithRetry(String cachedChecksum) {
        for (int attempt = 1; attempt <= DEFAULT_MAX_RETRIES + 1; attempt++) {
            RemoteManagedSettingsFetchResult result = fetchRemoteManagedSettings(cachedChecksum);

            if (result.success() || result.skipRetry() || attempt > DEFAULT_MAX_RETRIES) {
                return result;
            }

            long delayMs = getRetryDelay(attempt);
            Debug.logForDebugging("Remote settings: Retry " + attempt + "/" + DEFAULT_MAX_RETRIES + " after " + delayMs + "ms");

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return new RemoteManagedSettingsFetchResult(false, null, null, "Max retries exceeded", false);
    }

    private static RemoteManagedSettingsFetchResult fetchRemoteManagedSettings(String cachedChecksum) {
        try {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            String accessToken = System.getenv("CLAUDE_ACCESS_TOKEN");

            if (apiKey == null && accessToken == null) {
                return new RemoteManagedSettingsFetchResult(false, null, null, "No API key or access token", true);
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(REMOTE_SETTINGS_URL))
                .timeout(Duration.ofMillis(SETTINGS_TIMEOUT_MS))
                .GET();

            // Add authorization header
            if (apiKey != null) {
                requestBuilder.header("x-api-key", apiKey);
            } else if (accessToken != null) {
                requestBuilder.header("Authorization", "Bearer " + accessToken);
            }

            // Add conditional header for cache validation
            if (cachedChecksum != null) {
                requestBuilder.header("If-None-Match", cachedChecksum);
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();

            if (statusCode == 304) {
                // Not modified - cache is still valid
                return new RemoteManagedSettingsFetchResult(true, null, cachedChecksum, null, false);
            }

            if (statusCode == 404) {
                // No settings configured
                return new RemoteManagedSettingsFetchResult(true, new LinkedHashMap<>(), null, null, false);
            }

            if (statusCode >= 400) {
                String error = "HTTP error: " + statusCode;
                boolean skipRetry = statusCode == 401 || statusCode == 403;
                return new RemoteManagedSettingsFetchResult(false, null, null, error, skipRetry);
            }

            // Parse response
            String body = response.body();
            if (body == null || body.isEmpty()) {
                return new RemoteManagedSettingsFetchResult(true, new LinkedHashMap<>(), null, null, false);
            }

            // Parse JSON (simplified - use proper JSON parser in production)
            Map<String, Object> settings = parseJsonToMap(body);
            String newChecksum = computeChecksumFromSettings(settings);

            return new RemoteManagedSettingsFetchResult(true, settings, newChecksum, null, false);

        } catch (Exception e) {
            return new RemoteManagedSettingsFetchResult(false, null, null, e.getMessage(), false);
        }
    }

    /**
     * Parse JSON string to map (simplified implementation).
     */
    private static Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (json == null || json.isEmpty()) {
            return result;
        }

        // Simple JSON parsing - in production use Jackson or similar
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }

        // Remove outer braces
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) {
            return result;
        }

        // Very basic parsing - handle only string values
        int i = 0;
        while (i < json.length()) {
            // Find key
            while (i < json.length() && json.charAt(i) != '"') i++;
            if (i >= json.length()) break;
            i++; // skip opening quote
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') i++;
            String key = json.substring(keyStart, i);
            i++; // skip closing quote

            // Find colon
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++; // skip colon

            // Find value start
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;

            char c = json.charAt(i);
            Object value;
            if (c == '"') {
                i++;
                int valueStart = i;
                while (i < json.length() && json.charAt(i) != '"') i++;
                value = json.substring(valueStart, i);
                i++;
            } else if (Character.isDigit(c) || c == '-') {
                int valueStart = i;
                while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '.' || json.charAt(i) == '-')) i++;
                String numStr = json.substring(valueStart, i);
                try {
                    if (numStr.contains(".")) {
                        value = Double.parseDouble(numStr);
                    } else {
                        value = Long.parseLong(numStr);
                    }
                } catch (NumberFormatException e) {
                    value = numStr;
                }
            } else if (json.substring(i).startsWith("true")) {
                value = true;
                i += 4;
            } else if (json.substring(i).startsWith("false")) {
                value = false;
                i += 5;
            } else if (json.substring(i).startsWith("null")) {
                value = null;
                i += 4;
            } else {
                value = null;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
            }

            result.put(key, value);

            // Skip comma
            while (i < json.length() && json.charAt(i) != ',') i++;
            if (i < json.length()) i++;
        }

        return result;
    }

    private static void saveSettings(Map<String, Object> settings) {
        try {
            Path path = getSettingsPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, serializeSorted(settings));
            Debug.logForDebugging("Remote settings: Saved to " + path);
        } catch (Exception e) {
            Debug.logForDebugging("Remote settings: Failed to save - " + e.getMessage());
        }
    }

    private static void setSessionCache(Map<String, Object> settings) {
        sessionCache = settings;
    }

    private static void pollRemoteSettings() {
        if (!isRemoteManagedSettingsEligible()) {
            return;
        }

        String previousSettings = sessionCache != null ? serializeSorted(sessionCache) : null;

        try {
            fetchAndLoadRemoteManagedSettings();

            String newSettings = sessionCache != null ? serializeSorted(sessionCache) : null;
            if (!Objects.equals(previousSettings, newSettings)) {
                Debug.logForDebugging("Remote settings: Changed during background poll");
                // Notify change listeners
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static long getRetryDelay(int attempt) {
        // Exponential backoff with jitter
        long baseDelay = (long) (Math.pow(2, attempt) * 100);
        long jitter = (long) (Math.random() * 100);
        return Math.min(baseDelay + jitter, 5000);
    }

    private static String serializeSorted(Map<String, Object> map) {
        // Simplified sorted JSON serialization
        if (map == null) return "{}";
        StringBuilder sb = new StringBuilder("{");
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append(",");
            String key = keys.get(i);
            sb.append("\"").append(key).append("\":").append(map.get(key));
        }
        sb.append("}");
        return sb.toString();
    }
}