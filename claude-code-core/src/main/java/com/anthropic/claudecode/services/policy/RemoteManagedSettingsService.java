/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/remoteManagedSettings
 */
package com.anthropic.claudecode.services.policy;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Remote managed settings service for enterprise settings synchronization.
 */
public final class RemoteManagedSettingsService {
    private RemoteManagedSettingsService() {}

    // Constants
    private static final String SETTINGS_FILENAME = "remote-settings.json";
    private static final int SETTINGS_TIMEOUT_MS = 10000;
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long POLLING_INTERVAL_MS = 60 * 60 * 1000; // 1 hour

    // Background polling state
    private static ScheduledFuture<?> pollingTask = null;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Loading promise
    private static CompletableFuture<Void> loadingPromise = null;

    // Session cache
    private static final AtomicReference<Map<String, Object>> sessionCache = new AtomicReference<>(null);

    // Change listeners
    private static final Set<Runnable> changeListeners = ConcurrentHashMap.newKeySet();

    /**
     * Settings response.
     */
    public record SettingsResponse(
        Map<String, Object> settings,
        String checksum
    ) {}

    /**
     * Settings fetch result.
     */
    public record SettingsFetchResult(
        boolean success,
        Map<String, Object> settings,
        String checksum,
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
     * Check if user is eligible for remote managed settings.
     */
    public static boolean isEligible() {
        // Check for first-party provider
        String provider = getApiProvider();
        if (!"firstParty".equals(provider)) {
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
     * Wait for remote settings to load.
     */
    public static CompletableFuture<Void> waitForLoad() {
        if (loadingPromise != null) {
            return loadingPromise;
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get settings from cache.
     */
    public static Map<String, Object> getSettingsFromCache() {
        if (!isEligible()) {
            return null;
        }

        Map<String, Object> cached = sessionCache.get();
        if (cached != null) {
            return cached;
        }

        // Load from file
        try {
            Path cachePath = getSettingsPath();
            if (Files.exists(cachePath)) {
                String content = Files.readString(cachePath);
                // Parse JSON - simplified
                cached = new HashMap<>();
                sessionCache.set(cached);
                return cached;
            }
        } catch (IOException ignored) {}

        return null;
    }

    /**
     * Get a specific setting value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSetting(String key, T defaultValue) {
        Map<String, Object> settings = getSettingsFromCache();
        if (settings == null) {
            return defaultValue;
        }

        Object value = settings.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Load remote managed settings during initialization.
     */
    public static CompletableFuture<Void> loadRemoteManagedSettings() {
        if (isEligible() && loadingPromise == null) {
            loadingPromise = new CompletableFuture<>();
        }

        // Apply cached settings immediately if available
        Map<String, Object> cached = getSettingsFromCache();
        if (cached != null && loadingPromise != null) {
            loadingPromise.complete(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> settings = fetchAndLoadSettings();

                if (isEligible()) {
                    startBackgroundPolling();
                }

                if (settings != null) {
                    notifyChange();
                }
            } finally {
                if (loadingPromise != null) {
                    loadingPromise.complete(null);
                }
            }
        });
    }

    /**
     * Refresh remote settings after auth change.
     */
    public static CompletableFuture<Void> refreshRemoteManagedSettings() {
        return clearCache().thenRun(() -> {
            if (isEligible()) {
                fetchAndLoadSettings();
                notifyChange();
            }
        });
    }

    /**
     * Clear all remote settings cache.
     */
    public static CompletableFuture<Void> clearCache() {
        stopBackgroundPolling();
        sessionCache.set(null);
        loadingPromise = null;

        try {
            Files.deleteIfExists(getSettingsPath());
        } catch (IOException ignored) {}

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Register a change listener.
     */
    public static Runnable addChangeListener(Runnable listener) {
        changeListeners.add(listener);
        return () -> changeListeners.remove(listener);
    }

    /**
     * Start background polling.
     */
    public static void startBackgroundPolling() {
        if (pollingTask != null) return;
        if (!isEligible()) return;

        pollingTask = scheduler.scheduleAtFixedRate(
            () -> pollRemoteSettings(),
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

    private static Map<String, Object> fetchAndLoadSettings() {
        Map<String, Object> cachedSettings = getSettingsFromCache();

        try {
            SettingsFetchResult result = fetchWithRetry(cachedSettings);

            if (!result.success()) {
                if (cachedSettings != null) {
                    sessionCache.set(cachedSettings);
                    return cachedSettings;
                }
                return null;
            }

            Map<String, Object> newSettings = result.settings();
            if (newSettings != null) {
                sessionCache.set(newSettings);
                saveSettings(newSettings);
                return newSettings;
            }
        } catch (Exception e) {
            if (cachedSettings != null) {
                sessionCache.set(cachedSettings);
                return cachedSettings;
            }
        }

        return null;
    }

    private static SettingsFetchResult fetchWithRetry(Map<String, Object> cached) {
        for (int attempt = 1; attempt <= DEFAULT_MAX_RETRIES + 1; attempt++) {
            SettingsFetchResult result = fetchSettings(cached);

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

        return new SettingsFetchResult(false, null, null, "Max retries exceeded", false);
    }

    private static SettingsFetchResult fetchSettings(Map<String, Object> cached) {
        try {
            // Build HTTP request to remote settings endpoint
            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = "https://api.anthropic.com";
            }

            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return new SettingsFetchResult(false, null, null, "No API key", true);
            }

            String endpoint = baseUrl + "/v1/organizations/settings";

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
                Map<String, Object> settings = parseSettingsResponse(response.body());
                String checksum = extractChecksum(response.body());
                return new SettingsFetchResult(true, settings, checksum, null, false);
            } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                return new SettingsFetchResult(false, null, null, "Auth error", true);
            } else {
                return new SettingsFetchResult(false, null, null, "HTTP " + response.statusCode(), false);
            }
        } catch (Exception e) {
            return new SettingsFetchResult(false, null, null, e.getMessage(), false);
        }
    }

    /**
     * Parse settings response JSON.
     */
    private static Map<String, Object> parseSettingsResponse(String json) {
        Map<String, Object> settings = new HashMap<>();
        try {
            // Simple JSON parsing - find settings object
            int settingsStart = json.indexOf("\"settings\"");
            if (settingsStart < 0) {
                // Try direct object
                int objStart = json.indexOf("{");
                if (objStart >= 0) {
                    int depth = 1;
                    int objEnd = objStart + 1;
                    while (objEnd < json.length() && depth > 0) {
                        char c = json.charAt(objEnd);
                        if (c == '{') depth++;
                        else if (c == '}') depth--;
                        objEnd++;
                    }
                    String settingsObj = json.substring(objStart, objEnd);
                    parseJsonToMap(settingsObj, settings);
                }
            } else {
                int objStart = json.indexOf("{", settingsStart);
                if (objStart >= 0) {
                    int depth = 1;
                    int objEnd = objStart + 1;
                    while (objEnd < json.length() && depth > 0) {
                        char c = json.charAt(objEnd);
                        if (c == '{') depth++;
                        else if (c == '}') depth--;
                        objEnd++;
                    }
                    String settingsObj = json.substring(objStart, objEnd);
                    parseJsonToMap(settingsObj, settings);
                }
            }
        } catch (Exception e) {
            // Return empty map on parse error
        }
        return settings;
    }

    /**
     * Parse JSON object into map.
     */
    private static void parseJsonToMap(String json, Map<String, Object> map) {
        int i = 0;
        while (i < json.length()) {
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length() || json.charAt(i) != '"') break;

            // Read key
            i++;
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++;
                i++;
            }
            String key = json.substring(keyStart, i);
            i++;

            // Skip to value
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

            // Parse value
            if (i < json.length()) {
                char c = json.charAt(i);
                if (c == '"') {
                    i++;
                    int valStart = i;
                    while (i < json.length() && json.charAt(i) != '"') {
                        if (json.charAt(i) == '\\') i++;
                        i++;
                    }
                    map.put(key, json.substring(valStart, i));
                    i++;
                } else if (c == '{') {
                    int depth = 1;
                    int objStart = i;
                    i++;
                    while (i < json.length() && depth > 0) {
                        if (json.charAt(i) == '{') depth++;
                        else if (json.charAt(i) == '}') depth--;
                        i++;
                    }
                    Map<String, Object> nested = new HashMap<>();
                    parseJsonToMap(json.substring(objStart, i), nested);
                    map.put(key, nested);
                } else if (c == '[') {
                    int depth = 1;
                    int arrStart = i;
                    i++;
                    while (i < json.length() && depth > 0) {
                        if (json.charAt(i) == '[') depth++;
                        else if (json.charAt(i) == ']') depth--;
                        i++;
                    }
                    map.put(key, parseJsonArray(json.substring(arrStart, i)));
                } else if (c == 't' && i + 3 < json.length() && json.substring(i, i + 4).equals("true")) {
                    map.put(key, true);
                    i += 4;
                } else if (c == 'f' && i + 4 < json.length() && json.substring(i, i + 5).equals("false")) {
                    map.put(key, false);
                    i += 5;
                } else if (c == 'n' && i + 3 < json.length() && json.substring(i, i + 4).equals("null")) {
                    map.put(key, null);
                    i += 4;
                } else {
                    // Number
                    int valStart = i;
                    while (i < json.length() && !Character.isWhitespace(json.charAt(i)) &&
                           json.charAt(i) != ',' && json.charAt(i) != '}') {
                        i++;
                    }
                    String numStr = json.substring(valStart, i);
                    try {
                        if (numStr.contains(".")) {
                            map.put(key, Double.parseDouble(numStr));
                        } else {
                            map.put(key, Long.parseLong(numStr));
                        }
                    } catch (NumberFormatException e) {
                        map.put(key, numStr);
                    }
                }
            }

            // Skip comma
            while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
    }

    /**
     * Parse JSON array.
     */
    private static List<Object> parseJsonArray(String json) {
        List<Object> list = new ArrayList<>();
        int i = 1; // Skip opening [
        while (i < json.length() - 1) {
            while (Character.isWhitespace(json.charAt(i))) i++;
            if (json.charAt(i) == ']') break;

            char c = json.charAt(i);
            if (c == '"') {
                i++;
                int valStart = i;
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\') i++;
                    i++;
                }
                list.add(json.substring(valStart, i));
                i++;
            } else if (c == '{') {
                int depth = 1;
                int objStart = i;
                i++;
                while (i < json.length() && depth > 0) {
                    if (json.charAt(i) == '{') depth++;
                    else if (json.charAt(i) == '}') depth--;
                    i++;
                }
                Map<String, Object> nested = new HashMap<>();
                parseJsonToMap(json.substring(objStart, i), nested);
                list.add(nested);
            } else {
                int valStart = i;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != ']') {
                    i++;
                }
                String val = json.substring(valStart, i).trim();
                if ("true".equals(val)) list.add(true);
                else if ("false".equals(val)) list.add(false);
                else if ("null".equals(val)) list.add(null);
                else {
                    try {
                        if (val.contains(".")) list.add(Double.parseDouble(val));
                        else list.add(Long.parseLong(val));
                    } catch (NumberFormatException e) {
                        list.add(val);
                    }
                }
            }

            while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != ']') i++;
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
        return list;
    }

    /**
     * Extract checksum from response.
     */
    private static String extractChecksum(String json) {
        int checksumStart = json.indexOf("\"checksum\"");
        if (checksumStart < 0) return null;

        int valStart = json.indexOf("\"", checksumStart + 10);
        if (valStart < 0) return null;

        int valEnd = json.indexOf("\"", valStart + 1);
        if (valEnd < 0) return null;

        return json.substring(valStart + 1, valEnd);
    }

    private static void saveSettings(Map<String, Object> settings) {
        try {
            Path path = getSettingsPath();
            Files.createDirectories(path.getParent());
            // In real implementation, would write JSON
            Files.writeString(path, "{}");
        } catch (IOException ignored) {}
    }

    private static void notifyChange() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {}
        }
    }

    private static void pollRemoteSettings() {
        if (!isEligible()) return;

        Map<String, Object> previous = sessionCache.get();
        fetchAndLoadSettings();

        Map<String, Object> current = sessionCache.get();
        if (!Objects.equals(previous, current)) {
            notifyChange();
        }
    }

    private static Path getSettingsPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".claude", SETTINGS_FILENAME);
    }

    private static String getApiProvider() {
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_BEDROCK"))) return "bedrock";
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_VERTEX"))) return "vertex";
        if (isEnvTruthy(System.getenv("CLAUDE_CODE_USE_FOUNDRY"))) return "foundry";
        return "firstParty";
    }

    private static long getRetryDelay(int attempt) {
        return 500L * (long) Math.pow(2, attempt - 1);
    }

    private static boolean isEnvTruthy(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }
}