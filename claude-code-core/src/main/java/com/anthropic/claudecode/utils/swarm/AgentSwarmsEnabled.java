/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code agent swarms enabled utilities
 */
package com.anthropic.claudecode.utils.swarm;

import java.util.*;

/**
 * Utilities for checking if agent teams/teammate features are enabled.
 */
public final class AgentSwarmsEnabled {
    private AgentSwarmsEnabled() {}

    /**
     * Check if --agent-teams flag is provided via CLI.
     */
    private static boolean isAgentTeamsFlagSet() {
        // Would check command line arguments in real implementation
        String cliArgs = System.getenv("CLAUDE_CLI_ARGS");
        return cliArgs != null && cliArgs.contains("--agent-teams");
    }

    /**
     * Centralized runtime check for agent teams/teammate features.
     * This is the single gate for teammate functionality.
     *
     * Ant builds: always enabled.
     * External builds require both:
     * 1. Opt-in via env var OR --agent-teams flag
     * 2. Feature flag gate (killswitch)
     */
    public static boolean isAgentSwarmsEnabled() {
        // Ant: always on
        if ("ant".equals(System.getenv("USER_TYPE"))) {
            return true;
        }

        // External: require opt-in via env var or --agent-teams flag
        boolean envTruthy = isEnvTruthy(System.getenv("CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS"));
        if (!envTruthy && !isAgentTeamsFlagSet()) {
            return false;
        }

        // Killswitch — always respected for external users
        if (!getFeatureValue("tengu_amber_flint", true)) {
            return false;
        }

        return true;
    }

    /**
     * Check if environment variable is truthy.
     */
    private static boolean isEnvTruthy(String value) {
        if (value == null) return false;
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "yes".equalsIgnoreCase(value);
    }

    /**
     * Get feature flag value (placeholder for GrowthBook integration).
     */
    private static boolean getFeatureValue(String feature, boolean defaultValue) {
        // Would integrate with feature flag system in real implementation
        return defaultValue;
    }
}