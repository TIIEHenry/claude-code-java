/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/EffortIndicator
 */
package com.anthropic.claudecode.components.effort;

import java.util.*;

/**
 * Effort indicator - Visual effort display.
 */
public final class EffortIndicator {

    /**
     * Effort level enum.
     */
    public enum EffortLevel {
        MINIMAL(1, "Minimal", "○○○○○", 0.1),
        LOW(2, "Low", "●○○○○", 0.3),
        MEDIUM(3, "Medium", "●●○○○", 0.5),
        HIGH(4, "High", "●●●○○", 0.7),
        MAXIMUM(5, "Maximum", "●●●●●", 1.0);

        private final int value;
        private final String label;
        private final String visual;
        private final double multiplier;

        EffortLevel(int value, String label, String visual, double multiplier) {
            this.value = value;
            this.label = label;
            this.visual = visual;
            this.multiplier = multiplier;
        }

        public int getValue() { return value; }
        public String getLabel() { return label; }
        public String getVisual() { return visual; }
        public double getMultiplier() { return multiplier; }

        public static EffortLevel fromValue(int value) {
            for (EffortLevel level : values()) {
                if (level.value == value) return level;
            }
            return MEDIUM;
        }

        public static EffortLevel fromMultiplier(double multiplier) {
            if (multiplier <= 0.2) return MINIMAL;
            if (multiplier <= 0.4) return LOW;
            if (multiplier <= 0.6) return MEDIUM;
            if (multiplier <= 0.8) return HIGH;
            return MAXIMUM;
        }
    }

    /**
     * Effort config record.
     */
    public record EffortConfig(
        EffortLevel level,
        boolean showDescription,
        boolean showTimeEstimate,
        String style
    ) {
        public static EffortConfig defaultConfig() {
            return new EffortConfig(EffortLevel.MEDIUM, true, true, "dots");
        }
    }

    /**
     * Get effort display string.
     */
    public static String format(EffortLevel level) {
        return level.getVisual() + " " + level.getLabel();
    }

    /**
     * Get effort display with config.
     */
    public static String format(EffortLevel level, EffortConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(level.getVisual());

        if (config.showDescription()) {
            sb.append(" ").append(level.getLabel());
        }

        return sb.toString();
    }

    /**
     * Get colored effort display.
     */
    public static String formatColored(EffortLevel level) {
        String color = switch (level) {
            case MINIMAL -> "\033[90m";    // Dark gray
            case LOW -> "\033[37m";        // Light gray
            case MEDIUM -> "\033[33m";     // Yellow
            case HIGH -> "\033[31m";       // Red
            case MAXIMUM -> "\033[35m";    // Magenta
        };

        return color + level.getVisual() + "\033[0m";
    }

    /**
     * Effort estimate record.
     */
    public record EffortEstimate(
        EffortLevel level,
        long estimatedTimeMs,
        int estimatedTokens,
        String description,
        List<String> tasks
    ) {
        public String formatTimeEstimate() {
            long seconds = estimatedTimeMs / 1000;
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return (seconds / 60) + "m";
            return (seconds / 3600) + "h " + (seconds % 3600 / 60) + "m";
        }
    }

    /**
     * Calculate effort from task complexity.
     */
    public static EffortLevel calculateEffort(
        int fileCount,
        int lineCount,
        int dependencies,
        boolean hasTests,
        boolean hasDocumentation
    ) {
        int score = 0;

        // File count factor
        if (fileCount > 20) score += 2;
        else if (fileCount > 10) score += 1;

        // Line count factor
        if (lineCount > 500) score += 2;
        else if (lineCount > 200) score += 1;

        // Dependencies factor
        if (dependencies > 10) score += 2;
        else if (dependencies > 5) score += 1;

        // Mitigating factors
        if (hasTests) score -= 1;
        if (hasDocumentation) score -= 1;

        return fromValue(Math.max(1, Math.min(5, 3 + score - 2)));
    }

    private static EffortLevel fromValue(int value) {
        return EffortLevel.fromValue(value);
    }

    /**
     * Effort bar for terminal display.
     */
    public static String createEffortBar(EffortLevel level, int width) {
        int filled = (int) (width * level.getMultiplier());
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (int i = 0; i < width; i++) {
            if (i < filled) {
                sb.append("█");
            } else {
                sb.append("░");
            }
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * Compare effort levels.
     */
    public static int compare(EffortLevel a, EffortLevel b) {
        return Integer.compare(a.getValue(), b.getValue());
    }
}