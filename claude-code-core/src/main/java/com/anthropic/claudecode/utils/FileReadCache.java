/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code file read cache
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

/**
 * A simple in-memory cache for file contents with automatic invalidation based on modification time.
 * This eliminates redundant file reads in FileEditTool operations.
 */
public final class FileReadCache {
    private final Map<String, CachedFileData> cache = new LinkedHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * Cached file data record.
     */
    private record CachedFileData(String content, Charset encoding, long mtime) {}

    /**
     * Reads a file with caching. Returns both content and encoding.
     * Cache key includes file path and modification time for automatic invalidation.
     */
    public CachedReadResult readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        // Get file stats for cache invalidation
        if (!Files.exists(path)) {
            cache.remove(filePath);
            throw new IOException("File not found: " + filePath);
        }

        long mtime = Files.getLastModifiedTime(path).toMillis();
        CachedFileData cachedData = cache.get(filePath);

        // Check if we have valid cached data
        if (cachedData != null && cachedData.mtime() == mtime) {
            return new CachedReadResult(cachedData.content(), cachedData.encoding());
        }

        // Cache miss or stale data - read the file
        Charset encoding = FileRead.detectEncoding(path);
        String rawContent = Files.readString(path, encoding);
        String content = rawContent.replace("\r\n", "\n");

        // Update cache
        cache.put(filePath, new CachedFileData(content, encoding, mtime));

        // Evict oldest entries if cache is too large
        if (cache.size() > MAX_CACHE_SIZE) {
            String firstKey = cache.keySet().iterator().next();
            cache.remove(firstKey);
        }

        return new CachedReadResult(content, encoding);
    }

    /**
     * Clears the entire cache. Useful for testing or memory management.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Removes a specific file from the cache.
     */
    public void invalidate(String filePath) {
        cache.remove(filePath);
    }

    /**
     * Gets cache statistics for debugging/monitoring.
     */
    public CacheStats getStats() {
        return new CacheStats(cache.size(), new ArrayList<>(cache.keySet()));
    }

    /**
     * Cached read result.
     */
    public record CachedReadResult(String content, Charset encoding) {}

    /**
     * Cache statistics.
     */
    public record CacheStats(int size, List<String> entries) {}
}