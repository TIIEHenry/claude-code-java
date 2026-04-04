/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/log.ts
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Logging utilities for Claude Code.
 */
public final class LogUtils {
    private LogUtils() {}

    // Maximum in-memory errors
    private static final int MAX_IN_MEMORY_ERRORS = 100;

    // In-memory error log
    private static final List<ErrorEntry> inMemoryErrorLog = 
        new CopyOnWriteArrayList<>();

    // Date formatter for ISO format
    private static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     * Error entry record.
     */
    public record ErrorEntry(String error, String timestamp) {}

    /**
     * Log an error.
     */
    public static void logError(Throwable error) {
        if (error == null) return;

        String errorStr = getStackTrace(error);
        String timestamp = Instant.now().toString();

        // Add to in-memory log
        synchronized (inMemoryErrorLog) {
            if (inMemoryErrorLog.size() >= MAX_IN_MEMORY_ERRORS) {
                inMemoryErrorLog.remove(0);
            }
            inMemoryErrorLog.add(new ErrorEntry(errorStr, timestamp));
        }

        // Also log to stderr for debugging
        System.err.println("[" + timestamp + "] ERROR: " + error.getMessage());
        if (error.getCause() != null) {
            System.err.println("  Caused by: " + error.getCause().getMessage());
        }
    }

    /**
     * Log an error with message.
     */
    public static void logError(String message, Throwable error) {
        System.err.println(message);
        logError(error);
    }

    /**
     * Log a debug message.
     */
    public static void logDebug(String message) {
        if (isDebugEnabled()) {
            System.err.println("[DEBUG] " + message);
        }
    }

    /**
     * Log an info message.
     */
    public static void logInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    /**
     * Log a warning message.
     */
    public static void logWarning(String message) {
        System.err.println("[WARN] " + message);
    }

    /**
     * Check if debug mode is enabled.
     */
    public static boolean isDebugEnabled() {
        return "true".equalsIgnoreCase(System.getenv("CLAUDE_DEBUG")) ||
               "1".equals(System.getenv("CLAUDE_DEBUG")) ||
               "true".equalsIgnoreCase(System.getProperty("claude.debug"));
    }

    /**
     * Get in-memory errors.
     */
    public static List<ErrorEntry> getInMemoryErrors() {
        return new ArrayList<>(inMemoryErrorLog);
    }

    /**
     * Clear in-memory errors.
     */
    public static void clearInMemoryErrors() {
        inMemoryErrorLog.clear();
    }

    /**
     * Get stack trace as string.
     */
    public static String getStackTrace(Throwable error) {
        if (error == null) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(error.toString()).append("\n");
        
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        
        if (error.getCause() != null) {
            sb.append("Caused by: ").append(getStackTrace(error.getCause()));
        }
        
        return sb.toString();
    }

    /**
     * Format timestamp for display.
     */
    public static String formatTimestamp(Instant instant) {
        return ISO_FORMATTER.format(instant.atZone(ZoneOffset.UTC));
    }

    /**
     * Format duration for display.
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return minutes + "m " + seconds + "s";
        }
    }

    /**
     * Format file size for display.
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
