/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileCache.
 */
class FileCacheTest {

    @BeforeEach
    void setUp() {
        FileCache.clear();
    }

    @Test
    @DisplayName("FileCache get returns empty for missing key")
    void getMissing() {
        Optional<byte[]> result = FileCache.get("missing-key");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("FileCache put and get returns data")
    void putAndGet() {
        byte[] data = "test data".getBytes();
        FileCache.put("key1", data);

        Optional<byte[]> result = FileCache.get("key1");
        assertTrue(result.isPresent());
        assertArrayEquals(data, result.get());
    }

    @Test
    @DisplayName("FileCache put with TTL")
    void putWithTtl() {
        byte[] data = "ttl data".getBytes();
        FileCache.put("key2", data, Duration.ofMinutes(5));

        Optional<byte[]> result = FileCache.get("key2");
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("FileCache remove deletes entry")
    void remove() {
        byte[] data = "removable".getBytes();
        FileCache.put("remove-key", data);
        assertTrue(FileCache.get("remove-key").isPresent());

        FileCache.remove("remove-key");
        assertFalse(FileCache.get("remove-key").isPresent());
    }

    @Test
    @DisplayName("FileCache clear removes all entries")
    void clear() {
        FileCache.put("key1", "data1".getBytes());
        FileCache.put("key2", "data2".getBytes());
        FileCache.clear();

        assertFalse(FileCache.get("key1").isPresent());
        assertFalse(FileCache.get("key2").isPresent());
    }

    @Test
    @DisplayName("FileCache CacheEntry isExpired false when TTL null")
    void cacheEntryNotExpiredNoTtl() {
        FileCache.CacheEntry entry = new FileCache.CacheEntry(
            "key", "data".getBytes(), Instant.now(), null, null
        );
        assertFalse(entry.isExpired());
    }

    @Test
    @DisplayName("FileCache CacheEntry isExpired true after TTL")
    void cacheEntryExpired() {
        Instant past = Instant.now().minus(Duration.ofMinutes(10));
        FileCache.CacheEntry entry = new FileCache.CacheEntry(
            "key", "data".getBytes(), past, Duration.ofMinutes(5), null
        );
        assertTrue(entry.isExpired());
    }

    @Test
    @DisplayName("FileCache CacheEntry ageSeconds")
    void cacheEntryAge() {
        Instant past = Instant.now().minusSeconds(30);
        FileCache.CacheEntry entry = new FileCache.CacheEntry(
            "key", "data".getBytes(), past, null, null
        );
        assertTrue(entry.ageSeconds() >= 30);
    }

    @Test
    @DisplayName("FileCache getStats returns non-negative values")
    void getStats() {
        FileCache.clear();
        FileCache.CacheStats stats = FileCache.getStats();

        assertTrue(stats.memoryEntries() >= 0);
        assertTrue(stats.fileEntries() >= 0);
        assertTrue(stats.totalBytes() >= 0);
    }

    @Test
    @DisplayName("FileCache CacheStats formattedSize formats bytes")
    void cacheStatsFormattedSizeBytes() {
        FileCache.CacheStats stats = new FileCache.CacheStats(0, 0, 500);
        assertEquals("500 B", stats.formattedSize());
    }

    @Test
    @DisplayName("FileCache CacheStats formattedSize formats KB")
    void cacheStatsFormattedSizeKB() {
        FileCache.CacheStats stats = new FileCache.CacheStats(0, 0, 2048);
        assertTrue(stats.formattedSize().contains("KB"));
    }

    @Test
    @DisplayName("FileCache CacheStats formattedSize formats MB")
    void cacheStatsFormattedSizeMB() {
        FileCache.CacheStats stats = new FileCache.CacheStats(0, 0, 2 * 1024 * 1024);
        assertTrue(stats.formattedSize().contains("MB"));
    }

    @Test
    @DisplayName("FileCache computeIfAbsent returns cached value")
    void computeIfAbsentCached() {
        byte[] data = "cached".getBytes();
        FileCache.put("compute-key", data);

        Optional<byte[]> result = FileCache.computeIfAbsent(
            "compute-key", Duration.ofMinutes(5),
            () -> Optional.of("new".getBytes())
        );

        assertTrue(result.isPresent());
        assertArrayEquals(data, result.get()); // Returns cached, not new
    }

    @Test
    @DisplayName("FileCache computeIfAbsent loads and caches new value")
    void computeIfAbsentNew() {
        Optional<byte[]> result = FileCache.computeIfAbsent(
            "new-key", Duration.ofMinutes(5),
            () -> Optional.of("loaded".getBytes())
        );

        assertTrue(result.isPresent());
        assertArrayEquals("loaded".getBytes(), result.get());

        // Verify it's now cached
        Optional<byte[]> cached = FileCache.get("new-key");
        assertTrue(cached.isPresent());
    }

    @Test
    @DisplayName("FileCache computeIfAbsent handles empty loader")
    void computeIfAbsentEmpty() {
        Optional<byte[]> result = FileCache.computeIfAbsent(
            "empty-key", Duration.ofMinutes(5),
            () -> Optional.empty()
        );

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("FileCache memory cache limit")
    void memoryCacheLimit() {
        for (int i = 0; i < 150; i++) {
            FileCache.put("mem-key-" + i, ("data-" + i).getBytes());
        }

        // Memory cache should have evicted some entries
        FileCache.CacheStats stats = FileCache.getStats();
        assertTrue(stats.memoryEntries() <= 100);
    }

    @Test
    @DisplayName("FileCache handles large data")
    void largeData() {
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        FileCache.put("large-key", largeData);
        Optional<byte[]> result = FileCache.get("large-key");

        assertTrue(result.isPresent());
        assertArrayEquals(largeData, result.get());
    }
}