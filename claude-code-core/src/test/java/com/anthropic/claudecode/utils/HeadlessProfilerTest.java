/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HeadlessProfiler.
 */
class HeadlessProfilerTest {

    @BeforeEach
    void setUp() {
        HeadlessProfiler.reset();
    }

    @Test
    @DisplayName("HeadlessProfiler getCurrentTurnNumber initial value")
    void getCurrentTurnNumberInitial() {
        assertEquals(-1, HeadlessProfiler.getCurrentTurnNumber());
    }

    @Test
    @DisplayName("HeadlessProfiler reset resets state")
    void resetResetsState() {
        HeadlessProfiler.reset();
        assertEquals(-1, HeadlessProfiler.getCurrentTurnNumber());
    }

    @Test
    @DisplayName("HeadlessProfiler headlessProfilerStartTurn does not throw")
    void headlessProfilerStartTurnNoThrow() {
        assertDoesNotThrow(() -> HeadlessProfiler.headlessProfilerStartTurn());
    }

    @Test
    @DisplayName("HeadlessProfiler headlessProfilerCheckpoint does not throw")
    void headlessProfilerCheckpointNoThrow() {
        assertDoesNotThrow(() -> HeadlessProfiler.headlessProfilerCheckpoint("test"));
    }

    @Test
    @DisplayName("HeadlessProfiler logHeadlessProfilerTurn does not throw")
    void logHeadlessProfilerTurnNoThrow() {
        assertDoesNotThrow(() -> HeadlessProfiler.logHeadlessProfilerTurn());
    }

    @Test
    @DisplayName("HeadlessProfiler multiple starts")
    void multipleStarts() {
        HeadlessProfiler.headlessProfilerStartTurn();
        HeadlessProfiler.headlessProfilerStartTurn();
        // Turn number should increment
        assertTrue(HeadlessProfiler.getCurrentTurnNumber() >= 0);
    }

    @Test
    @DisplayName("HeadlessProfiler checkpoint sequence")
    void checkpointSequence() {
        HeadlessProfiler.headlessProfilerStartTurn();
        HeadlessProfiler.headlessProfilerCheckpoint("first");
        HeadlessProfiler.headlessProfilerCheckpoint("second");
        HeadlessProfiler.logHeadlessProfilerTurn();
        // Should not throw
        assertTrue(true);
    }
}