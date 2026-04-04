/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code effort utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Effort level utilities for controlling model reasoning depth.
 */
public final class Effort {
    private Effort() {}

    /**
     * Effort level enum.
     */
    public enum EffortLevel {
        LOW("low", "Quick, straightforward implementation with minimal overhead"),
        MEDIUM("medium", "Balanced approach with standard implementation and testing"),
        HIGH("high", "Comprehensive implementation with extensive testing and documentation"),
        MAX("max", "Maximum capability with deepest reasoning (Opus 4.6 only)");

        private final String value;
        private final String description;

        EffortLevel(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EffortLevel fromString(String value) {
            if (value == null) return null;
            for (EffortLevel level : values()) {
                if (level.value.equalsIgnoreCase(value)) {
                    return level;
                }
            }
            return HIGH; // Default fallback
        }
    }

    /**
     * Effort value - can be a level or numeric.
     */
    public sealed interface EffortValue permits EffortLevelValue, NumericEffortValue {}

    public record EffortLevelValue(EffortLevel level) implements EffortValue {}
    public record NumericEffortValue(int value) implements EffortValue {}

    private static final List<String> EFFORT_LEVELS = List.of("low", "medium", "high", "max");

    /**
     * Check if model supports effort parameter.
     */
    public static boolean modelSupportsEffort(String model) {
        if (model == null || model.isEmpty()) return false;

        if (EnvUtils.isTruthy(System.getenv("CLAUDE_CODE_ALWAYS_ENABLE_EFFORT"))) {
            return true;
        }

        String m = model.toLowerCase();
        // Supported by a subset of Claude 4 models
        if (m.contains("opus-4-6") || m.contains("sonnet-4-6")) {
            return true;
        }

        // Exclude legacy models
        if (m.contains("haiku") || m.contains("sonnet") || m.contains("opus")) {
            return false;
        }

        // Default to true for first-party
        return true;
    }

    /**
     * Check if model supports 'max' effort.
     */
    public static boolean modelSupportsMaxEffort(String model) {
        if (model == null || model.isEmpty()) return false;

        String m = model.toLowerCase();
        return m.contains("opus-4-6");
    }

    /**
     * Check if value is a valid effort level.
     */
    public static boolean isEffortLevel(String value) {
        return value != null && EFFORT_LEVELS.contains(value.toLowerCase());
    }

    /**
     * Parse effort value from string or number.
     */
    public static EffortValue parseEffortValue(Object value) {
        if (value == null) return null;

        if (value instanceof Number) {
            int num = ((Number) value).intValue();
            if (isValidNumericEffort(num)) {
                return new NumericEffortValue(num);
            }
            return null;
        }

        String str = value.toString().toLowerCase();
        if (isEffortLevel(str)) {
            return new EffortLevelValue(EffortLevel.fromString(str));
        }

        try {
            int num = Integer.parseInt(str);
            if (isValidNumericEffort(num)) {
                return new NumericEffortValue(num);
            }
        } catch (NumberFormatException e) {
            // Ignore
        }

        return null;
    }

    /**
     * Check if numeric effort is valid.
     */
    public static boolean isValidNumericEffort(int value) {
        return value >= 0 && value <= 100;
    }

    /**
     * Convert effort value to level.
     */
    public static EffortLevel convertEffortValueToLevel(EffortValue value) {
        if (value instanceof EffortLevelValue levelValue) {
            return levelValue.level();
        }
        if (value instanceof NumericEffortValue numValue) {
            int v = numValue.value();
            if (v <= 50) return EffortLevel.LOW;
            if (v <= 85) return EffortLevel.MEDIUM;
            if (v <= 100) return EffortLevel.HIGH;
            return EffortLevel.MAX;
        }
        return EffortLevel.HIGH;
    }

    /**
     * Get default effort for model.
     */
    public static EffortLevel getDefaultEffortForModel(String model) {
        if (model == null || model.isEmpty()) {
            return EffortLevel.HIGH;
        }

        String m = model.toLowerCase();

        // Default effort on Opus 4.6 to medium
        if (m.contains("opus-4-6")) {
            return EffortLevel.MEDIUM;
        }

        // Check for ultrathink mode
        if (Thinking.isUltrathinkEnabled() && modelSupportsEffort(model)) {
            return EffortLevel.MEDIUM;
        }

        return EffortLevel.HIGH;
    }

    /**
     * Get effort level description.
     */
    public static String getEffortLevelDescription(EffortLevel level) {
        return level != null ? level.getDescription() : "Unknown effort level";
    }

    /**
     * Get environment override for effort level.
     */
    public static EffortValue getEffortEnvOverride() {
        String envOverride = System.getenv("CLAUDE_CODE_EFFORT_LEVEL");
        if (envOverride == null || envOverride.isEmpty()) {
            return null;
        }

        String lower = envOverride.toLowerCase();
        if (lower.equals("unset") || lower.equals("auto")) {
            return null; // Explicitly unset
        }

        return parseEffortValue(envOverride);
    }

    /**
     * Resolve the effort value to use for a model.
     */
    public static EffortLevel resolveAppliedEffort(String model, EffortValue appStateEffort) {
        EffortValue envOverride = getEffortEnvOverride();
        if (envOverride == null && !"unset".equalsIgnoreCase(System.getenv("CLAUDE_CODE_EFFORT_LEVEL"))) {
            // env is null because it's not set, not because it's explicitly unset
        } else if (envOverride == null) {
            // Explicitly unset via environment
            return null;
        }

        EffortValue resolved = envOverride != null ? envOverride : appStateEffort;
        if (resolved == null) {
            resolved = new EffortLevelValue(getDefaultEffortForModel(model));
        }

        EffortLevel level = convertEffortValueToLevel(resolved);

        // API rejects 'max' on non-Opus-4.6 models
        if (level == EffortLevel.MAX && !modelSupportsMaxEffort(model)) {
            return EffortLevel.HIGH;
        }

        return level;
    }

    /**
     * Get displayed effort level.
     */
    public static EffortLevel getDisplayedEffortLevel(String model, EffortValue appStateEffort) {
        EffortLevel resolved = resolveAppliedEffort(model, appStateEffort);
        return resolved != null ? resolved : EffortLevel.HIGH;
    }

    /**
     * Get effort suffix for display.
     */
    public static String getEffortSuffix(String model, EffortValue effortValue) {
        if (effortValue == null) return "";
        EffortLevel resolved = resolveAppliedEffort(model, effortValue);
        if (resolved == null) return "";
        return " with " + resolved.getValue() + " effort";
    }
}