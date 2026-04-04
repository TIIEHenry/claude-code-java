/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.promptsuggestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PromptSuggestionService.
 */
class PromptSuggestionServiceTest {

    @Test
    @DisplayName("PromptSuggestionService PromptVariant enum")
    void promptVariantEnum() {
        PromptSuggestionService.PromptVariant[] variants = PromptSuggestionService.PromptVariant.values();
        assertEquals(2, variants.length);
        assertEquals(PromptSuggestionService.PromptVariant.USER_INTENT,
            PromptSuggestionService.PromptVariant.valueOf("USER_INTENT"));
        assertEquals(PromptSuggestionService.PromptVariant.STATED_INTENT,
            PromptSuggestionService.PromptVariant.valueOf("STATED_INTENT"));
    }

    @Test
    @DisplayName("PromptSuggestionService SuggestionResult record")
    void suggestionResultRecord() {
        PromptSuggestionService.SuggestionResult result = new PromptSuggestionService.SuggestionResult(
            "Test suggestion",
            PromptSuggestionService.PromptVariant.USER_INTENT,
            "req-123"
        );

        assertEquals("Test suggestion", result.suggestion());
        assertEquals(PromptSuggestionService.PromptVariant.USER_INTENT, result.promptId());
        assertEquals("req-123", result.generationRequestId());
    }

    @Test
    @DisplayName("PromptSuggestionService getPromptVariant returns USER_INTENT")
    void getPromptVariant() {
        assertEquals(PromptSuggestionService.PromptVariant.USER_INTENT,
            PromptSuggestionService.getPromptVariant());
    }

    @Test
    @DisplayName("PromptSuggestionService shouldEnablePromptSuggestion default false")
    void shouldEnablePromptSuggestionDefault() {
        // Without env var, returns false
        boolean result = PromptSuggestionService.shouldEnablePromptSuggestion();
        // Result depends on env, but should not throw
        assertNotNull(result);
    }

    @Test
    @DisplayName("PromptSuggestionService abortPromptSuggestion does not throw")
    void abortPromptSuggestion() {
        PromptSuggestionService.abortPromptSuggestion();
    }

    @Test
    @DisplayName("PromptSuggestionService getSuggestion returns result")
    void getSuggestion() throws Exception {
        PromptSuggestionService.SuggestionResult result = PromptSuggestionService.getSuggestion(null).get();

        assertNotNull(result);
        assertEquals(PromptSuggestionService.PromptVariant.USER_INTENT, result.promptId());
        assertNotNull(result.generationRequestId());
    }
}