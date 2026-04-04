/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/Spinner/useShimmerAnimation
 */
package com.anthropic.claudecode.components.spinner;

import java.util.*;
import java.util.concurrent.*;

/**
 * Shimmer animation - Shimmer effect for spinner characters.
 */
public final class ShimmerAnimation {
    private final ScheduledExecutorService scheduler;
    private final List<ShimmerFrame> frames;
    private volatile int currentFrame = 0;
    private volatile boolean running = false;

    /**
     * Create shimmer animation.
     */
    public ShimmerAnimation() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.frames = generateFrames();
    }

    /**
     * Start the animation.
     */
    public void start(int intervalMs) {
        if (running) return;
        running = true;

        scheduler.scheduleAtFixedRate(() -> {
            currentFrame = (currentFrame + 1) % frames.size();
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop the animation.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
    }

    /**
     * Get current frame.
     */
    public ShimmerFrame getCurrentFrame() {
        return frames.get(currentFrame);
    }

    /**
     * Get shimmer color for position.
     */
    public SpinnerUtils.RGBColor getShimmerColor(int position, int total) {
        double progress = (double) position / total;
        double hue = (progress * 360 + currentFrame * 10) % 360;
        return SpinnerUtils.hueToRgb(hue);
    }

    /**
     * Check if animation is running.
     */
    public boolean isRunning() {
        return running;
    }

    private List<ShimmerFrame> generateFrames() {
        List<ShimmerFrame> frameList = new ArrayList<>();

        // Generate 36 frames for smooth animation
        for (int i = 0; i < 36; i++) {
            double hue = (i * 10) % 360;
            SpinnerUtils.RGBColor color = SpinnerUtils.hueToRgb(hue);
            frameList.add(new ShimmerFrame(color, i * 50));
        }

        return frameList;
    }

    /**
     * Shimmer frame record.
     */
    public record ShimmerFrame(SpinnerUtils.RGBColor color, long timestamp) {}
}