/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/internalLogging
 */
package com.anthropic.claudecode.services.internal;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
import java.nio.file.*;

/**
 * Internal logging service - Internal system logging.
 */
public final class InternalLoggingService {
    private static volatile InternalLoggingService instance;
    private final Path logPath;
    private final List<InternalLogEntry> entries = new ArrayList<>();
    private final List<InternalLogListener> listeners = new CopyOnWriteArrayList<>();
    private volatile LogLevel threshold = LogLevel.INFO;
    private volatile boolean enabled = true;

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

        public boolean shouldLog(LogLevel other) {
            return other.value >= this.value;
        }
    }

    /**
     * Log entry record.
     */
    public record InternalLogEntry(
        String id,
        LogLevel level,
        String category,
        String message,
        String source,
        Map<String, Object> context,
        Throwable exception,
        Instant timestamp,
        String threadName
    ) {
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timestamp).append("] ");
            sb.append("[").append(level).append("] ");
            sb.append("[").append(category).append("] ");
            sb.append("[").append(source).append("] ");
            sb.append(message);

            if (context != null && !context.isEmpty()) {
                sb.append(" | ");
                context.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
            }

            if (exception != null) {
                sb.append("\n  Exception: ").append(exception.getMessage());
            }

            return sb.toString();
        }
    }

    /**
     * Get singleton instance.
     */
    public static InternalLoggingService getInstance() {
        if (instance == null) {
            Path path = Paths.get(System.getProperty("user.home"), ".claude", "logs", "internal.log");
            instance = new InternalLoggingService(path);
        }
        return instance;
    }

    /**
     * Create internal logging service.
     */
    private InternalLoggingService(Path logPath) {
        this.logPath = logPath;
        try {
            Files.createDirectories(logPath.getParent());
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Log trace.
     */
    public void trace(String category, String message) {
        log(LogLevel.TRACE, category, message, null, null);
    }

    /**
     * Log debug.
     */
    public void debug(String category, String message) {
        log(LogLevel.DEBUG, category, message, null, null);
    }

    /**
     * Log info.
     */
    public void info(String category, String message) {
        log(LogLevel.INFO, category, message, null, null);
    }

    /**
     * Log warn.
     */
    public void warn(String category, String message) {
        log(LogLevel.WARN, category, message, null, null);
    }

    /**
     * Log error.
     */
    public void error(String category, String message, Throwable exception) {
        log(LogLevel.ERROR, category, message, exception, null);
    }

    /**
     * Log fatal.
     */
    public void fatal(String category, String message, Throwable exception) {
        log(LogLevel.FATAL, category, message, exception, null);
    }

    /**
     * Core log method.
     */
    public void log(LogLevel level, String category, String message, Throwable exception, Map<String, Object> context) {
        if (!enabled || !threshold.shouldLog(level)) {
            return;
        }

        InternalLogEntry entry = new InternalLogEntry(
            UUID.randomUUID().toString(),
            level,
            category,
            message,
            getCallerInfo(),
            context != null ? context : new HashMap<>(),
            exception,
            Instant.now(),
            Thread.currentThread().getName()
        );

        entries.add(entry);
        writeToFile(entry);
        notifyListeners(entry);
    }

    /**
     * Get caller info.
     */
    private String getCallerInfo() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (stack.length > 4) {
            StackTraceElement element = stack[4];
            return element.getClassName() + "." + element.getMethodName();
        }
        return "unknown";
    }

    /**
     * Write to file.
     */
    private void writeToFile(InternalLogEntry entry) {
        try {
            Files.writeString(
                logPath,
                entry.format() + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Set threshold.
     */
    public void setThreshold(LogLevel level) {
        this.threshold = level;
    }

    /**
     * Set enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get entries.
     */
    public List<InternalLogEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Get entries by level.
     */
    public List<InternalLogEntry> getEntriesByLevel(LogLevel level) {
        return entries.stream()
            .filter(e -> e.level() == level)
            .toList();
    }

    /**
     * Get entries by category.
     */
    public List<InternalLogEntry> getEntriesByCategory(String category) {
        return entries.stream()
            .filter(e -> e.category().equals(category))
            .toList();
    }

    /**
     * Clear entries.
     */
    public void clearEntries() {
        entries.clear();
    }

    /**
     * Add listener.
     */
    public void addListener(InternalLogListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(InternalLogListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(InternalLogEntry entry) {
        for (InternalLogListener listener : listeners) {
            listener.onLog(entry);
        }
    }

    /**
     * Log listener interface.
     */
    public interface InternalLogListener {
        void onLog(InternalLogEntry entry);
    }
}