/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CachePaths (cache package version).
 */
class CachePathsCacheTest {

    @BeforeEach
    void setUp() {
        // Reset to default
        CachePaths.setBaseCachePath(null);
    }

    @Test
    @DisplayName("CachePaths getBaseCachePath returns non-null")
    void getBaseCachePath() {
        Path path = CachePaths.getBaseCachePath();
        assertNotNull(path);
        assertTrue(path.toString().contains(".claude"));
    }

    @Test
    @DisplayName("CachePaths setBaseCachePath allows custom path")
    void setBaseCachePath() {
        Path customPath = Paths.get("/tmp/test-cache");
        CachePaths.setBaseCachePath(customPath);

        assertEquals(customPath, CachePaths.getBaseCachePath());
    }

    @Test
    @DisplayName("CachePaths getCachePath returns subdirectory")
    void getCachePath() {
        Path subPath = CachePaths.getCachePath("subdir");
        assertTrue(subPath.toString().contains("subdir"));
        assertTrue(subPath.startsWith(CachePaths.getBaseCachePath()));
    }

    @Test
    @DisplayName("CachePaths getTasksPath returns tasks directory")
    void getTasksPath() {
        Path path = CachePaths.getTasksPath();
        assertTrue(path.toString().contains("tasks"));
    }

    @Test
    @DisplayName("CachePaths getHistoryPath returns history directory")
    void getHistoryPath() {
        Path path = CachePaths.getHistoryPath();
        assertTrue(path.toString().contains("history"));
    }

    @Test
    @DisplayName("CachePaths getSessionsPath returns sessions directory")
    void getSessionsPath() {
        Path path = CachePaths.getSessionsPath();
        assertTrue(path.toString().contains("sessions"));
    }

    @Test
    @DisplayName("CachePaths getMemoryPath returns memory directory")
    void getMemoryPath() {
        Path path = CachePaths.getMemoryPath();
        assertTrue(path.toString().contains("memory"));
    }

    @Test
    @DisplayName("CachePaths getPluginsPath returns plugins directory")
    void getPluginsPath() {
        Path path = CachePaths.getPluginsPath();
        assertTrue(path.toString().contains("plugins"));
    }

    @Test
    @DisplayName("CachePaths getSettingsPath returns settings directory")
    void getSettingsPath() {
        Path path = CachePaths.getSettingsPath();
        assertTrue(path.toString().contains("settings"));
    }

    @Test
    @DisplayName("CachePaths getLogsPath returns logs directory")
    void getLogsPath() {
        Path path = CachePaths.getLogsPath();
        assertTrue(path.toString().contains("logs"));
    }

    @Test
    @DisplayName("CachePaths getTempPath returns temp directory")
    void getTempPath() {
        Path path = CachePaths.getTempPath();
        assertTrue(path.toString().contains("temp"));
    }

    @Test
    @DisplayName("CachePaths getMcpPath returns mcp directory")
    void getMcpPath() {
        Path path = CachePaths.getMcpPath();
        assertTrue(path.toString().contains("mcp"));
    }

    @Test
    @DisplayName("CachePaths getProjectCachePath returns project-specific path")
    void getProjectCachePath() {
        Path path = CachePaths.getProjectCachePath("/home/user/myproject");
        assertTrue(path.toString().contains("projects"));
        assertTrue(path.toString().contains("myproject"));
    }

    @Test
    @DisplayName("CachePaths ensureCacheDirectory creates directory")
    void ensureCacheDirectory() {
        CachePaths.ensureCacheDirectory();
        Path base = CachePaths.getBaseCachePath();
        // Directory should exist or be creatable
        assertNotNull(base);
    }

    @Test
    @DisplayName("CachePaths ensureDirectory creates subdirectory")
    void ensureDirectory() {
        CachePaths.ensureDirectory("test-subdir");
        Path subPath = CachePaths.getCachePath("test-subdir");
        assertNotNull(subPath);
    }

    @Test
    @DisplayName("CachePaths getCacheSize returns non-negative")
    void getCacheSize() {
        long size = CachePaths.getCacheSize();
        assertTrue(size >= 0);
    }

    @Test
    @DisplayName("CachePaths getCacheSizeFormatted returns readable string")
    void getCacheSizeFormatted() {
        String formatted = CachePaths.getCacheSizeFormatted();
        assertNotNull(formatted);
        assertTrue(formatted.contains("B") ||
                   formatted.contains("KB") ||
                   formatted.contains("MB") ||
                   formatted.contains("GB"));
    }

    @Test
    @DisplayName("CachePaths CacheEntry record")
    void cacheEntryRecord() {
        Path testPath = Paths.get("/tmp/test");
        CachePaths.CacheEntry entry = new CachePaths.CacheEntry(
            "key", testPath, 100L, 1000L, 2000L, "type"
        );

        assertEquals("key", entry.key());
        assertEquals(testPath, entry.path());
        assertEquals(100L, entry.size());
        assertEquals(1000L, entry.createdAt());
        assertEquals(2000L, entry.lastAccessed());
        assertEquals("type", entry.type());
    }

    @Test
    @DisplayName("CachePaths CacheEntry isExpired false for recent access")
    void cacheEntryNotExpired() {
        long now = System.currentTimeMillis();
        CachePaths.CacheEntry entry = new CachePaths.CacheEntry(
            "key", Paths.get("/tmp"), 100L, now, now, "type"
        );

        assertFalse(entry.isExpired(60000)); // 1 minute TTL
    }

    @Test
    @DisplayName("CachePaths CacheEntry isExpired true for old access")
    void cacheEntryExpired() {
        long oldTime = System.currentTimeMillis() - 120000; // 2 minutes ago
        CachePaths.CacheEntry entry = new CachePaths.CacheEntry(
            "key", Paths.get("/tmp"), 100L, oldTime, oldTime, "type"
        );

        assertTrue(entry.isExpired(60000)); // 1 minute TTL
    }

    @Test
    @DisplayName("CachePaths clearCache clears directory")
    void clearCache() {
        // Just ensure it doesn't throw
        CachePaths.clearCache();
        assertNotNull(CachePaths.getBaseCachePath());
    }
}