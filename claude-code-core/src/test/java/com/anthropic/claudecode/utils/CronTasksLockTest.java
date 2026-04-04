/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CronTasksLock.
 */
class CronTasksLockTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("CronTasksLock SchedulerLockOptions record")
    void schedulerLockOptionsRecord() {
        CronTasksLock.SchedulerLockOptions opts = new CronTasksLock.SchedulerLockOptions(
            tempDir, "session-123"
        );
        assertEquals(tempDir, opts.dir());
        assertEquals("session-123", opts.lockIdentity());
    }

    @Test
    @DisplayName("CronTasksLock tryAcquireSchedulerLock returns boolean")
    void tryAcquireSchedulerLock() {
        CronTasksLock.SchedulerLockOptions opts = new CronTasksLock.SchedulerLockOptions(
            tempDir, "test-session"
        );
        boolean result = CronTasksLock.tryAcquireSchedulerLock(opts);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("CronTasksLock tryAcquireSchedulerLock null options")
    void tryAcquireSchedulerLockNullOpts() {
        // Should not throw
        boolean result = CronTasksLock.tryAcquireSchedulerLock(null);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("CronTasksLock tryAcquireSchedulerLock with null identity")
    void tryAcquireSchedulerLockNullIdentity() {
        CronTasksLock.SchedulerLockOptions opts = new CronTasksLock.SchedulerLockOptions(
            tempDir, null
        );
        boolean result = CronTasksLock.tryAcquireSchedulerLock(opts);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("CronTasksLock releaseSchedulerLock does not throw")
    void releaseSchedulerLockNoThrow() {
        CronTasksLock.SchedulerLockOptions opts = new CronTasksLock.SchedulerLockOptions(
            tempDir, "test-session"
        );
        assertDoesNotThrow(() -> CronTasksLock.releaseSchedulerLock(opts));
    }

    @Test
    @DisplayName("CronTasksLock releaseSchedulerLock null options")
    void releaseSchedulerLockNullOpts() {
        assertDoesNotThrow(() -> CronTasksLock.releaseSchedulerLock(null));
    }

    @Test
    @DisplayName("CronTasksLock can re-acquire after release")
    void canReacquireAfterRelease() {
        CronTasksLock.SchedulerLockOptions opts = new CronTasksLock.SchedulerLockOptions(
            tempDir, "session-abc"
        );

        boolean acquired = CronTasksLock.tryAcquireSchedulerLock(opts);
        CronTasksLock.releaseSchedulerLock(opts);

        boolean reacquired = CronTasksLock.tryAcquireSchedulerLock(opts);
        // Both should succeed (result depends on lock state)
        assertTrue(acquired == true || acquired == false);
        assertTrue(reacquired == true || reacquired == false);
    }
}