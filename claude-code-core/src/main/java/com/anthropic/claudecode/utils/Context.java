/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/context.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Context window utilities.
 *
 * Manages context window sizes and calculations for different models.
 */
public final class Context {
    private Context() {}

    /**
     * Default model context window (200k tokens).
     */
    public static final int MODEL_CONTEXT_WINDOW_DEFAULT = 200_000;

    /**
     * Maximum output tokens for compact operations.
     */
    public static final int COMPACT_MAX_OUTPUT_TOKENS = 20_000;

    /**
     * Default max output tokens.
     */
    public static final int MAX_OUTPUT_TOKENS_DEFAULT = 32_000;

    /**
     * Upper limit for max output tokens.
     */
    public static final int MAX_OUTPUT_TOKENS_UPPER_LIMIT = 64_000;

    /**
     * Capped default for slot-reservation optimization.
     */
    public static final int CAPPED_DEFAULT_MAX_TOKENS = 8_000;

    /**
     * Escalated max tokens after hitting cap.
     */
    public static final int ESCALATED_MAX_TOKENS = 64_000;

    /**
     * 1M context window size.
     */
    public static final int CONTEXT_1M = 1_000_000;

    private static volatile boolean disable1mContext = false;
    private static volatile Integer maxContextTokensOverride = null;

    /**
     * Check if 1M context is disabled.
     */
    public static boolean is1mContextDisabled() {
        return disable1mContext;
    }

    /**
     * Set 1M context disabled state.
     */
    public static void setDisable1mContext(boolean disabled) {
        disable1mContext = disabled;
    }

    /**
     * Set max context tokens override.
     */
    public static void setMaxContextTokensOverride(Integer override) {
        maxContextTokensOverride = override;
    }

    /**
     * Check if model has 1M context marker.
     */
    public static boolean has1mContext(String model) {
        if (is1mContextDisabled()) {
            return false;
        }
        return model != null && model.toLowerCase().contains("[1m]");
    }

    /**
     * Check if model supports 1M context.
     */
    public static boolean modelSupports1M(String model) {
        if (is1mContextDisabled()) {
            return false;
        }
        String canonical = toCanonicalName(model);
        return canonical.contains("claude-sonnet-4") || canonical.contains("opus-4-6");
    }

    /**
     * Get context window size for a model.
     */
    public static int getContextWindowForModel(String model) {
        return getContextWindowForModel(model, null);
    }

    /**
     * Get context window size for a model with betas.
     */
    public static int getContextWindowForModel(String model, List<String> betas) {
        // Check override
        if (maxContextTokensOverride != null && maxContextTokensOverride > 0) {
            return maxContextTokensOverride;
        }

        // [1m] suffix — explicit opt-in
        if (has1mContext(model)) {
            return CONTEXT_1M;
        }

        // Check model capabilities
        ModelCapability cap = getModelCapability(model);
        if (cap != null && cap.maxInputTokens() >= 100_000) {
            if (cap.maxInputTokens() > MODEL_CONTEXT_WINDOW_DEFAULT && is1mContextDisabled()) {
                return MODEL_CONTEXT_WINDOW_DEFAULT;
            }
            return cap.maxInputTokens();
        }

        // Check betas for 1M context
        if (betas != null && betas.contains("context-1m-2025-04-01") && modelSupports1M(model)) {
            return CONTEXT_1M;
        }

        return MODEL_CONTEXT_WINDOW_DEFAULT;
    }

    /**
     * Calculate context window usage percentages.
     */
    public static ContextPercentages calculateContextPercentages(
            TokenUsage usage,
            int contextWindowSize
    ) {
        if (usage == null) {
            return new ContextPercentages(null, null);
        }

        long totalInputTokens = usage.inputTokens() +
                usage.cacheCreationInputTokens() +
                usage.cacheReadInputTokens();

        int usedPercentage = (int) Math.round((double) totalInputTokens / contextWindowSize * 100);
        int clampedUsed = Math.min(100, Math.max(0, usedPercentage));

        return new ContextPercentages(clampedUsed, 100 - clampedUsed);
    }

    /**
     * Get max output tokens for a model.
     */
    public static MaxOutputTokens getModelMaxOutputTokens(String model) {
        String canonical = toCanonicalName(model);

        // Opus models have higher limits
        if (canonical.contains("opus")) {
            return new MaxOutputTokens(MAX_OUTPUT_TOKENS_DEFAULT, MAX_OUTPUT_TOKENS_UPPER_LIMIT);
        }

        // Sonnet models
        if (canonical.contains("sonnet")) {
            return new MaxOutputTokens(MAX_OUTPUT_TOKENS_DEFAULT, MAX_OUTPUT_TOKENS_UPPER_LIMIT);
        }

        // Haiku models have lower limits
        if (canonical.contains("haiku")) {
            return new MaxOutputTokens(8_000, 16_000);
        }

        return new MaxOutputTokens(MAX_OUTPUT_TOKENS_DEFAULT, MAX_OUTPUT_TOKENS_UPPER_LIMIT);
    }

    /**
     * Get max output tokens with cap applied.
     */
    public static int getMaxOutputTokensWithCap(String model, boolean applyCap) {
        MaxOutputTokens limits = getModelMaxOutputTokens(model);
        return applyCap ? Math.min(limits.defaultValue(), CAPPED_DEFAULT_MAX_TOKENS) : limits.defaultValue();
    }

    /**
     * Convert model name to canonical form.
     */
    private static String toCanonicalName(String model) {
        if (model == null) return "";
        return model.toLowerCase()
                .replace("anthropic/", "")
                .replace("bedrock/", "")
                .replace("vertex/", "")
                .replaceAll("-\\d{8}$", "");
    }

    /**
     * Get model capability.
     */
    private static ModelCapability getModelCapability(String model) {
        String canonical = toCanonicalName(model);

        // Check for known models
        if (canonical.contains("opus-4-6")) {
            return new ModelCapability(CONTEXT_1M, MAX_OUTPUT_TOKENS_UPPER_LIMIT);
        }
        if (canonical.contains("opus-4-5") || canonical.contains("opus-4-1")) {
            return new ModelCapability(CONTEXT_1M, MAX_OUTPUT_TOKENS_UPPER_LIMIT);
        }
        if (canonical.contains("sonnet-4-6")) {
            return new ModelCapability(CONTEXT_1M, MAX_OUTPUT_TOKENS_DEFAULT);
        }
        if (canonical.contains("sonnet-4-5") || canonical.contains("sonnet-4")) {
            return new ModelCapability(MODEL_CONTEXT_WINDOW_DEFAULT, MAX_OUTPUT_TOKENS_DEFAULT);
        }
        if (canonical.contains("haiku-4-5") || canonical.contains("haiku")) {
            return new ModelCapability(MODEL_CONTEXT_WINDOW_DEFAULT, 8_000);
        }

        return null;
    }

    // ==================== Record Classes ====================

    /**
     * Token usage record.
     */
    public record TokenUsage(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens
    ) {}

    /**
     * Context percentages record.
     */
    public record ContextPercentages(Integer used, Integer remaining) {}

    /**
     * Max output tokens record.
     */
    public record MaxOutputTokens(int defaultValue, int upperLimit) {}

    /**
     * Model capability record.
     */
    public record ModelCapability(int maxInputTokens, int maxOutputTokens) {}
}