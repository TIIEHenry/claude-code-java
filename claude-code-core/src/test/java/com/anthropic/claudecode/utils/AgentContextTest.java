/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentContext.
 */
class AgentContextTest {

    @BeforeEach
    void setUp() {
        AgentContext.AgentContextValue context = AgentContext.getAgentContext();
        // Clean up any existing context
    }

    @Test
    @DisplayName("AgentContext getAgentContext initially null")
    void getAgentContextInitiallyNull() {
        assertNull(AgentContext.getAgentContext());
    }

    @Test
    @DisplayName("AgentContext SubagentContext record")
    void subagentContext() {
        AgentContext.SubagentContext context = new AgentContext.SubagentContext(
            "agent-1", "session-1", "test-agent", true, null, null, false
        );
        assertEquals("agent-1", context.agentId());
        assertEquals("session-1", context.parentSessionId());
        assertEquals("test-agent", context.subagentName());
        assertTrue(context.isBuiltIn());
    }

    @Test
    @DisplayName("AgentContext TeammateAgentContext record")
    void teammateAgentContext() {
        AgentContext.TeammateAgentContext context = new AgentContext.TeammateAgentContext(
            "agent-1", "Teammate", "team-1", "blue", false, "session-1", true, null, null, false
        );
        assertEquals("agent-1", context.agentId());
        assertEquals("Teammate", context.agentName());
        assertEquals("team-1", context.teamName());
        assertTrue(context.isTeamLead());
    }

    @Test
    @DisplayName("AgentContext isSubagentContext true")
    void isSubagentContextTrue() {
        AgentContext.SubagentContext context = new AgentContext.SubagentContext(
            "agent-1", "session-1", "test-agent", true, null, null, false
        );
        assertTrue(AgentContext.isSubagentContext(context));
    }

    @Test
    @DisplayName("AgentContext isSubagentContext false")
    void isSubagentContextFalse() {
        AgentContext.TeammateAgentContext context = new AgentContext.TeammateAgentContext(
            "agent-1", "Teammate", "team-1", "blue", false, "session-1", true, null, null, false
        );
        assertFalse(AgentContext.isSubagentContext(context));
    }

    @Test
    @DisplayName("AgentContext getSubagentLogName built-in")
    void getSubagentLogNameBuiltIn() throws Exception {
        AgentContext.SubagentContext context = new AgentContext.SubagentContext(
            "agent-1", "session-1", "test-agent", true, null, null, false
        );
        String result = AgentContext.runWithAgentContext(context, () -> AgentContext.getSubagentLogName());
        assertEquals("test-agent", result);
    }

    @Test
    @DisplayName("AgentContext getSubagentLogName user-defined")
    void getSubagentLogNameUserDefined() throws Exception {
        AgentContext.SubagentContext context = new AgentContext.SubagentContext(
            "agent-1", "session-1", "custom-agent", false, null, null, false
        );
        String result = AgentContext.runWithAgentContext(context, () -> AgentContext.getSubagentLogName());
        assertEquals("user-defined", result);
    }

    @Test
    @DisplayName("AgentContext runWithAgentContext sets context")
    void runWithAgentContextSets() throws Exception {
        AgentContext.SubagentContext context = new AgentContext.SubagentContext(
            "agent-1", "session-1", "test-agent", true, null, null, false
        );
        AgentContext.AgentContextValue retrieved = AgentContext.runWithAgentContext(context, () -> AgentContext.getAgentContext());
        assertEquals(context, retrieved);
    }

    @Test
    @DisplayName("AgentContext runWithAgentContext clears after run")
    void runWithAgentContextClears() throws Exception {
        AgentContext.SubagentContext context = new AgentContext.SubagentContext(
            "agent-1", "session-1", "test-agent", true, null, null, false
        );
        AgentContext.runWithAgentContext(context, () -> {});
        assertNull(AgentContext.getAgentContext());
    }

    @Test
    @DisplayName("AgentContext runWithAgentContext Runnable")
    void runWithAgentContextRunnable() {
        AgentContext.SubagentContext context = new AgentContext.SubagentContext(
            "agent-1", "session-1", "test-agent", true, null, null, false
        );
        final AgentContext.AgentContextValue[] holder = new AgentContext.AgentContextValue[1];
        AgentContext.runWithAgentContext(context, () -> {
            holder[0] = AgentContext.getAgentContext();
        });
        assertEquals(context, holder[0]);
    }

    @Test
    @DisplayName("AgentContext InvokingRequest record")
    void invokingRequest() {
        AgentContext.InvokingRequest request = new AgentContext.InvokingRequest("req-1", "spawn");
        assertEquals("req-1", request.invokingRequestId());
        assertEquals("spawn", request.invocationKind());
    }
}
