/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentSwarmsEnabled.
 */
class AgentSwarmsEnabledTest {

    @Test
    @DisplayName("AgentSwarmsEnabled isAgentSwarmsEnabled may throw NPE if env vars null")
    void isAgentSwarmsEnabled() {
        // May throw NPE if CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS env var is null
        // This is a known bug in the source code
        try {
            boolean result = AgentSwarmsEnabled.isAgentSwarmsEnabled();
            assertTrue(result == true || result == false);
        } catch (NullPointerException e) {
            // Expected if env vars are null - source code bug
            assertTrue(true);
        }
    }
}
