/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/betas
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Betas - Beta feature management.
 */
public final class Betas {
    private static final Set<String> activeBetas = ConcurrentHashMap.newKeySet();
    private static final Map<String, BetaFeature> betaFeatures = new ConcurrentHashMap<>();

    /**
     * Beta feature record.
     */
    public record BetaFeature(
        String name,
        String description,
        BetaStatus status,
        String rolloutPercentage,
        List<String> requirements
    ) {
        public boolean isEnabled() {
            return status == BetaStatus.ENABLED || status == BetaStatus.ROLLOUT;
        }
    }

    /**
     * Beta status enum.
     */
    public enum BetaStatus {
        DISABLED,
        INTERNAL,
        ROLLOUT,
        ENABLED,
        DEPRECATED
    }

    /**
     * Register beta feature.
     */
    public static void register(BetaFeature feature) {
        betaFeatures.put(feature.name(), feature);
    }

    /**
     * Enable beta.
     */
    public static void enable(String betaName) {
        activeBetas.add(betaName);
        BetaFeature feature = betaFeatures.get(betaName);
        if (feature != null) {
            betaFeatures.put(betaName, new BetaFeature(
                feature.name(),
                feature.description(),
                BetaStatus.ENABLED,
                feature.rolloutPercentage(),
                feature.requirements()
            ));
        }
    }

    /**
     * Disable beta.
     */
    public static void disable(String betaName) {
        activeBetas.remove(betaName);
    }

    /**
     * Check if beta is active.
     */
    public static boolean isActive(String betaName) {
        return activeBetas.contains(betaName);
    }

    /**
     * Check if beta is enabled.
     */
    public static boolean isEnabled(String betaName) {
        BetaFeature feature = betaFeatures.get(betaName);
        if (feature == null) return false;
        return feature.isEnabled() || activeBetas.contains(betaName);
    }

    /**
     * Get all active betas.
     */
    public static Set<String> getActiveBetas() {
        return Collections.unmodifiableSet(activeBetas);
    }

    /**
     * Get all beta features.
     */
    public static Map<String, BetaFeature> getAllFeatures() {
        return Collections.unmodifiableMap(betaFeatures);
    }

    /**
     * Get beta feature by name.
     */
    public static BetaFeature getFeature(String name) {
        return betaFeatures.get(name);
    }

    /**
     * Standard beta headers for API requests.
     */
    public static List<String> getBetaHeaders() {
        List<String> headers = new ArrayList<>();
        for (String beta : activeBetas) {
            headers.add("anthropic-beta:" + beta);
        }
        return headers;
    }

    /**
     * Parse beta from string.
     */
    public static BetaFeature parse(String betaString) {
        // Parse format like "feature-name:description"
        String[] parts = betaString.split(":", 2);
        String name = parts[0];
        String description = parts.length > 1 ? parts[1] : "";

        return new BetaFeature(
            name,
            description,
            BetaStatus.INTERNAL,
            "0%",
            Collections.emptyList()
        );
    }
}