/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LogUtils.
 */
class LogUtilsTest {

    @BeforeEach
    void setUp() {
        LogUtils.clearInMemoryErrors();
    }

    @Test
    @DisplayName("LogUtils ErrorEntry record")
    void errorEntryRecord() {
        LogUtils.ErrorEntry entry = new LogUtils.ErrorEntry("Test error", "2024-01-01T00:00:00Z");
        assertEquals("Test error", entry.error());
        assertEquals("2024-01-01T00:00:00Z", entry.timestamp());
    }

    @Test
    @DisplayName("LogUtils logError adds to in-memory log")
    void logErrorAddsToLog() {
        Exception error = new RuntimeException("Test error");
        LogUtils.logError(error);

        List<LogUtils.ErrorEntry> errors = LogUtils.getInMemoryErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).error().contains("RuntimeException"));
        assertNotNull(errors.get(0).timestamp());
    }

    @Test
    @DisplayName("LogUtils logError null does nothing")
    void logErrorNull() {
        LogUtils.logError(null);
        List<LogUtils.ErrorEntry> errors = LogUtils.getInMemoryErrors();
        assertEquals(0, errors.size());
    }

    @Test
    @DisplayName("LogUtils logError with message")
    void logErrorWithMessage() {
        Exception error = new RuntimeException("Test error");
        LogUtils.logError("Custom message", error);

        List<LogUtils.ErrorEntry> errors = LogUtils.getInMemoryErrors();
        assertEquals(1, errors.size());
    }

    @Test
    @DisplayName("LogUtils clearInMemoryErrors clears log")
    void clearInMemoryErrors() {
        LogUtils.logError(new RuntimeException("Test"));
        LogUtils.clearInMemoryErrors();
        assertEquals(0, LogUtils.getInMemoryErrors().size());
    }

    @Test
    @DisplayName("LogUtils getStackTrace returns string")
    void getStackTrace() {
        Exception error = new RuntimeException("Test");
        String trace = LogUtils.getStackTrace(error);
        assertNotNull(trace);
        assertTrue(trace.contains("RuntimeException"));
        assertTrue(trace.contains("Test"));
    }

    @Test
    @DisplayName("LogUtils getStackTrace null returns empty")
    void getStackTraceNull() {
        String trace = LogUtils.getStackTrace(null);
        assertEquals("", trace);
    }

    @Test
    @DisplayName("LogUtils getStackTrace with cause")
    void getStackTraceWithCause() {
        Exception cause = new IllegalArgumentException("Cause");
        Exception error = new RuntimeException("Wrapper", cause);
        String trace = LogUtils.getStackTrace(error);
        assertTrue(trace.contains("RuntimeException"));
        assertTrue(trace.contains("Caused by"));
        assertTrue(trace.contains("IllegalArgumentException"));
    }

    @Test
    @DisplayName("LogUtils formatDuration milliseconds")
    void formatDurationMillis() {
        assertEquals("500ms", LogUtils.formatDuration(500));
        assertEquals("999ms", LogUtils.formatDuration(999));
    }

    @Test
    @DisplayName("LogUtils formatDuration seconds")
    void formatDurationSeconds() {
        String result = LogUtils.formatDuration(5000);
        assertTrue(result.contains("s"));
    }

    @Test
    @DisplayName("LogUtils formatDuration minutes")
    void formatDurationMinutes() {
        String result = LogUtils.formatDuration(125000); // 2m 5s
        assertTrue(result.contains("m"));
        assertTrue(result.contains("s"));
    }

    @Test
    @DisplayName("LogUtils formatSize bytes")
    void formatSizeBytes() {
        assertEquals("100 B", LogUtils.formatSize(100));
        assertEquals("500 B", LogUtils.formatSize(500));
    }

    @Test
    @DisplayName("LogUtils formatSize kilobytes")
    void formatSizeKilobytes() {
        String result = LogUtils.formatSize(1024);
        assertTrue(result.contains("KB"));
    }

    @Test
    @DisplayName("LogUtils formatSize megabytes")
    void formatSizeMegabytes() {
        String result = LogUtils.formatSize(1024 * 1024);
        assertTrue(result.contains("MB"));
    }

    @Test
    @DisplayName("LogUtils formatSize gigabytes")
    void formatSizeGigabytes() {
        String result = LogUtils.formatSize(1024L * 1024 * 1024);
        assertTrue(result.contains("GB"));
    }

    @Test
    @DisplayName("LogUtils formatTimestamp returns ISO format")
    void formatTimestamp() {
        java.time.Instant instant = java.time.Instant.parse("2024-01-01T00:00:00Z");
        String formatted = LogUtils.formatTimestamp(instant);
        assertNotNull(formatted);
        assertTrue(formatted.contains("2024"));
    }

    @Test
    @DisplayName("LogUtils isDebugEnabled default false")
    void isDebugEnabledDefault() {
        // Without setting env var, should be false
        assertFalse(LogUtils.isDebugEnabled());
    }
}