/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/modelCost.ts
 */
package com.anthropic.claudecode.utils.model;

import java.util.*;

/**
 * Model cost calculation utilities.
 *
 * Calculates USD costs based on token usage and model pricing.
 */
public final class ModelCost {
    private ModelCost() {}

    /**
     * Model costs record.
     */
    public record Costs(
        double inputTokens,
        double outputTokens,
        double promptCacheWriteTokens,
        double promptCacheReadTokens,
        double webSearchRequests
    ) {
        /**
         * Format pricing as string.
         */
        public String formatPricing() {
            return formatPrice(inputTokens) + "/" + formatPrice(outputTokens) + " per Mtok";
        }

        private String formatPrice(double price) {
            if (price == Math.floor(price)) {
                return "$" + (int) price;
            }
            return String.format("$%.2f", price);
        }
    }

    // Pricing tiers
    public static final Costs TIER_3_15 = new Costs(3, 15, 3.75, 0.3, 0.01);
    public static final Costs TIER_15_75 = new Costs(15, 75, 18.75, 1.5, 0.01);
    public static final Costs TIER_5_25 = new Costs(5, 25, 6.25, 0.5, 0.01);
    public static final Costs TIER_30_150 = new Costs(30, 150, 37.5, 3, 0.01);
    public static final Costs HAIKU_35 = new Costs(0.8, 4, 1, 0.08, 0.01);
    public static final Costs HAIKU_45 = new Costs(1, 5, 1.25, 0.1, 0.01);

    // Default for unknown models
    public static final Costs DEFAULT_COSTS = TIER_5_25;

    /**
     * Model costs map.
     */
    public static final Map<String, Costs> MODEL_COSTS = Map.ofEntries(
        // Haiku models
        Map.entry("claude-3-5-haiku", HAIKU_35),
        Map.entry("claude-haiku-3-5", HAIKU_35),
        Map.entry("claude-4-5-haiku", HAIKU_45),
        Map.entry("claude-haiku-4-5", HAIKU_45),

        // Sonnet models
        Map.entry("claude-3-5-sonnet", TIER_3_15),
        Map.entry("claude-3-5-sonnet-v2", TIER_3_15),
        Map.entry("claude-3-7-sonnet", TIER_3_15),
        Map.entry("claude-4-sonnet", TIER_3_15),
        Map.entry("claude-4-5-sonnet", TIER_3_15),
        Map.entry("claude-sonnet-4-5", TIER_3_15),
        Map.entry("claude-4-6-sonnet", TIER_3_15),
        Map.entry("claude-sonnet-4-6", TIER_3_15),

        // Opus models
        Map.entry("claude-4-opus", TIER_15_75),
        Map.entry("claude-opus-4", TIER_15_75),
        Map.entry("claude-4-1-opus", TIER_15_75),
        Map.entry("claude-opus-4-1", TIER_15_75),
        Map.entry("claude-4-5-opus", TIER_5_25),
        Map.entry("claude-opus-4-5", TIER_5_25),
        Map.entry("claude-4-6-opus", TIER_5_25),
        Map.entry("claude-opus-4-6", TIER_5_25)
    );

    /**
     * Token usage record.
     */
    public record TokenUsage(
        long inputTokens,
        long outputTokens,
        long cacheReadInputTokens,
        long cacheCreationInputTokens,
        int webSearchRequests,
        String speed
    ) {
        public static TokenUsage of(long input, long output) {
            return new TokenUsage(input, output, 0, 0, 0, null);
        }

        public static TokenUsage withCache(long input, long output, long cacheRead, long cacheCreation) {
            return new TokenUsage(input, output, cacheRead, cacheCreation, 0, null);
        }
    }

    /**
     * Get costs for a model.
     */
    public static Costs getModelCosts(String model) {
        return getModelCosts(model, null);
    }

    /**
     * Get costs for a model with usage info (for fast mode detection).
     */
    public static Costs getModelCosts(String model, TokenUsage usage) {
        String shortName = toShortName(model);

        // Special handling for Opus 4.6 fast mode
        if (shortName.equals("claude-opus-4-6") && usage != null && "fast".equals(usage.speed())) {
            return TIER_30_150;
        }

        Costs costs = MODEL_COSTS.get(shortName);
        if (costs == null) {
            return DEFAULT_COSTS;
        }
        return costs;
    }

    /**
     * Calculate USD cost for token usage.
     */
    public static double calculateCost(String model, TokenUsage usage) {
        Costs costs = getModelCosts(model, usage);
        return calculateCost(costs, usage);
    }

    /**
     * Calculate USD cost with known costs.
     */
    public static double calculateCost(Costs costs, TokenUsage usage) {
        return (usage.inputTokens() / 1_000_000.0) * costs.inputTokens() +
               (usage.outputTokens() / 1_000_000.0) * costs.outputTokens() +
               (usage.cacheReadInputTokens() / 1_000_000.0) * costs.promptCacheReadTokens() +
               (usage.cacheCreationInputTokens() / 1_000_000.0) * costs.promptCacheWriteTokens() +
               usage.webSearchRequests() * costs.webSearchRequests();
    }

    /**
     * Format cost as USD string.
     */
    public static String formatCost(double cost) {
        if (cost < 0.01) {
            return String.format("$%.4f", cost);
        }
        return String.format("$%.2f", cost);
    }

    /**
     * Get pricing string for a model.
     */
    public static String getPricingString(String model) {
        Costs costs = getModelCosts(model);
        return costs.formatPricing();
    }

    /**
     * Convert model name to short name.
     */
    private static String toShortName(String model) {
        if (model == null) return "";

        String normalized = model.toLowerCase()
            .replace("anthropic/", "")
            .replace("bedrock/", "")
            .replace("vertex/", "");

        // Handle date suffixes
        normalized = normalized.replaceAll("-\\d{8}$", "");

        return normalized;
    }

    /**
     * Check if model is an Opus model.
     */
    public static boolean isOpusModel(String model) {
        String shortName = toShortName(model);
        return shortName.contains("opus");
    }

    /**
     * Check if model is a Sonnet model.
     */
    public static boolean isSonnetModel(String model) {
        String shortName = toShortName(model);
        return shortName.contains("sonnet");
    }

    /**
     * Check if model is a Haiku model.
     */
    public static boolean isHaikuModel(String model) {
        String shortName = toShortName(model);
        return shortName.contains("haiku");
    }
}