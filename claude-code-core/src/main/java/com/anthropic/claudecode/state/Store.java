/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code state/store.ts
 */
package com.anthropic.claudecode.state;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Generic store for state management.
 */
public class Store<T> {
    private volatile T state;
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
    private final List<ChangeListener<T>> changeListeners = new CopyOnWriteArrayList<>();

    public Store(T initialState) {
        this.state = initialState;
    }

    /**
     * Get current state.
     */
    public T getState() {
        return state;
    }

    /**
     * Set state.
     */
    public void setState(T newState) {
        T oldState = this.state;
        this.state = newState;
        notifyListeners(oldState, newState);
    }

    /**
     * Update state using function.
     */
    public void updateState(Function<T, T> updater) {
        T oldState = this.state;
        T newState = updater.apply(oldState);
        this.state = newState;
        notifyListeners(oldState, newState);
    }

    /**
     * Subscribe to state changes.
     */
    public Subscription subscribe(Consumer<T> listener) {
        listeners.add(listener);
        return new Subscription(() -> listeners.remove(listener));
    }

    /**
     * Subscribe to changes with old and new state.
     */
    public Subscription onChange(ChangeListener<T> listener) {
        changeListeners.add(listener);
        return new Subscription(() -> changeListeners.remove(listener));
    }

    private void notifyListeners(T oldState, T newState) {
        for (Consumer<T> listener : listeners) {
            listener.accept(newState);
        }
        for (ChangeListener<T> listener : changeListeners) {
            listener.onChange(oldState, newState);
        }
    }

    /**
     * Create a store with initial state.
     */
    public static <T> Store<T> create(T initialState) {
        return new Store<>(initialState);
    }

    /**
     * Change listener interface.
     */
    @FunctionalInterface
    public interface ChangeListener<T> {
        void onChange(T oldState, T newState);
    }

    /**
     * Simple subscription class.
     */
    public static class Subscription {
        private final Runnable unsubscribe;
        private volatile boolean closed = false;

        public Subscription(Runnable unsubscribe) {
            this.unsubscribe = unsubscribe;
        }

        public void unsubscribe() {
            if (!closed) {
                closed = true;
                unsubscribe.run();
            }
        }
    }
}