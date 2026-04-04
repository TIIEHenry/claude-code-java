/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/denialTracking.ts
 */
package com.anthropic.claudecode.utils.permissions;

/**
 * Denial tracking state for permission classifiers.
 *
 * Tracks consecutive denials and total denials to determine
 * when to fall back to prompting.
 */
public record DenialTrackingState(
    int consecutiveDenials,
    int totalDenials
) {
    /**
     * Denial limits constants.
     */
    public static final int MAX_CONSECUTIVE = 3;
    public static final int MAX_TOTAL = 20;

    /**
     * Create initial denial tracking state.
     */
    public static DenialTrackingState create() {
        return new DenialTrackingState(0, 0);
    }

    /**
     * Record a denial - increment both counters.
     */
    public DenialTrackingState recordDenial() {
        return new DenialTrackingState(
            consecutiveDenials + 1,
            totalDenials + 1
        );
    }

    /**
     * Record a success - reset consecutive denials.
     */
    public DenialTrackingState recordSuccess() {
        if (consecutiveDenials == 0) return this;
        return new DenialTrackingState(0, totalDenials);
    }

    /**
     * Check if should fall back to prompting.
     * Triggers when either limit is exceeded.
     */
    public boolean shouldFallbackToPrompting() {
        return consecutiveDenials >= MAX_CONSECUTIVE ||
               totalDenials >= MAX_TOTAL;
    }

    /**
     * Check if consecutive limit exceeded.
     */
    public boolean isConsecutiveLimitExceeded() {
        return consecutiveDenials >= MAX_CONSECUTIVE;
    }

    /**
     * Check if total limit exceeded.
     */
    public boolean isTotalLimitExceeded() {
        return totalDenials >= MAX_TOTAL;
    }

    /**
     * Reset all counters.
     */
    public DenialTrackingState reset() {
        return new DenialTrackingState(0, 0);
    }
}