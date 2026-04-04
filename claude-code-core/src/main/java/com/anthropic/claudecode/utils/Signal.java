/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Simple signal for reactive updates.
 */
public class Signal {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private int revision = 0;

    /**
     * Subscribe to changes.
     */
    public Subscription subscribe(Runnable listener) {
        listeners.add(listener);
        return new Subscription(() -> listeners.remove(listener));
    }

    /**
     * Notify all listeners.
     */
    public void notifyListeners() {
        revision++;
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    /**
     * Emit a signal (alias for notifyListeners).
     */
    public void emit() {
        notifyListeners();
    }

    /**
     * Get current revision.
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Create a new signal.
     */
    public static Signal create() {
        return new Signal();
    }
}