/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CachePaths.
 */
class CachePathsTest {

    @Test
    @DisplayName("CachePaths getCacheRoot returns non-null path")
    void getCacheRoot() {
        Path root = CachePaths.getCacheRoot();

        assertNotNull(root);
        assertTrue(root.toString().contains("claude-code"));
    }

    @Test
    @DisplayName("CachePaths getCacheDir returns subdirectory")
    void getCacheDir() {
        Path dir = CachePaths.getCacheDir("test");

        assertTrue(dir.toString().contains("test"));
        assertTrue(dir.startsWith(CachePaths.getCacheRoot()));
    }

    @Test
    @DisplayName("CachePaths getPromptCacheDir returns prompts directory")
    void getPromptCacheDir() {
        Path dir = CachePaths.getPromptCacheDir();

        assertTrue(dir.toString().contains("prompts"));
    }

    @Test
    @DisplayName("CachePaths getToolCacheDir returns tools directory")
    void getToolCacheDir() {
        Path dir = CachePaths.getToolCacheDir();

        assertTrue(dir.toString().contains("tools"));
    }

    @Test
    @DisplayName("CachePaths getSessionCacheDir returns sessions directory")
    void getSessionCacheDir() {
        Path dir = CachePaths.getSessionCacheDir();

        assertTrue(dir.toString().contains("sessions"));
    }

    @Test
    @DisplayName("CachePaths getHttpCacheDir returns http directory")
    void getHttpCacheDir() {
        Path dir = CachePaths.getHttpCacheDir();

        assertTrue(dir.toString().contains("http"));
    }

    @Test
    @DisplayName("CachePaths getPluginCacheDir returns plugins directory")
    void getPluginCacheDir() {
        Path dir = CachePaths.getPluginCacheDir();

        assertTrue(dir.toString().contains("plugins"));
    }

    @Test
    @DisplayName("CachePaths getLspCacheDir returns lsp directory")
    void getLspCacheDir() {
        Path dir = CachePaths.getLspCacheDir();

        assertTrue(dir.toString().contains("lsp"));
    }

    @Test
    @DisplayName("CachePaths getModelsCacheDir returns models directory")
    void getModelsCacheDir() {
        Path dir = CachePaths.getModelsCacheDir();

        assertTrue(dir.toString().contains("models"));
    }

    @Test
    @DisplayName("CachePaths getTempCacheDir returns temp directory")
    void getTempCacheDir() {
        Path dir = CachePaths.getTempCacheDir();

        assertTrue(dir.toString().contains("temp"));
    }

    @Test
    @DisplayName("CachePaths errors returns errors directory")
    void errors() {
        Path dir = CachePaths.errors();

        assertTrue(dir.toString().contains("errors"));
    }

    @Test
    @DisplayName("CachePaths mcpLogs returns mcp server directory")
    void mcpLogs() {
        Path dir = CachePaths.mcpLogs("myserver");

        assertTrue(dir.toString().contains("mcp"));
        assertTrue(dir.toString().contains("myserver"));
    }

    @Test
    @DisplayName("CachePaths ensureCacheDir creates directory")
    void ensureCacheDir() {
        Path dir = CachePaths.ensureCacheDir("test-dir");

        assertNotNull(dir);
    }

    @Test
    @DisplayName("CachePaths getCacheSize returns non-negative")
    void getCacheSize() {
        long size = CachePaths.getCacheSize();

        assertTrue(size >= 0);
    }

    @Test
    @DisplayName("CachePaths formatCacheSize returns readable string")
    void formatCacheSize() {
        String size = CachePaths.formatCacheSize();

        assertNotNull(size);
        assertTrue(size.contains("B") || size.contains("KB") || size.contains("MB") || size.contains("GB"));
    }
}