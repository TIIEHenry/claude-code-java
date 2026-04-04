/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentContextUtils.
 */
class AgentContextUtilsTest {

    @BeforeEach
    void setUp() {
        AgentContextUtils.clearContext();
    }

    @Test
    @DisplayName("AgentContextUtils AgentType enum values")
    void agentTypeEnum() {
        AgentContextUtils.AgentType[] types = AgentContextUtils.AgentType.values();
        assertEquals(2, types.length);
    }

    @Test
    @DisplayName("AgentContextUtils InvocationKind enum values")
    void invocationKindEnum() {
        AgentContextUtils.InvocationKind[] kinds = AgentContextUtils.InvocationKind.values();
        assertEquals(2, kinds.length);
    }

    @Test
    @DisplayName("AgentContextUtils SubagentContext record")
    void subagentContext() {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        assertEquals("agent-1", context.getAgentId());
        assertEquals("session-1", context.getParentSessionId());
        assertEquals(AgentContextUtils.AgentType.SUBAGENT, context.getAgentType());
        assertEquals("test-agent", context.subagentName());
        assertTrue(context.isBuiltIn());
    }

    @Test
    @DisplayName("AgentContextUtils TeammateAgentContext record")
    void teammateAgentContext() {
        AgentContextUtils.TeammateAgentContext context = new AgentContextUtils.TeammateAgentContext(
            "agent-1", "Teammate", "team-1", "session-1", true
        );
        assertEquals("agent-1", context.getAgentId());
        assertEquals("Teammate", context.agentName());
        assertEquals("team-1", context.teamName());
        assertTrue(context.isTeamLead());
        assertEquals(AgentContextUtils.AgentType.TEAMMATE, context.getAgentType());
    }

    @Test
    @DisplayName("AgentContextUtils getAgentContext initially null")
    void getAgentContextInitiallyNull() {
        assertNull(AgentContextUtils.getAgentContext());
    }

    @Test
    @DisplayName("AgentContextUtils hasAgentContext initially false")
    void hasAgentContextInitiallyFalse() {
        assertFalse(AgentContextUtils.hasAgentContext());
    }

    @Test
    @DisplayName("AgentContextUtils runWithAgentContext sets context")
    void runWithAgentContextSets() throws Exception {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        AgentContextUtils.AgentContext retrieved = AgentContextUtils.runWithAgentContext(context, () -> AgentContextUtils.getAgentContext());
        assertEquals(context, retrieved);
    }

    @Test
    @DisplayName("AgentContextUtils runWithAgentContext clears after run")
    void runWithAgentContextClears() throws Exception {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        AgentContextUtils.runWithAgentContext(context, () -> {});
        assertNull(AgentContextUtils.getAgentContext());
    }

    @Test
    @DisplayName("AgentContextUtils runWithAgentContext Runnable")
    void runWithAgentContextRunnable() {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        final AgentContextUtils.AgentContext[] holder = new AgentContextUtils.AgentContext[1];
        AgentContextUtils.runWithAgentContext(context, () -> {
            holder[0] = AgentContextUtils.getAgentContext();
        });
        assertEquals(context, holder[0]);
    }

    @Test
    @DisplayName("AgentContextUtils isSubagentContext true")
    void isSubagentContextTrue() {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        assertTrue(AgentContextUtils.isSubagentContext(context));
    }

    @Test
    @DisplayName("AgentContextUtils isSubagentContext false")
    void isSubagentContextFalse() {
        AgentContextUtils.TeammateAgentContext context = new AgentContextUtils.TeammateAgentContext(
            "agent-1", "Teammate", "team-1", "session-1", true
        );
        assertFalse(AgentContextUtils.isSubagentContext(context));
    }

    @Test
    @DisplayName("AgentContextUtils isTeammateAgentContext true")
    void isTeammateAgentContextTrue() {
        AgentContextUtils.TeammateAgentContext context = new AgentContextUtils.TeammateAgentContext(
            "agent-1", "Teammate", "team-1", "session-1", true
        );
        assertTrue(AgentContextUtils.isTeammateAgentContext(context));
    }

    @Test
    @DisplayName("AgentContextUtils isTeammateAgentContext false")
    void isTeammateAgentContextFalse() {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        assertFalse(AgentContextUtils.isTeammateAgentContext(context));
    }

    @Test
    @DisplayName("AgentContextUtils getSubagentLogName built-in")
    void getSubagentLogNameBuiltIn() throws Exception {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        String result = AgentContextUtils.runWithAgentContext(context, () -> AgentContextUtils.getSubagentLogName());
        assertEquals("test-agent", result);
    }

    @Test
    @DisplayName("AgentContextUtils getSubagentLogName user-defined")
    void getSubagentLogNameUserDefined() throws Exception {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "custom-agent", false
        );
        String result = AgentContextUtils.runWithAgentContext(context, () -> AgentContextUtils.getSubagentLogName());
        assertEquals("user-defined", result);
    }

    @Test
    @DisplayName("AgentContextUtils clearContext clears")
    void clearContext() throws Exception {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        AgentContextUtils.runWithAgentContext(context, () -> {});
        AgentContextUtils.clearContext();
        assertNull(AgentContextUtils.getAgentContext());
    }

    @Test
    @DisplayName("AgentContextUtils MutableAgentContext")
    void mutableAgentContext() {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        AgentContextUtils.MutableAgentContext mutable = new AgentContextUtils.MutableAgentContext(context);
        assertEquals(context, mutable.getContext());
        assertFalse(mutable.isInvocationEmitted());
        mutable.setInvocationEmitted(true);
        assertTrue(mutable.isInvocationEmitted());
    }

    @Test
    @DisplayName("AgentContextUtils InvocationInfo record")
    void invocationInfo() {
        AgentContextUtils.InvocationInfo info = new AgentContextUtils.InvocationInfo("req-1", AgentContextUtils.InvocationKind.SPAWN);
        assertEquals("req-1", info.invokingRequestId());
        assertEquals(AgentContextUtils.InvocationKind.SPAWN, info.invocationKind());
    }

    @Test
    @DisplayName("AgentContextUtils SubagentContext withInvocationEmitted")
    void subagentContextWithInvocationEmitted() {
        AgentContextUtils.SubagentContext context = new AgentContextUtils.SubagentContext(
            "agent-1", "session-1", "test-agent", true
        );
        AgentContextUtils.SubagentContext emitted = context.withInvocationEmitted(true);
        assertTrue(emitted.invocationEmitted());
    }

    @Test
    @DisplayName("AgentContextUtils TeammateAgentContext withInvocationEmitted")
    void teammateAgentContextWithInvocationEmitted() {
        AgentContextUtils.TeammateAgentContext context = new AgentContextUtils.TeammateAgentContext(
            "agent-1", "Teammate", "team-1", "session-1", true
        );
        AgentContextUtils.TeammateAgentContext emitted = context.withInvocationEmitted(true);
        assertTrue(emitted.invocationEmitted());
    }
}
