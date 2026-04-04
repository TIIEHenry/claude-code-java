/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code effort utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Effort level utilities for controlling model thinking depth.
 */
public final class EffortUtils {
    private EffortUtils() {}

    /**
     * Effort levels supported by the API.
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
            for (EffortLevel level : values()) {
                if (level.value.equalsIgnoreCase(value)) {
                    return level;
                }
            }
            return HIGH; // Default fallback
        }
    }

    /**
     * Effort value - can be a named level or numeric value.
     */
    public sealed interface EffortValue permits EffortValue.Named, EffortValue.Numeric {
        record Named(EffortLevel level) implements EffortValue {}
        record Numeric(int value) implements EffortValue {}
    }

    /**
     * Check if model supports effort parameter.
     */
    public static boolean modelSupportsEffort(String model) {
        if (isEnvTruthy("CLAUDE_CODE_ALWAYS_ENABLE_EFFORT")) {
            return true;
        }

        String m = model.toLowerCase();

        // Supported by Claude 4.6 models
        if (m.contains("opus-4-6") || m.contains("sonnet-4-6")) {
            return true;
        }

        // Exclude known legacy models
        if (m.contains("haiku") || m.contains("sonnet") || m.contains("opus")) {
            return false;
        }

        // Default to true for first-party API
        return isFirstPartyAPI();
    }

    /**
     * Check if model supports 'max' effort level.
     */
    public static boolean modelSupportsMaxEffort(String model) {
        String m = model.toLowerCase();
        if (m.contains("opus-4-6")) {
            return true;
        }
        return false;
    }

    /**
     * Parse effort value from string or number.
     */
    public static EffortValue parseEffortValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            int num = ((Number) value).intValue();
            if (isValidNumericEffort(num)) {
                return new EffortValue.Numeric(num);
            }
            return null;
        }

        String str = value.toString().toLowerCase();

        // Check for named levels
        try {
            EffortLevel level = EffortLevel.fromString(str);
            return new EffortValue.Named(level);
        } catch (Exception e) {
            // Not a named level
        }

        // Try parsing as number
        try {
            int num = Integer.parseInt(str);
            if (isValidNumericEffort(num)) {
                return new EffortValue.Numeric(num);
            }
        } catch (NumberFormatException e) {
            // Not a number
        }

        return null;
    }

    /**
     * Check if numeric effort value is valid.
     */
    public static boolean isValidNumericEffort(int value) {
        return value >= 0 && value <= 200;
    }

    /**
     * Convert effort value to named level.
     */
    public static EffortLevel convertToLevel(EffortValue value) {
        if (value instanceof EffortValue.Named named) {
            return named.level();
        }

        if (value instanceof EffortValue.Numeric numeric) {
            int v = numeric.value();
            if (v <= 50) return EffortLevel.LOW;
            if (v <= 85) return EffortLevel.MEDIUM;
            if (v <= 100) return EffortLevel.HIGH;
            return EffortLevel.MAX;
        }

        return EffortLevel.HIGH;
    }

    /**
     * Get effort suffix for display.
     */
    public static String getEffortSuffix(EffortValue value) {
        if (value == null) return "";
        EffortLevel level = convertToLevel(value);
        return " with " + level.getValue() + " effort";
    }

    /**
     * Get the default effort level for a model.
     */
    public static EffortLevel getDefaultEffortForModel(String model) {
        String m = model.toLowerCase();

        // Opus 4.6 defaults to medium
        if (m.contains("opus-4-6")) {
            return EffortLevel.MEDIUM;
        }

        // Default to high (API default)
        return EffortLevel.HIGH;
    }

    /**
     * Resolve the applied effort value.
     * Follows precedence: env → appState → model default
     */
    public static EffortValue resolveAppliedEffort(String model, EffortValue appStateValue) {
        // Check environment override
        EffortValue envOverride = getEffortEnvOverride();
        if (envOverride != null) {
            // null means explicitly unset
            return envOverride;
        }

        EffortValue resolved = appStateValue != null ? appStateValue :
                new EffortValue.Named(getDefaultEffortForModel(model));

        // API rejects 'max' on non-Opus-4.6 models — downgrade to 'high'
        if (resolved instanceof EffortValue.Named named &&
            named.level() == EffortLevel.MAX &&
            !modelSupportsMaxEffort(model)) {
            return new EffortValue.Named(EffortLevel.HIGH);
        }

        return resolved;
    }

    /**
     * Get effort override from environment.
     */
    private static EffortValue getEffortEnvOverride() {
        String envValue = System.getenv("CLAUDE_CODE_EFFORT_LEVEL");
        if (envValue == null || envValue.isEmpty()) {
            return null;
        }

        String lower = envValue.toLowerCase();
        if ("unset".equals(lower) || "auto".equals(lower)) {
            // Explicitly unset
            return null;
        }

        return parseEffortValue(envValue);
    }

    /**
     * Get description for effort level.
     */
    public static String getDescription(EffortLevel level) {
        return level.getDescription();
    }

    /**
     * Get description for effort value.
     */
    public static String getDescription(EffortValue value) {
        if (value instanceof EffortValue.Named named) {
            return named.level().getDescription();
        }
        if (value instanceof EffortValue.Numeric numeric) {
            return "Numeric effort value of " + numeric.value();
        }
        return "Standard implementation approach";
    }

    // Helper methods
    private static boolean isEnvTruthy(String name) {
        String value = System.getenv(name);
        if (value == null) return false;
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "yes".equalsIgnoreCase(value);
    }

    private static boolean isFirstPartyAPI() {
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        return baseUrl == null || baseUrl.contains("anthropic.com");
    }
}