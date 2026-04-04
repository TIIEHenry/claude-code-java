/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/betas.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Beta feature management utilities.
 */
public final class BetaUtils {
    private BetaUtils() {}

    // Beta header cache per model
    private static final Map<String, List<String>> betaCache = new ConcurrentHashMap<>();

    // Model-specific beta features
    private static final Map<String, List<String>> MODEL_BETAS = Map.of(
        "claude-sonnet-4-6", List.of("interleaved-thinking-2025-05-14"),
        "claude-opus-4-6", List.of("interleaved-thinking-2025-05-14"),
        "claude-haiku-4-5-20251001", List.of("interleaved-thinking-2025-05-14")
    );

    // Global beta headers
    private static final List<String> GLOBAL_BETAS = List.of(
        "claude-code-20250219",
        "token-efficient-tools-2026-03-28"
    );

    // Latched beta states
    private static volatile boolean afkModeHeaderLatched = false;
    private static volatile boolean fastModeHeaderLatched = false;
    private static volatile boolean cacheEditingHeaderLatched = false;

    /**
     * Get beta headers for a model.
     */
    public static List<String> getModelBetas(String modelId) {
        if (modelId == null) {
            return new ArrayList<>(GLOBAL_BETAS);
        }

        return betaCache.computeIfAbsent(modelId, model -> {
            List<String> betas = new ArrayList<>(GLOBAL_BETAS);

            // Add model-specific betas
            String canonical = model.toLowerCase();
            for (Map.Entry<String, List<String>> entry : MODEL_BETAS.entrySet()) {
                if (canonical.contains(entry.getKey().toLowerCase())) {
                    betas.addAll(entry.getValue());
                    break;
                }
            }

            return betas;
        });
    }

    /**
     * Check if a beta is enabled for a model.
     */
    public static boolean isBetaEnabled(String modelId, String betaName) {
        return getModelBetas(modelId).contains(betaName);
    }

    /**
     * Add beta header for a model.
     */
    public static void addBeta(String modelId, String beta) {
        List<String> betas = new ArrayList<>(getModelBetas(modelId));
        if (!betas.contains(beta)) {
            betas.add(beta);
            betaCache.put(modelId, betas);
        }
    }

    /**
     * Remove beta header for a model.
     */
    public static void removeBeta(String modelId, String beta) {
        List<String> betas = new ArrayList<>(getModelBetas(modelId));
        betas.remove(beta);
        betaCache.put(modelId, betas);
    }

    /**
     * Clear beta cache.
     */
    public static void clearBetaCache() {
        betaCache.clear();
    }

    // Latch management for sticky beta headers

    public static boolean isAfkModeHeaderLatched() {
        return afkModeHeaderLatched;
    }

    public static void setAfkModeHeaderLatched(boolean latched) {
        afkModeHeaderLatched = latched;
    }

    public static boolean isFastModeHeaderLatched() {
        return fastModeHeaderLatched;
    }

    public static void setFastModeHeaderLatched(boolean latched) {
        fastModeHeaderLatched = latched;
    }

    public static boolean isCacheEditingHeaderLatched() {
        return cacheEditingHeaderLatched;
    }

    public static void setCacheEditingHeaderLatched(boolean latched) {
        cacheEditingHeaderLatched = latched;
    }

    /**
     * Check if should use global cache scope.
     */
    public static boolean shouldUseGlobalCacheScope() {
        // Check if cache-editing beta is enabled
        return cacheEditingHeaderLatched;
    }

    /**
     * Get all latched beta headers.
     */
    public static List<String> getLatchedBetaHeaders() {
        List<String> headers = new ArrayList<>();

        if (afkModeHeaderLatched) {
            headers.add("afk-mode-2026-01-31");
        }
        if (fastModeHeaderLatched) {
            headers.add("fast-mode-2026-02-01");
        }
        if (cacheEditingHeaderLatched) {
            headers.add("prompt-caching-scope-2026-01-05");
        }

        return headers;
    }

    /**
     * Clear all latches.
     */
    public static void clearLatches() {
        afkModeHeaderLatched = false;
        fastModeHeaderLatched = false;
        cacheEditingHeaderLatched = false;
    }
}