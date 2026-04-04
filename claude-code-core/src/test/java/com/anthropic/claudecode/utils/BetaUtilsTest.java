/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BetaUtils.
 */
class BetaUtilsTest {

    @BeforeEach
    void clearState() {
        BetaUtils.clearBetaCache();
        BetaUtils.clearLatches();
    }

    @Test
    @DisplayName("BetaUtils getModelBetas returns global betas for null model")
    void getModelBetasNull() {
        List<String> betas = BetaUtils.getModelBetas(null);

        assertFalse(betas.isEmpty());
        assertTrue(betas.contains("claude-code-20250219"));
    }

    @Test
    @DisplayName("BetaUtils getModelBetas returns model-specific betas")
    void getModelBetasModel() {
        List<String> betas = BetaUtils.getModelBetas("claude-sonnet-4-6");

        assertTrue(betas.contains("claude-code-20250219"));
        assertTrue(betas.contains("interleaved-thinking-2025-05-14"));
    }

    @Test
    @DisplayName("BetaUtils getModelBetas caches result")
    void getModelBetasCaches() {
        List<String> betas1 = BetaUtils.getModelBetas("claude-opus-4-6");
        List<String> betas2 = BetaUtils.getModelBetas("claude-opus-4-6");

        assertEquals(betas1, betas2);
    }

    @Test
    @DisplayName("BetaUtils isBetaEnabled checks beta")
    void isBetaEnabled() {
        assertTrue(BetaUtils.isBetaEnabled("claude-sonnet-4-6", "claude-code-20250219"));
        assertTrue(BetaUtils.isBetaEnabled("claude-sonnet-4-6", "interleaved-thinking-2025-05-14"));
        assertFalse(BetaUtils.isBetaEnabled("claude-sonnet-4-6", "unknown-beta"));
    }

    @Test
    @DisplayName("BetaUtils addBeta adds beta")
    void addBeta() {
        BetaUtils.addBeta("test-model", "new-beta");

        assertTrue(BetaUtils.isBetaEnabled("test-model", "new-beta"));
    }

    @Test
    @DisplayName("BetaUtils addBeta doesn't duplicate")
    void addBetaNoDuplicate() {
        BetaUtils.addBeta("test-model", "beta1");
        BetaUtils.addBeta("test-model", "beta1");

        List<String> betas = BetaUtils.getModelBetas("test-model");
        int count = 0;
        for (String b : betas) {
            if (b.equals("beta1")) count++;
        }
        assertEquals(1, count);
    }

    @Test
    @DisplayName("BetaUtils removeBeta removes beta")
    void removeBeta() {
        BetaUtils.addBeta("test-model", "beta-to-remove");
        BetaUtils.removeBeta("test-model", "beta-to-remove");

        assertFalse(BetaUtils.isBetaEnabled("test-model", "beta-to-remove"));
    }

    @Test
    @DisplayName("BetaUtils clearBetaCache clears cache")
    void clearBetaCache() {
        BetaUtils.getModelBetas("model1");
        BetaUtils.clearBetaCache();

        // Should still work after clear
        List<String> betas = BetaUtils.getModelBetas("model1");
        assertFalse(betas.isEmpty());
    }

    @Test
    @DisplayName("BetaUtils latch getters and setters work")
    void latchGettersSetters() {
        BetaUtils.setAfkModeHeaderLatched(true);
        assertTrue(BetaUtils.isAfkModeHeaderLatched());

        BetaUtils.setFastModeHeaderLatched(true);
        assertTrue(BetaUtils.isFastModeHeaderLatched());

        BetaUtils.setCacheEditingHeaderLatched(true);
        assertTrue(BetaUtils.isCacheEditingHeaderLatched());
    }

    @Test
    @DisplayName("BetaUtils getLatchedBetaHeaders returns latched")
    void getLatchedBetaHeaders() {
        BetaUtils.setAfkModeHeaderLatched(true);
        BetaUtils.setFastModeHeaderLatched(true);

        List<String> headers = BetaUtils.getLatchedBetaHeaders();

        assertTrue(headers.contains("afk-mode-2026-01-31"));
        assertTrue(headers.contains("fast-mode-2026-02-01"));
        assertFalse(headers.contains("prompt-caching-scope-2026-01-05"));
    }

    @Test
    @DisplayName("BetaUtils clearLatches clears all")
    void clearLatches() {
        BetaUtils.setAfkModeHeaderLatched(true);
        BetaUtils.setFastModeHeaderLatched(true);
        BetaUtils.setCacheEditingHeaderLatched(true);

        BetaUtils.clearLatches();

        assertFalse(BetaUtils.isAfkModeHeaderLatched());
        assertFalse(BetaUtils.isFastModeHeaderLatched());
        assertFalse(BetaUtils.isCacheEditingHeaderLatched());
    }

    @Test
    @DisplayName("BetaUtils shouldUseGlobalCacheScope checks latch")
    void shouldUseGlobalCacheScope() {
        assertFalse(BetaUtils.shouldUseGlobalCacheScope());

        BetaUtils.setCacheEditingHeaderLatched(true);
        assertTrue(BetaUtils.shouldUseGlobalCacheScope());
    }
}