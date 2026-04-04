/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code duration utilities
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Duration utilities.
 */
public final class DurationUtils {
    private DurationUtils() {}

    /**
     * Parse duration from string.
     * Supports: 1s, 5m, 2h, 3d, 100ms, 1w
     */
    public static Duration parse(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            throw new IllegalArgumentException("Duration string cannot be empty");
        }

        String trimmed = durationStr.trim().toLowerCase();
        long multiplier = 1;

        // Check for negative
        if (trimmed.startsWith("-")) {
            multiplier = -1;
            trimmed = trimmed.substring(1);
        }

        // Extract number and unit
        int i = 0;
        while (i < trimmed.length() && (Character.isDigit(trimmed.charAt(i)) || trimmed.charAt(i) == '.')) {
            i++;
        }

        if (i == 0) {
            throw new IllegalArgumentException("No number in duration: " + durationStr);
        }

        double number = Double.parseDouble(trimmed.substring(0, i));
        String unit = trimmed.substring(i);

        Duration duration = switch (unit) {
            case "ns", "nanos", "nanoseconds" -> Duration.ofNanos((long) number);
            case "ms", "millis", "milliseconds" -> Duration.ofMillis((long) number);
            case "s", "sec", "seconds" -> Duration.ofSeconds((long) number);
            case "m", "min", "minutes" -> Duration.ofMinutes((long) number);
            case "h", "hour", "hours" -> Duration.ofHours((long) number);
            case "d", "day", "days" -> Duration.ofDays((long) number);
            case "w", "week", "weeks" -> Duration.ofDays((long) (number * 7));
            default -> throw new IllegalArgumentException("Unknown duration unit: " + unit);
        };

        return duration.multipliedBy(multiplier);
    }

    /**
     * Parse duration safely with default.
     */
    public static Duration parseOrDefault(String durationStr, Duration defaultDuration) {
        try {
            return parse(durationStr);
        } catch (Exception e) {
            return defaultDuration;
        }
    }

    /**
     * Format duration to human-readable string.
     */
    public static String format(Duration duration) {
        if (duration.isZero()) return "0ms";
        if (duration.isNegative()) return "-" + format(duration.abs());

        long nanos = duration.toNanos();

        if (nanos < 1_000_000) {
            return nanos + "ns";
        } else if (nanos < 1_000_000_000) {
            return (nanos / 1_000_000) + "ms";
        } else if (nanos < 60_000_000_000L) {
            long seconds = nanos / 1_000_000_000;
            long millis = (nanos % 1_000_000_000) / 1_000_000;
            if (millis == 0) return seconds + "s";
            return seconds + "s " + millis + "ms";
        } else if (nanos < 3_600_000_000_000L) {
            long minutes = nanos / 60_000_000_000L;
            long seconds = (nanos % 60_000_000_000L) / 1_000_000_000;
            if (seconds == 0) return minutes + "m";
            return minutes + "m " + seconds + "s";
        } else if (nanos < 86_400_000_000_000L) {
            long hours = nanos / 3_600_000_000_000L;
            long minutes = (nanos % 3_600_000_000_000L) / 60_000_000_000L;
            if (minutes == 0) return hours + "h";
            return hours + "h " + minutes + "m";
        } else {
            long days = nanos / 86_400_000_000_000L;
            long hours = (nanos % 86_400_000_000_000L) / 3_600_000_000_000L;
            if (hours == 0) return days + "d";
            return days + "d " + hours + "h";
        }
    }

    /**
     * Format duration compactly.
     */
    public static String formatCompact(Duration duration) {
        if (duration.isZero()) return "0";

        long totalSeconds = duration.toSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString();
    }

    /**
     * Format as ISO-8601.
     */
    public static String formatIso(Duration duration) {
        return duration.toString();
    }

    /**
     * Compare durations.
     */
    public static Comparator<Duration> comparator() {
        return Comparator.comparingLong(Duration::toNanos);
    }

    /**
     * Min duration.
     */
    public static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    /**
     * Max duration.
     */
    public static Duration max(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /**
     * Sum durations.
     */
    public static Duration sum(Collection<Duration> durations) {
        return durations.stream().reduce(Duration.ZERO, Duration::plus);
    }

    /**
     * Average duration.
     */
    public static Duration average(Collection<Duration> durations) {
        if (durations.isEmpty()) return Duration.ZERO;
        long totalNanos = durations.stream().mapToLong(Duration::toNanos).sum();
        return Duration.ofNanos(totalNanos / durations.size());
    }

    /**
     * Clamp duration between min and max.
     */
    public static Duration clamp(Duration value, Duration min, Duration max) {
        if (value.compareTo(min) < 0) return min;
        if (value.compareTo(max) > 0) return max;
        return value;
    }

    /**
     * Scale duration by factor.
     */
    public static Duration scale(Duration duration, double factor) {
        return Duration.ofNanos((long) (duration.toNanos() * factor));
    }

    /**
     * Between two instants.
     */
    public static Duration between(Instant start, Instant end) {
        return Duration.between(start, end);
    }

    /**
     * Since instant.
     */
    public static Duration since(Instant instant) {
        return Duration.between(instant, Instant.now());
    }

    /**
     * Until instant.
     */
    public static Duration until(Instant instant) {
        return Duration.between(Instant.now(), instant);
    }

    /**
     * Is expired (duration since instant > max duration).
     */
    public static boolean isExpired(Instant instant, Duration maxDuration) {
        return since(instant).compareTo(maxDuration) > 0;
    }

    /**
     * Remaining duration until instant (clamped to zero if past).
     */
    public static Duration remaining(Instant deadline) {
        Duration remaining = until(deadline);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Sleep for duration.
     */
    public static void sleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis(), (int) (duration.toNanos() % 1_000_000));
    }

    /**
     * Sleep with exception handling.
     */
    public static void sleepQuietly(Duration duration) {
        try {
            sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Convert to milliseconds.
     */
    public static long toMillis(Duration duration) {
        return duration.toMillis();
    }

    /**
     * Convert to seconds.
     */
    public static long toSeconds(Duration duration) {
        return duration.toSeconds();
    }

    /**
     * Convert to minutes.
     */
    public static long toMinutes(Duration duration) {
        return duration.toMinutes();
    }

    /**
     * Convert to hours.
     */
    public static long toHours(Duration duration) {
        return duration.toHours();
    }

    /**
     * Convert to days.
     */
    public static long toDays(Duration duration) {
        return duration.toDays();
    }

    /**
     * Convert to nanoseconds.
     */
    public static long toNanos(Duration duration) {
        return duration.toNanos();
    }

    /**
     * Create from millis.
     */
    public static Duration ofMillis(long millis) {
        return Duration.ofMillis(millis);
    }

    /**
     * Create from seconds.
     */
    public static Duration ofSeconds(long seconds) {
        return Duration.ofSeconds(seconds);
    }

    /**
     * Create from minutes.
     */
    public static Duration ofMinutes(long minutes) {
        return Duration.ofMinutes(minutes);
    }

    /**
     * Create from hours.
     */
    public static Duration ofHours(long hours) {
        return Duration.ofHours(hours);
    }

    /**
     * Create from days.
     */
    public static Duration ofDays(long days) {
        return Duration.ofDays(days);
    }

    /**
     * Zero duration.
     */
    public static Duration zero() {
        return Duration.ZERO;
    }

    /**
     * One millisecond.
     */
    public static Duration oneMillisecond() {
        return Duration.ofMillis(1);
    }

    /**
     * One second.
     */
    public static Duration oneSecond() {
        return Duration.ofSeconds(1);
    }

    /**
     * One minute.
     */
    public static Duration oneMinute() {
        return Duration.ofMinutes(1);
    }

    /**
     * One hour.
     */
    public static Duration oneHour() {
        return Duration.ofHours(1);
    }

    /**
     * One day.
     */
    public static Duration oneDay() {
        return Duration.ofDays(1);
    }

    /**
     * Check if duration is zero.
     */
    public static boolean isZero(Duration duration) {
        return duration.isZero();
    }

    /**
     * Check if duration is negative.
     */
    public static boolean isNegative(Duration duration) {
        return duration.isNegative();
    }

    /**
     * Check if duration is positive.
     */
    public static boolean isPositive(Duration duration) {
        return !duration.isNegative() && !duration.isZero();
    }

    /**
     * Abs duration.
     */
    public static Duration abs(Duration duration) {
        return duration.isNegative() ? duration.abs() : duration;
    }

    /**
     * Negate duration.
     */
    public static Duration negate(Duration duration) {
        return duration.negated();
    }

    /**
     * Divide duration by divisor.
     */
    public static Duration divide(Duration duration, long divisor) {
        return duration.dividedBy(divisor);
    }

    /**
     * Divide duration by another duration.
     */
    public static double divideBy(Duration dividend, Duration divisor) {
        return (double) dividend.toNanos() / divisor.toNanos();
    }
}