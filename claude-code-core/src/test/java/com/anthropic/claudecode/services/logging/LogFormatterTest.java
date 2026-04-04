/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LogFormatter.
 */
class LogFormatterTest {

    @Test
    @DisplayName("LogFormatter FormatStyle enum values")
    void formatStyleEnum() {
        LogFormatter.FormatStyle[] styles = LogFormatter.FormatStyle.values();
        assertEquals(4, styles.length);
        assertEquals(LogFormatter.FormatStyle.SIMPLE, LogFormatter.FormatStyle.valueOf("SIMPLE"));
        assertEquals(LogFormatter.FormatStyle.DETAILED, LogFormatter.FormatStyle.valueOf("DETAILED"));
        assertEquals(LogFormatter.FormatStyle.JSON, LogFormatter.FormatStyle.valueOf("JSON"));
        assertEquals(LogFormatter.FormatStyle.ANSI, LogFormatter.FormatStyle.valueOf("ANSI"));
    }

    @Test
    @DisplayName("LogFormatter simple factory")
    void simpleFactory() {
        LogFormatter formatter = LogFormatter.simple();
        assertNotNull(formatter);
    }

    @Test
    @DisplayName("LogFormatter detailed factory")
    void detailedFactory() {
        LogFormatter formatter = LogFormatter.detailed();
        assertNotNull(formatter);
    }

    @Test
    @DisplayName("LogFormatter json factory")
    void jsonFactory() {
        LogFormatter formatter = LogFormatter.json();
        assertNotNull(formatter);
    }

    @Test
    @DisplayName("LogFormatter ansi factory")
    void ansiFactory() {
        LogFormatter formatter = LogFormatter.ansi();
        assertNotNull(formatter);
    }

    @Test
    @DisplayName("LogFormatter formatSimple formats correctly")
    void formatSimple() {
        LogFormatter formatter = LogFormatter.simple();
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.INFO,
            "Test message",
            "test",
            null,
            null,
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("[INFO]"));
        assertTrue(result.contains("Test message"));
    }

    @Test
    @DisplayName("LogFormatter formatDetailed includes timestamp and category")
    void formatDetailed() {
        LogFormatter formatter = LogFormatter.detailed();
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.WARNING,
            "Warning message",
            "test-category",
            null,
            null,
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("[WARNING]"));
        assertTrue(result.contains("[test-category]"));
        assertTrue(result.contains("Warning message"));
    }

    @Test
    @DisplayName("LogFormatter formatDetailed includes context")
    void formatDetailedWithContext() {
        LogFormatter formatter = LogFormatter.detailed();
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.INFO,
            "Message with context",
            "test",
            null,
            Map.of("key1", "value1", "key2", "value2"),
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("Context:"));
        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("key2=value2"));
    }

    @Test
    @DisplayName("LogFormatter formatDetailed includes exception")
    void formatDetailedWithException() {
        LogFormatter formatter = LogFormatter.detailed();
        RuntimeException ex = new RuntimeException("Test exception");
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.ERROR,
            "Error occurred",
            "test",
            ex,
            null,
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("Exception:"));
        assertTrue(result.contains("RuntimeException"));
        assertTrue(result.contains("Test exception"));
    }

    @Test
    @DisplayName("LogFormatter formatJson produces valid JSON structure")
    void formatJson() {
        LogFormatter formatter = LogFormatter.json();
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.INFO,
            "JSON test",
            "json-category",
            null,
            null,
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("\"level\":\"INFO\""));
        assertTrue(result.contains("\"message\":\"JSON test\""));
        assertTrue(result.contains("\"category\":\"json-category\""));
    }

    @Test
    @DisplayName("LogFormatter formatJson escapes special characters")
    void formatJsonEscapes() {
        LogFormatter formatter = LogFormatter.json();
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.INFO,
            "Message with \"quotes\" and \n newlines",
            "test",
            null,
            null,
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("\\\""));
        assertTrue(result.contains("\\n"));
    }

    @Test
    @DisplayName("LogFormatter formatJson includes context")
    void formatJsonWithContext() {
        LogFormatter formatter = LogFormatter.json();
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.DEBUG,
            "Debug message",
            "debug",
            null,
            Map.of("user", "testuser"),
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("\"context\":{"));
        assertTrue(result.contains("\"user\":\"testuser\""));
    }

    @Test
    @DisplayName("LogFormatter formatJson includes exception")
    void formatJsonWithException() {
        LogFormatter formatter = LogFormatter.json();
        RuntimeException ex = new RuntimeException("JSON exception");
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.ERROR,
            "Error in JSON",
            "test",
            ex,
            null,
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("\"exception\":{"));
        assertTrue(result.contains("\"type\":\"java.lang.RuntimeException\""));
        assertTrue(result.contains("\"message\":\"JSON exception\""));
    }

    @Test
    @DisplayName("LogFormatter formatAnsi includes ANSI codes")
    void formatAnsi() {
        LogFormatter formatter = LogFormatter.ansi();
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.INFO,
            "ANSI message",
            "ansi",
            null,
            null,
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("\033["));
        assertTrue(result.contains("[INFO]"));
        assertTrue(result.contains("ANSI message"));
    }

    @Test
    @DisplayName("LogFormatter formatAnsi uses correct colors for levels")
    void formatAnsiColors() {
        LogFormatter formatter = LogFormatter.ansi();

        // DEBUG - Cyan
        LoggingService.LogEntry debugEntry = new LoggingService.LogEntry(
            LoggingService.LogLevel.DEBUG, "debug", "test", null, null, Instant.now(), "main"
        );
        assertTrue(formatter.format(debugEntry).contains("\033[36m"));

        // INFO - Green
        LoggingService.LogEntry infoEntry = new LoggingService.LogEntry(
            LoggingService.LogLevel.INFO, "info", "test", null, null, Instant.now(), "main"
        );
        assertTrue(formatter.format(infoEntry).contains("\033[32m"));

        // WARNING - Yellow
        LoggingService.LogEntry warningEntry = new LoggingService.LogEntry(
            LoggingService.LogLevel.WARNING, "warning", "test", null, null, Instant.now(), "main"
        );
        assertTrue(formatter.format(warningEntry).contains("\033[33m"));

        // ERROR - Red
        LoggingService.LogEntry errorEntry = new LoggingService.LogEntry(
            LoggingService.LogLevel.ERROR, "error", "test", null, null, Instant.now(), "main"
        );
        assertTrue(formatter.format(errorEntry).contains("\033[31m"));

        // CRITICAL - Magenta
        LoggingService.LogEntry criticalEntry = new LoggingService.LogEntry(
            LoggingService.LogLevel.CRITICAL, "critical", "test", null, null, Instant.now(), "main"
        );
        assertTrue(formatter.format(criticalEntry).contains("\033[35m"));
    }

    @Test
    @DisplayName("LogFormatter formatAnsi includes exception in red")
    void formatAnsiWithException() {
        LogFormatter formatter = LogFormatter.ansi();
        RuntimeException ex = new RuntimeException("ANSI exception");
        LoggingService.LogEntry entry = new LoggingService.LogEntry(
            LoggingService.LogLevel.ERROR,
            "Error",
            "test",
            ex,
            null,
            Instant.now(),
            "main"
        );

        String result = formatter.format(entry);

        assertTrue(result.contains("\033[31m"));
        assertTrue(result.contains("ANSI exception"));
    }
}