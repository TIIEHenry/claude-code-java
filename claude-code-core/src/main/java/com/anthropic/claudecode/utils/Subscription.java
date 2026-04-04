/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Subscription utilities for event handling.
 */
public class Subscription implements AutoCloseable {
    private final Runnable unsubscribe;
    private volatile boolean closed = false;

    public Subscription(Runnable unsubscribe) {
        this.unsubscribe = unsubscribe;
    }

    /**
     * Unsubscribe.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            unsubscribe.run();
        }
    }

    /**
     * Check if closed.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Create a composite subscription.
     */
    public static Subscription composite(Subscription... subscriptions) {
        return new Subscription(() -> {
            for (Subscription sub : subscriptions) {
                sub.close();
            }
        });
    }
}