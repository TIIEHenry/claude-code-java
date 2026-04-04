/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/fileStateCache.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * File state cache for tracking file contents.
 */
public class FileStateCache {
    private final Map<String, FileState> cache = new ConcurrentHashMap<>();
    private final int maxSize;

    public FileStateCache() {
        this(1000);
    }

    public FileStateCache(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Get file state.
     */
    public Optional<FileState> get(String path) {
        return Optional.ofNullable(cache.get(path));
    }

    /**
     * Put file state.
     */
    public void put(String path, String content, String hash) {
        if (cache.size() >= maxSize) {
            // Remove oldest entry
            String oldestKey = cache.entrySet().stream()
                .min(Comparator.comparingLong(e -> e.getValue().timestamp()))
                .map(Map.Entry::getKey)
                .orElse(null);
            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }
        cache.put(path, new FileState(content, hash, System.currentTimeMillis()));
    }

    /**
     * Check if path is cached.
     */
    public boolean has(String path) {
        return cache.containsKey(path);
    }

    /**
     * Remove a path from cache.
     */
    public void remove(String path) {
        cache.remove(path);
    }

    /**
     * Clear cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get cache size.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Clone the cache.
     */
    public FileStateCache clone() {
        FileStateCache cloned = new FileStateCache(maxSize);
        cloned.cache.putAll(this.cache);
        return cloned;
    }

    /**
     * File state record.
     */
    public record FileState(String content, String hash, long timestamp) {}
}