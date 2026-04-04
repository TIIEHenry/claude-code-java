/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Selectors.
 */
class SelectorsTest {

    @Test
    @DisplayName("Selectors sessionId returns session ID")
    void sessionId() {
        AppState state = new AppState();
        state.setSessionId("session-123");

        String id = Selectors.sessionId().apply(state);

        assertEquals("session-123", id);
    }

    @Test
    @DisplayName("Selectors conversationId returns conversation ID")
    void conversationId() {
        AppState state = new AppState();
        state.setConversationId("conv-456");

        String id = Selectors.conversationId().apply(state);

        assertEquals("conv-456", id);
    }

    @Test
    @DisplayName("Selectors model returns model")
    void model() {
        AppState state = new AppState();
        state.setModel("claude-sonnet-4-6");

        String model = Selectors.model().apply(state);

        assertEquals("claude-sonnet-4-6", model);
    }

    @Test
    @DisplayName("Selectors queryState returns query state")
    void queryState() {
        AppState state = new AppState();
        state.setQueryState(AppState.QueryState.RUNNING);

        AppState.QueryState qs = Selectors.queryState().apply(state);

        assertEquals(AppState.QueryState.RUNNING, qs);
    }

    @Test
    @DisplayName("Selectors isQueryRunning returns true for RUNNING")
    void isQueryRunningRunning() {
        AppState state = new AppState();
        state.setQueryState(AppState.QueryState.RUNNING);

        assertTrue(Selectors.isQueryRunning().apply(state));
    }

    @Test
    @DisplayName("Selectors isQueryRunning returns true for STREAMING")
    void isQueryRunningStreaming() {
        AppState state = new AppState();
        state.setQueryState(AppState.QueryState.STREAMING);

        assertTrue(Selectors.isQueryRunning().apply(state));
    }

    @Test
    @DisplayName("Selectors isQueryRunning returns false for IDLE")
    void isQueryRunningIdle() {
        AppState state = new AppState();
        state.setQueryState(AppState.QueryState.IDLE);

        assertFalse(Selectors.isQueryRunning().apply(state));
    }

    @Test
    @DisplayName("Selectors currentTurn returns turn")
    void currentTurn() {
        AppState state = new AppState();
        state.setCurrentTurn(5);

        assertEquals(5, Selectors.currentTurn().apply(state));
    }

    @Test
    @DisplayName("Selectors permissionMode returns mode")
    void permissionMode() {
        AppState state = new AppState();
        state.setPermissionMode(AppState.PermissionMode.AUTO);

        assertEquals(AppState.PermissionMode.AUTO, Selectors.permissionMode().apply(state));
    }

    @Test
    @DisplayName("Selectors inputTokens returns tokens")
    void inputTokens() {
        AppState state = new AppState();
        state.addTokens(1000L, 0L);

        assertEquals(1000L, Selectors.inputTokens().apply(state));
    }

    @Test
    @DisplayName("Selectors outputTokens returns tokens")
    void outputTokens() {
        AppState state = new AppState();
        state.addTokens(0L, 500L);

        assertEquals(500L, Selectors.outputTokens().apply(state));
    }

    @Test
    @DisplayName("Selectors totalTokens returns sum")
    void totalTokens() {
        AppState state = new AppState();
        state.addTokens(1000L, 500L);

        assertEquals(1500L, Selectors.totalTokens().apply(state));
    }

    @Test
    @DisplayName("Selectors totalCost returns cost")
    void totalCost() {
        AppState state = new AppState();
        state.addCost(0.05);

        assertEquals(0.05, Selectors.totalCost().apply(state), 0.001);
    }

    @Test
    @DisplayName("Selectors inProgressTools returns set")
    void inProgressTools() {
        AppState state = new AppState();
        state.addInProgressTool("tool1");
        state.addInProgressTool("tool2");

        Set<String> tools = Selectors.inProgressTools().apply(state);

        assertEquals(2, tools.size());
        assertTrue(tools.contains("tool1"));
    }

    @Test
    @DisplayName("Selectors hasInProgressTools returns true when not empty")
    void hasInProgressToolsTrue() {
        AppState state = new AppState();
        state.addInProgressTool("tool1");

        assertTrue(Selectors.hasInProgressTools().apply(state));
    }

    @Test
    @DisplayName("Selectors hasInProgressTools returns false when empty")
    void hasInProgressToolsFalse() {
        AppState state = new AppState();

        assertFalse(Selectors.hasInProgressTools().apply(state));
    }
}