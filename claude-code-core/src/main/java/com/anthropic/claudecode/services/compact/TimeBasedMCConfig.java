/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/timeBasedMCConfig
 */
package com.anthropic.claudecode.services.compact;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Time-based microcompact config - Configuration for time-based microcompact.
 *
 * GrowthBook config for time-based microcompact.
 * Triggers content-clearing microcompact when the gap since the last main-loop
 * assistant message exceeds a threshold - the server-side prompt cache has
 * almost certainly expired, so the full prefix will be rewritten anyway.
 * Clearing old tool results before the request shrinks what gets rewritten.
 */
public final class TimeBasedMCConfig {
    private static final AtomicReference<TimeBasedMCConfig> config =
        new AtomicReference<>(defaults());

    /**
     * Time-based MC config record.
     */
    public record Config(
        boolean enabled,
        int gapThresholdMinutes,
        int keepRecent
    ) {
        public static Config defaults() {
            return new Config(false, 60, 5);
        }
    }

    private final boolean enabled;
    private final int gapThresholdMinutes;
    private final int keepRecent;

    /**
     * Create time-based MC config.
     */
    public TimeBasedMCConfig(boolean enabled, int gapThresholdMinutes, int keepRecent) {
        this.enabled = enabled;
        this.gapThresholdMinutes = gapThresholdMinutes;
        this.keepRecent = keepRecent;
    }

    /**
     * Create with defaults.
     */
    public TimeBasedMCConfig() {
        this(false, 60, 5);
    }

    /**
     * Get enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get gap threshold minutes.
     */
    public int getGapThresholdMinutes() {
        return gapThresholdMinutes;
    }

    /**
     * Get keep recent.
     */
    public int getKeepRecent() {
        return keepRecent;
    }

    /**
     * Get current config.
     */
    public static TimeBasedMCConfig getConfig() {
        return config.get();
    }

    /**
     * Set config.
     */
    public static void setConfig(TimeBasedMCConfig newConfig) {
        config.set(newConfig);
    }

    /**
     * Reset to defaults.
     */
    public static void reset() {
        config.set(defaults());
    }

    /**
     * Default config.
     */
    public static TimeBasedMCConfig defaults() {
        return new TimeBasedMCConfig(false, 60, 5);
    }

    /**
     * Config snapshot record.
     */
    public record ConfigSnapshot(
        boolean enabled,
        int gapThresholdMinutes,
        int keepRecent
    ) {
        public String format() {
            return String.format(
                "TimeBasedMCConfig(enabled=%s, gapThreshold=%dm, keepRecent=%d)",
                enabled, gapThresholdMinutes, keepRecent
            );
        }
    }

    /**
     * Get snapshot.
     */
    public ConfigSnapshot getSnapshot() {
        return new ConfigSnapshot(enabled, gapThresholdMinutes, keepRecent);
    }
}