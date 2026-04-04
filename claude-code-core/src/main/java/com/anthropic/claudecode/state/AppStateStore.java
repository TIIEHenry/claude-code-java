/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code state/AppStateStore.ts
 */
package com.anthropic.claudecode.state;

import java.util.*;
import java.util.function.*;

/**
 * Application state store with selector support.
 */
public class AppStateStore {
    private static volatile AppStateStore instance;
    private final Store<AppState> store;

    private AppStateStore() {
        this.store = Store.create(new AppState());
    }

    /**
     * Get singleton instance.
     */
    public static AppStateStore getInstance() {
        if (instance == null) {
            synchronized (AppStateStore.class) {
                if (instance == null) {
                    instance = new AppStateStore();
                }
            }
        }
        return instance;
    }

    /**
     * Get current state.
     */
    public AppState getState() {
        return store.getState();
    }

    /**
     * Update state.
     */
    public void setState(AppState newState) {
        store.setState(newState);
    }

    /**
     * Update state using function.
     */
    public void updateState(Function<AppState, AppState> updater) {
        store.updateState(updater);
    }

    /**
     * Subscribe to state changes.
     */
    public Store.Subscription subscribe(Consumer<AppState> listener) {
        return store.subscribe(listener);
    }

    /**
     * Select a value from state.
     */
    public <R> R select(Function<AppState, R> selector) {
        return selector.apply(getState());
    }

    /**
     * Create a selector that caches results.
     */
    public <R> Supplier<R> createSelector(Function<AppState, R> selector) {
        return () -> selector.apply(getState());
    }

    /**
     * Reset state.
     */
    public void reset() {
        updateState(state -> {
            state.reset();
            return state;
        });
    }
}