/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/Spinner/useStalledAnimation
 */
package com.anthropic.claudecode.components.spinner;

import java.util.*;
import java.util.concurrent.*;

/**
 * Stalled animation - Stalled indicator animation.
 */
public final class StalledAnimation {
    private final ScheduledExecutorService scheduler;
    private volatile boolean isStalled = false;
    private volatile int stallIndicator = 0;
    private volatile long lastActivityTime;
    private final long stallThresholdMs;

    /**
     * Create stalled animation with default threshold.
     */
    public StalledAnimation() {
        this(30000); // 30 seconds default
    }

    /**
     * Create stalled animation with custom threshold.
     */
    public StalledAnimation(long stallThresholdMs) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.stallThresholdMs = stallThresholdMs;
        this.lastActivityTime = System.currentTimeMillis();

        // Start monitoring
        scheduler.scheduleAtFixedRate(this::checkStalled, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Record activity.
     */
    public void recordActivity() {
        lastActivityTime = System.currentTimeMillis();
        isStalled = false;
        stallIndicator = 0;
    }

    /**
     * Check if stalled.
     */
    private void checkStalled() {
        long elapsed = System.currentTimeMillis() - lastActivityTime;

        if (elapsed > stallThresholdMs) {
            isStalled = true;
            stallIndicator = (stallIndicator + 1) % 4;
        }
    }

    /**
     * Get stalled status.
     */
    public boolean isStalled() {
        return isStalled;
    }

    /**
     * Get stall indicator.
     */
    public String getStallIndicator() {
        if (!isStalled) return "";

        return switch (stallIndicator) {
            case 0 -> "⠋";
            case 1 -> "⠙";
            case 2 -> "⠹";
            case 3 -> "⠸";
            default -> "⠋";
        };
    }

    /**
     * Get stalled message.
     */
    public String getStalledMessage() {
        if (!isStalled) return "";

        long elapsed = (System.currentTimeMillis() - lastActivityTime) / 1000;
        return String.format("Still working... (%ds)", elapsed);
    }

    /**
     * Stop the animation.
     */
    public void stop() {
        scheduler.shutdown();
    }

    /**
     * Get time since last activity.
     */
    public long getTimeSinceLastActivity() {
        return System.currentTimeMillis() - lastActivityTime;
    }

    /**
     * Format elapsed time.
     */
    public static String formatElapsed(long elapsedMs) {
        long seconds = elapsedMs / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m " + seconds + "s";
        }

        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }
}