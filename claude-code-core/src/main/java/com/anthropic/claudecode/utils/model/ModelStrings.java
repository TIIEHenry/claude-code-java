/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/model/modelStrings.ts
 */
package com.anthropic.claudecode.utils.model;

import java.util.*;

/**
 * Model string constants for Claude models.
 *
 * These are the canonical model identifiers used throughout the codebase.
 */
public final class ModelStrings {
    private ModelStrings() {}

    // ─── Current Models ─────────────────────────────────────────────────────

    // Claude 4.6 series (latest)
    public static final String OPUS_4_6 = "claude-opus-4-6-20250514";
    public static final String SONNET_4_6 = "claude-sonnet-4-6-20250514";

    // Short aliases for convenience
    public static final String OPUS_46 = OPUS_4_6;
    public static final String SONNET_46 = SONNET_4_6;

    // Claude 4.5 series
    public static final String OPUS_4_5 = "claude-opus-4-5-20250101";
    public static final String HAIKU_4_5 = "claude-haiku-4-5-20251001";

    // Claude 4 series
    public static final String OPUS_4_0 = "claude-opus-4-0-20250514";
    public static final String OPUS_4_1 = "claude-opus-4-1-20250514";
    public static final String SONNET_4_0 = "claude-sonnet-4-0-20250514";
    public static final String SONNET_4_5 = "claude-sonnet-4-5-20250514";

    // Claude 3.5 series
    public static final String SONNET_3_5 = "claude-3-5-sonnet-20241022";
    public static final String SONNET_3_5_NEW = "claude-3-5-sonnet-20240620";
    public static final String HAIKU_3_5 = "claude-3-5-haiku-20241022";

    // Claude 3 series
    public static final String OPUS_3 = "claude-3-opus-20240229";
    public static final String SONNET_3 = "claude-3-sonnet-20240229";
    public static final String HAIKU_3 = "claude-3-haiku-20240307";

    // ─── Model Aliases ──────────────────────────────────────────────────────

    // Short aliases
    public static final String ALIAS_OPUS = "opus";
    public static final String ALIAS_SONNET = "sonnet";
    public static final String ALIAS_HAIKU = "haiku";
    public static final String ALIAS_OPUS_LATEST = "claude-opus-latest";
    public static final String ALIAS_SONNET_LATEST = "claude-sonnet-latest";
    public static final String ALIAS_HAIKU_LATEST = "claude-haiku-latest";

    // ─── Model Lists ────────────────────────────────────────────────────────

    /**
     * All current production models.
     */
    public static final List<String> CURRENT_MODELS = List.of(
        OPUS_4_6,
        SONNET_4_6,
        HAIKU_4_5,
        OPUS_4_1,
        SONNET_4_5,
        SONNET_3_5,
        HAIKU_3_5
    );

    /**
     * Opus models in order of preference (newest first).
     */
    public static final List<String> OPUS_MODELS = List.of(
        OPUS_4_6,
        OPUS_4_1,
        OPUS_4_0,
        OPUS_3
    );

    /**
     * Sonnet models in order of preference (newest first).
     */
    public static final List<String> SONNET_MODELS = List.of(
        SONNET_4_6,
        SONNET_4_5,
        SONNET_4_0,
        SONNET_3_5,
        SONNET_3
    );

    /**
     * Haiku models in order of preference (newest first).
     */
    public static final List<String> HAIKU_MODELS = List.of(
        HAIKU_4_5,
        HAIKU_3_5,
        HAIKU_3
    );

    // ─── Model Features ─────────────────────────────────────────────────────

    /**
     * Models that support extended thinking.
     */
    public static final Set<String> EXTENDED_THINKING_MODELS = Set.of(
        OPUS_4_6,
        SONNET_4_6
    );

    /**
     * Models that support 1M context.
     */
    public static final Set<String> ONE_M_CONTEXT_MODELS = Set.of(
        OPUS_4_6,
        SONNET_4_6,
        HAIKU_4_5,
        OPUS_4_1,
        SONNET_4_5
    );

    /**
     * Models that support computer use.
     */
    public static final Set<String> COMPUTER_USE_MODELS = Set.of(
        OPUS_4_6,
        SONNET_4_6,
        SONNET_3_5,
        SONNET_3_5_NEW
    );

    // ─── Helper Methods ─────────────────────────────────────────────────────

    /**
     * Get model strings for API provider.
     */
    public static ModelStringsInstance getModelStrings() {
        return new ModelStringsInstance();
    }

    /**
     * Model strings instance with accessor methods.
     */
    public static final class ModelStringsInstance {
        public String opus40() { return OPUS_3; }
        public String opus41() { return OPUS_4_1; }
        public String opus45() { return OPUS_4_5; }
        public String opus46() { return OPUS_4_6; }
        public String sonnet40() { return SONNET_4_0; }
        public String sonnet45() { return SONNET_4_5; }
        public String sonnet46() { return SONNET_4_6; }
        public String haiku45() { return HAIKU_4_5; }
    }

    /**
     * Check if a model ID is a valid alias.
     */
    public static boolean isAlias(String modelId) {
        if (modelId == null) return false;
        return Set.of(
            ALIAS_OPUS, ALIAS_SONNET, ALIAS_HAIKU,
            ALIAS_OPUS_LATEST, ALIAS_SONNET_LATEST, ALIAS_HAIKU_LATEST
        ).contains(modelId.toLowerCase());
    }

    /**
     * Resolve an alias to its canonical model ID.
     */
    public static String resolveAlias(String alias) {
        if (alias == null) return null;

        return switch (alias.toLowerCase()) {
            case ALIAS_OPUS, ALIAS_OPUS_LATEST -> OPUS_4_6;
            case ALIAS_SONNET, ALIAS_SONNET_LATEST -> SONNET_4_6;
            case ALIAS_HAIKU, ALIAS_HAIKU_LATEST -> HAIKU_4_5;
            default -> alias;
        };
    }

    /**
     * Check if model supports extended thinking.
     */
    public static boolean supportsExtendedThinking(String modelId) {
        if (modelId == null) return false;
        return EXTENDED_THINKING_MODELS.contains(modelId) ||
               modelId.contains("opus-4-6") ||
               modelId.contains("sonnet-4-6");
    }

    /**
     * Check if model supports 1M context.
     */
    public static boolean supports1MContext(String modelId) {
        if (modelId == null) return false;
        return ONE_M_CONTEXT_MODELS.contains(modelId) ||
               modelId.contains("opus-4") ||
               modelId.contains("sonnet-4") ||
               modelId.contains("haiku-4");
    }

    /**
     * Check if model supports computer use.
     */
    public static boolean supportsComputerUse(String modelId) {
        if (modelId == null) return false;
        return COMPUTER_USE_MODELS.contains(modelId) ||
               modelId.contains("sonnet") ||
               modelId.contains("opus-4");
    }

    /**
     * Get the default Sonnet model.
     */
    public static String getDefaultSonnetModel() {
        return SONNET_4_6;
    }

    /**
     * Get the default Haiku model.
     */
    public static String getDefaultHaikuModel() {
        return HAIKU_4_5;
    }

    /**
     * Get the default Opus model.
     */
    public static String getDefaultOpusModel() {
        return OPUS_4_6;
    }

    /**
     * Check if a model has 1M context variant.
     */
    public static boolean has1mContext(String modelId) {
        if (modelId == null) return false;
        return modelId.endsWith("[1m]") || modelId.toLowerCase().contains("1m");
    }

    /**
     * Strip the 1M context suffix from a model ID.
     */
    public static String strip1mContext(String modelId) {
        if (modelId == null) return null;
        return modelId.replaceAll("\\[1m\\]", "").replaceAll("-1m", "");
    }

    /**
     * Get public display name for a model.
     */
    public static String getPublicModelDisplayName(String modelId) {
        if (modelId == null) return null;

        String stripped = strip1mContext(modelId);
        String suffix = has1mContext(modelId) ? " (1M context)" : "";

        if (stripped.equals(OPUS_4_6) || stripped.equals(OPUS_4_1) || stripped.equals(OPUS_3)) {
            return "Claude Opus" + suffix;
        }
        if (stripped.equals(SONNET_4_6) || stripped.equals(SONNET_4_5) || stripped.equals(SONNET_3_5)) {
            return "Claude Sonnet" + suffix;
        }
        if (stripped.equals(HAIKU_4_5) || stripped.equals(HAIKU_3_5)) {
            return "Claude Haiku" + suffix;
        }

        // Return original if no match
        return modelId;
    }
}