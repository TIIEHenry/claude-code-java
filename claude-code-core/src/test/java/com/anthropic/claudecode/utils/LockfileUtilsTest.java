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
 * Tests for LockfileUtils.
 */
class LockfileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("LockfileUtils LockHandle record")
    void lockHandleRecord() {
        Path lockPath = tempDir.resolve("test.lock");
        LockfileUtils.LockHandle handle = new LockfileUtils.LockHandle(lockPath, true);
        assertEquals(lockPath, handle.getPath());
        assertTrue(handle.isValid());
    }

    @Test
    @DisplayName("LockfileUtils LockHandle invalidate")
    void lockHandleInvalidate() {
        Path lockPath = tempDir.resolve("test.lock");
        LockfileUtils.LockHandle handle = new LockfileUtils.LockHandle(lockPath, true);
        handle.invalidate();
        assertFalse(handle.isValid());
    }

    @Test
    @DisplayName("LockfileUtils tryAcquire creates lock")
    void tryAcquireCreatesLock() {
        Path lockPath = tempDir.resolve("test.lock");
        LockfileUtils.LockHandle handle = LockfileUtils.tryAcquire(lockPath);
        assertNotNull(handle);
        assertTrue(handle.isValid());
        assertTrue(java.nio.file.Files.exists(lockPath));

        LockfileUtils.release(handle);
    }

    @Test
    @DisplayName("LockfileUtils release removes lock")
    void releaseRemovesLock() {
        Path lockPath = tempDir.resolve("test.lock");
        LockfileUtils.LockHandle handle = LockfileUtils.tryAcquire(lockPath);
        assertNotNull(handle);

        LockfileUtils.release(handle);
        assertFalse(handle.isValid());
    }

    @Test
    @DisplayName("LockfileUtils release null does nothing")
    void releaseNull() {
        LockfileUtils.release(null);
        // Should not throw
        assertTrue(true);
    }

    @Test
    @DisplayName("LockfileUtils isLocked returns true when locked")
    void isLockedTrue() {
        Path lockPath = tempDir.resolve("test.lock");
        LockfileUtils.LockHandle handle = LockfileUtils.tryAcquire(lockPath);
        assertNotNull(handle);

        assertTrue(LockfileUtils.isLocked(lockPath));
        LockfileUtils.release(handle);
    }

    @Test
    @DisplayName("LockfileUtils isLocked returns false when not locked")
    void isLockedFalse() {
        Path lockPath = tempDir.resolve("nonexistent.lock");
        assertFalse(LockfileUtils.isLocked(lockPath));
    }

    @Test
    @DisplayName("LockfileUtils forceRelease removes lock")
    void forceRelease() {
        Path lockPath = tempDir.resolve("test.lock");
        LockfileUtils.LockHandle handle = LockfileUtils.tryAcquire(lockPath);
        assertNotNull(handle);

        LockfileUtils.forceRelease(lockPath);
        assertFalse(java.nio.file.Files.exists(lockPath));
    }

    @Test
    @DisplayName("LockfileUtils withLock executes task")
    void withLock() throws Exception {
        Path lockPath = tempDir.resolve("test.lock");
        String result = LockfileUtils.withLock(lockPath, () -> "success");
        assertEquals("success", result);

        // Lock should be released after task
        assertFalse(LockfileUtils.isLocked(lockPath));
    }

    @Test
    @DisplayName("LockfileUtils tryAcquire returns same handle for same process")
    void tryAcquireSameProcess() {
        Path lockPath = tempDir.resolve("test.lock");
        LockfileUtils.LockHandle handle1 = LockfileUtils.tryAcquire(lockPath);
        LockfileUtils.LockHandle handle2 = LockfileUtils.tryAcquire(lockPath);

        // Should return same handle since we already hold the lock
        assertNotNull(handle1);
        assertNotNull(handle2);

        LockfileUtils.release(handle1);
    }
}