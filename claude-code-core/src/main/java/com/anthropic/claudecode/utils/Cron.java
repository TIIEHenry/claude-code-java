/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cron expression utilities
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal cron expression parsing and next-run calculation.
 *
 * Supports the standard 5-field cron subset:
 *   minute hour day-of-month month day-of-week
 *
 * Field syntax: wildcard, N, step (star-slash-N), range (N-M), list (N,M,...).
 * No L, W, ?, or name aliases. All times are interpreted in the process's
 * local timezone.
 */
public final class Cron {
    private Cron() {}

    /**
     * Cron fields container.
     */
    public record CronFields(
            Set<Integer> minute,
            Set<Integer> hour,
            Set<Integer> dayOfMonth,
            Set<Integer> month,
            Set<Integer> dayOfWeek
    ) {}

    // Field ranges
    private static final int[][] FIELD_RANGES = {
            {0, 59},   // minute
            {0, 23},   // hour
            {1, 31},   // dayOfMonth
            {1, 12},   // month
            {0, 6}     // dayOfWeek (0=Sunday; 7 accepted as Sunday alias)
    };

    private static final String[] DAY_NAMES = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    /**
     * Parse a 5-field cron expression into expanded number sets.
     * Returns null if invalid or unsupported syntax.
     */
    public static CronFields parseCronExpression(String expr) {
        if (expr == null || expr.isEmpty()) {
            return null;
        }

        String[] parts = expr.trim().split("\\s+");
        if (parts.length != 5) {
            return null;
        }

        Set<Integer>[] expanded = new Set[5];
        for (int i = 0; i < 5; i++) {
            Set<Integer> result = expandField(parts[i], FIELD_RANGES[i][0], FIELD_RANGES[i][1]);
            if (result == null) {
                return null;
            }
            expanded[i] = result;
        }

        return new CronFields(
                Collections.unmodifiableSet(expanded[0]),
                Collections.unmodifiableSet(expanded[1]),
                Collections.unmodifiableSet(expanded[2]),
                Collections.unmodifiableSet(expanded[3]),
                Collections.unmodifiableSet(expanded[4])
        );
    }

    /**
     * Expand a single cron field into a set of matching values.
     */
    private static Set<Integer> expandField(String field, int min, int max) {
        Set<Integer> result = new TreeSet<>();

        for (String part : field.split(",")) {
            // wildcard or star-slash-N
            Pattern stepPattern = Pattern.compile("^\\*(?:/(\\d+))?$");
            Matcher stepMatcher = stepPattern.matcher(part);
            if (stepMatcher.matches()) {
                int step = stepMatcher.group(1) != null ? Integer.parseInt(stepMatcher.group(1)) : 1;
                if (step < 1) return null;
                for (int i = min; i <= max; i += step) {
                    result.add(i);
                }
                continue;
            }

            // N-M or N-M/S
            Pattern rangePattern = Pattern.compile("^(\\d+)-(\\d+)(?:/(\\d+))?$");
            Matcher rangeMatcher = rangePattern.matcher(part);
            if (rangeMatcher.matches()) {
                int lo = Integer.parseInt(rangeMatcher.group(1));
                int hi = Integer.parseInt(rangeMatcher.group(2));
                int step = rangeMatcher.group(3) != null ? Integer.parseInt(rangeMatcher.group(3)) : 1;
                // dayOfWeek: accept 7 as Sunday alias
                int effMax = (min == 0 && max == 6) ? 7 : max;
                if (lo > hi || step < 1 || lo < min || hi > effMax) return null;
                for (int i = lo; i <= hi; i += step) {
                    // Convert 7 to 0 for Sunday
                    result.add((min == 0 && max == 6 && i == 7) ? 0 : i);
                }
                continue;
            }

            // plain N
            Pattern singlePattern = Pattern.compile("^\\d+$");
            Matcher singleMatcher = singlePattern.matcher(part);
            if (singleMatcher.matches()) {
                int n = Integer.parseInt(part);
                // dayOfWeek: accept 7 as Sunday alias
                if (min == 0 && max == 6 && n == 7) n = 0;
                if (n < min || n > max) return null;
                result.add(n);
                continue;
            }

            return null;
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Compute the next LocalDateTime strictly after `from` that matches the cron fields.
     * Returns null if no match within 366 days.
     */
    public static LocalDateTime computeNextCronRun(CronFields fields, LocalDateTime from) {
        Set<Integer> minuteSet = fields.minute();
        Set<Integer> hourSet = fields.hour();
        Set<Integer> domSet = fields.dayOfMonth();
        Set<Integer> monthSet = fields.month();
        Set<Integer> dowSet = fields.dayOfWeek();

        boolean domWild = fields.dayOfMonth().size() == 31;
        boolean dowWild = fields.dayOfWeek().size() == 7;

        // Round up to the next whole minute
        LocalDateTime t = from.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);

        int maxIter = 366 * 24 * 60;
        for (int i = 0; i < maxIter; i++) {
            int month = t.getMonthValue();
            if (!monthSet.contains(month)) {
                // Jump to start of next month
                t = t.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                t = t.plusMonths(1);
                continue;
            }

            int dom = t.getDayOfMonth();
            int dow = t.getDayOfWeek().getValue() % 7; // Convert to Sunday=0

            // When both dom/dow are constrained, either match is sufficient (OR semantics)
            boolean dayMatches;
            if (domWild && dowWild) {
                dayMatches = true;
            } else if (domWild) {
                dayMatches = dowSet.contains(dow);
            } else if (dowWild) {
                dayMatches = domSet.contains(dom);
            } else {
                dayMatches = domSet.contains(dom) || dowSet.contains(dow);
            }

            if (!dayMatches) {
                // Jump to start of next day
                t = t.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                continue;
            }

            if (!hourSet.contains(t.getHour())) {
                t = t.withHour(t.getHour() + 1).withMinute(0).withSecond(0).withNano(0);
                continue;
            }

            if (!minuteSet.contains(t.getMinute())) {
                t = t.plusMinutes(1);
                continue;
            }

            return t;
        }

        return null;
    }

    /**
     * Convert cron expression to human-readable string.
     */
    public static String cronToHuman(String cron) {
        return cronToHuman(cron, false);
    }

    /**
     * Convert cron expression to human-readable string with UTC option.
     */
    public static String cronToHuman(String cron, boolean utc) {
        String[] parts = cron.trim().split("\\s+");
        if (parts.length != 5) return cron;

        String minute = parts[0];
        String hour = parts[1];
        String dayOfMonth = parts[2];
        String month = parts[3];
        String dayOfWeek = parts[4];

        // Every N minutes: */N * * * *
        Pattern everyMinPattern = Pattern.compile("^\\*/(\\d+)$");
        Matcher everyMinMatcher = everyMinPattern.matcher(minute);
        if (everyMinMatcher.matches() && hour.equals("*") && dayOfMonth.equals("*") && month.equals("*") && dayOfWeek.equals("*")) {
            int n = Integer.parseInt(everyMinMatcher.group(1));
            return n == 1 ? "Every minute" : "Every " + n + " minutes";
        }

        // Every hour: 0 * * * *
        if (minute.matches("\\d+") && hour.equals("*") && dayOfMonth.equals("*") && month.equals("*") && dayOfWeek.equals("*")) {
            int m = Integer.parseInt(minute);
            if (m == 0) return "Every hour";
            return String.format("Every hour at :%02d", m);
        }

        // Every N hours: 0 */N * * *
        Pattern everyHourPattern = Pattern.compile("^\\*/(\\d+)$");
        Matcher everyHourMatcher = everyHourPattern.matcher(hour);
        if (minute.matches("\\d+") && everyHourMatcher.matches() && dayOfMonth.equals("*") && month.equals("*") && dayOfWeek.equals("*")) {
            int n = Integer.parseInt(everyHourMatcher.group(1));
            int m = Integer.parseInt(minute);
            String suffix = m == 0 ? "" : String.format(" at :%02d", m);
            return n == 1 ? "Every hour" + suffix : "Every " + n + " hours" + suffix;
        }

        // Remaining cases need numeric minute and hour
        if (!minute.matches("\\d+") || !hour.matches("\\d+")) return cron;
        int m = Integer.parseInt(minute);
        int h = Integer.parseInt(hour);

        // Daily at specific time: M H * * *
        if (dayOfMonth.equals("*") && month.equals("*") && dayOfWeek.equals("*")) {
            return "Every day at " + formatTime(m, h, utc);
        }

        // Specific day of week: M H * * D
        if (dayOfMonth.equals("*") && month.equals("*") && dayOfWeek.matches("\\d")) {
            int dayIndex = Integer.parseInt(dayOfWeek) % 7;
            String dayName = DAY_NAMES[dayIndex];
            if (dayName != null) {
                return "Every " + dayName + " at " + formatTime(m, h, utc);
            }
        }

        // Weekdays: M H * * 1-5
        if (dayOfMonth.equals("*") && month.equals("*") && dayOfWeek.equals("1-5")) {
            return "Weekdays at " + formatTime(m, h, utc);
        }

        return cron;
    }

    /**
     * Format time for display.
     */
    private static String formatTime(int minute, int hour, boolean utc) {
        if (utc) {
            ZonedDateTime utcTime = ZonedDateTime.of(LocalDate.now(), LocalTime.of(hour, minute), ZoneOffset.UTC);
            ZonedDateTime localTime = utcTime.withZoneSameInstant(ZoneId.systemDefault());
            return localTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a z"));
        } else {
            LocalTime time = LocalTime.of(hour, minute);
            return time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
        }
    }

    /**
     * Check if a cron expression is valid.
     */
    public static boolean isValidCronExpression(String expr) {
        return parseCronExpression(expr) != null;
    }
}