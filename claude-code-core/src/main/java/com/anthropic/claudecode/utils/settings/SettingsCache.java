/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code settings cache
 */
package com.anthropic.claudecode.utils.settings;

import java.util.*;
import java.util.concurrent.*;

/**
 * Settings cache for performance optimization.
 */
public final class SettingsCache {
    private SettingsCache() {}

    // Session-level settings cache
    private static volatile SettingsWithErrors sessionSettingsCache = null;

    // Per-source cache
    private static final ConcurrentHashMap<String, Map<String, Object>> perSourceCache = new ConcurrentHashMap<>();

    // Parsed file cache
    private static final ConcurrentHashMap<String, ParsedSettings> parseFileCache = new ConcurrentHashMap<>();

    // Plugin settings base layer
    private static volatile Map<String, Object> pluginSettingsBase = null;

    /**
     * Settings with errors result.
     */
    public record SettingsWithErrors(
            Map<String, Object> settings,
            List<ValidationError> errors
    ) {
        public static SettingsWithErrors empty() {
            return new SettingsWithErrors(new LinkedHashMap<>(), new ArrayList<>());
        }
    }

    /**
     * Validation error.
     */
    public record ValidationError(
            String file,
            String path,
            String message,
            String expected,
            Object invalidValue,
            String suggestion
    ) {}

    /**
     * Parsed settings from file.
     */
    public record ParsedSettings(
            Map<String, Object> settings,
            List<ValidationError> errors
    ) {}

    /**
     * Get session settings cache.
     */
    public static SettingsWithErrors getSessionSettingsCache() {
        return sessionSettingsCache;
    }

    /**
     * Set session settings cache.
     */
    public static void setSessionSettingsCache(SettingsWithErrors value) {
        sessionSettingsCache = value;
    }

    /**
     * Get cached settings for a source.
     */
    public static Map<String, Object> getCachedSettingsForSource(String source) {
        return perSourceCache.get(source);
    }

    /**
     * Check if source has cached value.
     */
    public static boolean hasCachedSettingsForSource(String source) {
        return perSourceCache.containsKey(source);
    }

    /**
     * Set cached settings for a source.
     */
    public static void setCachedSettingsForSource(String source, Map<String, Object> value) {
        if (value == null) {
            perSourceCache.put(source, new LinkedHashMap<>());
        } else {
            perSourceCache.put(source, value);
        }
    }

    /**
     * Get cached parsed file.
     */
    public static ParsedSettings getCachedParsedFile(String path) {
        return parseFileCache.get(path);
    }

    /**
     * Set cached parsed file.
     */
    public static void setCachedParsedFile(String path, ParsedSettings value) {
        parseFileCache.put(path, value);
    }

    /**
     * Reset all settings caches.
     */
    public static void resetSettingsCache() {
        sessionSettingsCache = null;
        perSourceCache.clear();
        parseFileCache.clear();
    }

    /**
     * Get plugin settings base.
     */
    public static Map<String, Object> getPluginSettingsBase() {
        return pluginSettingsBase;
    }

    /**
     * Set plugin settings base.
     */
    public static void setPluginSettingsBase(Map<String, Object> settings) {
        pluginSettingsBase = settings;
    }

    /**
     * Clear plugin settings base.
     */
    public static void clearPluginSettingsBase() {
        pluginSettingsBase = null;
    }

    /**
     * Get all cached sources.
     */
    public static Set<String> getCachedSources() {
        return new HashSet<>(perSourceCache.keySet());
    }

    /**
     * Get all cached file paths.
     */
    public static Set<String> getCachedFilePaths() {
        return new HashSet<>(parseFileCache.keySet());
    }

    /**
     * Get cache statistics.
     */
    public static CacheStats getCacheStats() {
        return new CacheStats(
                sessionSettingsCache != null,
                perSourceCache.size(),
                parseFileCache.size(),
                pluginSettingsBase != null
        );
    }

    /**
     * Cache statistics.
     */
    public record CacheStats(
            boolean hasSessionCache,
            int sourceCacheSize,
            int fileCacheSize,
            boolean hasPluginBase
    ) {}

    /**
     * Merge settings from multiple sources.
     */
    public static Map<String, Object> mergeSettings(
            List<Map<String, Object>> sources) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map<String, Object> source : sources) {
            if (source != null) {
                deepMerge(result, source);
            }
        }

        return result;
    }

    /**
     * Deep merge two maps.
     */
    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map && target.get(key) instanceof Map) {
                Map<String, Object> nestedTarget = (Map<String, Object>) target.get(key);
                Map<String, Object> nestedSource = (Map<String, Object>) value;
                deepMerge(nestedTarget, nestedSource);
            } else {
                target.put(key, value);
            }
        }
    }

    /**
     * Compute settings with cache.
     */
    public static Map<String, Object> computeIfAbsent(
            String source,
            java.util.function.Supplier<Map<String, Object>> supplier) {

        Map<String, Object> cached = getCachedSettingsForSource(source);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> value = supplier.get();
        setCachedSettingsForSource(source, value);
        return value;
    }
}