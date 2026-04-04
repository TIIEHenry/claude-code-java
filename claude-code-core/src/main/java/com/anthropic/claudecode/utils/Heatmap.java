/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code heatmap utilities
 */
package com.anthropic.claudecode.utils;

import java.time.*;
import java.util.*;

/**
 * Generates a GitHub-style activity heatmap for the terminal.
 */
public final class Heatmap {
    private Heatmap() {}

    // Claude orange color (hex #da7756)
    private static final String CLAUDE_ORANGE = "\u001b[38;2;218;119;86m";
    private static final String ANSI_RESET = "\u001b[0m";
    private static final String ANSI_GRAY = "\u001b[90m";

    /**
     * Heatmap options.
     */
    public record HeatmapOptions(
            int terminalWidth,
            boolean showMonthLabels
    ) {
        public HeatmapOptions() {
            this(80, true);
        }
    }

    /**
     * Percentiles for intensity calculation.
     */
    private record Percentiles(int p25, int p50, int p75) {}

    /**
     * Daily activity data.
     */
    public record DailyActivity(String date, int messageCount) {}

    /**
     * Generate a GitHub-style activity heatmap.
     */
    public static String generateHeatmap(List<DailyActivity> dailyActivity) {
        return generateHeatmap(dailyActivity, new HeatmapOptions());
    }

    /**
     * Generate a GitHub-style activity heatmap with options.
     */
    public static String generateHeatmap(List<DailyActivity> dailyActivity, HeatmapOptions options) {
        int terminalWidth = options.terminalWidth();
        boolean showMonthLabels = options.showMonthLabels();

        // Day labels take 4 characters ("Mon "), calculate weeks that fit
        // Cap at 52 weeks (1 year) to match GitHub style
        int dayLabelWidth = 4;
        int availableWidth = terminalWidth - dayLabelWidth;
        int width = Math.min(52, Math.max(10, availableWidth));

        // Build activity map by date
        Map<String, DailyActivity> activityMap = new HashMap<>();
        for (DailyActivity activity : dailyActivity) {
            activityMap.put(activity.date(), activity);
        }

        // Pre-calculate percentiles
        Percentiles percentiles = calculatePercentiles(dailyActivity);

        // Calculate date range - end at today, go back N weeks
        LocalDate today = LocalDate.now();

        // Find the Sunday of the current week (start of the week containing today)
        LocalDate currentWeekStart = today.minusDays(today.getDayOfWeek().getValue() % 7);

        // Go back (width - 1) weeks from the current week start
        LocalDate startDate = currentWeekStart.minusWeeks(width - 1);

        // Generate grid (7 rows for days of week, width columns for weeks)
        String[][] grid = new String[7][width];
        List<MonthStart> monthStarts = new ArrayList<>();
        int lastMonth = -1;

        LocalDate currentDate = startDate;
        for (int week = 0; week < width; week++) {
            for (int day = 0; day < 7; day++) {
                // Don't show future dates
                if (currentDate.isAfter(today)) {
                    grid[day][week] = " ";
                    currentDate = currentDate.plusDays(1);
                    continue;
                }

                String dateStr = currentDate.toString();
                DailyActivity activity = activityMap.get(dateStr);

                // Track month changes (on day 0 = Sunday of each week)
                if (day == 0) {
                    int month = currentDate.getMonthValue() - 1;
                    if (month != lastMonth) {
                        monthStarts.add(new MonthStart(month, week));
                        lastMonth = month;
                    }
                }

                // Determine intensity level based on message count
                int intensity = getIntensity(activity != null ? activity.messageCount() : 0, percentiles);
                grid[day][week] = getHeatmapChar(intensity);

                currentDate = currentDate.plusDays(1);
            }
        }

        // Build output
        List<String> lines = new ArrayList<>();

        // Month labels
        if (showMonthLabels) {
            String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            Set<Integer> uniqueMonths = new HashSet<>();
            for (MonthStart ms : monthStarts) {
                uniqueMonths.add(ms.month());
            }

            int labelWidth = uniqueMonths.isEmpty() ? width : width / uniqueMonths.size();
            StringBuilder monthLabels = new StringBuilder();
            for (int month : uniqueMonths) {
                monthLabels.append(String.format("%-" + labelWidth + "s", monthNames[month]));
            }

            lines.add("    " + monthLabels);
        }

        // Day labels
        String[] dayLabels = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        // Grid
        for (int day = 0; day < 7; day++) {
            // Only show labels for Mon, Wed, Fri
            String label = (day == 1 || day == 3 || day == 5)
                    ? String.format("%-3s", dayLabels[day])
                    : "   ";
            StringBuilder row = new StringBuilder(label + " ");
            for (int week = 0; week < width; week++) {
                row.append(grid[day][week]);
            }
            lines.add(row.toString());
        }

        // Legend
        lines.add("");
        lines.add("    Less " +
                claudeOrange("░") + " " +
                claudeOrange("▒") + " " +
                claudeOrange("▓") + " " +
                claudeOrange("█") + " More");

        return String.join("\n", lines);
    }

    /**
     * Calculate percentiles from activity data.
     */
    private static Percentiles calculatePercentiles(List<DailyActivity> dailyActivity) {
        List<Integer> counts = dailyActivity.stream()
                .map(DailyActivity::messageCount)
                .filter(c -> c > 0)
                .sorted()
                .toList();

        if (counts.isEmpty()) return null;

        return new Percentiles(
                counts.get((int) Math.floor(counts.size() * 0.25)),
                counts.get((int) Math.floor(counts.size() * 0.5)),
                counts.get((int) Math.floor(counts.size() * 0.75))
        );
    }

    /**
     * Get intensity level based on message count.
     */
    private static int getIntensity(int messageCount, Percentiles percentiles) {
        if (messageCount == 0 || percentiles == null) return 0;

        if (messageCount >= percentiles.p75()) return 4;
        if (messageCount >= percentiles.p50()) return 3;
        if (messageCount >= percentiles.p25()) return 2;
        return 1;
    }

    /**
     * Get heatmap character for intensity.
     */
    private static String getHeatmapChar(int intensity) {
        switch (intensity) {
            case 0: return ANSI_GRAY + "·" + ANSI_RESET;
            case 1: return claudeOrange("░");
            case 2: return claudeOrange("▒");
            case 3: return claudeOrange("▓");
            case 4: return claudeOrange("█");
            default: return ANSI_GRAY + "·" + ANSI_RESET;
        }
    }

    /**
     * Apply Claude orange color.
     */
    private static String claudeOrange(String text) {
        return CLAUDE_ORANGE + text + ANSI_RESET;
    }

    /**
     * Month start tracking.
     */
    private record MonthStart(int month, int week) {}
}