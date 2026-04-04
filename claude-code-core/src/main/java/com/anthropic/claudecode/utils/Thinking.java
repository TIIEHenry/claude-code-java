/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code thinking utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

/**
 * Utilities for thinking mode and ultrathink support.
 */
public final class Thinking {
    private Thinking() {}

    /**
     * Thinking configuration.
     */
    public sealed interface ThinkingConfig permits
            ThinkingConfig.Adaptive,
            ThinkingConfig.Enabled,
            ThinkingConfig.Disabled {

        String type();

        public static final class Adaptive implements ThinkingConfig {
            @Override public String type() { return "adaptive"; }
        }

        public static final class Enabled implements ThinkingConfig {
            private final int budgetTokens;

            public Enabled(int budgetTokens) {
                this.budgetTokens = budgetTokens;
            }

            public int budgetTokens() { return budgetTokens; }
            @Override public String type() { return "enabled"; }
        }

        public static final class Disabled implements ThinkingConfig {
            @Override public String type() { return "disabled"; }
        }
    }

    // Rainbow colors for ultrathink highlighting
    private static final String[] RAINBOW_COLORS = {
            "rainbow_red",
            "rainbow_orange",
            "rainbow_yellow",
            "rainbow_green",
            "rainbow_blue",
            "rainbow_indigo",
            "rainbow_violet"
    };

    private static final String[] RAINBOW_SHIMMER_COLORS = {
            "rainbow_red_shimmer",
            "rainbow_orange_shimmer",
            "rainbow_yellow_shimmer",
            "rainbow_green_shimmer",
            "rainbow_blue_shimmer",
            "rainbow_indigo_shimmer",
            "rainbow_violet_shimmer"
    };

    // Ultrathink keyword pattern
    private static final Pattern ULTRATHINK_PATTERN = Pattern.compile("\\bultrathink\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Check if ultrathink is enabled.
     */
    public static boolean isUltrathinkEnabled() {
        return EnvUtils.isTruthy(System.getenv("CLAUDE_CODE_ULTRATHINK"));
    }

    /**
     * Check if text contains the "ultrathink" keyword.
     */
    public static boolean hasUltrathinkKeyword(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return ULTRATHINK_PATTERN.matcher(text).find();
    }

    /**
     * Find positions of "ultrathink" keyword in text.
     */
    public static List<ThinkingTriggerPosition> findThinkingTriggerPositions(String text) {
        List<ThinkingTriggerPosition> positions = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return positions;
        }

        Matcher matcher = ULTRATHINK_PATTERN.matcher(text);
        while (matcher.find()) {
            positions.add(new ThinkingTriggerPosition(
                    matcher.group(),
                    matcher.start(),
                    matcher.end()
            ));
        }

        return positions;
    }

    /**
     * Get rainbow color for character index.
     */
    public static String getRainbowColor(int charIndex, boolean shimmer) {
        String[] colors = shimmer ? RAINBOW_SHIMMER_COLORS : RAINBOW_COLORS;
        return colors[charIndex % colors.length];
    }

    /**
     * Check if model supports thinking.
     */
    public static boolean modelSupportsThinking(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }

        String canonical = getCanonicalName(model);
        String provider = getAPIProvider();

        // First party and Foundry: all Claude 4+ models
        if ("foundry".equals(provider) || "firstParty".equals(provider)) {
            return !canonical.contains("claude-3-");
        }

        // Third party: only Opus 4+ and Sonnet 4+
        return canonical.contains("sonnet-4") || canonical.contains("opus-4");
    }

    /**
     * Check if model supports adaptive thinking.
     */
    public static boolean modelSupportsAdaptiveThinking(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }

        String canonical = getCanonicalName(model);

        // Supported by Opus 4.6 and Sonnet 4.6
        if (canonical.contains("opus-4-6") || canonical.contains("sonnet-4-6")) {
            return true;
        }

        // Exclude legacy models
        if (canonical.contains("opus") || canonical.contains("sonnet") || canonical.contains("haiku")) {
            return false;
        }

        // Default to true for first party and Foundry
        String provider = getAPIProvider();
        return "firstParty".equals(provider) || "foundry".equals(provider);
    }

    /**
     * Check if thinking should be enabled by default.
     */
    public static boolean shouldEnableThinkingByDefault() {
        String maxTokens = System.getenv("MAX_THINKING_TOKENS");
        if (maxTokens != null) {
            try {
                return Integer.parseInt(maxTokens) > 0;
            } catch (NumberFormatException e) {
                // Fall through
            }
        }

        // Enable thinking by default unless explicitly disabled
        return !EnvUtils.isTruthy(System.getenv("CLAUDE_CODE_DISABLE_THINKING"));
    }

    /**
     * Get canonical model name.
     */
    private static String getCanonicalName(String model) {
        if (model == null) return "";
        String lower = model.toLowerCase();

        if (lower.contains("opus-4-6")) return "claude-opus-4-6";
        if (lower.contains("opus-4-5")) return "claude-opus-4-5";
        if (lower.contains("opus-4")) return "claude-opus-4";
        if (lower.contains("sonnet-4-6")) return "claude-sonnet-4-6";
        if (lower.contains("sonnet-4-5")) return "claude-sonnet-4-5";
        if (lower.contains("sonnet-4")) return "claude-sonnet-4";
        if (lower.contains("sonnet-3-7")) return "claude-sonnet-3-7";
        if (lower.contains("haiku-4-5")) return "claude-haiku-4-5";
        if (lower.contains("haiku-3-5")) return "claude-haiku-3-5";
        if (lower.contains("haiku")) return "claude-haiku";

        return lower;
    }

    /**
     * Get API provider.
     */
    private static String getAPIProvider() {
        String provider = System.getenv("CLAUDE_CODE_API_PROVIDER");
        return provider != null ? provider : "firstParty";
    }

    /**
     * Thinking trigger position.
     */
    public record ThinkingTriggerPosition(String word, int start, int end) {}
}