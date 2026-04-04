/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/Spinner/types
 */
package com.anthropic.claudecode.components.spinner;

/**
 * Spinner types - Type definitions for spinner.
 */
public final class SpinnerTypes {

    /**
     * Spinner mode enum.
     */
    public enum SpinnerMode {
        DEFAULT,
        SHIMMER,
        PULSE,
        BOUNCE,
        ELASTIC
    }

    /**
     * Spinner state record.
     */
    public record SpinnerState(
        String text,
        SpinnerMode mode,
        int frame,
        long startTime,
        boolean isStalled
    ) {
        public SpinnerState(String text) {
            this(text, SpinnerMode.DEFAULT, 0, System.currentTimeMillis(), false);
        }

        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Animation frame record.
     */
    public record AnimationFrame(
        String character,
        SpinnerUtils.RGBColor color,
        int delayMs
    ) {}
}