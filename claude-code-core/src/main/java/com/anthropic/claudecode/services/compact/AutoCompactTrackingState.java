/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code auto compact tracking
 */
package com.anthropic.claudecode.services.compact;

/**
 * Tracking state for auto-compact during agentic loop.
 *
 * <p>Corresponds to AutoCompactTrackingState in TypeScript.
 */
public record AutoCompactTrackingState(
    int consecutiveFailures,
    long lastCompactTokenCount,
    boolean shouldCompact
) {
    public static AutoCompactTrackingState initial() {
        return new AutoCompactTrackingState(0, 0, false);
    }

    public AutoCompactTrackingState withFailure() {
        return new AutoCompactTrackingState(consecutiveFailures + 1, lastCompactTokenCount, shouldCompact);
    }

    public AutoCompactTrackingState withCompact(long tokenCount) {
        return new AutoCompactTrackingState(0, tokenCount, false);
    }

    public AutoCompactTrackingState requestCompact() {
        return new AutoCompactTrackingState(consecutiveFailures, lastCompactTokenCount, true);
    }

    public boolean shouldAttemptCompact() {
        return shouldCompact && consecutiveFailures < 3;
    }
}