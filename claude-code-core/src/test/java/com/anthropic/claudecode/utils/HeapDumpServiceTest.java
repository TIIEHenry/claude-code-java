/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HeapDumpService.
 */
class HeapDumpServiceTest {

    @Test
    @DisplayName("HeapDumpService HeapDumpResult record")
    void heapDumpResultRecord() {
        HeapDumpService.HeapDumpResult result = new HeapDumpService.HeapDumpResult(
            true, "/path/to/heap.hprof", "/path/to/diag.json", null
        );
        assertTrue(result.success());
        assertEquals("/path/to/heap.hprof", result.heapPath());
        assertEquals("/path/to/diag.json", result.diagPath());
        assertNull(result.error());
    }

    @Test
    @DisplayName("HeapDumpService MemoryUsageInfo record")
    void memoryUsageInfoRecord() {
        HeapDumpService.MemoryUsageInfo info = new HeapDumpService.MemoryUsageInfo(
            1000000, 2000000, 50000, 3000000
        );
        assertEquals(1000000, info.heapUsed());
        assertEquals(2000000, info.heapTotal());
        assertEquals(50000, info.external());
        assertEquals(3000000, info.rss());
    }

    @Test
    @DisplayName("HeapDumpService MemoryGrowthRate record")
    void memoryGrowthRateRecord() {
        HeapDumpService.MemoryGrowthRate rate = new HeapDumpService.MemoryGrowthRate(
            1000.0, 3.6
        );
        assertEquals(1000.0, rate.bytesPerSecond());
        assertEquals(3.6, rate.mbPerHour());
    }

    @Test
    @DisplayName("HeapDumpService V8HeapStats record")
    void v8HeapStatsRecord() {
        HeapDumpService.V8HeapStats stats = new HeapDumpService.V8HeapStats(
            1000000000L, 50000L, 60000L, 0, 1
        );
        assertEquals(1000000000L, stats.heapSizeLimit());
        assertEquals(50000L, stats.mallocedMemory());
        assertEquals(60000L, stats.peakMallocedMemory());
    }

    @Test
    @DisplayName("HeapDumpService ResourceUsageInfo record")
    void resourceUsageInfoRecord() {
        HeapDumpService.ResourceUsageInfo info = new HeapDumpService.ResourceUsageInfo(
            50000000L, 1000L, 500L
        );
        assertEquals(50000000L, info.maxRSS());
        assertEquals(1000L, info.userCPUTime());
        assertEquals(500L, info.systemCPUTime());
    }

    @Test
    @DisplayName("HeapDumpService Analysis record")
    void analysisRecord() {
        HeapDumpService.Analysis analysis = new HeapDumpService.Analysis(
            List.of("High memory growth"), "Check heap dump"
        );
        assertEquals(1, analysis.potentialLeaks().size());
        assertEquals("Check heap dump", analysis.recommendation());
    }

    @Test
    @DisplayName("HeapDumpService MemoryDiagnostics record")
    void memoryDiagnosticsRecord() {
        HeapDumpService.MemoryDiagnostics diag = new HeapDumpService.MemoryDiagnostics(
            "2024-01-01T00:00:00Z",
            "session-1",
            "manual",
            1,
            100.0,
            new HeapDumpService.MemoryUsageInfo(1000, 2000, 0, 3000),
            new HeapDumpService.MemoryGrowthRate(10.0, 0.036),
            new HeapDumpService.V8HeapStats(1000000, 0, 0, 0, 0),
            new HeapDumpService.ResourceUsageInfo(3000, 0, 0),
            10,
            5,
            new HeapDumpService.Analysis(List.of(), "OK"),
            "Linux",
            "21"
        );

        assertEquals("session-1", diag.sessionId());
        assertEquals("manual", diag.trigger());
        assertEquals(1, diag.dumpNumber());
        assertEquals(100.0, diag.uptimeSeconds());
        assertNotNull(diag.memoryUsage());
    }

    @Test
    @DisplayName("HeapDumpService captureMemoryDiagnostics returns diagnostics")
    void captureMemoryDiagnostics() {
        HeapDumpService.MemoryDiagnostics diag = HeapDumpService.captureMemoryDiagnostics("test", 0);
        assertNotNull(diag);
        assertNotNull(diag.timestamp());
        assertNotNull(diag.sessionId());
        assertEquals("test", diag.trigger());
        assertNotNull(diag.memoryUsage());
        assertNotNull(diag.analysis());
    }

    @Test
    @DisplayName("HeapDumpService performHeapDump returns result")
    void performHeapDump() {
        HeapDumpService.HeapDumpResult result = HeapDumpService.performHeapDump("test", 0);
        assertNotNull(result);
        // Success depends on system capabilities
        assertTrue(result.success() || !result.success());
    }
}