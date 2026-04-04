/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CleanupUtils.
 */
class CleanupUtilsTest {

    @BeforeEach
    void clearTasks() {
        CleanupUtils.clearPendingTasks();
    }

    @Test
    @DisplayName("CleanupUtils register adds task")
    void registerWorks() {
        boolean[] executed = {false};
        CleanupUtils.register(() -> executed[0] = true, "test task");

        List<String> pending = CleanupUtils.getPendingCleanupTasks();
        assertTrue(pending.contains("test task"));
    }

    @Test
    @DisplayName("CleanupUtils runCleanup executes tasks")
    void runCleanupWorks() {
        boolean[] executed = {false};
        CleanupUtils.register(() -> executed[0] = true, "test task");

        CleanupUtils.runCleanup();

        assertTrue(executed[0]);
    }

    @Test
    @DisplayName("CleanupUtils runCleanup clears tasks")
    void runCleanupClears() {
        CleanupUtils.register(() -> {}, "test task");

        CleanupUtils.runCleanup();

        assertTrue(CleanupUtils.getPendingCleanupTasks().isEmpty());
    }

    @Test
    @DisplayName("CleanupUtils unregister removes task")
    void unregisterWorks() {
        Runnable task = () -> {};
        CleanupUtils.register(task, "test task");

        CleanupUtils.unregister(task);

        assertFalse(CleanupUtils.getPendingCleanupTasks().contains("test task"));
    }

    @Test
    @DisplayName("CleanupUtils registerHighPriority adds with high priority")
    void registerHighPriorityWorks() {
        CleanupUtils.register(() -> {}, "low priority", 0);
        CleanupUtils.registerHighPriority(() -> {}, "high priority");

        List<String> pending = CleanupUtils.getPendingCleanupTasks();
        assertEquals("high priority", pending.get(0));
    }

    @Test
    @DisplayName("CleanupUtils clearPendingTasks removes all")
    void clearPendingTasksWorks() {
        CleanupUtils.register(() -> {}, "task1");
        CleanupUtils.register(() -> {}, "task2");

        CleanupUtils.clearPendingTasks();

        assertTrue(CleanupUtils.getPendingCleanupTasks().isEmpty());
    }

    @Test
    @DisplayName("CleanupUtils getPendingCleanupTasks returns descriptions")
    void getPendingCleanupTasksWorks() {
        CleanupUtils.register(() -> {}, "task1");
        CleanupUtils.register(() -> {}, "task2");

        List<String> pending = CleanupUtils.getPendingCleanupTasks();

        assertEquals(2, pending.size());
        assertTrue(pending.contains("task1"));
        assertTrue(pending.contains("task2"));
    }

    @Test
    @DisplayName("CleanupUtils runCleanup handles exceptions")
    void runCleanupHandlesExceptions() {
        CleanupUtils.register(() -> {
            throw new RuntimeException("Test exception");
        }, "failing task");

        // Should not throw
        assertDoesNotThrow(CleanupUtils::runCleanup);
    }

    @Test
    @DisplayName("CleanupUtils CleanupScope runs tasks in reverse order")
    void cleanupScopeReverseOrder() {
        StringBuilder sb = new StringBuilder();

        try (CleanupUtils.CleanupScope scope = new CleanupUtils.CleanupScope()) {
            scope.register(() -> sb.append("first"));
            scope.register(() -> sb.append("second"));
        }

        assertEquals("secondfirst", sb.toString());
    }

    @Test
    @DisplayName("CleanupUtils CleanupScope handles exceptions")
    void cleanupScopeExceptions() {
        try (CleanupUtils.CleanupScope scope = new CleanupUtils.CleanupScope()) {
            scope.register(() -> {});
            scope.register(() -> {
                throw new RuntimeException("Test");
            });
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cleanup errors"));
        }
    }

    @Test
    @DisplayName("CleanupUtils cleanupNpmCacheForAnthropicPackages does not throw")
    void cleanupNpmCacheDoesNotThrow() {
        assertDoesNotThrow(CleanupUtils::cleanupNpmCacheForAnthropicPackages);
    }

    @Test
    @DisplayName("CleanupUtils cleanupOldVersionsThrottled does not throw")
    void cleanupOldVersionsThrottledDoesNotThrow() {
        assertDoesNotThrow(CleanupUtils::cleanupOldVersionsThrottled);
    }

    @Test
    @DisplayName("CleanupUtils cleanupOldMessageFilesInBackground does not throw")
    void cleanupOldMessageFilesDoesNotThrow() {
        assertDoesNotThrow(CleanupUtils::cleanupOldMessageFilesInBackground);
    }

    @Test
    @DisplayName("CleanupUtils cleanupOldVersions does not throw")
    void cleanupOldVersionsDoesNotThrow() {
        assertDoesNotThrow(CleanupUtils::cleanupOldVersions);
    }
}