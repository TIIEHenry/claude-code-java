/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ErrorLogSink.
 */
class ErrorLogSinkTest {

    @Test
    @DisplayName("ErrorLogSink getErrorsPath returns path")
    void getErrorsPath() {
        Path path = ErrorLogSink.getErrorsPath();
        assertNotNull(path);
        assertTrue(path.toString().contains("errors"));
        assertTrue(path.toString().endsWith(".jsonl"));
    }

    @Test
    @DisplayName("ErrorLogSink getMCPLogsPath returns path")
    void getMCPLogsPath() {
        Path path = ErrorLogSink.getMCPLogsPath("test-server");
        assertNotNull(path);
        assertTrue(path.toString().contains("mcp-logs"));
        assertTrue(path.toString().contains("test-server"));
    }

    @Test
    @DisplayName("ErrorLogSink initializeErrorLogSink does not throw")
    void initializeErrorLogSinkNoThrow() {
        assertDoesNotThrow(() -> ErrorLogSink.initializeErrorLogSink());
    }

    @Test
    @DisplayName("ErrorLogSink logError does not throw")
    void logErrorNoThrow() {
        ErrorLogSink.initializeErrorLogSink();
        assertDoesNotThrow(() -> ErrorLogSink.logError(new RuntimeException("test")));
    }

    @Test
    @DisplayName("ErrorLogSink logMCPError does not throw")
    void logMCPErrorNoThrow() {
        assertDoesNotThrow(() -> ErrorLogSink.logMCPError("test-server", new RuntimeException("test")));
    }

    @Test
    @DisplayName("ErrorLogSink logMCPDebug does not throw")
    void logMCPDebugNoThrow() {
        assertDoesNotThrow(() -> ErrorLogSink.logMCPDebug("test-server", "debug message"));
    }

    @Test
    @DisplayName("ErrorLogSink flushLogWriters does not throw")
    void flushLogWritersNoThrow() {
        assertDoesNotThrow(() -> ErrorLogSink.flushLogWriters());
    }

    @Test
    @DisplayName("ErrorLogSink clearLogWriters does not throw")
    void clearLogWritersNoThrow() {
        assertDoesNotThrow(() -> ErrorLogSink.clearLogWriters());
    }
}