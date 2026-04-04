/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Heatmap.
 */
class HeatmapTest {

    @Test
    @DisplayName("Heatmap HeatmapOptions record defaults")
    void heatmapOptionsDefaults() {
        Heatmap.HeatmapOptions opts = new Heatmap.HeatmapOptions();
        assertEquals(80, opts.terminalWidth());
        assertTrue(opts.showMonthLabels());
    }

    @Test
    @DisplayName("Heatmap HeatmapOptions record")
    void heatmapOptionsRecord() {
        Heatmap.HeatmapOptions opts = new Heatmap.HeatmapOptions(100, false);
        assertEquals(100, opts.terminalWidth());
        assertFalse(opts.showMonthLabels());
    }

    @Test
    @DisplayName("Heatmap DailyActivity record")
    void dailyActivityRecord() {
        Heatmap.DailyActivity activity = new Heatmap.DailyActivity("2024-01-01", 10);
        assertEquals("2024-01-01", activity.date());
        assertEquals(10, activity.messageCount());
    }

    @Test
    @DisplayName("Heatmap generateHeatmap empty activity")
    void generateHeatmapEmpty() {
        String heatmap = Heatmap.generateHeatmap(new ArrayList<>());
        assertNotNull(heatmap);
        assertTrue(heatmap.contains("Less"));
        assertTrue(heatmap.contains("More"));
    }

    @Test
    @DisplayName("Heatmap generateHeatmap with activity")
    void generateHeatmapWithActivity() {
        List<Heatmap.DailyActivity> activities = List.of(
            new Heatmap.DailyActivity("2024-01-01", 5),
            new Heatmap.DailyActivity("2024-01-02", 10),
            new Heatmap.DailyActivity("2024-01-03", 15)
        );
        String heatmap = Heatmap.generateHeatmap(activities);
        assertNotNull(heatmap);
        assertTrue(heatmap.contains("Less"));
        assertTrue(heatmap.contains("More"));
    }

    @Test
    @DisplayName("Heatmap generateHeatmap with options")
    void generateHeatmapWithOptions() {
        Heatmap.HeatmapOptions opts = new Heatmap.HeatmapOptions(60, false);
        String heatmap = Heatmap.generateHeatmap(new ArrayList<>(), opts);
        assertNotNull(heatmap);
    }

    @Test
    @DisplayName("Heatmap generateHeatmap shows day labels")
    void generateHeatmapShowsDayLabels() {
        String heatmap = Heatmap.generateHeatmap(new ArrayList<>());
        assertTrue(heatmap.contains("Mon") || heatmap.contains("Wed") || heatmap.contains("Fri"));
    }

    @Test
    @DisplayName("Heatmap generateHeatmap with zero counts")
    void generateHeatmapZeroCounts() {
        List<Heatmap.DailyActivity> activities = List.of(
            new Heatmap.DailyActivity("2024-01-01", 0),
            new Heatmap.DailyActivity("2024-01-02", 0)
        );
        String heatmap = Heatmap.generateHeatmap(activities);
        assertNotNull(heatmap);
    }

    @Test
    @DisplayName("Heatmap generateHeatmap with high counts")
    void generateHeatmapHighCounts() {
        List<Heatmap.DailyActivity> activities = List.of(
            new Heatmap.DailyActivity("2024-01-01", 100),
            new Heatmap.DailyActivity("2024-01-02", 200),
            new Heatmap.DailyActivity("2024-01-03", 300)
        );
        String heatmap = Heatmap.generateHeatmap(activities);
        assertNotNull(heatmap);
    }
}