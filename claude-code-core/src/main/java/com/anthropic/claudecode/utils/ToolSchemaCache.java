/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/toolSchemaCache
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Tool schema cache - Caches tool JSON schemas.
 */
public final class ToolSchemaCache {
    private static final ConcurrentHashMap<String, SchemaEntry> cache = new ConcurrentHashMap<>();
    private static final long DEFAULT_TTL = 5 * 60 * 1000; // 5 minutes

    /**
     * Get a cached schema.
     */
    public static String get(String toolName) {
        SchemaEntry entry = cache.get(toolName);
        if (entry == null || entry.isExpired()) {
            return null;
        }
        return entry.schema;
    }

    /**
     * Put a schema in the cache.
     */
    public static void put(String toolName, String schema) {
        put(toolName, schema, DEFAULT_TTL);
    }

    /**
     * Put a schema in the cache with custom TTL.
     */
    public static void put(String toolName, String schema, long ttlMs) {
        cache.put(toolName, new SchemaEntry(schema, System.currentTimeMillis() + ttlMs));
    }

    /**
     * Remove a cached schema.
     */
    public static void remove(String toolName) {
        cache.remove(toolName);
    }

    /**
     * Check if a schema is cached.
     */
    public static boolean contains(String toolName) {
        SchemaEntry entry = cache.get(toolName);
        if (entry == null) return false;
        if (entry.isExpired()) {
            cache.remove(toolName);
            return false;
        }
        return true;
    }

    /**
     * Get or compute a schema.
     */
    public static String getOrCompute(String toolName, java.util.function.Supplier<String> supplier) {
        String cached = get(toolName);
        if (cached != null) {
            return cached;
        }

        String schema = supplier.get();
        if (schema != null) {
            put(toolName, schema);
        }
        return schema;
    }

    /**
     * Clear the cache.
     */
    public static void clear() {
        cache.clear();
    }

    /**
     * Evict expired entries.
     */
    public static void evictExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Get cache size.
     */
    public static int size() {
        return cache.size();
    }

    /**
     * Get cached tool names.
     */
    public static Set<String> getCachedTools() {
        return new HashSet<>(cache.keySet());
    }

    /**
     * Schema entry record.
     */
    private record SchemaEntry(String schema, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}