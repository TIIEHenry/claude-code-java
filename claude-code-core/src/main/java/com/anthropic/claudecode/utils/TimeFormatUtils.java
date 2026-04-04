/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code time formatting utilities
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

/**
 * Time formatting and parsing utilities.
 */
public final class TimeFormatUtils {
    private TimeFormatUtils() {}

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter READABLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SHORT_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
    private static final DateTimeFormatter TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Format instant to ISO string.
     */
    public static String toIsoString(Instant instant) {
        if (instant == null) return null;
        return ISO_FORMATTER.format(instant);
    }

    /**
     * Format instant to readable string.
     */
    public static String toReadableString(Instant instant) {
        if (instant == null) return null;
        return READABLE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }

    /**
     * Format instant to short readable string.
     */
    public static String toShortString(Instant instant) {
        if (instant == null) return null;
        return SHORT_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }

    /**
     * Format instant to time only string.
     */
    public static String toTimeString(Instant instant) {
        if (instant == null) return null;
        return TIME_ONLY_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }

    /**
     * Parse ISO string to instant.
     */
    public static Instant fromIsoString(String isoString) {
        if (isoString == null || isoString.isEmpty()) return null;
        try {
            return Instant.parse(isoString);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Get current timestamp in milliseconds.
     */
    public static long currentMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Get current timestamp in seconds.
     */
    public static long currentSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Get current instant.
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Format duration in human readable form.
     */
    public static String formatDuration(Duration duration) {
        if (duration == null) return "";

        long seconds = duration.getSeconds();
        long millis = duration.toMillis() % 1000;

        if (seconds < 60) {
            if (millis > 0) {
                return String.format("%.2fs", seconds + millis / 1000.0);
            }
            return seconds + "s";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes < 60) {
            if (seconds > 0) {
                return minutes + "m " + seconds + "s";
            }
            return minutes + "m";
        }

        long hours = minutes / 60;
        minutes = minutes % 60;

        if (hours < 24) {
            if (minutes > 0) {
                return hours + "h " + minutes + "m";
            }
            return hours + "h";
        }

        long days = hours / 24;
        hours = hours % 24;

        if (hours > 0) {
            return days + "d " + hours + "h";
        }
        return days + "d";
    }

    /**
     * Format duration in compact form.
     */
    public static String formatDurationCompact(Duration duration) {
        if (duration == null) return "";

        long millis = duration.toMillis();

        if (millis < 1000) {
            return millis + "ms";
        }

        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h";
        }

        long days = hours / 24;
        return days + "d";
    }

    /**
     * Format relative time (e.g., "5 minutes ago").
     */
    public static String formatRelativeTime(Instant instant) {
        if (instant == null) return "";

        Duration duration = Duration.between(instant, Instant.now());
        boolean future = duration.isNegative();
        duration = duration.abs();

        String suffix = future ? " from now" : " ago";
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            if (seconds <= 1) return future ? "soon" : "just now";
            return seconds + " seconds" + suffix;
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute" : " minutes") + suffix;
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour" : " hours") + suffix;
        }

        long days = hours / 24;
        if (days < 7) {
            return days + (days == 1 ? " day" : " days") + suffix;
        }

        long weeks = days / 7;
        if (weeks < 4) {
            return weeks + (weeks == 1 ? " week" : " weeks") + suffix;
        }

        long months = days / 30;
        if (months < 12) {
            return months + (months == 1 ? " month" : " months") + suffix;
        }

        long years = days / 365;
        return years + (years == 1 ? " year" : " years") + suffix;
    }

    /**
     * Format timestamp for display.
     */
    public static String formatTimestamp(long timestampMs) {
        return toReadableString(Instant.ofEpochMilli(timestampMs));
    }

    /**
     * Format timestamp for logs.
     */
    public static String formatLogTimestamp(long timestampMs) {
        Instant instant = Instant.ofEpochMilli(timestampMs);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .format(instant.atZone(ZoneId.systemDefault()));
    }

    /**
     * Calculate age from timestamp.
     */
    public static Duration getAge(long timestampMs) {
        return Duration.between(Instant.ofEpochMilli(timestampMs), Instant.now());
    }

    /**
     * Check if timestamp is recent (within threshold).
     */
    public static boolean isRecent(long timestampMs, Duration threshold) {
        return getAge(timestampMs).compareTo(threshold) < 0;
    }

    /**
     * Check if timestamp is expired.
     */
    public static boolean isExpired(long timestampMs, Duration ttl) {
        return getAge(timestampMs).compareTo(ttl) > 0;
    }

    /**
     * Get start of day for an instant.
     */
    public static Instant startOfDay(Instant instant) {
        return instant.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();
    }

    /**
     * Get end of day for an instant.
     */
    public static Instant endOfDay(Instant instant) {
        return instant.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atTime(23, 59, 59, 999999999)
            .atZone(ZoneId.systemDefault())
            .toInstant();
    }

    /**
     * Check if instant is today.
     */
    public static boolean isToday(Instant instant) {
        LocalDate today = LocalDate.now();
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return today.equals(date);
    }

    /**
     * Check if instant is yesterday.
     */
    public static boolean isYesterday(Instant instant) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return yesterday.equals(date);
    }

    /**
     * Check if instant is this week.
     */
    public static boolean isThisWeek(Instant instant) {
        LocalDate now = LocalDate.now();
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        TemporalField weekField = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
        return now.get(weekField) == date.get(weekField) &&
               now.getYear() == date.getYear();
    }

    /**
     * Check if instant is this month.
     */
    public static boolean isThisMonth(Instant instant) {
        LocalDate now = LocalDate.now();
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return now.getMonth() == date.getMonth() &&
               now.getYear() == date.getYear();
    }

    /**
     * Format date smartly based on recency.
     */
    public static String formatSmart(Instant instant) {
        if (instant == null) return "";

        if (isToday(instant)) {
            return "Today at " + toTimeString(instant);
        }

        if (isYesterday(instant)) {
            return "Yesterday at " + toTimeString(instant);
        }

        if (isThisWeek(instant)) {
            String day = instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEEE"));
            return day + " at " + toTimeString(instant);
        }

        if (isThisMonth(instant)) {
            return instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM dd 'at' HH:mm"));
        }

        return toShortString(instant);
    }

    /**
     * Parse human-readable duration string.
     */
    public static Duration parseDuration(String input) {
        if (input == null || input.isEmpty()) return Duration.ZERO;

        input = input.trim().toLowerCase();

        // Try simple format: "30s", "5m", "2h", "1d"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d+)(ms|s|m|h|d)$");
        java.util.regex.Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            return switch (unit) {
                case "ms" -> Duration.ofMillis(value);
                case "s" -> Duration.ofSeconds(value);
                case "m" -> Duration.ofMinutes(value);
                case "h" -> Duration.ofHours(value);
                case "d" -> Duration.ofDays(value);
                default -> Duration.ZERO;
            };
        }

        // Try complex format: "1h30m", "2d5h", etc.
        pattern = java.util.regex.Pattern.compile("(\\d+)(ms|s|m|h|d)");
        matcher = pattern.matcher(input);

        Duration total = Duration.ZERO;
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            total = total.plus(switch (unit) {
                case "ms" -> Duration.ofMillis(value);
                case "s" -> Duration.ofSeconds(value);
                case "m" -> Duration.ofMinutes(value);
                case "h" -> Duration.ofHours(value);
                case "d" -> Duration.ofDays(value);
                default -> Duration.ZERO;
            });
        }

        return total;
    }

    /**
     * Sleep for a duration.
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis(), (int) (duration.toNanosPart() % 1_000_000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sleep for milliseconds.
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Create a deadline from now plus duration.
     */
    public static Instant deadlineFromNow(Duration duration) {
        return Instant.now().plus(duration);
    }

    /**
     * Check if deadline has passed.
     */
    public static boolean isDeadlinePassed(Instant deadline) {
        return Instant.now().isAfter(deadline);
    }

    /**
     * Get time remaining until deadline.
     */
    public static Duration timeRemaining(Instant deadline) {
        Duration remaining = Duration.between(Instant.now(), deadline);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Measure execution time of a runnable.
     */
    public static Duration measureTime(Runnable action) {
        long start = System.nanoTime();
        action.run();
        long end = System.nanoTime();
        return Duration.ofNanos(end - start);
    }

    /**
     * Measure execution time of a supplier.
     */
    public static <T> TimedResult<T> measureTime(java.util.function.Supplier<T> action) {
        long start = System.nanoTime();
        T result = action.get();
        long end = System.nanoTime();
        return new TimedResult<>(result, Duration.ofNanos(end - start));
    }

    /**
     * Timed result record.
     */
    public record TimedResult<T>(T value, Duration duration) {}

    /**
     * Format date according to pattern.
     */
    public static String format(Instant instant, String pattern) {
        if (instant == null) return null;
        return DateTimeFormatter.ofPattern(pattern)
            .format(instant.atZone(ZoneId.systemDefault()));
    }

    /**
     * Parse date from string according to pattern.
     */
    public static Instant parse(String text, String pattern) {
        if (text == null || text.isEmpty()) return null;
        try {
            LocalDateTime local = LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
            return local.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}