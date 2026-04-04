/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Signal that can emit values to subscribers.
 */
public class SignalNew<T> {
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> simpleListeners = new CopyOnWriteArrayList<>();

    /**
     * Subscribe to value changes.
     */
    public void subscribe(Consumer<T> listener) {
        listeners.add(listener);
    }

    /**
     * Subscribe with a simple runnable.
     */
    public void subscribe(Runnable listener) {
        simpleListeners.add(listener);
    }

    /**
     * Emit a value to all subscribers.
     */
    public void emit(T value) {
        for (Consumer<T> listener : listeners) {
            listener.accept(value);
        }
        for (Runnable listener : simpleListeners) {
            listener.run();
        }
    }

    /**
     * Clear all listeners.
     */
    public void clear() {
        listeners.clear();
        simpleListeners.clear();
    }

    /**
     * Get listener count.
     */
    public int getListenerCount() {
        return listeners.size() + simpleListeners.size();
    }

    /**
     * Create a new signal.
     */
    public static <T> SignalNew<T> create() {
        return new SignalNew<>();
    }
}