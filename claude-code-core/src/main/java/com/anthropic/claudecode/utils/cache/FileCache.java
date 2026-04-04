/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cache utilities
 */
package com.anthropic.claudecode.utils.cache;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * File-based cache utilities.
 */
public final class FileCache {
    private FileCache() {}

    private static final Path CACHE_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "claude-code-cache");
    private static final ConcurrentHashMap<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();
    private static final int MAX_MEMORY_ENTRIES = 100;

    static {
        try {
            Files.createDirectories(CACHE_DIR);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Cache entry record.
     */
    public record CacheEntry(
            String key,
            byte[] data,
            Instant createdAt,
            Duration ttl,
            String etag
    ) {
        public boolean isExpired() {
            if (ttl == null) return false;
            return Instant.now().isAfter(createdAt.plus(ttl));
        }

        public long ageSeconds() {
            return Duration.between(createdAt, Instant.now()).getSeconds();
        }
    }

    /**
     * Get from cache.
     */
    public static Optional<byte[]> get(String key) {
        // Check memory cache first
        CacheEntry entry = memoryCache.get(key);
        if (entry != null) {
            if (entry.isExpired()) {
                memoryCache.remove(key);
            } else {
                return Optional.of(entry.data());
            }
        }

        // Check file cache
        Path cacheFile = getCachePath(key);
        if (Files.exists(cacheFile)) {
            try {
                byte[] data = Files.readAllBytes(cacheFile);
                // Add to memory cache
                if (memoryCache.size() < MAX_MEMORY_ENTRIES) {
                    memoryCache.put(key, new CacheEntry(key, data, Instant.now(), null, null));
                }
                return Optional.of(data);
            } catch (Exception e) {
                // Ignore
            }
        }

        return Optional.empty();
    }

    /**
     * Put to cache.
     */
    public static void put(String key, byte[] data) {
        put(key, data, null);
    }

    /**
     * Put to cache with TTL.
     */
    public static void put(String key, byte[] data, Duration ttl) {
        // Memory cache
        CacheEntry entry = new CacheEntry(key, data, Instant.now(), ttl, null);
        if (memoryCache.size() >= MAX_MEMORY_ENTRIES) {
            // Evict oldest
            String oldestKey = memoryCache.keySet().iterator().next();
            memoryCache.remove(oldestKey);
        }
        memoryCache.put(key, entry);

        // File cache
        Path cacheFile = getCachePath(key);
        try {
            Files.createDirectories(cacheFile.getParent());
            Files.write(cacheFile, data);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Remove from cache.
     */
    public static void remove(String key) {
        memoryCache.remove(key);

        Path cacheFile = getCachePath(key);
        try {
            Files.deleteIfExists(cacheFile);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Clear all cache.
     */
    public static void clear() {
        memoryCache.clear();

        try {
            Files.walk(CACHE_DIR)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
            Files.createDirectories(CACHE_DIR);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Clear expired entries.
     */
    public static void clearExpired() {
        // Clear expired memory entries
        memoryCache.entrySet().removeIf(e -> e.getValue().isExpired());

        // Clear old file entries (older than 7 days)
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));
        try (var stream = Files.list(CACHE_DIR)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Get cache path for a key.
     */
    private static Path getCachePath(String key) {
        // Use hash to avoid filename issues
        String hash = Integer.toHexString(key.hashCode());
        return CACHE_DIR.resolve(hash.substring(0, 2)).resolve(hash);
    }

    /**
     * Get cache statistics.
     */
    public static CacheStats getStats() {
        int memoryCount = memoryCache.size();
        long fileCount = 0;
        long totalSize = 0;

        try (var stream = Files.walk(CACHE_DIR)) {
            var stats = stream.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .summaryStatistics();

            fileCount = stats.getCount();
            totalSize = stats.getSum();
        } catch (Exception e) {
            // Ignore
        }

        return new CacheStats(memoryCount, (int) fileCount, totalSize);
    }

    /**
     * Cache statistics record.
     */
    public record CacheStats(
            int memoryEntries,
            int fileEntries,
            long totalBytes
    ) {
        public String formattedSize() {
            if (totalBytes < 1024) {
                return totalBytes + " B";
            } else if (totalBytes < 1024 * 1024) {
                return String.format("%.1f KB", totalBytes / 1024.0);
            } else {
                return String.format("%.1f MB", totalBytes / (1024.0 * 1024));
            }
        }
    }

    /**
     * Compute if absent with loader.
     */
    public static Optional<byte[]> computeIfAbsent(String key, Duration ttl, java.util.function.Supplier<Optional<byte[]>> loader) {
        Optional<byte[]> cached = get(key);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<byte[]> loaded = loader.get();
        loaded.ifPresent(data -> put(key, data, ttl));

        return loaded;
    }
}