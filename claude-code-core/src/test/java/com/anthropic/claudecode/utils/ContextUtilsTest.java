/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContextUtils.
 */
class ContextUtilsTest {

    @Test
    @DisplayName("ContextUtils constants")
    void constants() {
        assertEquals(200_000, ContextUtils.DEFAULT_CONTEXT_WINDOW);
        assertEquals(8192, ContextUtils.MAX_OUTPUT_TOKENS);
        assertEquals(4096, ContextUtils.RESERVED_OUTPUT_TOKENS);
    }

    @Test
    @DisplayName("ContextUtils ContextStats record")
    void contextStatsRecord() {
        ContextUtils.ContextStats stats = new ContextUtils.ContextStats(
            10000, 2000, 500, 300, 200000, 187500
        );

        assertEquals(10000, stats.inputTokens());
        assertEquals(2000, stats.outputTokens());
        assertEquals(500, stats.cacheCreationTokens());
        assertEquals(300, stats.cacheReadTokens());
        assertEquals(200000, stats.contextWindowSize());
        assertEquals(187500, stats.remainingTokens());
    }

    @Test
    @DisplayName("ContextUtils ContextStats utilizationPercent")
    void contextStatsUtilizationPercent() {
        ContextUtils.ContextStats stats = new ContextUtils.ContextStats(
            100000, 0, 0, 0, 200000, 96000
        );

        assertEquals(50.0, stats.utilizationPercent(), 0.1);
    }

    @Test
    @DisplayName("ContextUtils ContextStats isNearLimit")
    void contextStatsIsNearLimit() {
        ContextUtils.ContextStats nearLimit = new ContextUtils.ContextStats(
            180000, 0, 0, 0, 200000, 16000
        );
        assertTrue(nearLimit.isNearLimit());

        ContextUtils.ContextStats safe = new ContextUtils.ContextStats(
            100000, 0, 0, 0, 200000, 96000
        );
        assertFalse(safe.isNearLimit());
    }

    @Test
    @DisplayName("ContextUtils calculateRemainingTokens")
    void calculateRemainingTokens() {
        int remaining = ContextUtils.calculateRemainingTokens(50000, 200000);
        // contextWindow - RESERVED_OUTPUT_TOKENS - usedTokens
        // = 200000 - 4096 - 50000 = 145904
        assertEquals(145904, remaining);
    }

    @Test
    @DisplayName("ContextUtils calculateRemainingTokens negative")
    void calculateRemainingTokensNegative() {
        int remaining = ContextUtils.calculateRemainingTokens(300000, 200000);
        // Should return 0, not negative
        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("ContextUtils calculateUtilization")
    void calculateUtilization() {
        double utilization = ContextUtils.calculateUtilization(97904, 200000);
        // usedTokens / (contextWindow - RESERVED_OUTPUT_TOKENS) * 100
        // = 97904 / 195904 * 100 = 50%
        assertEquals(50.0, utilization, 0.1);
    }

    @Test
    @DisplayName("ContextUtils isApproachingLimit true")
    void isApproachingLimitTrue() {
        // 95% utilized
        boolean approaching = ContextUtils.isApproachingLimit(186000, 200000, 0.9);
        assertTrue(approaching);
    }

    @Test
    @DisplayName("ContextUtils isApproachingLimit false")
    void isApproachingLimitFalse() {
        // 50% utilized
        boolean approaching = ContextUtils.isApproachingLimit(97904, 200000, 0.9);
        assertFalse(approaching);
    }

    @Test
    @DisplayName("ContextUtils estimateResponseTokens null")
    void estimateResponseTokensNull() {
        int tokens = ContextUtils.estimateResponseTokens(null);
        assertEquals(500, tokens);
    }

    @Test
    @DisplayName("ContextUtils estimateResponseTokens empty")
    void estimateResponseTokensEmpty() {
        int tokens = ContextUtils.estimateResponseTokens("");
        assertEquals(500, tokens);
    }

    @Test
    @DisplayName("ContextUtils estimateResponseTokens with content")
    void estimateResponseTokensWithContent() {
        int tokens = ContextUtils.estimateResponseTokens("This is a test prompt");
        assertTrue(tokens >= 500);
    }

    @Test
    @DisplayName("ContextUtils calculateCompactThreshold")
    void calculateCompactThreshold() {
        int threshold = ContextUtils.calculateCompactThreshold(200000);
        // (contextWindow - RESERVED_OUTPUT_TOKENS) * 0.9
        // = (200000 - 4096) * 0.9 = 176313.6 -> 176313
        assertEquals(176313, threshold);
    }

    @Test
    @DisplayName("ContextUtils needsCompaction true")
    void needsCompactionTrue() {
        boolean needs = ContextUtils.needsCompaction(180000, 200000);
        assertTrue(needs);
    }

    @Test
    @DisplayName("ContextUtils needsCompaction false")
    void needsCompactionFalse() {
        boolean needs = ContextUtils.needsCompaction(100000, 200000);
        assertFalse(needs);
    }

    @Test
    @DisplayName("ContextUtils getContextWindowSize")
    void getContextWindowSize() {
        int size = ContextUtils.getContextWindowSize("claude-sonnet-4-6");
        assertTrue(size > 0);
    }

    @Test
    @DisplayName("ContextUtils TokenBudget record")
    void tokenBudgetRecord() {
        ContextUtils.TokenBudget budget = new ContextUtils.TokenBudget(
            1000, 500, 10000, 4096, 200000
        );

        assertEquals(1000, budget.systemPromptTokens());
        assertEquals(500, budget.toolsTokens());
        assertEquals(10000, budget.messagesTokens());
        assertEquals(4096, budget.reservedOutputTokens());
        assertEquals(200000, budget.contextWindow());
    }

    @Test
    @DisplayName("ContextUtils TokenBudget totalUsed")
    void tokenBudgetTotalUsed() {
        ContextUtils.TokenBudget budget = new ContextUtils.TokenBudget(
            1000, 500, 10000, 4096, 200000
        );

        assertEquals(11500, budget.totalUsed());
    }

    @Test
    @DisplayName("ContextUtils TokenBudget remaining")
    void tokenBudgetRemaining() {
        ContextUtils.TokenBudget budget = new ContextUtils.TokenBudget(
            1000, 500, 10000, 4096, 200000
        );

        // contextWindow - totalUsed - reservedOutputTokens
        // = 200000 - 11500 - 4096 = 184404
        assertEquals(184404, budget.remaining());
    }

    @Test
    @DisplayName("ContextUtils TokenBudget utilizationPercent")
    void tokenBudgetUtilizationPercent() {
        ContextUtils.TokenBudget budget = new ContextUtils.TokenBudget(
            97904, 0, 0, 4096, 200000
        );

        // totalUsed / (contextWindow - reservedOutputTokens) * 100
        // = 97904 / 195904 * 100 = 50%
        assertEquals(50.0, budget.utilizationPercent(), 0.1);
    }

    @Test
    @DisplayName("ContextUtils createBudget")
    void createBudget() {
        ContextUtils.TokenBudget budget = ContextUtils.createBudget(200000, 1000, 500, 10000);

        assertEquals(1000, budget.systemPromptTokens());
        assertEquals(500, budget.toolsTokens());
        assertEquals(10000, budget.messagesTokens());
        assertEquals(4096, budget.reservedOutputTokens());
        assertEquals(200000, budget.contextWindow());
    }

    @Test
    @DisplayName("ContextUtils CompactionResult record")
    void compactionResultRecord() {
        ContextUtils.CompactionResult result = new ContextUtils.CompactionResult(
            true, 50000, 25000, 25000, 10, "Compacted successfully"
        );

        assertTrue(result.success());
        assertEquals(50000, result.tokensBefore());
        assertEquals(25000, result.tokensAfter());
        assertEquals(25000, result.tokensSaved());
        assertEquals(10, result.messagesRemoved());
        assertEquals("Compacted successfully", result.summary());
    }

    @Test
    @DisplayName("ContextUtils CompactionResult savingsPercent")
    void compactionResultSavingsPercent() {
        ContextUtils.CompactionResult result = new ContextUtils.CompactionResult(
            true, 50000, 25000, 25000, 10, "Success"
        );

        assertEquals(50.0, result.savingsPercent(), 0.1);
    }

    @Test
    @DisplayName("ContextUtils CompactionResult savingsPercent zero before")
    void compactionResultSavingsPercentZeroBefore() {
        ContextUtils.CompactionResult result = new ContextUtils.CompactionResult(
            true, 0, 0, 0, 0, "Success"
        );

        assertEquals(0.0, result.savingsPercent(), 0.1);
    }
}