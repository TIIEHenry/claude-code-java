/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code context
 */
package com.anthropic.claudecode.context;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Application state container.
 */
public class AppState {
    private final Map<String, Object> state = new ConcurrentHashMap<>();
    private final List<Consumer<AppState>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Get a value.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) state.get(key));
    }

    /**
     * Get a value with default.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) state.getOrDefault(key, defaultValue);
    }

    /**
     * Set a value.
     */
    public void set(String key, Object value) {
        state.put(key, value);
        notifyListeners();
    }

    /**
     * Remove a key.
     */
    public void remove(String key) {
        state.remove(key);
        notifyListeners();
    }

    /**
     * Update state atomically.
     */
    public void update(Function<AppState, AppState> updater) {
        updater.apply(this);
        notifyListeners();
    }

    /**
     * Check if key exists.
     */
    public boolean has(String key) {
        return state.containsKey(key);
    }

    /**
     * Get all keys.
     */
    public Set<String> keys() {
        return new HashSet<>(state.keySet());
    }

    /**
     * Subscribe to changes.
     */
    public void subscribe(Consumer<AppState> listener) {
        listeners.add(listener);
    }

    /**
     * Unsubscribe.
     */
    public void unsubscribe(Consumer<AppState> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Consumer<AppState> listener : listeners) {
            listener.accept(this);
        }
    }

    /**
     * Create empty state.
     */
    public static AppState create() {
        return new AppState();
    }
}