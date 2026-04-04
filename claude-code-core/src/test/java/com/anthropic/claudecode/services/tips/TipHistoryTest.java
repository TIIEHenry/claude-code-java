/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.tips;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TipHistory.
 */
class TipHistoryTest {

    @BeforeEach
    void setUp() {
        TipHistory.reset();
    }

    @Test
    @DisplayName("TipHistory getSessionsSinceLastShown returns MAX_VALUE for never shown")
    void getSessionsSinceLastShownNeverShown() {
        assertEquals(Integer.MAX_VALUE, TipHistory.getSessionsSinceLastShown("new-tip"));
    }

    @Test
    @DisplayName("TipHistory recordTipShown sets counter to 0")
    void recordTipShown() {
        TipHistory.recordTipShown("my-tip");

        assertEquals(0, TipHistory.getSessionsSinceLastShown("my-tip"));
    }

    @Test
    @DisplayName("TipHistory incrementSessionCount increments counters")
    void incrementSessionCount() {
        TipHistory.recordTipShown("tip1");
        assertEquals(0, TipHistory.getSessionsSinceLastShown("tip1"));

        TipHistory.incrementSessionCount();
        assertEquals(1, TipHistory.getSessionsSinceLastShown("tip1"));

        TipHistory.incrementSessionCount();
        assertEquals(2, TipHistory.getSessionsSinceLastShown("tip1"));
    }

    @Test
    @DisplayName("TipHistory incrementSessionCount affects all recorded tips")
    void incrementSessionCountAffectsAll() {
        TipHistory.recordTipShown("tip1");
        TipHistory.recordTipShown("tip2");

        TipHistory.incrementSessionCount();

        assertEquals(1, TipHistory.getSessionsSinceLastShown("tip1"));
        assertEquals(1, TipHistory.getSessionsSinceLastShown("tip2"));
    }

    @Test
    @DisplayName("TipHistory incrementSessionCount does not affect never shown tips")
    void incrementSessionCountDoesNotAffectNeverShown() {
        TipHistory.recordTipShown("shown-tip");

        TipHistory.incrementSessionCount();

        assertEquals(1, TipHistory.getSessionsSinceLastShown("shown-tip"));
        assertEquals(Integer.MAX_VALUE, TipHistory.getSessionsSinceLastShown("never-shown"));
    }

    @Test
    @DisplayName("TipHistory reset clears all history")
    void resetClearsHistory() {
        TipHistory.recordTipShown("tip1");
        TipHistory.recordTipShown("tip2");
        TipHistory.incrementSessionCount();

        assertEquals(1, TipHistory.getSessionsSinceLastShown("tip1"));

        TipHistory.reset();

        assertEquals(Integer.MAX_VALUE, TipHistory.getSessionsSinceLastShown("tip1"));
        assertEquals(Integer.MAX_VALUE, TipHistory.getSessionsSinceLastShown("tip2"));
    }

    @Test
    @DisplayName("TipHistory multiple shows resets counter")
    void multipleShowsResetCounter() {
        TipHistory.recordTipShown("tip");
        TipHistory.incrementSessionCount();
        TipHistory.incrementSessionCount();

        assertEquals(2, TipHistory.getSessionsSinceLastShown("tip"));

        TipHistory.recordTipShown("tip");

        assertEquals(0, TipHistory.getSessionsSinceLastShown("tip"));
    }

    @Test
    @DisplayName("TipHistory tracks different tips independently")
    void independentTipTracking() {
        TipHistory.recordTipShown("tip1");
        TipHistory.incrementSessionCount();
        TipHistory.recordTipShown("tip2");

        assertEquals(1, TipHistory.getSessionsSinceLastShown("tip1"));
        assertEquals(0, TipHistory.getSessionsSinceLastShown("tip2"));
    }
}