/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/format.ts
 */
package com.anthropic.claudecode.utils;

import java.text.*;
import java.util.*;

/**
 * Formatting utilities.
 */
public final class FormatUtils {
    private FormatUtils() {}

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    /**
     * Format a number with thousands separator.
     */
    public static String formatNumber(long number) {
        return NUMBER_FORMAT.format(number);
    }

    /**
     * Format a number with thousands separator.
     */
    public static String formatNumber(int number) {
        return NUMBER_FORMAT.format(number);
    }

    /**
     * Format a decimal with thousands separator.
     */
    public static String formatDecimal(double number) {
        return DECIMAL_FORMAT.format(number);
    }

    /**
     * Format token count.
     */
    public static String formatTokens(long tokens) {
        if (tokens >= 1_000_000) {
            return String.format("%.1fM", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format("%.1fK", tokens / 1_000.0);
        }
        return String.valueOf(tokens);
    }

    /**
     * Format currency.
     */
    public static String formatCurrency(double amount) {
        return String.format("$%.2f", amount);
    }

    /**
     * Format duration in milliseconds.
     */
    public static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60000) {
            return String.format("%.1fs", ms / 1000.0);
        } else {
            long minutes = ms / 60000;
            long seconds = (ms % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    /**
     * Format file size.
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

    /**
     * Format percentage.
     */
    public static String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }
}