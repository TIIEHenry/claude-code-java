/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ErrorIds.
 */
class ErrorIdsTest {

    @Test
    @DisplayName("ErrorIds E_TOOL_USE_SUMMARY_GENERATION_FAILED is defined")
    void toolUseSummaryGenerationFailed() {
        assertEquals(344, ErrorIds.E_TOOL_USE_SUMMARY_GENERATION_FAILED);
    }

    @Test
    @DisplayName("ErrorIds error ID is positive")
    void errorIdIsPositive() {
        assertTrue(ErrorIds.E_TOOL_USE_SUMMARY_GENERATION_FAILED > 0);
    }

    @Test
    @DisplayName("ErrorIds error ID is within expected range")
    void errorIdInRange() {
        // Based on comment: Next ID: 346
        assertTrue(ErrorIds.E_TOOL_USE_SUMMARY_GENERATION_FAILED < 346);
    }
}