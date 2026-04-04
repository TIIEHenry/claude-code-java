/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AppStateStore.
 */
class AppStateStoreTest {

    @Test
    @DisplayName("AppStateStore getInstance returns instance")
    void getInstance() {
        AppStateStore store = AppStateStore.getInstance();

        assertNotNull(store);
    }

    @Test
    @DisplayName("AppStateStore getState returns state")
    void getState() {
        AppStateStore store = AppStateStore.getInstance();

        AppState state = store.getState();

        assertNotNull(state);
    }

    @Test
    @DisplayName("AppStateStore setState updates state")
    void setState() {
        AppStateStore store = AppStateStore.getInstance();
        AppState state = new AppState();
        state.setSessionId("test-session");

        store.setState(state);

        assertEquals("test-session", store.getState().getSessionId());
    }

    @Test
    @DisplayName("AppStateStore updateState updates using function")
    void updateState() {
        AppStateStore store = AppStateStore.getInstance();
        store.getState().setSessionId("original");

        store.updateState(s -> {
            s.setSessionId("updated");
            return s;
        });

        assertEquals("updated", store.getState().getSessionId());
    }

    @Test
    @DisplayName("AppStateStore select returns value")
    void select() {
        AppStateStore store = AppStateStore.getInstance();
        store.getState().setCurrentTurn(5);

        int turn = store.select(Selectors.currentTurn());

        assertEquals(5, turn);
    }

    @Test
    @DisplayName("AppStateStore createSelector returns supplier")
    void createSelector() {
        AppStateStore store = AppStateStore.getInstance();
        store.getState().setCurrentTurn(10);

        var turnSupplier = store.createSelector(Selectors.currentTurn());

        assertEquals(10, turnSupplier.get());
    }

    @Test
    @DisplayName("AppStateStore reset resets state")
    void reset() {
        AppStateStore store = AppStateStore.getInstance();
        store.getState().setCurrentTurn(100);

        store.reset();

        assertEquals(0, store.getState().getCurrentTurn());
    }
}