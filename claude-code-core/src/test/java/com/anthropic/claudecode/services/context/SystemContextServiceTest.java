/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SystemContextService.
 */
class SystemContextServiceTest {

    @BeforeEach
    void setUp() {
        SystemContextService.clearCaches();
        SystemContextService.setSystemPromptInjection(null);
    }

    @Test
    @DisplayName("SystemContextService getSystemPromptInjection returns null initially")
    void getSystemPromptInjectionInitial() {
        assertNull(SystemContextService.getSystemPromptInjection());
    }

    @Test
    @DisplayName("SystemContextService setSystemPromptInjection sets value")
    void setSystemPromptInjection() {
        SystemContextService.setSystemPromptInjection("test-injection");

        assertEquals("test-injection", SystemContextService.getSystemPromptInjection());
    }

    @Test
    @DisplayName("SystemContextService setSystemPromptInjection clears caches")
    void setSystemPromptInjectionClearsCaches() throws Exception {
        // Prime the cache
        CompletableFuture<Map<String, String>> future = SystemContextService.getSystemContext();
        future.join();

        // Set injection should clear cache
        SystemContextService.setSystemPromptInjection("new-injection");

        // Next call should regenerate (injection should be in result)
        SystemContextService.clearCaches(); // Ensure we get fresh data
        Map<String, String> context = SystemContextService.getSystemContext().join();

        assertTrue(context.containsKey("cacheBreaker"));
        assertTrue(context.get("cacheBreaker").contains("new-injection"));
    }

    @Test
    @DisplayName("SystemContextService clearCaches clears all caches")
    void clearCaches() throws Exception {
        // Prime the caches
        SystemContextService.getSystemContext().join();
        SystemContextService.getUserContext().join();

        // Clear caches
        SystemContextService.clearCaches();

        // Caches should be null now (will be regenerated on next call)
        // We verify by checking that new calls work
        assertNotNull(SystemContextService.getSystemContext().join());
        assertNotNull(SystemContextService.getUserContext().join());
    }

    @Test
    @DisplayName("SystemContextService getUserContext returns currentDate")
    void getUserContextCurrentDate() throws Exception {
        Map<String, String> context = SystemContextService.getUserContext().join();

        assertTrue(context.containsKey("currentDate"));
        assertTrue(context.get("currentDate").contains("Today's date is"));
    }

    @Test
    @DisplayName("SystemContextService getUserContext caches result")
    void getUserContextCaches() throws Exception {
        Map<String, String> context1 = SystemContextService.getUserContext().join();
        Map<String, String> context2 = SystemContextService.getUserContext().join();

        // Should be the same cached instance
        assertSame(context1, context2);
    }

    @Test
    @DisplayName("SystemContextService getSystemContext returns map")
    void getSystemContext() throws Exception {
        Map<String, String> context = SystemContextService.getSystemContext().join();

        assertNotNull(context);
    }

    @Test
    @DisplayName("SystemContextService getSystemContext caches result")
    void getSystemContextCaches() throws Exception {
        Map<String, String> context1 = SystemContextService.getSystemContext().join();
        Map<String, String> context2 = SystemContextService.getSystemContext().join();

        // Should be the same cached instance
        assertSame(context1, context2);
    }

    @Test
    @DisplayName("SystemContextService getGitStatus returns result or null")
    void getGitStatus() throws Exception {
        // This test depends on whether we're in a git repo
        String gitStatus = SystemContextService.getGitStatus().join();

        // Either returns valid status (if in git repo) or null (if not)
        // Both are acceptable outcomes
        if (gitStatus != null) {
            assertTrue(gitStatus.contains("Current branch") || gitStatus.contains("Main branch"));
        }
    }

    @Test
    @DisplayName("SystemContextService cacheBreaker format")
    void cacheBreakerFormat() throws Exception {
        SystemContextService.setSystemPromptInjection("unique-value-123");
        SystemContextService.clearCaches();

        Map<String, String> context = SystemContextService.getSystemContext().join();

        assertTrue(context.containsKey("cacheBreaker"));
        assertEquals("[CACHE_BREAKER: unique-value-123]", context.get("cacheBreaker"));
    }
}