/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code classifier approvals hook
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * React-style hook for classifier approvals store.
 * In Java, this is implemented using listeners/callbacks.
 */
public final class ClassifierApprovalsHook {
    private ClassifierApprovalsHook() {}

    // Map of toolUseID -> isChecking
    private static final Map<String, Boolean> checkingStates = new ConcurrentHashMap<>();
    // Listeners for state changes
    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Check if classifier is checking for a specific tool use.
     */
    public static boolean isClassifierChecking(String toolUseID) {
        return checkingStates.getOrDefault(toolUseID, false);
    }

    /**
     * Set the checking state for a tool use.
     */
    public static void setClassifierChecking(String toolUseID, boolean isChecking) {
        checkingStates.put(toolUseID, isChecking);
        notifyListeners(toolUseID);
    }

    /**
     * Subscribe to classifier checking state changes.
     */
    public static Runnable subscribeClassifierChecking(Consumer<String> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Notify all listeners of a state change.
     */
    private static void notifyListeners(String toolUseID) {
        for (Consumer<String> listener : listeners) {
            listener.accept(toolUseID);
        }
    }

    /**
     * React-style hook implementation for Java.
     * Returns a supplier that provides the current checking state.
     */
    public static Supplier<Boolean> useIsClassifierChecking(String toolUseID) {
        return () -> isClassifierChecking(toolUseID);
    }

    /**
     * Clear all checking states.
     */
    public static void clear() {
        checkingStates.clear();
        listeners.clear();
    }

    /**
     * Get all currently checking tool use IDs.
     */
    public static Set<String> getCheckingToolUseIDs() {
        return new HashSet<>(checkingStates.keySet());
    }
}