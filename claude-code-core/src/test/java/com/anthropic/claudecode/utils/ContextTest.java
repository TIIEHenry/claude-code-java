/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Context.
 */
class ContextTest {

    @Test
    @DisplayName("Context constants are defined")
    void constants() {
        assertEquals(200_000, Context.MODEL_CONTEXT_WINDOW_DEFAULT);
        assertEquals(20_000, Context.COMPACT_MAX_OUTPUT_TOKENS);
        assertEquals(32_000, Context.MAX_OUTPUT_TOKENS_DEFAULT);
        assertEquals(64_000, Context.MAX_OUTPUT_TOKENS_UPPER_LIMIT);
        assertEquals(1_000_000, Context.CONTEXT_1M);
    }

    @Test
    @DisplayName("Context is1mContextDisabled defaults to false")
    void is1mContextDisabledDefault() {
        Context.setDisable1mContext(false);
        assertFalse(Context.is1mContextDisabled());
    }

    @Test
    @DisplayName("Context setDisable1mContext works")
    void setDisable1mContext() {
        Context.setDisable1mContext(true);
        assertTrue(Context.is1mContextDisabled());
        Context.setDisable1mContext(false);
    }

    @Test
    @DisplayName("Context has1mContext detects [1m] marker")
    void has1mContext() {
        Context.setDisable1mContext(false);

        assertTrue(Context.has1mContext("model[1m]"));
        assertTrue(Context.has1mContext("claude-sonnet-4[1m]"));
        assertFalse(Context.has1mContext("model"));
        assertFalse(Context.has1mContext(null));
    }

    @Test
    @DisplayName("Context has1mContext respects disabled flag")
    void has1mContextDisabled() {
        Context.setDisable1mContext(true);

        assertFalse(Context.has1mContext("model[1m]"));

        Context.setDisable1mContext(false);
    }

    @Test
    @DisplayName("Context modelSupports1M checks model")
    void modelSupports1M() {
        Context.setDisable1mContext(false);

        assertTrue(Context.modelSupports1M("claude-sonnet-4"));
        assertTrue(Context.modelSupports1M("claude-opus-4-6"));
        assertFalse(Context.modelSupports1M("claude-haiku"));
    }

    @Test
    @DisplayName("Context getContextWindowForModel returns default")
    void getContextWindowForModelDefault() {
        Context.setMaxContextTokensOverride(null);
        Context.setDisable1mContext(false);

        int window = Context.getContextWindowForModel("unknown-model");

        assertEquals(Context.MODEL_CONTEXT_WINDOW_DEFAULT, window);
    }

    @Test
    @DisplayName("Context getContextWindowForModel with [1m] returns 1M")
    void getContextWindowForModel1M() {
        Context.setMaxContextTokensOverride(null);
        Context.setDisable1mContext(false);

        int window = Context.getContextWindowForModel("model[1m]");

        assertEquals(Context.CONTEXT_1M, window);
    }

    @Test
    @DisplayName("Context getContextWindowForModel with override")
    void getContextWindowForModelOverride() {
        Context.setMaxContextTokensOverride(500_000);

        int window = Context.getContextWindowForModel("model");

        assertEquals(500_000, window);

        Context.setMaxContextTokensOverride(null);
    }

    @Test
    @DisplayName("Context calculateContextPercentages calculates correctly")
    void calculateContextPercentages() {
        Context.TokenUsage usage = new Context.TokenUsage(50_000, 10_000, 5_000, 5_000);
        int windowSize = 200_000;

        Context.ContextPercentages percentages = Context.calculateContextPercentages(usage, windowSize);

        // (50000 + 5000 + 5000) / 200000 = 30%
        assertEquals(30, percentages.used());
        assertEquals(70, percentages.remaining());
    }

    @Test
    @DisplayName("Context calculateContextPercentages handles null")
    void calculateContextPercentagesNull() {
        Context.ContextPercentages percentages = Context.calculateContextPercentages(null, 200_000);

        assertNull(percentages.used());
        assertNull(percentages.remaining());
    }

    @Test
    @DisplayName("Context getModelMaxOutputTokens returns for opus")
    void getModelMaxOutputTokensOpus() {
        Context.MaxOutputTokens tokens = Context.getModelMaxOutputTokens("claude-opus-4-6");

        assertEquals(32_000, tokens.defaultValue());
        assertEquals(64_000, tokens.upperLimit());
    }

    @Test
    @DisplayName("Context getModelMaxOutputTokens returns for sonnet")
    void getModelMaxOutputTokensSonnet() {
        Context.MaxOutputTokens tokens = Context.getModelMaxOutputTokens("claude-sonnet-4");

        assertEquals(32_000, tokens.defaultValue());
        assertEquals(64_000, tokens.upperLimit());
    }

    @Test
    @DisplayName("Context getModelMaxOutputTokens returns for haiku")
    void getModelMaxOutputTokensHaiku() {
        Context.MaxOutputTokens tokens = Context.getModelMaxOutputTokens("claude-haiku");

        assertEquals(8_000, tokens.defaultValue());
        assertEquals(16_000, tokens.upperLimit());
    }

    @Test
    @DisplayName("Context getMaxOutputTokensWithCap applies cap")
    void getMaxOutputTokensWithCap() {
        int capped = Context.getMaxOutputTokensWithCap("claude-sonnet-4", true);
        int uncapped = Context.getMaxOutputTokensWithCap("claude-sonnet-4", false);

        assertEquals(8_000, capped);
        assertEquals(32_000, uncapped);
    }

    @Test
    @DisplayName("Context TokenUsage record works")
    void tokenUsageRecord() {
        Context.TokenUsage usage = new Context.TokenUsage(1000L, 500L, 100L, 50L);

        assertEquals(1000L, usage.inputTokens());
        assertEquals(500L, usage.outputTokens());
        assertEquals(100L, usage.cacheCreationInputTokens());
        assertEquals(50L, usage.cacheReadInputTokens());
    }

    @Test
    @DisplayName("Context ContextPercentages record works")
    void contextPercentagesRecord() {
        Context.ContextPercentages percentages = new Context.ContextPercentages(30, 70);

        assertEquals(30, percentages.used());
        assertEquals(70, percentages.remaining());
    }

    @Test
    @DisplayName("Context MaxOutputTokens record works")
    void maxOutputTokensRecord() {
        Context.MaxOutputTokens tokens = new Context.MaxOutputTokens(32000, 64000);

        assertEquals(32000, tokens.defaultValue());
        assertEquals(64000, tokens.upperLimit());
    }

    @Test
    @DisplayName("Context ModelCapability record works")
    void modelCapabilityRecord() {
        Context.ModelCapability cap = new Context.ModelCapability(200_000, 32_000);

        assertEquals(200_000, cap.maxInputTokens());
        assertEquals(32_000, cap.maxOutputTokens());
    }
}