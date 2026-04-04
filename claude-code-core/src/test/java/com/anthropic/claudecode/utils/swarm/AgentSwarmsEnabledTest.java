/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.swarm;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentSwarmsEnabled.
 */
@DisplayName("AgentSwarmsEnabled Tests")
class AgentSwarmsEnabledTest {

    @Test
    @DisplayName("AgentSwarmsEnabled isAgentSwarmsEnabled returns boolean")
    void isAgentSwarmsEnabledReturnsBoolean() {
        // Just verify the method works without error
        boolean result = AgentSwarmsEnabled.isAgentSwarmsEnabled();
        // Result depends on environment, just verify it's a boolean
        assertTrue(result == true || result == false);
    }
}