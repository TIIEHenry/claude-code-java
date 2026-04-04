/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tips/Tip.java
 */
package com.anthropic.claudecode.services.tips;

import java.util.concurrent.CompletableFuture;

/**
 * A tip that can be shown to users.
 */
public record Tip(
    String id,
    TipContentProvider contentProvider,
    int cooldownSessions,
    TipRelevanceChecker isRelevant
) {
    /**
     * Functional interface for providing tip content.
     */
    @FunctionalInterface
    public interface TipContentProvider {
        String getContent(TipContext context);
    }

    /**
     * Functional interface for checking tip relevance.
     */
    @FunctionalInterface
    public interface TipRelevanceChecker {
        boolean isRelevant(TipContext context);
    }

    /**
     * Create a simple tip with static content.
     */
    public static Tip of(String id, String content, int cooldownSessions) {
        return new Tip(id, ctx -> content, cooldownSessions, ctx -> true);
    }

    /**
     * Create a tip with dynamic content.
     */
    public static Tip of(String id, TipContentProvider content, int cooldownSessions, TipRelevanceChecker relevance) {
        return new Tip(id, content, cooldownSessions, relevance);
    }

    /**
     * Get the content for this tip.
     */
    public String getContent(TipContext context) {
        return contentProvider.getContent(context);
    }

    /**
     * Check if this tip is relevant for the given context.
     */
    public boolean checkRelevance(TipContext context) {
        return isRelevant.isRelevant(context);
    }
}