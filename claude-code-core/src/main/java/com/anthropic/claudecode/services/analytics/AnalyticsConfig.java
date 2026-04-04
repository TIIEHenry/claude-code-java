/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/analytics/config.ts
 */
package com.anthropic.claudecode.services.analytics;

import com.anthropic.claudecode.constants.Keys;

/**
 * Shared analytics configuration.
 */
public final class AnalyticsConfig {
    private AnalyticsConfig() {}

    // Event sampling config
    private static volatile double eventSamplingRate = 1.0;
    private static volatile boolean sinkKilled = false;

    /**
     * Check if analytics operations should be disabled.
     *
     * Analytics is disabled in the following cases:
     * - Test environment
     * - Third-party cloud providers (Bedrock/Vertex)
     * - Privacy level is no-telemetry or essential-traffic
     */
    public static boolean isAnalyticsDisabled() {
        return "test".equals(System.getenv("NODE_ENV")) ||
               isEnvTruthy(System.getenv("CLAUDE_CODE_USE_BEDROCK")) ||
               isEnvTruthy(System.getenv("CLAUDE_CODE_USE_VERTEX")) ||
               isEnvTruthy(System.getenv("CLAUDE_CODE_USE_FOUNDRY")) ||
               isTelemetryDisabled();
    }

    /**
     * Check if 1P event logging is enabled.
     */
    public static boolean is1PEventLoggingEnabled() {
        return !isAnalyticsDisabled() && !isSinkKilled();
    }

    /**
     * Check if the feedback survey should be suppressed.
     */
    public static boolean isFeedbackSurveyDisabled() {
        return "test".equals(System.getenv("NODE_ENV")) || isTelemetryDisabled();
    }

    /**
     * Get GrowthBook client key.
     */
    public static String getGrowthBookClientKey() {
        return Keys.getGrowthBookClientKey();
    }

    /**
     * Get event sampling rate.
     */
    public static double getEventSamplingRate() {
        return eventSamplingRate;
    }

    /**
     * Set event sampling rate.
     */
    public static void setEventSamplingRate(double rate) {
        eventSamplingRate = rate;
    }

    /**
     * Check if the analytics sink is killed.
     */
    public static boolean isSinkKilled() {
        return sinkKilled;
    }

    /**
     * Set sink killed status.
     */
    public static void setSinkKilled(boolean killed) {
        sinkKilled = killed;
    }

    /**
     * Get event sampling config.
     */
    public static EventSamplingConfig getEventSamplingConfig() {
        return new EventSamplingConfig(eventSamplingRate, sinkKilled);
    }

    /**
     * Event sampling configuration.
     */
    public record EventSamplingConfig(double samplingRate, boolean sinkKilled) {}

    private static boolean isEnvTruthy(String value) {
        if (value == null) return false;
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static boolean isTelemetryDisabled() {
        String privacy = System.getenv("CLAUDE_CODE_PRIVACY_LEVEL");
        return "no-telemetry".equals(privacy) || "essential-traffic".equals(privacy);
    }
}