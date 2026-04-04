/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AppState.
 */
class AppStateTest {

    private AppState appState;

    @BeforeEach
    void setUp() {
        appState = new AppState();
    }

    @Test
    @DisplayName("AppState default constructor initializes values")
    void defaultConstructor() {
        assertNotNull(appState.getSessionId());
        assertEquals("claude-sonnet-4-6", appState.getModel());
        assertEquals(AppState.PermissionMode.DEFAULT, appState.getPermissionMode());
        assertEquals(AppState.QueryState.IDLE, appState.getQueryState());
    }

    @Test
    @DisplayName("AppState QueryState enum values")
    void queryStateEnum() {
        AppState.QueryState[] states = AppState.QueryState.values();
        assertEquals(7, states.length);
        assertEquals(AppState.QueryState.IDLE, AppState.QueryState.valueOf("IDLE"));
        assertEquals(AppState.QueryState.RUNNING, AppState.QueryState.valueOf("RUNNING"));
        assertEquals(AppState.QueryState.STREAMING, AppState.QueryState.valueOf("STREAMING"));
        assertEquals(AppState.QueryState.INTERRUPTED, AppState.QueryState.valueOf("INTERRUPTED"));
        assertEquals(AppState.QueryState.COMPLETED, AppState.QueryState.valueOf("COMPLETED"));
        assertEquals(AppState.QueryState.FAILED, AppState.QueryState.valueOf("FAILED"));
    }

    @Test
    @DisplayName("AppState PermissionMode enum values")
    void permissionModeEnum() {
        AppState.PermissionMode[] modes = AppState.PermissionMode.values();
        assertEquals(6, modes.length);
        assertEquals(AppState.PermissionMode.DEFAULT, AppState.PermissionMode.valueOf("DEFAULT"));
        assertEquals(AppState.PermissionMode.PLAN, AppState.PermissionMode.valueOf("PLAN"));
        assertEquals(AppState.PermissionMode.AUTO, AppState.PermissionMode.valueOf("AUTO"));
    }

    @Test
    @DisplayName("AppState setters update values")
    void settersUpdateValues() {
        appState.setSessionId("new-session-id");
        assertEquals("new-session-id", appState.getSessionId());

        appState.setConversationId("conv-123");
        assertEquals("conv-123", appState.getConversationId());

        appState.setModel("claude-opus-4-6");
        assertEquals("claude-opus-4-6", appState.getModel());

        appState.setPermissionMode(AppState.PermissionMode.PLAN);
        assertEquals(AppState.PermissionMode.PLAN, appState.getPermissionMode());

        appState.setQueryState(AppState.QueryState.RUNNING);
        assertEquals(AppState.QueryState.RUNNING, appState.getQueryState());
    }

    @Test
    @DisplayName("AppState turn tracking")
    void turnTracking() {
        assertEquals(0, appState.getCurrentTurn());
        assertEquals(100, appState.getMaxTurns());

        appState.setCurrentTurn(5);
        assertEquals(5, appState.getCurrentTurn());

        appState.setMaxTurns(50);
        assertEquals(50, appState.getMaxTurns());
    }

    @Test
    @DisplayName("AppState tool tracking")
    void toolTracking() {
        assertTrue(appState.getInProgressToolIds().isEmpty());

        appState.addInProgressTool("tool-1");
        assertTrue(appState.hasInProgressTool("tool-1"));
        assertEquals(1, appState.getInProgressToolIds().size());

        appState.addInProgressTool("tool-2");
        assertEquals(2, appState.getInProgressToolIds().size());

        appState.removeInProgressTool("tool-1");
        assertFalse(appState.hasInProgressTool("tool-1"));
        assertEquals(1, appState.getInProgressToolIds().size());
    }

    @Test
    @DisplayName("AppState interruptible tool tracking")
    void interruptibleToolTracking() {
        assertFalse(appState.isHasInterruptibleToolInProgress());

        appState.setHasInterruptibleToolInProgress(true);
        assertTrue(appState.isHasInterruptibleToolInProgress());
    }

    @Test
    @DisplayName("AppState token tracking")
    void tokenTracking() {
        assertEquals(0, appState.getInputTokens());
        assertEquals(0, appState.getOutputTokens());

        appState.addTokens(100, 50);
        assertEquals(100, appState.getInputTokens());
        assertEquals(50, appState.getOutputTokens());

        appState.addTokens(200, 100);
        assertEquals(300, appState.getInputTokens());
        assertEquals(150, appState.getOutputTokens());
    }

    @Test
    @DisplayName("AppState cost tracking")
    void costTracking() {
        assertEquals(0.0, appState.getTotalCost(), 0.001);

        appState.addCost(0.05);
        assertEquals(0.05, appState.getTotalCost(), 0.001);

        appState.addCost(0.03);
        assertEquals(0.08, appState.getTotalCost(), 0.001);
    }

    @Test
    @DisplayName("AppState metadata")
    void metadata() {
        appState.setMetadata("key1", "value1");
        assertEquals("value1", appState.getMetadata("key1"));

        appState.removeMetadata("key1");
        assertNull(appState.getMetadata("key1"));
    }

    @Test
    @DisplayName("AppState reset clears state")
    void resetClearsState() {
        appState.setCurrentTurn(10);
        appState.addTokens(100, 50);
        appState.addCost(0.10);
        appState.addInProgressTool("tool-1");
        appState.setQueryState(AppState.QueryState.RUNNING);

        appState.reset();

        assertEquals(0, appState.getCurrentTurn());
        assertEquals(0, appState.getInputTokens());
        assertEquals(0, appState.getOutputTokens());
        assertEquals(0.0, appState.getTotalCost(), 0.001);
        assertTrue(appState.getInProgressToolIds().isEmpty());
        assertEquals(AppState.QueryState.IDLE, appState.getQueryState());
        // Session ID is regenerated
        assertNotNull(appState.getSessionId());
    }

    @Test
    @DisplayName("AppState copy creates independent copy")
    void copyCreatesIndependentCopy() {
        appState.setCurrentTurn(5);
        appState.setModel("claude-opus-4-6");
        appState.addTokens(100, 50);

        AppState copy = appState.copy();

        assertEquals(5, copy.getCurrentTurn());
        assertEquals("claude-opus-4-6", copy.getModel());
        assertEquals(100, copy.getInputTokens());

        // Modify original, copy should be unchanged
        appState.setCurrentTurn(10);
        assertEquals(5, copy.getCurrentTurn());
    }

    @Test
    @DisplayName("AppState copy includes tool IDs")
    void copyIncludesToolIds() {
        appState.addInProgressTool("tool-1");
        appState.addInProgressTool("tool-2");

        AppState copy = appState.copy();

        assertEquals(2, copy.getInProgressToolIds().size());
        assertTrue(copy.hasInProgressTool("tool-1"));
        assertTrue(copy.hasInProgressTool("tool-2"));
    }

    @Test
    @DisplayName("AppState unique session IDs on reset")
    void uniqueSessionIdsOnReset() {
        String originalId = appState.getSessionId();
        appState.reset();
        String newId = appState.getSessionId();

        assertNotEquals(originalId, newId);
    }
}