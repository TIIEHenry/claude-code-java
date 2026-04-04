/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/logs
 */
package com.anthropic.claudecode.types;

import java.util.*;
import java.time.*;

/**
 * Log types - Logging type definitions.
 */
public final class LogTypes {

    /**
     * Log level enum.
     */
    public enum LogLevel {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        FATAL(5);

        private final int value;

        LogLevel(int value) {
            this.value = value;
        }

        public int getValue() { return value; }

        public boolean shouldLog(LogLevel threshold) {
            return this.value >= threshold.value;
        }
    }

    /**
     * Log entry record.
     */
    public record LogEntry(
        String id,
        LogLevel level,
        String message,
        String logger,
        String category,
        Map<String, Object> context,
        Throwable exception,
        Instant timestamp,
        String threadName,
        String correlationId,
        String sessionId
    ) {
        public static LogEntry of(LogLevel level, String message) {
            return new LogEntry(
                UUID.randomUUID().toString(),
                level,
                message,
                "default",
                "general",
                new HashMap<>(),
                null,
                Instant.now(),
                Thread.currentThread().getName(),
                null,
                null
            );
        }

        public LogEntry withContext(String key, Object value) {
            Map<String, Object> newContext = new HashMap<>(context);
            newContext.put(key, value);
            return new LogEntry(id, level, message, logger, category, newContext, exception, timestamp, threadName, correlationId, sessionId);
        }

        public LogEntry withException(Throwable e) {
            return new LogEntry(id, level, message, logger, category, context, e, timestamp, threadName, correlationId, sessionId);
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timestamp).append("] ");
            sb.append("[").append(level).append("] ");
            sb.append("[").append(logger).append("] ");
            sb.append(message);

            if (exception != null) {
                sb.append("\n  Exception: ").append(exception.getMessage());
            }

            return sb.toString();
        }
    }

    /**
     * Log configuration record.
     */
    public record LogConfig(
        LogLevel threshold,
        List<LogAppender> appenders,
        Map<String, LogLevel> loggerLevels,
        boolean includeStackTrace,
        boolean includeContext,
        int maxEntries
    ) {
        public static LogConfig defaults() {
            return new LogConfig(
                LogLevel.INFO,
                List.of(new ConsoleAppender()),
                new HashMap<>(),
                true,
                true,
                10000
            );
        }
    }

    /**
     * Log appender interface.
     */
    public interface LogAppender {
        void append(LogEntry entry);
        void flush();
        String getName();
    }

    /**
     * Console appender.
     */
    public static final class ConsoleAppender implements LogAppender {
        @Override
        public void append(LogEntry entry) {
            System.out.println(entry.format());
        }

        @Override
        public void flush() {}

        @Override
        public String getName() { return "console"; }
    }

    /**
     * File appender.
     */
    public static final class FileAppender implements LogAppender {
        private final String filePath;

        public FileAppender(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void append(LogEntry entry) {
            // Would write to file
        }

        @Override
        public void flush() {}

        @Override
        public String getName() { return "file"; }
    }

    /**
     * Log aggregation record.
     */
    public record LogAggregation(
        Instant startTime,
        Instant endTime,
        Map<LogLevel, Integer> countByLevel,
        Map<String, Integer> countByCategory,
        int totalEntries,
        List<LogEntry> errors
    ) {
        public static LogAggregation empty() {
            return new LogAggregation(
                Instant.now(),
                Instant.now(),
                new HashMap<>(),
                new HashMap<>(),
                0,
                new ArrayList<>()
            );
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("Log Summary (").append(startTime).append(" - ").append(endTime).append(")\n");
            sb.append("Total: ").append(totalEntries).append("\n");
            sb.append("By Level:\n");
            countByLevel.forEach((level, count) ->
                sb.append("  ").append(level).append(": ").append(count).append("\n")
            );
            return sb.toString();
        }
    }

    /**
     * Log filter record.
     */
    public record LogFilter(
        LogLevel minLevel,
        LogLevel maxLevel,
        List<String> categories,
        List<String> loggers,
        String searchText,
        Instant startTime,
        Instant endTime,
        String correlationId
    ) {
        public static LogFilter all() {
            return new LogFilter(null, null, null, null, null, null, null, null);
        }

        public static LogFilter errorsOnly() {
            return new LogFilter(LogLevel.ERROR, null, null, null, null, null, null, null);
        }

        public boolean matches(LogEntry entry) {
            if (minLevel != null && entry.level().getValue() < minLevel.getValue()) return false;
            if (maxLevel != null && entry.level().getValue() > maxLevel.getValue()) return false;
            if (categories != null && !categories.contains(entry.category())) return false;
            if (loggers != null && !loggers.contains(entry.logger())) return false;
            if (searchText != null && !entry.message().contains(searchText)) return false;
            if (startTime != null && entry.timestamp().isBefore(startTime)) return false;
            if (endTime != null && entry.timestamp().isAfter(endTime)) return false;
            if (correlationId != null && !correlationId.equals(entry.correlationId())) return false;
            return true;
        }
    }
}