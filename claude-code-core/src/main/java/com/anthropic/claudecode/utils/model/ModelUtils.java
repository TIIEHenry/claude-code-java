/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/model/model.ts
 */
package com.anthropic.claudecode.utils.model;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Model identification and utilities.
 */
public final class ModelUtils {
    private ModelUtils() {}

    // Model families
    public static final String FAMILY_OPUS = "opus";
    public static final String FAMILY_SONNET = "sonnet";
    public static final String FAMILY_HAIKU = "haiku";

    // Model ID patterns
    private static final Pattern MODEL_ID_PATTERN = Pattern.compile(
        "claude-(opus|sonnet|haiku)[-]?(\\d+(?:\\.\\d+)*)?(?:-(\\d{8}))?"
    );

    // Model aliases
    private static final Map<String, String> MODEL_ALIASES = Map.of(
        "claude-3-opus", "claude-3-opus-20240229",
        "claude-3-sonnet", "claude-3-sonnet-20240229",
        "claude-3-haiku", "claude-3-haiku-20240307",
        "claude-3.5-sonnet", "claude-3-5-sonnet-20241022",
        "claude-3.5-haiku", "claude-3-5-haiku-20241022",
        "claude-4-opus", "claude-opus-4-6-20250514",
        "claude-4-sonnet", "claude-sonnet-4-6-20250514",
        "claude-4-haiku", "claude-haiku-4-5-20251001"
    );

    // Marketing names
    private static final Map<String, String> MARKETING_NAMES = Map.of(
        "claude-opus-4-6-20250514", "Claude Opus 4.6",
        "claude-sonnet-4-6-20250514", "Claude Sonnet 4.6",
        "claude-haiku-4-5-20251001", "Claude Haiku 4.5",
        "claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet",
        "claude-3-5-haiku-20241022", "Claude 3.5 Haiku",
        "claude-3-opus-20240229", "Claude 3 Opus"
    );

    /**
     * Model info record.
     */
    public record ModelInfo(
            String id,
            String canonicalId,
            String family,
            String version,
            String marketingName,
            int contextWindow,
            int maxOutputTokens,
            boolean supportsVision,
            boolean supportsTools,
            boolean supportsStreaming
    ) {}

    /**
     * Get canonical model ID from any alias or partial ID.
     */
    public static String getCanonicalName(String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            return "claude-sonnet-4-6-20250514"; // Default
        }

        String lower = modelId.toLowerCase().trim();

        // Check aliases
        if (MODEL_ALIASES.containsKey(lower)) {
            return MODEL_ALIASES.get(lower);
        }

        // Already canonical
        if (modelId.startsWith("claude-") && modelId.contains("-20")) {
            return modelId;
        }

        return modelId;
    }

    /**
     * Get marketing name for a model.
     */
    public static String getMarketingNameForModel(String modelId) {
        String canonical = getCanonicalName(modelId);
        return MARKETING_NAMES.getOrDefault(canonical, canonical);
    }

    /**
     * Get model family (opus, sonnet, haiku).
     */
    public static String getModelFamily(String modelId) {
        if (modelId == null) return FAMILY_SONNET;

        String lower = modelId.toLowerCase();
        if (lower.contains("opus")) return FAMILY_OPUS;
        if (lower.contains("haiku")) return FAMILY_HAIKU;
        return FAMILY_SONNET;
    }

    /**
     * Check if model is Opus family.
     */
    public static boolean isOpus(String modelId) {
        return FAMILY_OPUS.equals(getModelFamily(modelId));
    }

    /**
     * Check if model is Sonnet family.
     */
    public static boolean isSonnet(String modelId) {
        return FAMILY_SONNET.equals(getModelFamily(modelId));
    }

    /**
     * Check if model is Haiku family.
     */
    public static boolean isHaiku(String modelId) {
        return FAMILY_HAIKU.equals(getModelFamily(modelId));
    }

    /**
     * Get max output tokens for a model.
     */
    public static int getMaxOutputTokens(String modelId) {
        String canonical = getCanonicalName(modelId);

        if (canonical.contains("opus-4-6") || canonical.contains("sonnet-4-6")) {
            return 16384;
        }
        if (canonical.contains("opus-4-5")) {
            return 4096;
        }
        if (canonical.contains("3-5-sonnet") || canonical.contains("3-5-haiku")) {
            return 8192;
        }
        if (canonical.contains("opus")) {
            return 4096;
        }
        return 8192; // Default
    }

    /**
     * Check if model supports vision.
     */
    public static boolean supportsVision(String modelId) {
        // All current Claude models support vision
        return true;
    }

    /**
     * Check if model supports tools.
     */
    public static boolean supportsTools(String modelId) {
        // All current Claude models support tools
        return true;
    }

    /**
     * Get full model info.
     */
    public static ModelInfo getModelInfo(String modelId) {
        String canonical = getCanonicalName(modelId);
        String family = getModelFamily(modelId);

        return new ModelInfo(
            modelId,
            canonical,
            family,
            extractVersion(canonical),
            getMarketingNameForModel(canonical),
            com.anthropic.claudecode.utils.Tokens.getContextWindowSize(canonical),
            getMaxOutputTokens(canonical),
            supportsVision(canonical),
            supportsTools(canonical),
            true
        );
    }

    private static String extractVersion(String modelId) {
        var matcher = MODEL_ID_PATTERN.matcher(modelId);
        if (matcher.find()) {
            return matcher.group(2) != null ? matcher.group(2) : "unknown";
        }
        return "unknown";
    }

    /**
     * Compare model capabilities.
     */
    public static int compareCapabilities(String model1, String model2) {
        int tier1 = getModelTier(model1);
        int tier2 = getModelTier(model2);
        return Integer.compare(tier1, tier2);
    }

    private static int getModelTier(String modelId) {
        String family = getModelFamily(modelId);
        return switch (family) {
            case FAMILY_OPUS -> 3;
            case FAMILY_SONNET -> 2;
            case FAMILY_HAIKU -> 1;
            default -> 0;
        };
    }
}