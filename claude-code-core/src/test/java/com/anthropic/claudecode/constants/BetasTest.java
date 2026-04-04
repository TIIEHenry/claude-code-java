/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Betas.
 */
class BetasTest {

    @Test
    @DisplayName("Betas CLAUDE_CODE_20250219_BETA_HEADER has correct value")
    void claudeCodeBetaHeader() {
        assertEquals("claude-code-20250219", Betas.CLAUDE_CODE_20250219_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas INTERLEAVED_THINKING_BETA_HEADER has correct value")
    void interleavedThinkingBetaHeader() {
        assertEquals("interleaved-thinking-2025-05-14", Betas.INTERLEAVED_THINKING_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas CONTEXT_1M_BETA_HEADER has correct value")
    void context1mBetaHeader() {
        assertEquals("context-1m-2025-08-07", Betas.CONTEXT_1M_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas CONTEXT_MANAGEMENT_BETA_HEADER has correct value")
    void contextManagementBetaHeader() {
        assertEquals("context-management-2025-06-27", Betas.CONTEXT_MANAGEMENT_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas STRUCTURED_OUTPUTS_BETA_HEADER has correct value")
    void structuredOutputsBetaHeader() {
        assertEquals("structured-outputs-2025-12-15", Betas.STRUCTURED_OUTPUTS_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas WEB_SEARCH_BETA_HEADER has correct value")
    void webSearchBetaHeader() {
        assertEquals("web-search-2025-03-05", Betas.WEB_SEARCH_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas TOOL_SEARCH_BETA_HEADER_1P has correct value")
    void toolSearchBetaHeader1P() {
        assertEquals("advanced-tool-use-2025-11-20", Betas.TOOL_SEARCH_BETA_HEADER_1P);
    }

    @Test
    @DisplayName("Betas TOOL_SEARCH_BETA_HEADER_3P has correct value")
    void toolSearchBetaHeader3P() {
        assertEquals("tool-search-tool-2025-10-19", Betas.TOOL_SEARCH_BETA_HEADER_3P);
    }

    @Test
    @DisplayName("Betas EFFORT_BETA_HEADER has correct value")
    void effortBetaHeader() {
        assertEquals("effort-2025-11-24", Betas.EFFORT_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas TASK_BUDGETS_BETA_HEADER has correct value")
    void taskBudgetsBetaHeader() {
        assertEquals("task-budgets-2026-03-13", Betas.TASK_BUDGETS_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas FAST_MODE_BETA_HEADER has correct value")
    void fastModeBetaHeader() {
        assertEquals("fast-mode-2026-02-01", Betas.FAST_MODE_BETA_HEADER);
    }

    @Test
    @DisplayName("Betas BEDROCK_EXTRA_PARAMS_HEADERS contains expected headers")
    void bedrockExtraParamsHeaders() {
        assertTrue(Betas.BEDROCK_EXTRA_PARAMS_HEADERS.contains(Betas.INTERLEAVED_THINKING_BETA_HEADER));
        assertTrue(Betas.BEDROCK_EXTRA_PARAMS_HEADERS.contains(Betas.CONTEXT_1M_BETA_HEADER));
        assertTrue(Betas.BEDROCK_EXTRA_PARAMS_HEADERS.contains(Betas.TOOL_SEARCH_BETA_HEADER_3P));
        assertEquals(3, Betas.BEDROCK_EXTRA_PARAMS_HEADERS.size());
    }

    @Test
    @DisplayName("Betas VERTEX_COUNT_TOKENS_ALLOWED_BETAS contains expected headers")
    void vertexCountTokensAllowedBetas() {
        assertTrue(Betas.VERTEX_COUNT_TOKENS_ALLOWED_BETAS.contains(Betas.CLAUDE_CODE_20250219_BETA_HEADER));
        assertTrue(Betas.VERTEX_COUNT_TOKENS_ALLOWED_BETAS.contains(Betas.INTERLEAVED_THINKING_BETA_HEADER));
        assertTrue(Betas.VERTEX_COUNT_TOKENS_ALLOWED_BETAS.contains(Betas.CONTEXT_MANAGEMENT_BETA_HEADER));
        assertEquals(3, Betas.VERTEX_COUNT_TOKENS_ALLOWED_BETAS.size());
    }

    @Test
    @DisplayName("Betas all beta headers are non-null and non-empty")
    void allBetaHeadersAreValid() {
        assertNotNull(Betas.CLAUDE_CODE_20250219_BETA_HEADER);
        assertNotNull(Betas.INTERLEAVED_THINKING_BETA_HEADER);
        assertNotNull(Betas.CONTEXT_1M_BETA_HEADER);
        assertNotNull(Betas.CONTEXT_MANAGEMENT_BETA_HEADER);
        assertNotNull(Betas.STRUCTURED_OUTPUTS_BETA_HEADER);
        assertNotNull(Betas.WEB_SEARCH_BETA_HEADER);

        assertFalse(Betas.CLAUDE_CODE_20250219_BETA_HEADER.isEmpty());
        assertFalse(Betas.INTERLEAVED_THINKING_BETA_HEADER.isEmpty());
    }

    @Test
    @DisplayName("Betas sets are immutable")
    void setsAreImmutable() {
        // Verify sets cannot be modified (will throw UnsupportedOperationException)
        assertThrows(UnsupportedOperationException.class, () -> {
            Betas.BEDROCK_EXTRA_PARAMS_HEADERS.add("test");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            Betas.VERTEX_COUNT_TOKENS_ALLOWED_BETAS.add("test");
        });
    }
}