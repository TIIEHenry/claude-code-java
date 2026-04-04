/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.spinner;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for SpinnerTypes.
 */
@DisplayName("SpinnerTypes Tests")
class SpinnerTypesTest {

    @Test
    @DisplayName("SpinnerMode enum has correct values")
    void spinnerModeEnumHasCorrectValues() {
        SpinnerTypes.SpinnerMode[] modes = SpinnerTypes.SpinnerMode.values();

        assertEquals(5, modes.length);
        assertTrue(Arrays.asList(modes).contains(SpinnerTypes.SpinnerMode.DEFAULT));
        assertTrue(Arrays.asList(modes).contains(SpinnerTypes.SpinnerMode.SHIMMER));
        assertTrue(Arrays.asList(modes).contains(SpinnerTypes.SpinnerMode.PULSE));
        assertTrue(Arrays.asList(modes).contains(SpinnerTypes.SpinnerMode.BOUNCE));
        assertTrue(Arrays.asList(modes).contains(SpinnerTypes.SpinnerMode.ELASTIC));
    }

    @Test
    @DisplayName("SpinnerState constructor with text only works correctly")
    void spinnerStateConstructorWithTextOnlyWorksCorrectly() {
        SpinnerTypes.SpinnerState state = new SpinnerTypes.SpinnerState("Loading...");

        assertEquals("Loading...", state.text());
        assertEquals(SpinnerTypes.SpinnerMode.DEFAULT, state.mode());
        assertEquals(0, state.frame());
        assertFalse(state.isStalled());
        assertNotNull(state.startTime());
    }

    @Test
    @DisplayName("SpinnerState full constructor works correctly")
    void spinnerStateFullConstructorWorksCorrectly() {
        long startTime = System.currentTimeMillis() - 1000;
        SpinnerTypes.SpinnerState state = new SpinnerTypes.SpinnerState(
            "Processing",
            SpinnerTypes.SpinnerMode.SHIMMER,
            5,
            startTime,
            true
        );

        assertEquals("Processing", state.text());
        assertEquals(SpinnerTypes.SpinnerMode.SHIMMER, state.mode());
        assertEquals(5, state.frame());
        assertEquals(startTime, state.startTime());
        assertTrue(state.isStalled());
    }

    @Test
    @DisplayName("SpinnerState getDuration returns elapsed time")
    void spinnerStateGetDurationReturnsElapsedTime() {
        SpinnerTypes.SpinnerState state = new SpinnerTypes.SpinnerState("Test");

        // Duration should be small since we just created it
        long duration = state.getDuration();
        assertTrue(duration >= 0);
        assertTrue(duration < 1000); // Less than 1 second
    }

    @Test
    @DisplayName("AnimationFrame record works correctly")
    void animationFrameRecordWorksCorrectly() {
        SpinnerUtils.RGBColor color = new SpinnerUtils.RGBColor(255, 128, 0);
        SpinnerTypes.AnimationFrame frame = new SpinnerTypes.AnimationFrame(
            "●",
            color,
            100
        );

        assertEquals("●", frame.character());
        assertEquals(255, frame.color().r());
        assertEquals(128, frame.color().g());
        assertEquals(0, frame.color().b());
        assertEquals(100, frame.delayMs());
    }
}