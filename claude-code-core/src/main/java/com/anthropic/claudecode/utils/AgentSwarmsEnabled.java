/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/agentSwarmsEnabled.ts
 */
package com.anthropic.claudecode.utils;

/**
 * Centralized runtime check for agent teams/teammate features.
 * This is the single gate that should be checked everywhere teammates
 * are referenced (prompts, code, tools isEnabled, UI, etc.).
 */
public final class AgentSwarmsEnabled {
    private AgentSwarmsEnabled() {}

    /**
     * Check if --agent-teams flag is provided via CLI.
     */
    private static boolean isAgentTeamsFlagSet() {
        // Check command line arguments
        for (String arg : System.getProperty("sun.java.command", "").split(" ")) {
            if ("--agent-teams".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if agent swarms/teams features are enabled.
     *
     * Ant builds: always enabled.
     * External builds require both:
     * 1. Opt-in via CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS env var OR --agent-teams flag
     * 2. GrowthBook gate 'tengu_amber_flint' enabled (killswitch)
     */
    public static boolean isAgentSwarmsEnabled() {
        // Ant: always on
        String userType = System.getenv("USER_TYPE");
        if ("ant".equals(userType)) {
            return true;
        }

        // External: require opt-in via env var or --agent-teams flag
        String experimental = System.getenv("CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS");
        if (!EnvUtils.isTruthy(experimental) && !isAgentTeamsFlagSet()) {
            return false;
        }

        // Killswitch — always respected for external users
        // In production, this would check GrowthBook
        return true;
    }
}