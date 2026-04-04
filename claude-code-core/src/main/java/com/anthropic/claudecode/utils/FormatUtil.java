/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/format.ts
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Pure display formatters.
 */
public final class FormatUtil {
    private FormatUtil() {}

    /**
     * Formats a byte count to a human-readable string (KB, MB, GB).
     */
    public static String formatFileSize(long sizeInBytes) {
        double kb = sizeInBytes / 1024.0;
        if (kb < 1) {
            return sizeInBytes + " bytes";
        }
        if (kb < 1024) {
            return formatDecimal(kb) + "KB";
        }
        double mb = kb / 1024;
        if (mb < 1024) {
            return formatDecimal(mb) + "MB";
        }
        double gb = mb / 1024;
        return formatDecimal(gb) + "GB";
    }

    private static String formatDecimal(double value) {
        String formatted = String.format("%.1f", value);
        if (formatted.endsWith(".0")) {
            return formatted.substring(0, formatted.length() - 2);
        }
        return formatted;
    }

    /**
     * Formats milliseconds as seconds with 1 decimal place.
     */
    public static String formatSecondsShort(long ms) {
        return String.format("%.1fs", ms / 1000.0);
    }

    /**
     * Formats duration in milliseconds to human-readable string.
     */
    public static String formatDuration(long ms) {
        return formatDuration(ms, null);
    }

    public static String formatDuration(long ms, FormatDurationOptions options) {
        boolean hideTrailingZeros = options != null && options.hideTrailingZeros();
        boolean mostSignificantOnly = options != null && options.mostSignificantOnly();

        if (ms < 60000) {
            if (ms == 0) return "0s";
            if (ms < 1000) {
                return String.format("%.1fs", ms / 1000.0);
            }
            return (ms / 1000) + "s";
        }

        long days = ms / 86400000;
        long hours = (ms % 86400000) / 3600000;
        long minutes = (ms % 3600000) / 60000;
        long seconds = Math.round((ms % 60000) / 1000.0);

        // Handle rounding carry-over
        if (seconds == 60) {
            seconds = 0;
            minutes++;
        }
        if (minutes == 60) {
            minutes = 0;
            hours++;
        }
        if (hours == 24) {
            hours = 0;
            days++;
        }

        if (mostSignificantOnly) {
            if (days > 0) return days + "d";
            if (hours > 0) return hours + "h";
            if (minutes > 0) return minutes + "m";
            return seconds + "s";
        }

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d");
            if (!hideTrailingZeros || hours > 0 || minutes > 0) {
                sb.append(" ").append(hours).append("h");
            }
            if (!hideTrailingZeros || minutes > 0) {
                sb.append(" ").append(minutes).append("m");
            }
        } else if (hours > 0) {
            sb.append(hours).append("h");
            if (!hideTrailingZeros || minutes > 0 || seconds > 0) {
                sb.append(" ").append(minutes).append("m");
            }
            if (!hideTrailingZeros || seconds > 0) {
                sb.append(" ").append(seconds).append("s");
            }
        } else if (minutes > 0) {
            sb.append(minutes).append("m");
            if (!hideTrailingZeros || seconds > 0) {
                sb.append(" ").append(seconds).append("s");
            }
        } else {
            sb.append(seconds).append("s");
        }

        return sb.toString();
    }

    /**
     * Options for formatDuration.
     */
    public record FormatDurationOptions(
        boolean hideTrailingZeros,
        boolean mostSignificantOnly
    ) {}

    /**
     * Formats a number with compact notation (1.3K, 900, etc.).
     */
    public static String formatNumber(long number) {
        return formatNumber((double) number);
    }

    public static String formatNumber(double number) {
        if (number >= 1000) {
            double value = number / 1000;
            String suffix = "k";
            if (value >= 1000) {
                value /= 1000;
                suffix = "m";
            }
            if (value >= 1000) {
                value /= 1000;
                suffix = "b";
            }
            return String.format("%.1f", value).replace(".0", "") + suffix;
        }
        return String.valueOf((long) number);
    }

    /**
     * Formats token count.
     */
    public static String formatTokens(long count) {
        return formatNumber(count).replace(".0", "");
    }

    /**
     * Formats relative time from a date.
     */
    public static String formatRelativeTime(Instant date) {
        return formatRelativeTime(date, Instant.now());
    }

    public static String formatRelativeTime(Instant date, Instant now) {
        long diffInSeconds = date.getEpochSecond() - now.getEpochSecond();

        // Time intervals
        if (Math.abs(diffInSeconds) >= 31536000) {
            long years = diffInSeconds / 31536000;
            return formatRelativeValue(years, "y", diffInSeconds < 0);
        }
        if (Math.abs(diffInSeconds) >= 2592000) {
            long months = diffInSeconds / 2592000;
            return formatRelativeValue(months, "mo", diffInSeconds < 0);
        }
        if (Math.abs(diffInSeconds) >= 604800) {
            long weeks = diffInSeconds / 604800;
            return formatRelativeValue(weeks, "w", diffInSeconds < 0);
        }
        if (Math.abs(diffInSeconds) >= 86400) {
            long days = diffInSeconds / 86400;
            return formatRelativeValue(days, "d", diffInSeconds < 0);
        }
        if (Math.abs(diffInSeconds) >= 3600) {
            long hours = diffInSeconds / 3600;
            return formatRelativeValue(hours, "h", diffInSeconds < 0);
        }
        if (Math.abs(diffInSeconds) >= 60) {
            long minutes = diffInSeconds / 60;
            return formatRelativeValue(minutes, "m", diffInSeconds < 0);
        }

        return diffInSeconds <= 0 ? "0s ago" : "in 0s";
    }

    private static String formatRelativeValue(long value, String unit, boolean isPast) {
        long absValue = Math.abs(value);
        return isPast ? absValue + unit + " ago" : "in " + absValue + unit;
    }

    /**
     * Formats relative time ago.
     */
    public static String formatRelativeTimeAgo(Instant date) {
        return formatRelativeTimeAgo(date, Instant.now());
    }

    public static String formatRelativeTimeAgo(Instant date, Instant now) {
        if (date.isAfter(now)) {
            return formatRelativeTime(date, now);
        }
        return formatRelativeTime(date, now);
    }

    /**
     * Formats log metadata for display.
     */
    public static String formatLogMetadata(LogMetadata log) {
        List<String> parts = new ArrayList<>();
        parts.add(formatRelativeTimeAgo(log.modified()));

        if (log.gitBranch() != null) {
            parts.add(log.gitBranch());
        }

        if (log.fileSize() != null) {
            parts.add(formatFileSize(log.fileSize()));
        } else {
            parts.add(log.messageCount() + " messages");
        }

        if (log.tag() != null) {
            parts.add("#" + log.tag());
        }

        if (log.agentSetting() != null) {
            parts.add("@" + log.agentSetting());
        }

        if (log.prNumber() != null) {
            if (log.prRepository() != null) {
                parts.add(log.prRepository() + "#" + log.prNumber());
            } else {
                parts.add("#" + log.prNumber());
            }
        }

        return String.join(" · ", parts);
    }

    /**
     * Log metadata record.
     */
    public record LogMetadata(
        Instant modified,
        int messageCount,
        Long fileSize,
        String gitBranch,
        String tag,
        String agentSetting,
        Integer prNumber,
        String prRepository
    ) {}

    /**
     * Formats reset time.
     */
    public static String formatResetTime(long timestampInSeconds) {
        return formatResetTime(timestampInSeconds, false, true);
    }

    public static String formatResetTime(long timestampInSeconds, boolean showTimezone, boolean showTime) {
        if (timestampInSeconds <= 0) return null;

        Instant resetTime = Instant.ofEpochSecond(timestampInSeconds);
        Instant now = Instant.now();
        ZonedDateTime resetDate = resetTime.atZone(ZoneId.systemDefault());
        ZonedDateTime nowDate = now.atZone(ZoneId.systemDefault());

        long hoursUntilReset = Duration.between(now, resetTime).toHours();

        DateTimeFormatter formatter;
        if (hoursUntilReset > 24) {
            // Show date and time for resets more than a day away
            if (resetDate.getYear() != nowDate.getYear()) {
                formatter = DateTimeFormatter.ofPattern("MMM d, yyyy" +
                    (showTime ? " h:mm a" : ""));
            } else {
                formatter = DateTimeFormatter.ofPattern("MMM d" +
                    (showTime ? " h:mm a" : ""));
            }
        } else {
            // For resets within 24 hours, show just the time
            formatter = DateTimeFormatter.ofPattern("h:mm a");
        }

        String result = resetDate.format(formatter).toLowerCase();
        if (showTimezone) {
            result += " (" + ZoneId.systemDefault().getId() + ")";
        }
        return result;
    }

    /**
     * Truncate string to max length with ellipsis.
     */
    public static String truncate(String s, int maxLength) {
        if (s == null || s.length() <= maxLength) return s;
        return s.substring(0, maxLength - 3) + "...";
    }

    /**
     * Truncate path in the middle.
     */
    public static String truncatePathMiddle(String path, int maxLength) {
        if (path == null || path.length() <= maxLength) return path;

        int half = (maxLength - 3) / 2;
        return path.substring(0, half) + "..." +
               path.substring(path.length() - half);
    }
}