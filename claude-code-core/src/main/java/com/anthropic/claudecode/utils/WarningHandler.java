/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code warning handler utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Warning handling utilities.
 *
 * In TypeScript/Node.js, this handles process warnings. In Java, warnings
 * are typically logged through a logging framework. This utility provides
 * similar functionality for tracking and suppressing warnings.
 */
public final class WarningHandler {
    private WarningHandler() {}

    /**
     * Maximum number of unique warning keys to track.
     */
    public static final int MAX_WARNING_KEYS = 1000;

    private static final ConcurrentHashMap<String, Integer> warningCounts = new ConcurrentHashMap<>();

    /**
     * Warning metadata.
     */
    public record WarningInfo(
            String key,
            String classname,
            String message,
            int occurrenceCount,
            boolean isInternal
    ) {}

    /**
     * Internal warning patterns that should be suppressed.
     */
    private static final List<String> INTERNAL_WARNING_PATTERNS = List.of(
            "MaxListenersExceededWarning: AbortSignal",
            "MaxListenersExceededWarning: EventTarget"
    );

    /**
     * Check if a warning is internal (should be suppressed).
     */
    public static boolean isInternalWarning(String warningClass, String message) {
        String warningStr = warningClass + ": " + message;
        for (String pattern : INTERNAL_WARNING_PATTERNS) {
            if (warningStr.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Record a warning and increment its count.
     */
    public static WarningInfo recordWarning(String classname, String message) {
        String key = classname + ": " + (message.length() > 50 ? message.substring(0, 50) : message);
        int count = warningCounts.getOrDefault(key, 0) + 1;

        // Bound the map to prevent unbounded memory growth
        if (warningCounts.containsKey(key) || warningCounts.size() < MAX_WARNING_KEYS) {
            warningCounts.put(key, count);
        }

        boolean isInternal = isInternalWarning(classname, message);

        return new WarningInfo(key, classname, message, count, isInternal);
    }

    /**
     * Get the count for a specific warning.
     */
    public static int getWarningCount(String key) {
        return warningCounts.getOrDefault(key, 0);
    }

    /**
     * Get all recorded warnings.
     */
    public static Map<String, Integer> getAllWarningCounts() {
        return new HashMap<>(warningCounts);
    }

    /**
     * Clear all warning counts.
     */
    public static void clearWarningCounts() {
        warningCounts.clear();
    }

    /**
     * Log a warning if debug mode is enabled.
     */
    public static void logWarning(WarningInfo warning, boolean debugMode) {
        if (debugMode) {
            String prefix = warning.isInternal() ? "[Internal Warning]" : "[Warning]";
            Debug.log(prefix + " " + warning.key, "warn");
        }
    }

    /**
     * Handle a warning event.
     */
    public static void handleWarning(String classname, String message, boolean debugMode) {
        WarningInfo info = recordWarning(classname, message);
        logWarning(info, debugMode);
    }

    /**
     * Get total number of warnings.
     */
    public static int getTotalWarnings() {
        return warningCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Get number of unique warnings.
     */
    public static int getUniqueWarnings() {
        return warningCounts.size();
    }

    /**
     * Check if we've seen a warning before.
     */
    public static boolean hasWarning(String key) {
        return warningCounts.containsKey(key);
    }

    /**
     * Suppress internal warnings by not logging them.
     */
    public static boolean shouldSuppress(String classname, String message) {
        return isInternalWarning(classname, message);
    }
}