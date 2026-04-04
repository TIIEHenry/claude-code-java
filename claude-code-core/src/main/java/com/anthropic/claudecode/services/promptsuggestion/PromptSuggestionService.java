/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/PromptSuggestion/promptSuggestion.ts
 */
package com.anthropic.claudecode.services.promptsuggestion;

import java.util.*;
import java.util.concurrent.*;
import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;

/**
 * Prompt suggestion service - suggests what the user might type next.
 */
public final class PromptSuggestionService {
    private PromptSuggestionService() {}

    private static volatile Object currentAbortController = null;

    /**
     * Prompt variant enum.
     */
    public enum PromptVariant {
        USER_INTENT, STATED_INTENT
    }

    /**
     * Suggestion result.
     */
    public record SuggestionResult(
        String suggestion,
        PromptVariant promptId,
        String generationRequestId
    ) {}

    /**
     * Get prompt variant.
     */
    public static PromptVariant getPromptVariant() {
        return PromptVariant.USER_INTENT;
    }

    /**
     * Check if prompt suggestion should be enabled.
     */
    public static boolean shouldEnablePromptSuggestion() {
        String envOverride = System.getenv("CLAUDE_CODE_ENABLE_PROMPT_SUGGESTION");
        if (envOverride != null) {
            if ("false".equalsIgnoreCase(envOverride) || "0".equals(envOverride)) {
                return false;
            }
            if ("true".equalsIgnoreCase(envOverride) || "1".equals(envOverride)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Abort prompt suggestion.
     */
    public static void abortPromptSuggestion() {
        currentAbortController = null;
    }

    /**
     * Get suggestion.
     */
    public static CompletableFuture<SuggestionResult> getSuggestion(Object context) {
        return CompletableFuture.supplyAsync(() -> {
            AnalyticsMetadata.logEvent("tengu_prompt_suggestion_requested", Map.of());
            return new SuggestionResult("", PromptVariant.USER_INTENT, UUID.randomUUID().toString());
        });
    }
}