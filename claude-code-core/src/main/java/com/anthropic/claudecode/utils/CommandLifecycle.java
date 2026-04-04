/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code command lifecycle
 */
package com.anthropic.claudecode.utils;

import java.util.function.*;

/**
 * Command lifecycle tracking.
 */
public final class CommandLifecycle {
    private CommandLifecycle() {}

    /**
     * Command lifecycle state.
     */
    public enum State {
        STARTED, COMPLETED
    }

    private static volatile BiConsumer<String, State> listener = null;

    /**
     * Set the lifecycle listener.
     */
    public static void setCommandLifecycleListener(BiConsumer<String, State> cb) {
        listener = cb;
    }

    /**
     * Notify command lifecycle state change.
     */
    public static void notifyCommandLifecycle(String uuid, State state) {
        if (listener != null) {
            listener.accept(uuid, state);
        }
    }

    /**
     * Notify command started.
     */
    public static void notifyStarted(String uuid) {
        notifyCommandLifecycle(uuid, State.STARTED);
    }

    /**
     * Notify command completed.
     */
    public static void notifyCompleted(String uuid) {
        notifyCommandLifecycle(uuid, State.COMPLETED);
    }

    /**
     * Clear the listener.
     */
    public static void clearListener() {
        listener = null;
    }
}