/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LogTypes.
 */
class LogTypesTest {

    @Test
    @DisplayName("LogTypes LogLevel enum values")
    void logLevelEnum() {
        LogTypes.LogLevel[] levels = LogTypes.LogLevel.values();
        assertEquals(6, levels.length);
    }

    @Test
    @DisplayName("LogTypes LogLevel getValue")
    void logLevelGetValue() {
        assertEquals(0, LogTypes.LogLevel.TRACE.getValue());
        assertEquals(1, LogTypes.LogLevel.DEBUG.getValue());
        assertEquals(2, LogTypes.LogLevel.INFO.getValue());
        assertEquals(3, LogTypes.LogLevel.WARN.getValue());
        assertEquals(4, LogTypes.LogLevel.ERROR.getValue());
        assertEquals(5, LogTypes.LogLevel.FATAL.getValue());
    }

    @Test
    @DisplayName("LogTypes LogLevel shouldLog")
    void logLevelShouldLog() {
        assertTrue(LogTypes.LogLevel.ERROR.shouldLog(LogTypes.LogLevel.INFO));
        assertTrue(LogTypes.LogLevel.FATAL.shouldLog(LogTypes.LogLevel.DEBUG));
        assertFalse(LogTypes.LogLevel.DEBUG.shouldLog(LogTypes.LogLevel.ERROR));
        assertFalse(LogTypes.LogLevel.TRACE.shouldLog(LogTypes.LogLevel.WARN));
    }

    @Test
    @DisplayName("LogTypes LogEntry of factory")
    void logEntryOf() {
        LogTypes.LogEntry entry = LogTypes.LogEntry.of(LogTypes.LogLevel.INFO, "Test message");
        assertNotNull(entry.id());
        assertEquals(LogTypes.LogLevel.INFO, entry.level());
        assertEquals("Test message", entry.message());
        assertNotNull(entry.timestamp());
    }

    @Test
    @DisplayName("LogTypes LogEntry withContext")
    void logEntryWithContext() {
        LogTypes.LogEntry entry = LogTypes.LogEntry.of(LogTypes.LogLevel.INFO, "Test")
            .withContext("key", "value");

        assertEquals("value", entry.context().get("key"));
    }

    @Test
    @DisplayName("LogTypes LogEntry withException")
    void logEntryWithException() {
        Exception e = new RuntimeException("Test error");
        LogTypes.LogEntry entry = LogTypes.LogEntry.of(LogTypes.LogLevel.ERROR, "Error")
            .withException(e);

        assertEquals(e, entry.exception());
    }

    @Test
    @DisplayName("LogTypes LogEntry format")
    void logEntryFormat() {
        LogTypes.LogEntry entry = LogTypes.LogEntry.of(LogTypes.LogLevel.INFO, "Test message");
        String formatted = entry.format();

        assertTrue(formatted.contains("INFO"));
        assertTrue(formatted.contains("Test message"));
        assertTrue(formatted.contains("["));
    }

    @Test
    @DisplayName("LogTypes LogEntry format with exception")
    void logEntryFormatWithException() {
        Exception e = new RuntimeException("Test error");
        LogTypes.LogEntry entry = LogTypes.LogEntry.of(LogTypes.LogLevel.ERROR, "Error")
            .withException(e);
        String formatted = entry.format();

        assertTrue(formatted.contains("Exception"));
        assertTrue(formatted.contains("Test error"));
    }

    @Test
    @DisplayName("LogTypes LogConfig defaults")
    void logConfigDefaults() {
        LogTypes.LogConfig config = LogTypes.LogConfig.defaults();

        assertEquals(LogTypes.LogLevel.INFO, config.threshold());
        assertEquals(1, config.appenders().size());
        assertTrue(config.includeStackTrace());
        assertEquals(10000, config.maxEntries());
    }

    @Test
    @DisplayName("LogTypes ConsoleAppender")
    void consoleAppender() {
        LogTypes.ConsoleAppender appender = new LogTypes.ConsoleAppender();
        assertEquals("console", appender.getName());

        // Should not throw
        LogTypes.LogEntry entry = LogTypes.LogEntry.of(LogTypes.LogLevel.INFO, "Test");
        appender.append(entry);
        appender.flush();
    }

    @Test
    @DisplayName("LogTypes FileAppender")
    void fileAppender() {
        LogTypes.FileAppender appender = new LogTypes.FileAppender("/tmp/test.log");
        assertEquals("file", appender.getName());

        // Should not throw
        LogTypes.LogEntry entry = LogTypes.LogEntry.of(LogTypes.LogLevel.INFO, "Test");
        appender.append(entry);
        appender.flush();
    }

    @Test
    @DisplayName("LogTypes LogAggregation empty")
    void logAggregationEmpty() {
        LogTypes.LogAggregation agg = LogTypes.LogAggregation.empty();

        assertEquals(0, agg.totalEntries());
        assertTrue(agg.countByLevel().isEmpty());
        assertTrue(agg.errors().isEmpty());
    }

    @Test
    @DisplayName("LogTypes LogAggregation format")
    void logAggregationFormat() {
        LogTypes.LogAggregation agg = LogTypes.LogAggregation.empty();
        String formatted = agg.format();

        assertTrue(formatted.contains("Total"));
        assertTrue(formatted.contains("0"));
    }

    @Test
    @DisplayName("LogTypes LogFilter all")
    void logFilterAll() {
        LogTypes.LogFilter filter = LogTypes.LogFilter.all();
        assertNull(filter.minLevel());
        assertNull(filter.maxLevel());
    }

    @Test
    @DisplayName("LogTypes LogFilter errorsOnly")
    void logFilterErrorsOnly() {
        LogTypes.LogFilter filter = LogTypes.LogFilter.errorsOnly();
        assertEquals(LogTypes.LogLevel.ERROR, filter.minLevel());
    }

    @Test
    @DisplayName("LogTypes LogFilter matches by level")
    void logFilterMatchesByLevel() {
        LogTypes.LogFilter filter = new LogTypes.LogFilter(
            LogTypes.LogLevel.WARN, null, null, null, null, null, null, null
        );

        LogTypes.LogEntry errorEntry = LogTypes.LogEntry.of(LogTypes.LogLevel.ERROR, "error");
        LogTypes.LogEntry infoEntry = LogTypes.LogEntry.of(LogTypes.LogLevel.INFO, "info");

        assertTrue(filter.matches(errorEntry));
        assertFalse(filter.matches(infoEntry));
    }

    @Test
    @DisplayName("LogTypes LogFilter matches by search text")
    void logFilterMatchesBySearchText() {
        LogTypes.LogFilter filter = new LogTypes.LogFilter(
            null, null, null, null, "found", null, null, null
        );

        LogTypes.LogEntry matchingEntry = LogTypes.LogEntry.of(LogTypes.LogLevel.INFO, "This was found");
        LogTypes.LogEntry nonMatchingEntry = LogTypes.LogEntry.of(LogTypes.LogLevel.INFO, "Not here");

        assertTrue(filter.matches(matchingEntry));
        assertFalse(filter.matches(nonMatchingEntry));
    }

    @Test
    @DisplayName("LogTypes LogFilter matches by category")
    void logFilterMatchesByCategory() {
        LogTypes.LogFilter filter = new LogTypes.LogFilter(
            null, null, List.of("auth", "api"), null, null, null, null, null
        );

        LogTypes.LogEntry authEntry = new LogTypes.LogEntry(
            "id", LogTypes.LogLevel.INFO, "msg", "logger", "auth",
            Map.of(), null, Instant.now(), "main", null, null
        );
        LogTypes.LogEntry otherEntry = new LogTypes.LogEntry(
            "id", LogTypes.LogLevel.INFO, "msg", "logger", "other",
            Map.of(), null, Instant.now(), "main", null, null
        );

        assertTrue(filter.matches(authEntry));
        assertFalse(filter.matches(otherEntry));
    }
}