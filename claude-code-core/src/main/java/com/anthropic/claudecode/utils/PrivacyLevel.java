/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/privacyLevel
 */
package com.anthropic.claudecode.utils;

/**
 * Privacy level - Controls how much nonessential network traffic and telemetry.
 *
 * Levels are ordered by restrictiveness:
 *   default < no-telemetry < essential-traffic
 *
 * - default:            Everything enabled.
 * - no-telemetry:       Analytics/telemetry disabled (Datadog, 1P events, feedback survey).
 * - essential-traffic:  ALL nonessential network traffic disabled
 *                       (telemetry + auto-updates, grove, release notes, etc.).
 */
public final class PrivacyLevel {
    private static volatile Level currentLevel = null;

    /**
     * Privacy level enum.
     */
    public enum Level {
        DEFAULT,
        NO_TELEMETRY,
        ESSENTIAL_TRAFFIC
    }

    /**
     * Get current privacy level.
     *
     * The resolved level is the most restrictive signal from:
     *   CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC  →  essential-traffic
     *   DISABLE_TELEMETRY                         →  no-telemetry
     */
    public static Level getPrivacyLevel() {
        if (currentLevel != null) {
            return currentLevel;
        }

        if (isEnvSet("CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC")) {
            return Level.ESSENTIAL_TRAFFIC;
        }
        if (isEnvSet("DISABLE_TELEMETRY")) {
            return Level.NO_TELEMETRY;
        }
        return Level.DEFAULT;
    }

    /**
     * Override privacy level (for testing).
     */
    public static void setPrivacyLevel(Level level) {
        currentLevel = level;
    }

    /**
     * Reset privacy level to environment-based.
     */
    public static void resetPrivacyLevel() {
        currentLevel = null;
    }

    /**
     * True when all nonessential network traffic should be suppressed.
     */
    public static boolean isEssentialTrafficOnly() {
        return getPrivacyLevel() == Level.ESSENTIAL_TRAFFIC;
    }

    /**
     * True when telemetry/analytics should be suppressed.
     * True at both no-telemetry and essential-traffic levels.
     */
    public static boolean isTelemetryDisabled() {
        return getPrivacyLevel() != Level.DEFAULT;
    }

    /**
     * Returns the env var name responsible for the current essential-traffic restriction,
     * or null if unrestricted.
     */
    public static String getEssentialTrafficOnlyReason() {
        if (isEnvSet("CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC")) {
            return "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC";
        }
        return null;
    }

    /**
     * Check if a feature is allowed at current privacy level.
     */
    public static boolean isFeatureAllowed(String feature) {
        Level level = getPrivacyLevel();

        switch (feature) {
            case "telemetry":
            case "analytics":
            case "datadog":
            case "first_party_events":
            case "feedback_survey":
                return level == Level.DEFAULT;

            case "auto_updates":
            case "grove":
            case "release_notes":
            case "model_capabilities":
            case "growthbook":
                return level != Level.ESSENTIAL_TRAFFIC;

            default:
                return true;
        }
    }

    /**
     * Get human-readable description of current level.
     */
    public static String getDescription() {
        switch (getPrivacyLevel()) {
            case DEFAULT:
                return "All features enabled";
            case NO_TELEMETRY:
                return "Telemetry disabled (DISABLE_TELEMETRY is set)";
            case ESSENTIAL_TRAFFIC:
                return "Essential traffic only (CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC is set)";
            default:
                return "Unknown";
        }
    }

    // Helper
    private static boolean isEnvSet(String name) {
        String value = System.getenv(name);
        if (value == null) return false;
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "yes".equalsIgnoreCase(value);
    }
}