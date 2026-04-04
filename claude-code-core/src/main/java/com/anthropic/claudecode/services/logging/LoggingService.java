/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/logging
 */
package com.anthropic.claudecode.services.logging;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;
import java.io.*;

/**
 * Logging service - Centralized logging system.
 */
public final class LoggingService {
    private static volatile LogLevel threshold = LogLevel.INFO;
    private static volatile Path logPath = null;
    private static volatile boolean fileLoggingEnabled = true;
    private static final List<LogHandler> handlers = new CopyOnWriteArrayList<>();
    private static final CircularBuffer<LogEntry> recentLogs = new CircularBuffer<>(1000);

    /**
     * Log level enum.
     */
    public enum LogLevel {
        DEBUG(0),
        INFO(1),
        WARNING(2),
        ERROR(3),
        CRITICAL(4);

        private final int value;

        LogLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public boolean shouldLog(LogLevel other) {
            return other.value >= this.value;
        }
    }

    /**
     * Log entry record.
     */
    public record LogEntry(
        LogLevel level,
        String message,
        String category,
        Throwable exception,
        Map<String, Object> context,
        Instant timestamp,
        String threadName
    ) {
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timestamp).append("] ");
            sb.append("[").append(level).append("] ");
            sb.append("[").append(category).append("] ");
            sb.append(message);

            if (context != null && !context.isEmpty()) {
                sb.append(" | ");
                context.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
            }

            if (exception != null) {
                sb.append("\nException: ").append(exception.getMessage());
            }

            return sb.toString();
        }
    }

    /**
     * Log handler interface.
     */
    public interface LogHandler {
        void handle(LogEntry entry);
    }

    /**
     * Log debug.
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message, null, null, null);
    }

    /**
     * Log debug with category.
     */
    public static void debug(String category, String message) {
        log(LogLevel.DEBUG, message, category, null, null);
    }

    /**
     * Log info.
     */
    public static void info(String message) {
        log(LogLevel.INFO, message, null, null, null);
    }

    /**
     * Log info with category.
     */
    public static void info(String category, String message) {
        log(LogLevel.INFO, message, category, null, null);
    }

    /**
     * Log warning.
     */
    public static void warning(String message) {
        log(LogLevel.WARNING, message, null, null, null);
    }

    /**
     * Log warning with category.
     */
    public static void warning(String category, String message) {
        log(LogLevel.WARNING, message, category, null, null);
    }

    /**
     * Log error.
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message, null, null, null);
    }

    /**
     * Log error with exception.
     */
    public static void error(String message, Throwable exception) {
        log(LogLevel.ERROR, message, null, exception, null);
    }

    /**
     * Log error with category.
     */
    public static void error(String category, String message, Throwable exception) {
        log(LogLevel.ERROR, message, category, exception, null);
    }

    /**
     * Log critical.
     */
    public static void critical(String message) {
        log(LogLevel.CRITICAL, message, null, null, null);
    }

    /**
     * Log critical with exception.
     */
    public static void critical(String message, Throwable exception) {
        log(LogLevel.CRITICAL, message, null, exception, null);
    }

    /**
     * Core log method.
     */
    public static void log(
        LogLevel level,
        String message,
        String category,
        Throwable exception,
        Map<String, Object> context
    ) {
        if (!threshold.shouldLog(level)) {
            return;
        }

        LogEntry entry = new LogEntry(
            level,
            message,
            category != null ? category : "default",
            exception,
            context,
            Instant.now(),
            Thread.currentThread().getName()
        );

        // Add to recent logs
        recentLogs.add(entry);

        // Notify handlers
        for (LogHandler handler : handlers) {
            handler.handle(entry);
        }

        // Write to file
        if (fileLoggingEnabled && logPath != null) {
            writeToFile(entry);
        }
    }

    /**
     * Write to file.
     */
    private static void writeToFile(LogEntry entry) {
        try {
            Files.writeString(
                logPath,
                entry.format() + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            // Avoid recursive logging
        }
    }

    /**
     * Set threshold.
     */
    public static void setThreshold(LogLevel level) {
        threshold = level;
    }

    /**
     * Set log path.
     */
    public static void setLogPath(Path path) {
        logPath = path;
    }

    /**
     * Enable/disable file logging.
     */
    public static void setFileLoggingEnabled(boolean enabled) {
        fileLoggingEnabled = enabled;
    }

    /**
     * Add handler.
     */
    public static void addHandler(LogHandler handler) {
        handlers.add(handler);
    }

    /**
     * Remove handler.
     */
    public static void removeHandler(LogHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Get recent logs.
     */
    public static List<LogEntry> getRecentLogs() {
        return recentLogs.toList();
    }

    /**
     * Get recent logs by level.
     */
    public static List<LogEntry> getRecentLogs(LogLevel level) {
        return recentLogs.toList()
            .stream()
            .filter(e -> e.level() == level)
            .toList();
    }

    /**
     * Clear recent logs.
     */
    public static void clearRecentLogs() {
        recentLogs.clear();
    }

    /**
     * Circular buffer for recent logs.
     */
    private static class CircularBuffer<T> {
        private final T[] buffer;
        private int head = 0;
        private int size = 0;

        CircularBuffer(int capacity) {
            this.buffer = (T[]) new Object[capacity];
        }

        void add(T item) {
            buffer[head] = item;
            head = (head + 1) % buffer.length;
            if (size < buffer.length) size++;
        }

        List<T> toList() {
            List<T> result = new ArrayList<>();
            int start = head - size;
            if (start < 0) start += buffer.length;

            for (int i = 0; i < size; i++) {
                int idx = (start + i) % buffer.length;
                if (buffer[idx] != null) {
                    result.add(buffer[idx]);
                }
            }

            return result;
        }

        void clear() {
            head = 0;
            size = 0;
        }
    }
}