/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code FPS tracker
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * FPS (frames per second) tracker for performance monitoring.
 */
public final class FpsTracker {
    private final List<Long> frameDurations = new ArrayList<>();
    private Long firstRenderTime = null;
    private Long lastRenderTime = null;

    /**
     * FPS metrics record.
     */
    public record FpsMetrics(double averageFps, double low1PctFps) {}

    /**
     * Record a frame duration.
     */
    public void record(long durationMs) {
        long now = System.nanoTime() / 1_000_000;
        if (firstRenderTime == null) {
            firstRenderTime = now;
        }
        lastRenderTime = now;
        frameDurations.add(durationMs);
    }

    /**
     * Get FPS metrics.
     */
    public FpsMetrics getMetrics() {
        if (frameDurations.isEmpty() || firstRenderTime == null || lastRenderTime == null) {
            return null;
        }

        long totalTimeMs = lastRenderTime - firstRenderTime;
        if (totalTimeMs <= 0) {
            return null;
        }

        int totalFrames = frameDurations.size();
        double averageFps = totalFrames / (totalTimeMs / 1000.0);

        // Sort for percentile calculation
        List<Long> sorted = new ArrayList<>(frameDurations);
        sorted.sort(Collections.reverseOrder());

        int p99Index = Math.max(0, (int) Math.ceil(sorted.size() * 0.01) - 1);
        long p99FrameTimeMs = sorted.get(p99Index);
        double low1PctFps = p99FrameTimeMs > 0 ? 1000.0 / p99FrameTimeMs : 0;

        return new FpsMetrics(
                Math.round(averageFps * 100.0) / 100.0,
                Math.round(low1PctFps * 100.0) / 100.0
        );
    }

    /**
     * Reset the tracker.
     */
    public void reset() {
        frameDurations.clear();
        firstRenderTime = null;
        lastRenderTime = null;
    }

    /**
     * Get the number of recorded frames.
     */
    public int getFrameCount() {
        return frameDurations.size();
    }

    /**
     * Get total time in milliseconds.
     */
    public long getTotalTimeMs() {
        if (firstRenderTime == null || lastRenderTime == null) {
            return 0;
        }
        return lastRenderTime - firstRenderTime;
    }

    /**
     * Get average frame time in milliseconds.
     */
    public double getAverageFrameTimeMs() {
        if (frameDurations.isEmpty()) {
            return 0;
        }
        long total = frameDurations.stream().mapToLong(Long::longValue).sum();
        return (double) total / frameDurations.size();
    }
}