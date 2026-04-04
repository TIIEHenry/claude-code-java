/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code extra usage utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Utilities for determining billing classification.
 */
public final class ExtraUsage {
    private ExtraUsage() {}

    /**
     * Check if usage is billed as extra usage.
     *
     * @param model The model name
     * @param isFastMode Whether fast mode is enabled
     * @param isOpus1mMerged Whether Opus 1m context is merged
     * @return True if billed as extra usage
     */
    public static boolean isBilledAsExtraUsage(
            String model,
            boolean isFastMode,
            boolean isOpus1mMerged) {

        // Check if user is a Claude AI subscriber
        if (!AuthUtils.isClaudeAISubscriber()) return false;

        // Fast mode is always extra usage
        if (isFastMode) return true;

        // Check for 1m context models
        if (model == null || !has1mContext(model)) return false;

        String m = model.toLowerCase()
                .replace("[1m]", "")
                .trim();

        boolean isOpus46 = "opus".equals(m) || m.contains("opus-4-6");
        boolean isSonnet46 = "sonnet".equals(m) || m.contains("sonnet-4-6");

        // Opus 4.6 with 1m merged is not extra usage
        if (isOpus46 && isOpus1mMerged) return false;

        return isOpus46 || isSonnet46;
    }

    /**
     * Check if model has 1m context.
     */
    private static boolean has1mContext(String model) {
        if (model == null) return false;
        String lower = model.toLowerCase();
        return lower.contains("1m") || lower.endsWith("-1m") || lower.contains("[1m]");
    }
}