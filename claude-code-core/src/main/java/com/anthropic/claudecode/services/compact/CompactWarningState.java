/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/compactWarningState
 */
package com.anthropic.claudecode.services.compact;

import java.util.*;
import java.time.*;

/**
 * Compact warning state - Track warning state.
 */
public final class CompactWarningState {
    private volatile boolean warningActive = false;
    private volatile CompactWarningHook.CompactWarning lastWarning = null;
    private volatile int warningCount = 0;
    private volatile Instant lastWarningTime = null;
    private volatile boolean dismissed = false;

    /**
     * Record warning.
     */
    public void recordWarning(CompactWarningHook.CompactWarning warning) {
        this.warningActive = true;
        this.lastWarning = warning;
        this.warningCount++;
        this.lastWarningTime = Instant.now();
        this.dismissed = false;
    }

    /**
     * Dismiss warning.
     */
    public void dismiss() {
        this.warningActive = false;
        this.dismissed = true;
    }

    /**
     * Clear warning.
     */
    public void clear() {
        this.warningActive = false;
        this.lastWarning = null;
        this.dismissed = false;
    }

    /**
     * Check if warning active.
     */
    public boolean isWarningActive() {
        return warningActive && !dismissed;
    }

    /**
     * Get last warning.
     */
    public CompactWarningHook.CompactWarning getLastWarning() {
        return lastWarning;
    }

    /**
     * Get warning count.
     */
    public int getWarningCount() {
        return warningCount;
    }

    /**
     * Get last warning time.
     */
    public Instant getLastWarningTime() {
        return lastWarningTime;
    }

    /**
     * Check if dismissed.
     */
    public boolean isDismissed() {
        return dismissed;
    }

    /**
     * Get time since last warning.
     */
    public Duration getTimeSinceLastWarning() {
        if (lastWarningTime == null) return Duration.ZERO;
        return Duration.between(lastWarningTime, Instant.now());
    }

    /**
     * State snapshot record.
     */
    public record StateSnapshot(
        boolean warningActive,
        CompactWarningHook.CompactWarning lastWarning,
        int warningCount,
        Instant lastWarningTime,
        boolean dismissed
    ) {
        public String format() {
            if (!warningActive) {
                return "No active warnings";
            }
            return String.format("Warning: %s (%d warnings total)",
                lastWarning.message(), warningCount);
        }
    }

    /**
     * Get state snapshot.
     */
    public StateSnapshot getSnapshot() {
        return new StateSnapshot(
            warningActive,
            lastWarning,
            warningCount,
            lastWarningTime,
            dismissed
        );
    }

    /**
     * Reset state.
     */
    public void reset() {
        warningActive = false;
        lastWarning = null;
        warningCount = 0;
        lastWarningTime = null;
        dismissed = false;
    }
}