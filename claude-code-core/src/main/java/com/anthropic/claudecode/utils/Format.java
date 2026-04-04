/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code format utilities
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Pure display formatters for various data types.
 */
public final class Format {
    private Format() {}

    // Cached number formatters
    private static String formatCompactNumber(double number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fb", number / 1_000_000_000).replace(".0", "");
        } else if (number >= 1_000_000) {
            return String.format("%.1fm", number / 1_000_000).replace(".0", "");
        } else if (number >= 1_000) {
            return String.format("%.1fk", number / 1_000).replace(".0", "");
        }
        return String.valueOf((long) number);
    }

    /**
     * Formats a byte count to a human-readable string (KB, MB, GB).
     */
    public static String formatFileSize(long sizeInBytes) {
        double kb = sizeInBytes / 1024.0;
        if (kb < 1) {
            return sizeInBytes + " bytes";
        }
        if (kb < 1024) {
            return String.format("%.1fKB", kb).replace(".0", "");
        }
        double mb = kb / 1024;
        if (mb < 1024) {
            return String.format("%.1fMB", mb).replace(".0", "");
        }
        double gb = mb / 1024;
        return String.format("%.1fGB", gb).replace(".0", "");
    }

    /**
     * Formats milliseconds as seconds with 1 decimal place.
     */
    public static String formatSecondsShort(long ms) {
        return String.format("%.1fs", ms / 1000.0);
    }

    /**
     * Format duration in human-readable format.
     */
    public static String formatDuration(long ms) {
        return formatDuration(ms, false, false);
    }

    /**
     * Format duration with options.
     */
    public static String formatDuration(long ms, boolean hideTrailingZeros, boolean mostSignificantOnly) {
        if (ms < 60000) {
            if (ms == 0) return "0s";
            if (ms < 1) return String.format("%.1fs", ms / 1000.0);
            return (ms / 1000) + "s";
        }

        long days = ms / 86400000;
        long hours = (ms % 86400000) / 3600000;
        long minutes = (ms % 3600000) / 60000;
        long seconds = Math.round((ms % 60000) / 1000.0);

        // Handle rounding carry-over
        if (seconds == 60) { seconds = 0; minutes++; }
        if (minutes == 60) { minutes = 0; hours++; }
        if (hours == 24) { hours = 0; days++; }

        if (mostSignificantOnly) {
            if (days > 0) return days + "d";
            if (hours > 0) return hours + "h";
            if (minutes > 0) return minutes + "m";
            return seconds + "s";
        }

        if (days > 0) {
            if (hideTrailingZeros && hours == 0 && minutes == 0) return days + "d";
            if (hideTrailingZeros && minutes == 0) return days + "d " + hours + "h";
            return days + "d " + hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            if (hideTrailingZeros && minutes == 0 && seconds == 0) return hours + "h";
            if (hideTrailingZeros && seconds == 0) return hours + "h " + minutes + "m";
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0) {
            if (hideTrailingZeros && seconds == 0) return minutes + "m";
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    /**
     * Format a number with compact notation (1.3k, 1.0m, etc.).
     */
    public static String formatNumber(long number) {
        return formatCompactNumber(number);
    }

    /**
     * Format token count.
     */
    public static String formatTokens(long count) {
        return formatCompactNumber(count);
    }

    /**
     * Format relative time (e.g., "2h ago", "in 5m").
     */
    public static String formatRelativeTime(Instant date, Instant now) {
        long diffInSeconds = ChronoUnit.SECONDS.between(now, date);

        // Time intervals
        if (Math.abs(diffInSeconds) >= 31536000) {
            long years = diffInSeconds / 31536000;
            return diffInSeconds < 0 ? Math.abs(years) + "y ago" : "in " + years + "y";
        }
        if (Math.abs(diffInSeconds) >= 2592000) {
            long months = diffInSeconds / 2592000;
            return diffInSeconds < 0 ? Math.abs(months) + "mo ago" : "in " + months + "mo";
        }
        if (Math.abs(diffInSeconds) >= 604800) {
            long weeks = diffInSeconds / 604800;
            return diffInSeconds < 0 ? Math.abs(weeks) + "w ago" : "in " + weeks + "w";
        }
        if (Math.abs(diffInSeconds) >= 86400) {
            long days = diffInSeconds / 86400;
            return diffInSeconds < 0 ? Math.abs(days) + "d ago" : "in " + days + "d";
        }
        if (Math.abs(diffInSeconds) >= 3600) {
            long hours = diffInSeconds / 3600;
            return diffInSeconds < 0 ? Math.abs(hours) + "h ago" : "in " + hours + "h";
        }
        if (Math.abs(diffInSeconds) >= 60) {
            long minutes = diffInSeconds / 60;
            return diffInSeconds < 0 ? Math.abs(minutes) + "m ago" : "in " + minutes + "m";
        }
        return diffInSeconds <= 0 ? "0s ago" : "in 0s";
    }

    /**
     * Format relative time from now.
     */
    public static String formatRelativeTimeAgo(Instant date) {
        return formatRelativeTime(date, Instant.now());
    }

    /**
     * Format a timestamp for display.
     */
    public static String formatTimestamp(Instant timestamp) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(timestamp);
    }

    /**
     * Format a date for display.
     */
    public static String formatDate(Instant timestamp) {
        return DateTimeFormatter.ofPattern("MMM d, yyyy")
                .withZone(ZoneId.systemDefault())
                .format(timestamp);
    }

    /**
     * Format time for display.
     */
    public static String formatTime(Instant timestamp) {
        return DateTimeFormatter.ofPattern("h:mm a")
                .withZone(ZoneId.systemDefault())
                .format(timestamp)
                .replace(" AM", "am")
                .replace(" PM", "pm");
    }

    /**
     * Format a percentage.
     */
    public static String formatPercent(double value) {
        return String.format("%.1f%%", value);
    }

    /**
     * Format a currency amount.
     */
    public static String formatCurrency(double amount) {
        return String.format("$%.2f", amount);
    }

    /**
     * Pluralize a word based on count.
     */
    public static String plural(long count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    /**
     * Pluralize with default -s suffix.
     */
    public static String plural(long count, String word) {
        return count == 1 ? word : word + "s";
    }

    /**
     * Format a list with commas and "and".
     */
    public static String formatList(java.util.List<String> items) {
        if (items == null || items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        if (items.size() == 2) return items.get(0) + " and " + items.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size() - 1; i++) {
            sb.append(items.get(i)).append(", ");
        }
        sb.append("and ").append(items.get(items.size() - 1));
        return sb.toString();
    }
}