/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/analytics/growthbook.ts
 */
package com.anthropic.claudecode.services.analytics;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * GrowthBook feature flag service with remote evaluation and caching.
 */
public final class GrowthBookService {
    private GrowthBookService() {}

    // Configuration
    private static volatile boolean enabled = false;
    private static volatile GrowthBookClient client = null;
    private static volatile boolean clientCreatedWithAuth = false;
    private static volatile CompletableFuture<Void> initializingPromise = null;

    // Feature caches
    private static final Map<String, Object> remoteEvalFeatureValues = new ConcurrentHashMap<>();
    private static final Map<String, ExperimentData> experimentDataByFeature = new ConcurrentHashMap<>();
    private static final Set<String> pendingExposures = ConcurrentHashMap.newKeySet();
    private static final Set<String> loggedExposures = ConcurrentHashMap.newKeySet();

    // Refresh listeners
    private static final Signal refreshSignal = new Signal();

    // Periodic refresh
    private static ScheduledFuture<?> refreshTask = null;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long REFRESH_INTERVAL_ANT_MS = 20 * 60 * 1000; // 20 min for ants
    private static final long REFRESH_INTERVAL_EXTERNAL_MS = 6 * 60 * 60 * 1000; // 6 hours

    // Env overrides (for eval harnesses)
    private static volatile Map<String, Object> envOverrides = null;
    private static volatile boolean envOverridesParsed = false;

    /**
     * User attributes for GrowthBook targeting.
     */
    public record GrowthBookUserAttributes(
        String id,
        String sessionId,
        String deviceId,
        String platform,
        String apiBaseUrlHost,
        String organizationUuid,
        String accountUuid,
        String userType,
        String subscriptionType,
        String rateLimitTier,
        Long firstTokenTime,
        String email,
        String appVersion,
        GitHubActionsMetadata github
    ) {}

    /**
     * GitHub Actions metadata.
     */
    public record GitHubActionsMetadata(
        String actorId,
        String repositoryId,
        String repositoryOwnerId
    ) {}

    /**
     * Experiment data for exposure logging.
     */
    public record ExperimentData(
        String experimentId,
        int variationId,
        boolean inExperiment,
        String hashAttribute,
        String hashValue
    ) {}

    /**
     * Feature definition from remote eval.
     */
    public record FeatureDefinition(
        Object defaultValue,
        Object value,
        String source,
        Object experimentResult,
        Object experiment
    ) {}

    /**
     * GrowthBook payload.
     */
    public record GrowthBookPayload(Map<String, FeatureDefinition> features) {}

    /**
     * GrowthBook client interface.
     */
    public interface GrowthBookClient {
        GrowthBookPayload getPayload();
        void setPayload(GrowthBookPayload payload);
        Map<String, FeatureDefinition> getFeatures();
        Object getFeatureValue(String feature, Object defaultValue);
        void refreshFeatures();
        void destroy();
    }

    /**
     * Check if GrowthBook is enabled.
     */
    public static boolean isGrowthBookEnabled() {
        return enabled && AnalyticsConfig.is1PEventLoggingEnabled();
    }

    /**
     * Initialize GrowthBook client.
     */
    public static synchronized CompletableFuture<GrowthBookClient> initialize() {
        if (!isGrowthBookEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        if (client != null) {
            return CompletableFuture.completedFuture(client);
        }

        initializingPromise = new CompletableFuture<>();
        GrowthBookUserAttributes attributes = getUserAttributes();
        String clientKey = AnalyticsConfig.getGrowthBookClientKey();

        // Create client
        client = createClient(clientKey, attributes);
        clientCreatedWithAuth = hasAuth();

        // Initialize with timeout
        CompletableFuture.runAsync(() -> {
            try {
                GrowthBookPayload payload = client.getPayload();
                if (payload != null && payload.features() != null && !payload.features().isEmpty()) {
                    processRemoteEvalPayload(client);

                    // Log pending exposures
                    for (String feature : pendingExposures) {
                        logExposureForFeature(feature);
                    }
                    pendingExposures.clear();
                    syncRemoteEvalToDisk();
                    refreshSignal.emit();
                }

                initializingPromise.complete(null);
            } catch (Exception e) {
                initializingPromise.completeExceptionally(e);
            }
        });

        // Set up periodic refresh after init
        initializingPromise.thenRun(() -> setupPeriodicRefresh());

        return initializingPromise.thenApply(v -> client);
    }

    /**
     * Get user attributes for GrowthBook.
     */
    public static GrowthBookUserAttributes getUserAttributes() {
        // In real implementation, would get from user service
        String platform = System.getProperty("os.name").toLowerCase();
        if (platform.contains("win")) platform = "win32";
        else if (platform.contains("mac")) platform = "darwin";
        else platform = "linux";

        return new GrowthBookUserAttributes(
            getDeviceId(),
            getSessionId(),
            getDeviceId(),
            platform,
            getApiBaseUrlHost(),
            null, // organizationUuid
            null, // accountUuid
            System.getenv("USER_TYPE"),
            null, // subscriptionType
            null, // rateLimitTier
            null, // firstTokenTime
            null, // email
            null, // appVersion
            null  // github
        );
    }

    /**
     * Get API base URL host for targeting enterprise proxy users.
     */
    public static String getApiBaseUrlHost() {
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            return null;
        }
        try {
            String host = new java.net.URL(baseUrl).getHost();
            if ("api.anthropic.com".equals(host)) {
                return null;
            }
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get feature value from cache (may be stale).
     * This is the preferred method for startup-critical paths.
     */
    public static <T> T getFeatureValueCached(String feature, T defaultValue) {
        // Check env overrides first
        Map<String, Object> overrides = getEnvOverrides();
        if (overrides != null && overrides.containsKey(feature)) {
            return (T) overrides.get(feature);
        }

        // Check config overrides
        Map<String, Object> configOverrides = getConfigOverrides();
        if (configOverrides != null && configOverrides.containsKey(feature)) {
            return (T) configOverrides.get(feature);
        }

        if (!isGrowthBookEnabled()) {
            return defaultValue;
        }

        // Log exposure if data available
        if (experimentDataByFeature.containsKey(feature)) {
            logExposureForFeature(feature);
        } else {
            pendingExposures.add(feature);
        }

        // Check in-memory cache first
        if (remoteEvalFeatureValues.containsKey(feature)) {
            return (T) remoteEvalFeatureValues.get(feature);
        }

        // Fall back to disk cache
        try {
            Object cached = getCachedFeatureFromDisk(feature);
            return cached != null ? (T) cached : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get feature value with blocking initialization.
     * @deprecated Use getFeatureValueCached instead.
     */
    public static <T> CompletableFuture<T> getFeatureValueBlocking(String feature, T defaultValue) {
        return initialize().thenApply(c -> {
            if (c == null) return defaultValue;

            if (remoteEvalFeatureValues.containsKey(feature)) {
                return (T) remoteEvalFeatureValues.get(feature);
            }
            return (T) c.getFeatureValue(feature, defaultValue);
        });
    }

    /**
     * Check a boolean feature gate from cache.
     */
    public static boolean checkFeatureGateCached(String gate) {
        return getFeatureValueCached(gate, false);
    }

    /**
     * Check a security restriction gate with re-init wait.
     */
    public static CompletableFuture<Boolean> checkSecurityRestrictionGate(String gate) {
        // Check env overrides first
        Map<String, Object> overrides = getEnvOverrides();
        if (overrides != null && overrides.containsKey(gate)) {
            return CompletableFuture.completedFuture(Boolean.TRUE.equals(overrides.get(gate)));
        }

        // Wait for re-initialization if in progress
        if (initializingPromise != null && !initializingPromise.isDone()) {
            return initializingPromise.thenApply(v -> checkFeatureGateCached(gate));
        }

        return CompletableFuture.completedFuture(checkFeatureGateCached(gate));
    }

    /**
     * Check gate with fallback-to-blocking semantics.
     */
    public static CompletableFuture<Boolean> checkGateCachedOrBlocking(String gate) {
        // Fast path: cache already says true
        Boolean cached = getFeatureValueCached(gate, false);
        if (Boolean.TRUE.equals(cached)) {
            return CompletableFuture.completedFuture(true);
        }

        // Slow path: may be stale, fetch fresh
        return getFeatureValueBlocking(gate, false);
    }

    /**
     * Get dynamic config from cache.
     */
    public static <T> T getDynamicConfigCached(String configName, T defaultValue) {
        return getFeatureValueCached(configName, defaultValue);
    }

    /**
     * Register a refresh listener.
     */
    public static Runnable onGrowthBookRefresh(Consumer<Void> listener) {
        Runnable unsubscribe = refreshSignal.subscribe(() -> listener.accept(null));

        // Fire immediately if already initialized
        if (!remoteEvalFeatureValues.isEmpty()) {
            CompletableFuture.runAsync(() -> listener.accept(null));
        }

        return unsubscribe;
    }

    /**
     * Refresh after auth change.
     */
    public static synchronized void refreshAfterAuthChange() {
        if (!isGrowthBookEnabled()) {
            return;
        }

        try {
            reset();
            refreshSignal.emit();

            initializingPromise = initialize()
                .exceptionally(e -> null)
                .thenApply(v -> {
                    initializingPromise = null;
                    return null;
                });
        } catch (Exception e) {
            // Log error silently
        }
    }

    /**
     * Reset GrowthBook state.
     */
    public static synchronized void reset() {
        stopPeriodicRefresh();

        if (client != null) {
            client.destroy();
            client = null;
        }

        clientCreatedWithAuth = false;
        initializingPromise = null;
        experimentDataByFeature.clear();
        pendingExposures.clear();
        loggedExposures.clear();
        remoteEvalFeatureValues.clear();
        envOverrides = null;
        envOverridesParsed = false;
    }

    /**
     * Get all features and their values.
     */
    public static Map<String, Object> getAllFeatures() {
        if (!remoteEvalFeatureValues.isEmpty()) {
            return new HashMap<>(remoteEvalFeatureValues);
        }
        return getCachedFeaturesFromDisk();
    }

    /**
     * Set config override for a feature.
     */
    public static void setConfigOverride(String feature, Object value) {
        if (!"ant".equals(System.getenv("USER_TYPE"))) {
            return;
        }
        // In real implementation, would save to global config
        refreshSignal.emit();
    }

    /**
     * Clear all config overrides.
     */
    public static void clearConfigOverrides() {
        if (!"ant".equals(System.getenv("USER_TYPE"))) {
            return;
        }
        // In real implementation, would clear from global config
        refreshSignal.emit();
    }

    // Private helpers

    private static GrowthBookClient createClient(String clientKey, GrowthBookUserAttributes attributes) {
        // Return a mock/default client for now
        // In real implementation, would connect to GrowthBook API
        return new DefaultGrowthBookClient(clientKey, attributes);
    }

    private static boolean hasAuth() {
        return System.getenv("ANTHROPIC_API_KEY") != null;
    }

    private static String getDeviceId() {
        return System.getenv("CLAUDE_CODE_DEVICE_ID") != null
            ? System.getenv("CLAUDE_CODE_DEVICE_ID")
            : UUID.randomUUID().toString();
    }

    private static String getSessionId() {
        return System.getenv("CLAUDE_CODE_SESSION_ID") != null
            ? System.getenv("CLAUDE_CODE_SESSION_ID")
            : UUID.randomUUID().toString();
    }

    private static void processRemoteEvalPayload(GrowthBookClient gbClient) {
        GrowthBookPayload payload = gbClient.getPayload();
        if (payload == null || payload.features() == null || payload.features().isEmpty()) {
            return;
        }

        experimentDataByFeature.clear();
        remoteEvalFeatureValues.clear();

        for (Map.Entry<String, FeatureDefinition> entry : payload.features().entrySet()) {
            String key = entry.getKey();
            FeatureDefinition f = entry.getValue();

            // Handle malformed features with 'value' instead of 'defaultValue'
            Object featureValue = f.value() != null ? f.value() : f.defaultValue();
            if (featureValue != null) {
                remoteEvalFeatureValues.put(key, featureValue);
            }

            // Store experiment data for exposure logging
            if ("experiment".equals(f.source()) && f.experimentResult() != null) {
                // Extract experiment data
                // In real implementation, would parse experiment result
            }
        }
    }

    private static void logExposureForFeature(String feature) {
        if (loggedExposures.contains(feature)) {
            return;
        }

        ExperimentData expData = experimentDataByFeature.get(feature);
        if (expData != null) {
            loggedExposures.add(feature);
            // Log to 1P event logger
            DatadogAnalytics.logEvent("growthbook_exposure", Map.of(
                "experiment_id", expData.experimentId(),
                "variation_id", expData.variationId(),
                "feature_id", feature
            ));
        }
    }

    private static void syncRemoteEvalToDisk() {
        // In real implementation, would save to global config file
    }

    private static Object getCachedFeatureFromDisk(String feature) {
        // In real implementation, would read from global config
        return null;
    }

    private static Map<String, Object> getCachedFeaturesFromDisk() {
        // In real implementation, would read from global config
        return new HashMap<>();
    }

    private static Map<String, Object> getEnvOverrides() {
        if (!envOverridesParsed) {
            envOverridesParsed = true;
            if ("ant".equals(System.getenv("USER_TYPE"))) {
                String raw = System.getenv("CLAUDE_INTERNAL_FC_OVERRIDES");
                if (raw != null && !raw.isEmpty()) {
                    try {
                        envOverrides = parseJsonToMap(raw);
                    } catch (Exception e) {
                        // Parse error
                    }
                }
            }
        }
        return envOverrides;
    }

    private static Map<String, Object> getConfigOverrides() {
        if (!"ant".equals(System.getenv("USER_TYPE"))) {
            return null;
        }
        // In real implementation, would get from global config
        return null;
    }

    private static void setupPeriodicRefresh() {
        if (!isGrowthBookEnabled()) {
            return;
        }

        long interval = "ant".equals(System.getenv("USER_TYPE"))
            ? REFRESH_INTERVAL_ANT_MS
            : REFRESH_INTERVAL_EXTERNAL_MS;

        if (refreshTask != null) {
            refreshTask.cancel(false);
        }

        refreshTask = scheduler.scheduleAtFixedRate(
            () -> refreshFeatures(),
            interval,
            interval,
            TimeUnit.MILLISECONDS
        );
    }

    private static void stopPeriodicRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    private static void refreshFeatures() {
        if (client == null) {
            return;
        }

        try {
            client.refreshFeatures();
            processRemoteEvalPayload(client);
            syncRemoteEvalToDisk();
            refreshSignal.emit();
        } catch (Exception e) {
            // Silently fail
        }
    }

    private static Map<String, Object> parseJsonToMap(String json) {
        // Simple JSON parsing implementation
        Map<String, Object> result = new HashMap<>();
        if (json == null || json.isEmpty()) return result;

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return result;

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        int i = 0;
        while (i < json.length()) {
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;

            // Find key
            if (json.charAt(i) != '"') break;
            int keyStart = i + 1;
            int keyEnd = json.indexOf('"', keyStart);
            if (keyEnd < 0) break;

            String key = json.substring(keyStart, keyEnd);

            // Find colon
            int colon = json.indexOf(':', keyEnd);
            if (colon < 0) break;

            // Find value start
            int valStart = colon + 1;
            while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
            if (valStart >= json.length()) break;

            // Parse value based on type
            char firstChar = json.charAt(valStart);
            Object value;

            if (firstChar == '"') {
                // String value
                int valEnd = json.indexOf('"', valStart + 1);
                if (valEnd < 0) break;
                value = json.substring(valStart + 1, valEnd);
                i = valEnd + 1;
            } else if (firstChar == '{') {
                // Nested object
                int depth = 1;
                int valEnd = valStart + 1;
                while (valEnd < json.length() && depth > 0) {
                    char c = json.charAt(valEnd);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    valEnd++;
                }
                value = parseJsonToMap(json.substring(valStart, valEnd));
                i = valEnd;
            } else if (firstChar == '[') {
                // Array - parse as list
                int depth = 1;
                int valEnd = valStart + 1;
                while (valEnd < json.length() && depth > 0) {
                    char c = json.charAt(valEnd);
                    if (c == '[') depth++;
                    else if (c == ']') depth--;
                    valEnd++;
                }
                value = parseJsonArray(json.substring(valStart, valEnd));
                i = valEnd;
            } else if (Character.isDigit(firstChar) || firstChar == '-') {
                // Number
                int valEnd = valStart;
                while (valEnd < json.length() && (Character.isDigit(json.charAt(valEnd)) ||
                       json.charAt(valEnd) == '.' || json.charAt(valEnd) == '-' || json.charAt(valEnd) == 'e' || json.charAt(valEnd) == 'E')) {
                    valEnd++;
                }
                String numStr = json.substring(valStart, valEnd);
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    value = Double.parseDouble(numStr);
                } else {
                    value = Long.parseLong(numStr);
                }
                i = valEnd;
            } else if (json.substring(valStart).startsWith("true")) {
                value = true;
                i = valStart + 4;
            } else if (json.substring(valStart).startsWith("false")) {
                value = false;
                i = valStart + 5;
            } else if (json.substring(valStart).startsWith("null")) {
                value = null;
                i = valStart + 4;
            } else {
                break;
            }

            result.put(key, value);

            // Skip comma
            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) i++;
        }

        return result;
    }

    private static List<Object> parseJsonArray(String json) {
        List<Object> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;

        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return result;

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;

            char firstChar = json.charAt(i);
            Object value;

            if (firstChar == '"') {
                int valEnd = json.indexOf('"', i + 1);
                if (valEnd < 0) break;
                value = json.substring(i + 1, valEnd);
                i = valEnd + 1;
            } else if (firstChar == '{') {
                int depth = 1;
                int valEnd = i + 1;
                while (valEnd < json.length() && depth > 0) {
                    char c = json.charAt(valEnd);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    valEnd++;
                }
                value = parseJsonToMap(json.substring(i, valEnd));
                i = valEnd;
            } else if (firstChar == '[') {
                int depth = 1;
                int valEnd = i + 1;
                while (valEnd < json.length() && depth > 0) {
                    char c = json.charAt(valEnd);
                    if (c == '[') depth++;
                    else if (c == ']') depth--;
                    valEnd++;
                }
                value = parseJsonArray(json.substring(i, valEnd));
                i = valEnd;
            } else if (Character.isDigit(firstChar) || firstChar == '-') {
                int valEnd = i;
                while (valEnd < json.length() && (Character.isDigit(json.charAt(valEnd)) ||
                       json.charAt(valEnd) == '.' || json.charAt(valEnd) == '-')) {
                    valEnd++;
                }
                String numStr = json.substring(i, valEnd);
                if (numStr.contains(".")) {
                    value = Double.parseDouble(numStr);
                } else {
                    value = Long.parseLong(numStr);
                }
                i = valEnd;
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
                break;
            }

            result.add(value);

            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) i++;
        }

        return result;
    }

    /**
     * Default GrowthBook client implementation.
     */
    private static class DefaultGrowthBookClient implements GrowthBookClient {
        private final String clientKey;
        private final GrowthBookUserAttributes attributes;
        private volatile GrowthBookPayload payload;

        DefaultGrowthBookClient(String clientKey, GrowthBookUserAttributes attributes) {
            this.clientKey = clientKey;
            this.attributes = attributes;
        }

        @Override
        public GrowthBookPayload getPayload() {
            return payload;
        }

        @Override
        public void setPayload(GrowthBookPayload payload) {
            this.payload = payload;
        }

        @Override
        public Map<String, FeatureDefinition> getFeatures() {
            return payload != null ? payload.features() : new HashMap<>();
        }

        @Override
        public Object getFeatureValue(String feature, Object defaultValue) {
            if (payload != null && payload.features() != null) {
                FeatureDefinition f = payload.features().get(feature);
                if (f != null) {
                    return f.value() != null ? f.value() : f.defaultValue();
                }
            }
            return defaultValue;
        }

        @Override
        public void refreshFeatures() {
            // In real implementation, would fetch from API
        }

        @Override
        public void destroy() {
            payload = null;
        }
    }

    /**
     * Simple signal for event notification.
     */
    private static class Signal {
        private final Set<Runnable> listeners = ConcurrentHashMap.newKeySet();

        public Runnable subscribe(Runnable listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        public void emit() {
            for (Runnable listener : listeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    // Ignore listener errors
                }
            }
        }
    }
}