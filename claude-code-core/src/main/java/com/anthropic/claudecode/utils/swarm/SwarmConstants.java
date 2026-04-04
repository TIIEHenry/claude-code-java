/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code swarm constants
 */
package com.anthropic.claudecode.utils.swarm;

/**
 * Swarm constants for agent team coordination.
 */
public final class SwarmConstants {
    private SwarmConstants() {}

    public static final String TEAM_LEAD_NAME = "team-lead";
    public static final String SWARM_SESSION_NAME = "claude-swarm";
    public static final String SWARM_VIEW_WINDOW_NAME = "swarm-view";
    public static final String TMUX_COMMAND = "tmux";
    public static final String HIDDEN_SESSION_NAME = "claude-hidden";

    /**
     * Environment variable to override the command used to spawn teammate instances.
     */
    public static final String TEAMMATE_COMMAND_ENV_VAR = "CLAUDE_CODE_TEAMMATE_COMMAND";

    /**
     * Environment variable set on spawned teammates to indicate their assigned color.
     */
    public static final String TEAMMATE_COLOR_ENV_VAR = "CLAUDE_CODE_AGENT_COLOR";

    /**
     * Environment variable set on spawned teammates to require plan mode.
     */
    public static final String PLAN_MODE_REQUIRED_ENV_VAR = "CLAUDE_CODE_PLAN_MODE_REQUIRED";

    /**
     * Gets the socket name for external swarm sessions.
     * Uses a separate socket to isolate swarm operations.
     * Includes PID to ensure multiple Claude instances don't conflict.
     */
    public static String getSwarmSocketName() {
        long pid = ProcessHandle.current().pid();
        return "claude-swarm-" + pid;
    }
}