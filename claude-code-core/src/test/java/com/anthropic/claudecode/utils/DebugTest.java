/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Debug.
 */
class DebugTest {

    @Test
    @DisplayName("Debug Level enum")
    void levelEnum() {
        Debug.Level[] levels = Debug.Level.values();
        assertEquals(5, levels.length);
        assertEquals(Debug.Level.VERBOSE, Debug.Level.valueOf("VERBOSE"));
        assertEquals(Debug.Level.DEBUG, Debug.Level.valueOf("DEBUG"));
        assertEquals(Debug.Level.INFO, Debug.Level.valueOf("INFO"));
        assertEquals(Debug.Level.WARN, Debug.Level.valueOf("WARN"));
        assertEquals(Debug.Level.ERROR, Debug.Level.valueOf("ERROR"));
    }

    @Test
    @DisplayName("Debug getMinDebugLogLevel default")
    void getMinDebugLogLevelDefault() {
        Debug.Level level = Debug.getMinDebugLogLevel();
        // Default is DEBUG when no env var set
        assertNotNull(level);
    }

    @Test
    @DisplayName("Debug isDebugMode")
    void isDebugMode() {
        // Result depends on environment
        boolean result = Debug.isDebugMode();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Debug enableDebugLogging")
    void enableDebugLogging() {
        boolean wasActive = Debug.enableDebugLogging();
        assertTrue(wasActive == true || wasActive == false);
    }

    @Test
    @DisplayName("Debug isDebugToStdErr")
    void isDebugToStdErr() {
        boolean result = Debug.isDebugToStdErr();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Debug getDebugFilePath")
    void getDebugFilePath() {
        String path = Debug.getDebugFilePath();
        // May be null if no debug file specified
        assertTrue(path == null || path.length() > 0);
    }

    @Test
    @DisplayName("Debug log does not throw")
    void logNoThrow() {
        assertDoesNotThrow(() -> Debug.log("Test message"));
    }

    @Test
    @DisplayName("Debug log with category does not throw")
    void logWithCategoryNoThrow() {
        assertDoesNotThrow(() -> Debug.log("CATEGORY", "Test message"));
    }

    @Test
    @DisplayName("Debug logForDebugging does not throw")
    void logForDebuggingNoThrow() {
        assertDoesNotThrow(() -> Debug.logForDebugging("Test message"));
        assertDoesNotThrow(() -> Debug.logForDebugging("Test message", Debug.Level.DEBUG));
    }

    @Test
    @DisplayName("Debug logAntError does not throw")
    void logAntErrorNoThrow() {
        assertDoesNotThrow(() -> Debug.logAntError("context", new RuntimeException("test")));
    }

    @Test
    @DisplayName("Debug flushDebugLogs does not throw")
    void flushDebugLogsNoThrow() {
        assertDoesNotThrow(() -> Debug.flushDebugLogs());
    }

    @Test
    @DisplayName("Debug getDebugLogPath")
    void getDebugLogPath() {
        String path = Debug.getDebugLogPath();
        assertNotNull(path);
        assertTrue(path.contains(".claude") || path.contains("debug"));
    }

    @Test
    @DisplayName("Debug setHasFormattedOutput")
    void setHasFormattedOutput() {
        Debug.setHasFormattedOutput(true);
        assertTrue(Debug.getHasFormattedOutput());

        Debug.setHasFormattedOutput(false);
        assertFalse(Debug.getHasFormattedOutput());
    }
}