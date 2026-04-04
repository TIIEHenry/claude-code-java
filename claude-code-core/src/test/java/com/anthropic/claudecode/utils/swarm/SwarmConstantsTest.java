/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.swarm;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SwarmConstants.
 */
@DisplayName("SwarmConstants Tests")
class SwarmConstantsTest {

    @Test
    @DisplayName("SwarmConstants TEAM_LEAD_NAME is correct")
    void teamLeadNameIsCorrect() {
        assertEquals("team-lead", SwarmConstants.TEAM_LEAD_NAME);
    }

    @Test
    @DisplayName("SwarmConstants SWARM_SESSION_NAME is correct")
    void swarmSessionNameIsCorrect() {
        assertEquals("claude-swarm", SwarmConstants.SWARM_SESSION_NAME);
    }

    @Test
    @DisplayName("SwarmConstants SWARM_VIEW_WINDOW_NAME is correct")
    void swarmViewWindowNameIsCorrect() {
        assertEquals("swarm-view", SwarmConstants.SWARM_VIEW_WINDOW_NAME);
    }

    @Test
    @DisplayName("SwarmConstants TMUX_COMMAND is correct")
    void tmuxCommandIsCorrect() {
        assertEquals("tmux", SwarmConstants.TMUX_COMMAND);
    }

    @Test
    @DisplayName("SwarmConstants HIDDEN_SESSION_NAME is correct")
    void hiddenSessionNameIsCorrect() {
        assertEquals("claude-hidden", SwarmConstants.HIDDEN_SESSION_NAME);
    }

    @Test
    @DisplayName("SwarmConstants env vars are correct")
    void envVarsAreCorrect() {
        assertEquals("CLAUDE_CODE_TEAMMATE_COMMAND", SwarmConstants.TEAMMATE_COMMAND_ENV_VAR);
        assertEquals("CLAUDE_CODE_AGENT_COLOR", SwarmConstants.TEAMMATE_COLOR_ENV_VAR);
        assertEquals("CLAUDE_CODE_PLAN_MODE_REQUIRED", SwarmConstants.PLAN_MODE_REQUIRED_ENV_VAR);
    }

    @Test
    @DisplayName("SwarmConstants getSwarmSocketName contains pid")
    void getSwarmSocketNameContainsPid() {
        String socketName = SwarmConstants.getSwarmSocketName();

        assertTrue(socketName.startsWith("claude-swarm-"));
        assertTrue(socketName.contains(String.valueOf(ProcessHandle.current().pid())));
    }

    @Test
    @DisplayName("SwarmConstants getSwarmSocketName is unique per process")
    void getSwarmSocketNameIsUniquePerProcess() {
        String name1 = SwarmConstants.getSwarmSocketName();
        String name2 = SwarmConstants.getSwarmSocketName();

        // Should be the same within the same process
        assertEquals(name1, name2);
    }
}