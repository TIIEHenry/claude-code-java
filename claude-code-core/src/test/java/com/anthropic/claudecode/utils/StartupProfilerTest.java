/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StartupProfiler.
 */
class StartupProfilerTest {

    @Test
    @DisplayName("StartupProfiler Checkpoint record")
    void checkpointRecord() {
        StartupProfiler.Checkpoint cp = new StartupProfiler.Checkpoint("test", 1000L, 500L);

        assertEquals("test", cp.name());
        assertEquals(1000L, cp.timestampMs());
        assertEquals(500L, cp.relativeMs());
    }

    @Test
    @DisplayName("StartupProfiler MemorySnapshot record")
    void memorySnapshotRecord() {
        StartupProfiler.MemorySnapshot snapshot = new StartupProfiler.MemorySnapshot(
            1000000L, 2000000L, 4000000L, 500000L
        );

        assertEquals(1000000L, snapshot.heapUsed());
        assertEquals(2000000L, snapshot.heapCommitted());
        assertEquals(4000000L, snapshot.heapMax());
        assertEquals(500000L, snapshot.nonHeapUsed());
    }

    @Test
    @DisplayName("StartupProfiler profileCheckpoint does not throw")
    void profileCheckpoint() {
        StartupProfiler.profileCheckpoint("test_checkpoint");
    }

    @Test
    @DisplayName("StartupProfiler profileReport does not throw")
    void profileReport() {
        StartupProfiler.profileReport();
    }

    @Test
    @DisplayName("StartupProfiler getStartupPerfLogPath returns path")
    void getStartupPerfLogPath() {
        String path = StartupProfiler.getStartupPerfLogPath();

        assertNotNull(path);
        assertTrue(path.contains(".claude"));
    }
}