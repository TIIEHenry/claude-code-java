/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/intl.ts
 */
package com.anthropic.claudecode.utils;

import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Shared locale-aware utilities with lazy initialization.
 * Intl constructors can be expensive, so we cache instances for reuse.
 */
public final class IntlUtils {
    private IntlUtils() {}

    // Cached timezone
    private static volatile String cachedTimeZone = null;

    // Cached system locale
    private static volatile Locale cachedLocale = null;

    // Cached number formats
    private static final ConcurrentHashMap<String, NumberFormat> numberFormatCache = new ConcurrentHashMap<>();

    // Cached date formats
    private static final ConcurrentHashMap<String, DateTimeFormatter> dateFormatCache = new ConcurrentHashMap<>();

    /**
     * Get the system timezone.
     */
    public static String getTimeZone() {
        if (cachedTimeZone == null) {
            cachedTimeZone = ZoneId.systemDefault().getId();
        }
        return cachedTimeZone;
    }

    /**
     * Get the system locale.
     */
    public static Locale getSystemLocale() {
        if (cachedLocale == null) {
            cachedLocale = Locale.getDefault();
        }
        return cachedLocale;
    }

    /**
     * Get the system locale language (e.g., "en", "ja").
     */
    public static String getSystemLocaleLanguage() {
        return getSystemLocale().getLanguage();
    }

    /**
     * Get a number format for the default locale.
     */
    public static NumberFormat getNumberFormat() {
        return getNumberFormat(getSystemLocale());
    }

    /**
     * Get a number format for a specific locale.
     */
    public static NumberFormat getNumberFormat(Locale locale) {
        String key = locale.toLanguageTag();
        return numberFormatCache.computeIfAbsent(key, k -> NumberFormat.getInstance(locale));
    }

    /**
     * Get an integer format for the default locale.
     */
    public static NumberFormat getIntegerFormat() {
        NumberFormat format = getNumberFormat();
        format.setMaximumFractionDigits(0);
        return format;
    }

    /**
     * Get a compact number format (e.g., 1K, 1M).
     */
    public static String formatCompact(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    /**
     * Get a date formatter for the default locale.
     */
    public static DateTimeFormatter getDateFormat() {
        return getDateFormat(getSystemLocale());
    }

    /**
     * Get a date formatter for a specific locale.
     */
    public static DateTimeFormatter getDateFormat(Locale locale) {
        String key = "date:" + locale.toLanguageTag();
        return dateFormatCache.computeIfAbsent(key, k ->
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        );
    }

    /**
     * Get a time formatter for the default locale.
     */
    public static DateTimeFormatter getTimeFormat() {
        return getTimeFormat(getSystemLocale());
    }

    /**
     * Get a time formatter for a specific locale.
     */
    public static DateTimeFormatter getTimeFormat(Locale locale) {
        String key = "time:" + locale.toLanguageTag();
        return dateFormatCache.computeIfAbsent(key, k ->
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        );
    }

    /**
     * Get a date-time formatter for the default locale.
     */
    public static DateTimeFormatter getDateTimeFormat() {
        return getDateTimeFormat(getSystemLocale());
    }

    /**
     * Get a date-time formatter for a specific locale.
     */
    public static DateTimeFormatter getDateTimeFormat(Locale locale) {
        String key = "datetime:" + locale.toLanguageTag();
        return dateFormatCache.computeIfAbsent(key, k ->
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
        );
    }

    /**
     * Format a relative time (e.g., "2 hours ago", "in 3 days").
     */
    public static String formatRelativeTime(Instant instant) {
        return formatRelativeTime(instant, Instant.now());
    }

    /**
     * Format a relative time from a reference point.
     */
    public static String formatRelativeTime(Instant instant, Instant reference) {
        Duration duration = Duration.between(instant, reference);
        boolean past = !duration.isNegative();
        long seconds = Math.abs(duration.getSeconds());

        if (seconds < 60) {
            return past ? seconds + " seconds ago" : "in " + seconds + " seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return past ? minutes + " minute" + (minutes > 1 ? "s" : "") + " ago" :
                          "in " + minutes + " minute" + (minutes > 1 ? "s" : "");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return past ? hours + " hour" + (hours > 1 ? "s" : "") + " ago" :
                          "in " + hours + " hour" + (hours > 1 ? "s" : "");
        } else if (seconds < 604800) {
            long days = seconds / 86400;
            return past ? days + " day" + (days > 1 ? "s" : "") + " ago" :
                          "in " + days + " day" + (days > 1 ? "s" : "");
        } else if (seconds < 2592000) {
            long weeks = seconds / 604800;
            return past ? weeks + " week" + (weeks > 1 ? "s" : "") + " ago" :
                          "in " + weeks + " week" + (weeks > 1 ? "s" : "");
        } else if (seconds < 31536000) {
            long months = seconds / 2592000;
            return past ? months + " month" + (months > 1 ? "s" : "") + " ago" :
                          "in " + months + " month" + (months > 1 ? "s" : "");
        } else {
            long years = seconds / 31536000;
            return past ? years + " year" + (years > 1 ? "s" : "") + " ago" :
                          "in " + years + " year" + (years > 1 ? "s" : "");
        }
    }

    /**
     * Clear all caches.
     */
    public static void clearCaches() {
        numberFormatCache.clear();
        dateFormatCache.clear();
        cachedTimeZone = null;
        cachedLocale = null;
    }
}