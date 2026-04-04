/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/abortController
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Abort controller - Cancellation support for async operations.
 */
public final class AbortController {
    private final AtomicBoolean aborted = new AtomicBoolean(false);
    private final AtomicReference<String> reason = new AtomicReference<>();
    private final CopyOnWriteArrayList<AbortListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Abort the controller.
     */
    public void abort() {
        abort(null);
    }

    /**
     * Abort the controller with a reason.
     */
    public void abort(String reason) {
        if (aborted.compareAndSet(false, true)) {
            this.reason.set(reason);
            notifyListeners();
        }
    }

    /**
     * Get the abort signal.
     */
    public AbortSignal getSignal() {
        return new AbortSignal(this);
    }

    /**
     * Check if aborted.
     */
    public boolean isAborted() {
        return aborted.get();
    }

    /**
     * Get the abort reason.
     */
    public String getReason() {
        return reason.get();
    }

    /**
     * Add an abort listener.
     */
    public void addListener(AbortListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove an abort listener.
     */
    public void removeListener(AbortListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (AbortListener listener : listeners) {
            try {
                listener.onAbort(reason.get());
            } catch (Exception e) {
                // Ignore listener errors
            }
        }
        listeners.clear();
    }

    /**
     * Create a child abort controller that aborts when parent aborts.
     */
    public AbortController createChild() {
        AbortController child = new AbortController();

        if (isAborted()) {
            child.abort(reason.get());
            return child;
        }

        AbortListener parentListener = childReason -> child.abort(childReason);
        addListener(parentListener);

        // Clean up parent listener when child aborts
        child.addListener(childReason -> removeListener(parentListener));

        return child;
    }

    /**
     * Abort listener interface.
     */
    @FunctionalInterface
    public interface AbortListener {
        void onAbort(String reason);
    }

    /**
     * Abort signal - Immutable view of abort state.
     */
    public static final class AbortSignal {
        private final AbortController controller;

        private AbortSignal(AbortController controller) {
            this.controller = controller;
        }

        public boolean isAborted() {
            return controller.isAborted();
        }

        public String getReason() {
            return controller.getReason();
        }

        public void addListener(AbortListener listener) {
            controller.addListener(listener);
        }

        public void removeListener(AbortListener listener) {
            controller.removeListener(listener);
        }
    }

    /**
     * Create an abort controller with timeout.
     */
    public static AbortController withTimeout(long timeoutMs) {
        AbortController controller = new AbortController();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(() -> {
            controller.abort("Timeout after " + timeoutMs + "ms");
            scheduler.shutdown();
        }, timeoutMs, TimeUnit.MILLISECONDS);

        return controller;
    }

    /**
     * Combine multiple abort signals.
     */
    public static AbortController combine(AbortSignal... signals) {
        AbortController combined = new AbortController();

        for (AbortSignal signal : signals) {
            if (signal.isAborted()) {
                combined.abort(signal.getReason());
                return combined;
            }

            signal.addListener(reason -> combined.abort(reason));
        }

        return combined;
    }
}