/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/context.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Context window management utilities.
 */
public final class ContextUtils {
    private ContextUtils() {}

    // Default context limits
    public static final int DEFAULT_CONTEXT_WINDOW = 200_000;
    public static final int MAX_OUTPUT_TOKENS = 8192;
    public static final int RESERVED_OUTPUT_TOKENS = 4096;

    /**
     * Context stats record.
     */
    public record ContextStats(
            int inputTokens,
            int outputTokens,
            int cacheCreationTokens,
            int cacheReadTokens,
            int contextWindowSize,
            int remainingTokens
    ) {
        public double utilizationPercent() {
            return (double) (inputTokens + outputTokens) / contextWindowSize * 100;
        }

        public boolean isNearLimit() {
            return remainingTokens < contextWindowSize * 0.1;
        }
    }

    /**
     * Calculate remaining context tokens.
     */
    public static int calculateRemainingTokens(int usedTokens, int contextWindow) {
        int available = contextWindow - RESERVED_OUTPUT_TOKENS;
        return Math.max(0, available - usedTokens);
    }

    /**
     * Calculate context utilization percentage.
     */
    public static double calculateUtilization(int usedTokens, int contextWindow) {
        int available = contextWindow - RESERVED_OUTPUT_TOKENS;
        return (double) usedTokens / available * 100;
    }

    /**
     * Check if context is approaching limit.
     */
    public static boolean isApproachingLimit(int usedTokens, int contextWindow, double threshold) {
        double utilization = calculateUtilization(usedTokens, contextWindow);
        return utilization >= threshold * 100;
    }

    /**
     * Estimate tokens needed for a response.
     */
    public static int estimateResponseTokens(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return 500; // Minimum estimate
        }

        // Rough estimation based on prompt length
        int promptTokens = Tokens.estimateTokens(prompt);
        return Math.min(Math.max(promptTokens / 2, 500), MAX_OUTPUT_TOKENS);
    }

    /**
     * Calculate compact threshold.
     */
    public static int calculateCompactThreshold(int contextWindow) {
        return (int) ((contextWindow - RESERVED_OUTPUT_TOKENS) * 0.9);
    }

    /**
     * Check if compaction is needed.
     */
    public static boolean needsCompaction(int usedTokens, int contextWindow) {
        return usedTokens >= calculateCompactThreshold(contextWindow);
    }

    /**
     * Get context window size for model.
     */
    public static int getContextWindowSize(String modelId) {
        return Tokens.getContextWindowSize(modelId);
    }

    /**
     * Token budget allocation.
     */
    public record TokenBudget(
            int systemPromptTokens,
            int toolsTokens,
            int messagesTokens,
            int reservedOutputTokens,
            int contextWindow
    ) {
        public int totalUsed() {
            return systemPromptTokens + toolsTokens + messagesTokens;
        }

        public int remaining() {
            return contextWindow - totalUsed() - reservedOutputTokens;
        }

        public double utilizationPercent() {
            return (double) totalUsed() / (contextWindow - reservedOutputTokens) * 100;
        }
    }

    /**
     * Create a token budget.
     */
    public static TokenBudget createBudget(
            int contextWindow,
            int systemPromptTokens,
            int toolsTokens,
            int messagesTokens
    ) {
        return new TokenBudget(
            systemPromptTokens,
            toolsTokens,
            messagesTokens,
            RESERVED_OUTPUT_TOKENS,
            contextWindow
        );
    }

    /**
     * Context compaction result.
     */
    public record CompactionResult(
            boolean success,
            int tokensBefore,
            int tokensAfter,
            int tokensSaved,
            int messagesRemoved,
            String summary
    ) {
        public double savingsPercent() {
            if (tokensBefore == 0) return 0;
            return (double) tokensSaved / tokensBefore * 100;
        }
    }
}