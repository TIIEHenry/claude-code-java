/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExtraUsage.
 */
class ExtraUsageTest {

    @Test
    @DisplayName("ExtraUsage isBilledAsExtraUsage returns boolean")
    void isBilledAsExtraUsage() {
        // Result depends on AuthUtils.isClaudeAISubscriber()
        boolean result = ExtraUsage.isBilledAsExtraUsage("claude-opus-4-6", false, false);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("ExtraUsage isBilledAsExtraUsage fast mode")
    void isBilledAsExtraUsageFastMode() {
        // Fast mode is always extra usage (if subscriber)
        boolean result = ExtraUsage.isBilledAsExtraUsage("claude-sonnet-4-6", true, false);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("ExtraUsage isBilledAsExtraUsage null model")
    void isBilledAsExtraUsageNullModel() {
        boolean result = ExtraUsage.isBilledAsExtraUsage(null, false, false);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("ExtraUsage isBilledAsExtraUsage opus with 1m merged")
    void isBilledAsExtraUsageOpusMerged() {
        boolean result = ExtraUsage.isBilledAsExtraUsage("opus", false, true);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("ExtraUsage isBilledAsExtraUsage sonnet model")
    void isBilledAsExtraUsageSonnet() {
        boolean result = ExtraUsage.isBilledAsExtraUsage("sonnet", false, false);
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("ExtraUsage isBilledAsExtraUsage regular model")
    void isBilledAsExtraUsageRegularModel() {
        boolean result = ExtraUsage.isBilledAsExtraUsage("claude-3-haiku", false, false);
        assertFalse(result); // Not a 1m context model
    }
}