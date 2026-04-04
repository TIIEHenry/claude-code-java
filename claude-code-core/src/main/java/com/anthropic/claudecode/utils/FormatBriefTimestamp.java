/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code brief timestamp formatter
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.time.format.*;
import java.util.Locale;

/**
 * Format an ISO timestamp for the brief/chat message label line.
 *
 * Display scales with age (like a messaging app):
 *   - same day:      "1:30 PM" or "13:30" (locale-dependent)
 *   - within 6 days: "Sunday, 4:15 PM" (locale-dependent)
 *   - older:         "Sunday, Feb 20, 4:30 PM" (locale-dependent)
 */
public final class FormatBriefTimestamp {
    private FormatBriefTimestamp() {}

    /**
     * Format an ISO timestamp string for display.
     */
    public static String formatBriefTimestamp(String isoString) {
        return formatBriefTimestamp(isoString, Instant.now());
    }

    /**
     * Format an ISO timestamp string for display with injectable now for tests.
     */
    public static String formatBriefTimestamp(String isoString, Instant now) {
        try {
            ZonedDateTime d = ZonedDateTime.parse(isoString);
            Locale locale = getLocale();
            ZonedDateTime nowZoned = now.atZone(d.getZone());

            long daysAgo = calculateDaysAgo(d, nowZoned);

            if (daysAgo == 0) {
                // Same day - just time
                return d.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale));
            }

            if (daysAgo > 0 && daysAgo < 7) {
                // Within 6 days - weekday + time
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale);
                return d.format(DateTimeFormatter.ofPattern("EEEE", locale)) + ", " + d.format(timeFormatter);
            }

            // Older - weekday + month + day + time
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale);
            return d.format(DateTimeFormatter.ofPattern("EEEE, MMM d", locale)) + ", " + d.format(timeFormatter);
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    /**
     * Derive a BCP 47 locale tag from POSIX env vars.
     * LC_ALL > LC_TIME > LANG, falls back to system default.
     */
    private static Locale getLocale() {
        String raw = System.getenv("LC_ALL");
        if (raw == null || raw.isEmpty()) {
            raw = System.getenv("LC_TIME");
        }
        if (raw == null || raw.isEmpty()) {
            raw = System.getenv("LANG");
        }

        if (raw == null || raw.isEmpty() || raw.equals("C") || raw.equals("POSIX")) {
            return Locale.getDefault();
        }

        // Strip codeset (.UTF-8) and modifier (@euro), replace _ with -
        String base = raw.split("\\.")[0].split("@")[0];
        if (base.isEmpty()) {
            return Locale.getDefault();
        }

        String tag = base.replace('_', '-');

        // Try to parse as locale
        try {
            return Locale.forLanguageTag(tag);
        } catch (Exception e) {
            return Locale.getDefault();
        }
    }

    /**
     * Calculate days ago from date to now.
     */
    private static long calculateDaysAgo(ZonedDateTime date, ZonedDateTime now) {
        LocalDate dateDay = date.toLocalDate();
        LocalDate nowDay = now.toLocalDate();
        return nowDay.toEpochDay() - dateDay.toEpochDay();
    }
}