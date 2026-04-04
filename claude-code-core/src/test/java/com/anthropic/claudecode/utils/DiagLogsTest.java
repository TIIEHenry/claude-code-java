/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DiagLogs.
 */
class DiagLogsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("DiagLogs Level enum values")
    void levelEnum() {
        assertEquals(4, DiagLogs.Level.values().length);
        assertEquals(DiagLogs.Level.DEBUG, DiagLogs.Level.values()[0]);
        assertEquals(DiagLogs.Level.INFO, DiagLogs.Level.values()[1]);
        assertEquals(DiagLogs.Level.WARN, DiagLogs.Level.values()[2]);
        assertEquals(DiagLogs.Level.ERROR, DiagLogs.Level.values()[3]);
    }

    @Test
    @DisplayName("DiagLogs DiagnosticLogEntry record")
    void diagnosticLogEntryRecord() {
        Map<String, Object> data = Map.of("key", "value");
        DiagLogs.DiagnosticLogEntry entry = new DiagLogs.DiagnosticLogEntry(
            "2024-01-01T00:00:00Z",
            DiagLogs.Level.INFO,
            "test_event",
            data
        );

        assertEquals("2024-01-01T00:00:00Z", entry.timestamp());
        assertEquals(DiagLogs.Level.INFO, entry.level());
        assertEquals("test_event", entry.event());
        assertEquals(data, entry.data());
    }

    @Test
    @DisplayName("DiagLogs logForDiagnosticsNoPII without file")
    void logWithoutFile() {
        // Should not throw when no log file is configured
        assertDoesNotThrow(() -> DiagLogs.logForDiagnosticsNoPII(DiagLogs.Level.INFO, "test"));
    }

    @Test
    @DisplayName("DiagLogs logForDiagnosticsNoPII with data without file")
    void logWithDataWithoutFile() {
        Map<String, Object> data = Map.of("count", 10);
        assertDoesNotThrow(() -> DiagLogs.logForDiagnosticsNoPII(DiagLogs.Level.INFO, "test", data));
    }

    @Test
    @DisplayName("DiagLogs logForDiagnosticsNoPII null data")
    void logNullData() {
        assertDoesNotThrow(() -> DiagLogs.logForDiagnosticsNoPII(DiagLogs.Level.INFO, "test", null));
    }

    @Test
    @DisplayName("DiagLogs withDiagnosticsTiming returns future")
    void withDiagnosticsTimingReturnsFuture() {
        CompletableFuture<String> future = DiagLogs.withDiagnosticsTiming(
            "test_event",
            () -> CompletableFuture.completedFuture("result")
        );

        assertNotNull(future);
        String result = future.join();
        assertEquals("result", result);
    }

    @Test
    @DisplayName("DiagLogs withDiagnosticsTiming with data extractor")
    void withDiagnosticsTimingWithDataExtractor() {
        CompletableFuture<Integer> future = DiagLogs.withDiagnosticsTiming(
            "test_event",
            () -> CompletableFuture.completedFuture(42),
            value -> Map.of("value", value)
        );

        int result = future.join();
        assertEquals(42, result);
    }

    @Test
    @DisplayName("DiagLogs withDiagnosticsTiming handles exception")
    void withDiagnosticsTimingHandlesException() {
        CompletableFuture<String> future = DiagLogs.withDiagnosticsTiming(
            "test_event",
            () -> CompletableFuture.failedFuture(new RuntimeException("test error"))
        );

        assertThrows(Exception.class, () -> future.join());
    }

    @Test
    @DisplayName("DiagLogs withDiagnosticsTiming null data extractor")
    void withDiagnosticsTimingNullDataExtractor() {
        CompletableFuture<String> future = DiagLogs.withDiagnosticsTiming(
            "test_event",
            () -> CompletableFuture.completedFuture("result"),
            null
        );

        assertEquals("result", future.join());
    }
}