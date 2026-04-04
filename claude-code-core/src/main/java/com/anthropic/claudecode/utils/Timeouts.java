/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code timeout utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Constants and utilities for timeout values.
 */
public final class Timeouts {
    private Timeouts() {}

    /**
     * Default timeout for bash operations (2 minutes).
     */
    public static final int DEFAULT_TIMEOUT_MS = 120_000;

    /**
     * Maximum timeout for bash operations (10 minutes).
     */
    public static final int MAX_TIMEOUT_MS = 600_000;

    /**
     * Get the default timeout for bash operations in milliseconds.
     * Checks BASH_DEFAULT_TIMEOUT_MS environment variable or returns default.
     */
    public static int getDefaultBashTimeoutMs() {
        String envValue = System.getenv("BASH_DEFAULT_TIMEOUT_MS");
        if (envValue != null && !envValue.isEmpty()) {
            try {
                int parsed = Integer.parseInt(envValue);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_TIMEOUT_MS;
    }

    /**
     * Get the maximum timeout for bash operations in milliseconds.
     * Checks BASH_MAX_TIMEOUT_MS environment variable or returns default.
     */
    public static int getMaxBashTimeoutMs() {
        String envValue = System.getenv("BASH_MAX_TIMEOUT_MS");
        if (envValue != null && !envValue.isEmpty()) {
            try {
                int parsed = Integer.parseInt(envValue);
                if (parsed > 0) {
                    // Ensure max is at least as large as default
                    return Math.max(parsed, getDefaultBashTimeoutMs());
                }
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        // Always ensure max is at least as large as default
        return Math.max(MAX_TIMEOUT_MS, getDefaultBashTimeoutMs());
    }

    /**
     * Clamp a timeout value to the allowed range.
     */
    public static int clampTimeout(int timeoutMs) {
        int min = 1000; // Minimum 1 second
        int max = getMaxBashTimeoutMs();
        return Math.max(min, Math.min(timeoutMs, max));
    }

    /**
     * Parse timeout from string, returning default if invalid.
     */
    public static int parseTimeout(String value, int defaultTimeoutMs) {
        if (value == null || value.isEmpty()) {
            return defaultTimeoutMs;
        }

        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                return clampTimeout(parsed);
            }
        } catch (NumberFormatException e) {
            // Fall through to default
        }

        return defaultTimeoutMs;
    }

    /**
     * Get timeout from environment variable.
     */
    public static int getEnvTimeout(String envVar, int defaultTimeoutMs) {
        return parseTimeout(System.getenv(envVar), defaultTimeoutMs);
    }
}