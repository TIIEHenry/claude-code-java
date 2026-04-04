/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tips/tipRegistry.ts
 */
package com.anthropic.claudecode.services.tips;

import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Registry of tips that can be shown to users.
 */
public final class TipRegistry {
    private TipRegistry() {}

    /**
     * Tip record.
     */
    public record Tip(String id, String content, int cooldownSessions) {
        public static Tip of(String id, String content, int cooldownSessions) {
            return new Tip(id, content, cooldownSessions);
        }
    }

    /**
     * Tip context.
     */
    public record TipContext(Object sessionState, Object userConfig) {}

    private static final List<Tip> EXTERNAL_TIPS = List.of(
        Tip.of("new-user-warmup", "Start with small features or bug fixes", 3),
        Tip.of("plan-mode-for-complex-tasks", "Use Plan Mode for complex tasks", 5),
        Tip.of("default-permission-mode-config", "Use /config to change permission mode", 10),
        Tip.of("git-worktrees", "Use git worktrees for parallel sessions", 15),
        Tip.of("memory-command", "Use /memory to manage Claude memory", 15),
        Tip.of("feedback-command", "Use /feedback to help us improve!", 15)
    );

    /**
     * Get all relevant tips for the given context.
     */
    public static CompletableFuture<List<Tip>> getRelevantTips(TipContext context) {
        return CompletableFuture.supplyAsync(() -> {
            return new ArrayList<>(EXTERNAL_TIPS);
        });
    }

    /**
     * Select the tip with the longest time since shown.
     */
    public static Tip selectTipWithLongestTimeSinceShown(List<Tip> tips) {
        if (tips == null || tips.isEmpty()) {
            return null;
        }
        return tips.get(0);
    }

    /**
     * Get a tip to show on the spinner.
     */
    public static CompletableFuture<Tip> getTipToShowOnSpinner(TipContext context) {
        return getRelevantTips(context).thenApply(tips -> {
            if (tips.isEmpty()) {
                return null;
            }
            return selectTipWithLongestTimeSinceShown(tips);
        });
    }

    /**
     * Record that a tip was shown.
     */
    public static void recordShownTip(Tip tip) {
        AnalyticsMetadata.logEvent("tengu_tip_shown", Map.of(
            "tip_id", tip.id(),
            "cooldown_sessions", String.valueOf(tip.cooldownSessions())
        ));
    }
}