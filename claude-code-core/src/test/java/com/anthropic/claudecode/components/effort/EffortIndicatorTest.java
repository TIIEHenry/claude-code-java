/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.effort;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for EffortIndicator.
 */
@DisplayName("EffortIndicator Tests")
class EffortIndicatorTest {

    @Test
    @DisplayName("EffortLevel enum has correct values")
    void effortLevelEnumHasCorrectValues() {
        EffortIndicator.EffortLevel[] levels = EffortIndicator.EffortLevel.values();

        assertEquals(5, levels.length);
        assertTrue(Arrays.asList(levels).contains(EffortIndicator.EffortLevel.MINIMAL));
        assertTrue(Arrays.asList(levels).contains(EffortIndicator.EffortLevel.LOW));
        assertTrue(Arrays.asList(levels).contains(EffortIndicator.EffortLevel.MEDIUM));
        assertTrue(Arrays.asList(levels).contains(EffortIndicator.EffortLevel.HIGH));
        assertTrue(Arrays.asList(levels).contains(EffortIndicator.EffortLevel.MAXIMUM));
    }

    @Test
    @DisplayName("EffortLevel getValue works correctly")
    void effortLevelGetValueWorksCorrectly() {
        assertEquals(1, EffortIndicator.EffortLevel.MINIMAL.getValue());
        assertEquals(2, EffortIndicator.EffortLevel.LOW.getValue());
        assertEquals(3, EffortIndicator.EffortLevel.MEDIUM.getValue());
        assertEquals(4, EffortIndicator.EffortLevel.HIGH.getValue());
        assertEquals(5, EffortIndicator.EffortLevel.MAXIMUM.getValue());
    }

    @Test
    @DisplayName("EffortLevel getLabel works correctly")
    void effortLevelGetLabelWorksCorrectly() {
        assertEquals("Minimal", EffortIndicator.EffortLevel.MINIMAL.getLabel());
        assertEquals("Low", EffortIndicator.EffortLevel.LOW.getLabel());
        assertEquals("Medium", EffortIndicator.EffortLevel.MEDIUM.getLabel());
        assertEquals("High", EffortIndicator.EffortLevel.HIGH.getLabel());
        assertEquals("Maximum", EffortIndicator.EffortLevel.MAXIMUM.getLabel());
    }

    @Test
    @DisplayName("EffortLevel getVisual works correctly")
    void effortLevelGetVisualWorksCorrectly() {
        assertEquals("○○○○○", EffortIndicator.EffortLevel.MINIMAL.getVisual());
        assertEquals("●○○○○", EffortIndicator.EffortLevel.LOW.getVisual());
        assertEquals("●●○○○", EffortIndicator.EffortLevel.MEDIUM.getVisual());
        assertEquals("●●●○○", EffortIndicator.EffortLevel.HIGH.getVisual());
        assertEquals("●●●●●", EffortIndicator.EffortLevel.MAXIMUM.getVisual());
    }

    @Test
    @DisplayName("EffortLevel getMultiplier works correctly")
    void effortLevelGetMultiplierWorksCorrectly() {
        assertEquals(0.1, EffortIndicator.EffortLevel.MINIMAL.getMultiplier());
        assertEquals(0.3, EffortIndicator.EffortLevel.LOW.getMultiplier());
        assertEquals(0.5, EffortIndicator.EffortLevel.MEDIUM.getMultiplier());
        assertEquals(0.7, EffortIndicator.EffortLevel.HIGH.getMultiplier());
        assertEquals(1.0, EffortIndicator.EffortLevel.MAXIMUM.getMultiplier());
    }

    @Test
    @DisplayName("EffortLevel fromValue works correctly")
    void effortLevelFromValueWorksCorrectly() {
        assertEquals(EffortIndicator.EffortLevel.MINIMAL, EffortIndicator.EffortLevel.fromValue(1));
        assertEquals(EffortIndicator.EffortLevel.MEDIUM, EffortIndicator.EffortLevel.fromValue(3));
        assertEquals(EffortIndicator.EffortLevel.MAXIMUM, EffortIndicator.EffortLevel.fromValue(5));
        assertEquals(EffortIndicator.EffortLevel.MEDIUM, EffortIndicator.EffortLevel.fromValue(99));
    }

    @Test
    @DisplayName("EffortLevel fromMultiplier works correctly")
    void effortLevelFromMultiplierWorksCorrectly() {
        assertEquals(EffortIndicator.EffortLevel.MINIMAL, EffortIndicator.EffortLevel.fromMultiplier(0.1));
        assertEquals(EffortIndicator.EffortLevel.LOW, EffortIndicator.EffortLevel.fromMultiplier(0.3));
        assertEquals(EffortIndicator.EffortLevel.MEDIUM, EffortIndicator.EffortLevel.fromMultiplier(0.5));
        assertEquals(EffortIndicator.EffortLevel.HIGH, EffortIndicator.EffortLevel.fromMultiplier(0.7));
        assertEquals(EffortIndicator.EffortLevel.MAXIMUM, EffortIndicator.EffortLevel.fromMultiplier(0.9));
    }

    @Test
    @DisplayName("EffortConfig defaultConfig works correctly")
    void effortConfigDefaultConfigWorksCorrectly() {
        EffortIndicator.EffortConfig config = EffortIndicator.EffortConfig.defaultConfig();

        assertEquals(EffortIndicator.EffortLevel.MEDIUM, config.level());
        assertTrue(config.showDescription());
        assertTrue(config.showTimeEstimate());
        assertEquals("dots", config.style());
    }

    @Test
    @DisplayName("EffortIndicator format works correctly")
    void effortIndicatorFormatWorksCorrectly() {
        String formatted = EffortIndicator.format(EffortIndicator.EffortLevel.HIGH);

        assertEquals("●●●○○ High", formatted);
    }

    @Test
    @DisplayName("EffortIndicator format with config works correctly")
    void effortIndicatorFormatWithConfigWorksCorrectly() {
        EffortIndicator.EffortConfig config = new EffortIndicator.EffortConfig(
            EffortIndicator.EffortLevel.HIGH, true, false, "dots"
        );

        String formatted = EffortIndicator.format(EffortIndicator.EffortLevel.HIGH, config);

        assertEquals("●●●○○ High", formatted);
    }

    @Test
    @DisplayName("EffortIndicator formatColored works correctly")
    void effortIndicatorFormatColoredWorksCorrectly() {
        String colored = EffortIndicator.formatColored(EffortIndicator.EffortLevel.HIGH);

        assertTrue(colored.contains("●●●○○"));
        assertTrue(colored.contains("\033["));
    }

    @Test
    @DisplayName("EffortEstimate record works correctly")
    void effortEstimateRecordWorksCorrectly() {
        EffortIndicator.EffortEstimate estimate = new EffortIndicator.EffortEstimate(
            EffortIndicator.EffortLevel.HIGH,
            3600000L,
            5000,
            "Complex task",
            List.of("task1", "task2")
        );

        assertEquals(EffortIndicator.EffortLevel.HIGH, estimate.level());
        assertEquals(3600000L, estimate.estimatedTimeMs());
        assertEquals(5000, estimate.estimatedTokens());
        assertEquals("Complex task", estimate.description());
        assertEquals(2, estimate.tasks().size());
    }

    @Test
    @DisplayName("EffortEstimate formatTimeEstimate works correctly")
    void effortEstimateFormatTimeEstimateWorksCorrectly() {
        EffortIndicator.EffortEstimate seconds = new EffortIndicator.EffortEstimate(
            EffortIndicator.EffortLevel.MINIMAL, 30000L, 0, null, List.of()
        );
        EffortIndicator.EffortEstimate minutes = new EffortIndicator.EffortEstimate(
            EffortIndicator.EffortLevel.MEDIUM, 1800000L, 0, null, List.of()
        );
        EffortIndicator.EffortEstimate hours = new EffortIndicator.EffortEstimate(
            EffortIndicator.EffortLevel.HIGH, 7200000L, 0, null, List.of()
        );

        assertEquals("30s", seconds.formatTimeEstimate());
        assertEquals("30m", minutes.formatTimeEstimate());
        assertEquals("2h 0m", hours.formatTimeEstimate());
    }

    @Test
    @DisplayName("EffortIndicator calculateEffort works correctly")
    void effortIndicatorCalculateEffortWorksCorrectly() {
        // Simple task
        EffortIndicator.EffortLevel simple = EffortIndicator.calculateEffort(
            5, 100, 2, true, true
        );
        assertTrue(simple.getValue() <= EffortIndicator.EffortLevel.MEDIUM.getValue());

        // Complex task
        EffortIndicator.EffortLevel complex = EffortIndicator.calculateEffort(
            30, 1000, 15, false, false
        );
        assertTrue(complex.getValue() >= EffortIndicator.EffortLevel.HIGH.getValue());
    }

    @Test
    @DisplayName("EffortIndicator createEffortBar works correctly")
    void effortIndicatorCreateEffortBarWorksCorrectly() {
        String bar = EffortIndicator.createEffortBar(EffortIndicator.EffortLevel.MEDIUM, 10);

        assertTrue(bar.startsWith("["));
        assertTrue(bar.endsWith("]"));
        assertEquals(12, bar.length()); // 10 chars + 2 brackets
    }

    @Test
    @DisplayName("EffortIndicator compare works correctly")
    void effortIndicatorCompareWorksCorrectly() {
        assertTrue(EffortIndicator.compare(EffortIndicator.EffortLevel.LOW, EffortIndicator.EffortLevel.HIGH) < 0);
        assertTrue(EffortIndicator.compare(EffortIndicator.EffortLevel.HIGH, EffortIndicator.EffortLevel.LOW) > 0);
        assertEquals(0, EffortIndicator.compare(EffortIndicator.EffortLevel.MEDIUM, EffortIndicator.EffortLevel.MEDIUM));
    }
}