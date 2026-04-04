/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code plan mode V2 utilities
 */
package com.anthropic.claudecode.utils.plan;

/**
 * Plan mode V2 configuration utilities.
 */
public final class PlanModeV2 {
    private PlanModeV2() {}

    /**
     * Pewter ledger variant for plan file structure experiment.
     */
    public enum PewterLedgerVariant {
        TRIM,
        CUT,
        CAP,
        CONTROL
    }

    /**
     * Get plan mode V2 agent count.
     */
    public static int getAgentCount() {
        // Environment variable override takes precedence
        String envValue = System.getenv("CLAUDE_CODE_PLAN_V2_AGENT_COUNT");
        if (envValue != null) {
            try {
                int count = Integer.parseInt(envValue);
                if (count > 0 && count <= 10) {
                    return count;
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Check subscription type
        String subscriptionType = getSubscriptionType();
        String rateLimitTier = getRateLimitTier();

        if ("max".equals(subscriptionType) &&
            "default_claude_max_20x".equals(rateLimitTier)) {
            return 3;
        }

        if ("enterprise".equals(subscriptionType) ||
            "team".equals(subscriptionType)) {
            return 3;
        }

        return 1;
    }

    /**
     * Get plan mode V2 explore agent count.
     */
    public static int getExploreAgentCount() {
        String envValue = System.getenv("CLAUDE_CODE_PLAN_V2_EXPLORE_AGENT_COUNT");
        if (envValue != null) {
            try {
                int count = Integer.parseInt(envValue);
                if (count > 0 && count <= 10) {
                    return count;
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        return 3;
    }

    /**
     * Check if plan mode interview phase is enabled.
     */
    public static boolean isInterviewPhaseEnabled() {
        // Always on for ants
        if ("ant".equals(System.getenv("USER_TYPE"))) {
            return true;
        }

        String env = System.getenv("CLAUDE_CODE_PLAN_MODE_INTERVIEW_PHASE");
        if ("true".equalsIgnoreCase(env)) return true;
        if ("false".equalsIgnoreCase(env)) return false;

        // Check feature flag (placeholder)
        return getFeatureValue("tengu_plan_mode_interview_phase", false);
    }

    /**
     * Get pewter ledger variant.
     */
    public static PewterLedgerVariant getPewterLedgerVariant() {
        String raw = getFeatureValue("tengu_pewter_ledger", null);
        if ("trim".equals(raw)) return PewterLedgerVariant.TRIM;
        if ("cut".equals(raw)) return PewterLedgerVariant.CUT;
        if ("cap".equals(raw)) return PewterLedgerVariant.CAP;
        return PewterLedgerVariant.CONTROL;
    }

    // Placeholder methods - would integrate with actual auth/feature systems

    private static String getSubscriptionType() {
        String env = System.getenv("CLAUDE_CODE_SUBSCRIPTION_TYPE");
        return env != null ? env : "free";
    }

    private static String getRateLimitTier() {
        String env = System.getenv("CLAUDE_CODE_RATE_LIMIT_TIER");
        return env != null ? env : "default";
    }

    private static <T> T getFeatureValue(String feature, T defaultValue) {
        // Placeholder - would integrate with feature flag system
        return defaultValue;
    }
}