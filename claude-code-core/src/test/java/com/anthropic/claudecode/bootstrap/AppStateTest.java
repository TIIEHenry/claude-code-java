/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AppState (bootstrap).
 */
class AppStateTest {

    @BeforeEach
    void setUp() {
        AppState.resetState();
    }

    @Test
    @DisplayName("AppState getInstance returns singleton")
    void getInstance() {
        AppState state1 = AppState.getInstance();
        AppState state2 = AppState.getInstance();

        assertSame(state1, state2);
    }

    @Test
    @DisplayName("AppState resetState clears instance")
    void resetState() {
        AppState state1 = AppState.getInstance();
        state1.setOriginalCwd("/test");

        AppState.resetState();
        AppState state2 = AppState.getInstance();

        assertNotSame(state1, state2);
        assertEquals("", state2.getOriginalCwd());
    }

    @Test
    @DisplayName("AppState originalCwd getter and setter")
    void originalCwd() {
        AppState state = AppState.getInstance();
        state.setOriginalCwd("/home/user");

        assertEquals("/home/user", state.getOriginalCwd());
    }

    @Test
    @DisplayName("AppState projectRoot getter and setter")
    void projectRoot() {
        AppState state = AppState.getInstance();
        state.setProjectRoot("/home/user/project");

        assertEquals("/home/user/project", state.getProjectRoot());
    }

    @Test
    @DisplayName("AppState totalCostUSD and addCost")
    void totalCostUSD() {
        AppState state = AppState.getInstance();
        assertEquals(0.0, state.getTotalCostUSD(), 0.001);

        state.addCost(0.05);
        assertEquals(0.05, state.getTotalCostUSD(), 0.001);

        state.addCost(0.03);
        assertEquals(0.08, state.getTotalCostUSD(), 0.001);
    }

    @Test
    @DisplayName("AppState totalAPIDuration and addAPIDuration")
    void totalAPIDuration() {
        AppState state = AppState.getInstance();
        assertEquals(0, state.getTotalAPIDuration());

        state.addAPIDuration(1000);
        assertEquals(1000, state.getTotalAPIDuration());

        state.addAPIDuration(500);
        assertEquals(1500, state.getTotalAPIDuration());
    }

    @Test
    @DisplayName("AppState totalToolDuration and addToolDuration")
    void totalToolDuration() {
        AppState state = AppState.getInstance();
        assertEquals(0, state.getTotalToolDuration());

        state.addToolDuration(100);
        assertEquals(100, state.getTotalToolDuration());
    }

    @Test
    @DisplayName("AppState totalLinesAdded and addLines")
    void totalLinesAdded() {
        AppState state = AppState.getInstance();
        assertEquals(0, state.getTotalLinesAdded());

        state.addLines(50);
        assertEquals(50, state.getTotalLinesAdded());
    }

    @Test
    @DisplayName("AppState totalLinesRemoved and removeLines")
    void totalLinesRemoved() {
        AppState state = AppState.getInstance();
        assertEquals(0, state.getTotalLinesRemoved());

        state.removeLines(30);
        assertEquals(30, state.getTotalLinesRemoved());
    }

    @Test
    @DisplayName("AppState cwd getter and setter")
    void cwd() {
        AppState state = AppState.getInstance();
        state.setCwd("/home/user/project");

        assertEquals("/home/user/project", state.getCwd());
    }

    @Test
    @DisplayName("AppState sessionId getter and setter")
    void sessionId() {
        AppState state = AppState.getInstance();
        String id = state.getSessionId();
        assertNotNull(id);

        state.setSessionId("custom-session-id");
        assertEquals("custom-session-id", state.getSessionId());
    }

    @Test
    @DisplayName("AppState parentSessionId getter and setter")
    void parentSessionId() {
        AppState state = AppState.getInstance();
        assertNull(state.getParentSessionId());

        state.setParentSessionId("parent-id");
        assertEquals("parent-id", state.getParentSessionId());
    }

    @Test
    @DisplayName("AppState interactive getter and setter")
    void interactive() {
        AppState state = AppState.getInstance();
        assertFalse(state.isInteractive());

        state.setInteractive(true);
        assertTrue(state.isInteractive());
    }

    @Test
    @DisplayName("AppState clientType getter and setter")
    void clientType() {
        AppState state = AppState.getInstance();
        assertEquals("cli", state.getClientType());

        state.setClientType("ide");
        assertEquals("ide", state.getClientType());
    }

    @Test
    @DisplayName("AppState mainLoopModel methods")
    void mainLoopModel() {
        AppState state = AppState.getInstance();
        state.setInitialMainLoopModel("claude-sonnet-4-6");

        assertEquals("claude-sonnet-4-6", state.getMainLoopModel());

        state.setMainLoopModelOverride("claude-opus-4-6");
        assertEquals("claude-opus-4-6", state.getMainLoopModel());
    }

    @Test
    @DisplayName("AppState modelUsage and addModelUsage")
    void modelUsage() {
        AppState state = AppState.getInstance();
        assertTrue(state.getModelUsage().isEmpty());

        state.addModelUsage("claude-sonnet-4-6", 1000, 500, 0.05);

        assertEquals(1, state.getModelUsage().size());
        AppState.ModelUsage usage = state.getModelUsage().get("claude-sonnet-4-6");
        assertEquals(1000, usage.inputTokens());
        assertEquals(500, usage.outputTokens());
        assertEquals(0.05, usage.cost(), 0.001);
    }

    @Test
    @DisplayName("AppState addModelUsage accumulates")
    void modelUsageAccumulates() {
        AppState state = AppState.getInstance();

        state.addModelUsage("claude-sonnet-4-6", 1000, 500, 0.05);
        state.addModelUsage("claude-sonnet-4-6", 2000, 1000, 0.10);

        AppState.ModelUsage usage = state.getModelUsage().get("claude-sonnet-4-6");
        assertEquals(3000, usage.inputTokens());
        assertEquals(1500, usage.outputTokens());
        assertEquals(0.15, usage.cost(), 0.001);
    }

    @Test
    @DisplayName("AppState turnToolCount and incrementToolCount")
    void turnToolCount() {
        AppState state = AppState.getInstance();
        assertEquals(0, state.getTurnToolCount());

        state.incrementToolCount();
        state.incrementToolCount();
        assertEquals(2, state.getTurnToolCount());
    }

    @Test
    @DisplayName("AppState resetTurnCounts")
    void resetTurnCounts() {
        AppState state = AppState.getInstance();
        state.incrementToolCount();
        state.incrementToolCount();

        state.resetTurnCounts();
        assertEquals(0, state.getTurnToolCount());
    }

    @Test
    @DisplayName("AppState sessionBypassPermissionsMode")
    void sessionBypassPermissionsMode() {
        AppState state = AppState.getInstance();
        assertFalse(state.isSessionBypassPermissionsMode());

        state.setSessionBypassPermissionsMode(true);
        assertTrue(state.isSessionBypassPermissionsMode());
    }

    @Test
    @DisplayName("AppState sessionTrustAccepted")
    void sessionTrustAccepted() {
        AppState state = AppState.getInstance();
        assertFalse(state.isSessionTrustAccepted());

        state.setSessionTrustAccepted(true);
        assertTrue(state.isSessionTrustAccepted());
    }

    @Test
    @DisplayName("AppState hasExitedPlanMode")
    void hasExitedPlanMode() {
        AppState state = AppState.getInstance();
        assertFalse(state.hasExitedPlanMode());

        state.setHasExitedPlanMode(true);
        assertTrue(state.hasExitedPlanMode());
    }

    @Test
    @DisplayName("AppState addError")
    void addError() {
        AppState state = AppState.getInstance();
        assertTrue(state.getInMemoryErrorLog().isEmpty());

        state.addError("Test error");

        assertEquals(1, state.getInMemoryErrorLog().size());
        assertEquals("Test error", state.getInMemoryErrorLog().get(0).get("error"));
    }

    @Test
    @DisplayName("AppState addError limits to 100 entries")
    void addErrorLimitsEntries() {
        AppState state = AppState.getInstance();

        for (int i = 0; i < 150; i++) {
            state.addError("Error " + i);
        }

        assertEquals(100, state.getInMemoryErrorLog().size());
    }

    @Test
    @DisplayName("AppState ModelUsage record")
    void modelUsageRecord() {
        AppState.ModelUsage usage = new AppState.ModelUsage(1000, 500, 0.05);

        assertEquals(1000, usage.inputTokens());
        assertEquals(500, usage.outputTokens());
        assertEquals(0.05, usage.cost(), 0.001);
    }

    @Test
    @DisplayName("AppState getSessionDuration")
    void getSessionDuration() throws InterruptedException {
        AppState state = AppState.getInstance();
        Thread.sleep(50);

        assertTrue(state.getSessionDuration() >= 50);
    }

    @Test
    @DisplayName("AppState getSessionStartDate")
    void getSessionStartDate() {
        AppState state = AppState.getInstance();
        String date = state.getSessionStartDate();

        assertNotNull(date);
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    @DisplayName("AppState updateInteractionTime")
    void updateInteractionTime() throws InterruptedException {
        AppState state = AppState.getInstance();
        long first = state.getLastInteractionTime();

        Thread.sleep(10);
        state.updateInteractionTime();

        assertTrue(state.getLastInteractionTime() > first);
    }
}