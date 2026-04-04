/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FpsTracker.
 */
class FpsTrackerTest {

    private FpsTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FpsTracker();
    }

    @Test
    @DisplayName("FpsTracker FpsMetrics record")
    void fpsMetricsRecord() {
        FpsTracker.FpsMetrics metrics = new FpsTracker.FpsMetrics(60.0, 30.0);
        assertEquals(60.0, metrics.averageFps());
        assertEquals(30.0, metrics.low1PctFps());
    }

    @Test
    @DisplayName("FpsTracker getMetrics null when empty")
    void getMetricsNullWhenEmpty() {
        assertNull(tracker.getMetrics());
    }

    @Test
    @DisplayName("FpsTracker record increments frame count")
    void recordIncrementsFrameCount() {
        assertEquals(0, tracker.getFrameCount());
        tracker.record(16);
        assertEquals(1, tracker.getFrameCount());
        tracker.record(16);
        assertEquals(2, tracker.getFrameCount());
    }

    @Test
    @DisplayName("FpsTracker getMetrics returns metrics after records")
    void getMetricsAfterRecords() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            tracker.record(16);
            Thread.sleep(10);
        }

        FpsTracker.FpsMetrics metrics = tracker.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.averageFps() > 0);
    }

    @Test
    @DisplayName("FpsTracker reset clears state")
    void resetClearsState() {
        tracker.record(16);
        tracker.record(17);
        assertEquals(2, tracker.getFrameCount());

        tracker.reset();

        assertEquals(0, tracker.getFrameCount());
        assertNull(tracker.getMetrics());
    }

    @Test
    @DisplayName("FpsTracker getTotalTimeMs zero when empty")
    void getTotalTimeMsZeroWhenEmpty() {
        assertEquals(0, tracker.getTotalTimeMs());
    }

    @Test
    @DisplayName("FpsTracker getTotalTimeMs returns positive after records")
    void getTotalTimeMsAfterRecords() throws InterruptedException {
        tracker.record(16);
        Thread.sleep(50);
        tracker.record(16);

        assertTrue(tracker.getTotalTimeMs() > 0);
    }

    @Test
    @DisplayName("FpsTracker getAverageFrameTimeMs zero when empty")
    void getAverageFrameTimeMsZeroWhenEmpty() {
        assertEquals(0.0, tracker.getAverageFrameTimeMs());
    }

    @Test
    @DisplayName("FpsTracker getAverageFrameTimeMs calculates average")
    void getAverageFrameTimeMsCalculates() {
        tracker.record(10);
        tracker.record(20);
        tracker.record(30);

        assertEquals(20.0, tracker.getAverageFrameTimeMs());
    }
}