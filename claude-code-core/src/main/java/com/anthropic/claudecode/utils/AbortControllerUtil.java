/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/abortController.ts
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.lang.ref.*;

/**
 * AbortController utilities with proper event listener limits.
 */
public final class AbortControllerUtil {
    private AbortControllerUtil() {}

    private static final int DEFAULT_MAX_LISTENERS = 50;

    /**
     * Abort controller with cleanup support.
     */
    public static class AbortController {
        private final AtomicBoolean aborted = new AtomicBoolean(false);
        private final AtomicReference<String> reason = new AtomicReference<>(null);
        private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
        private final AtomicInteger maxListeners = new AtomicInteger(DEFAULT_MAX_LISTENERS);

        public void abort() {
            abort(null);
        }

        public void abort(String reason) {
            if (aborted.compareAndSet(false, true)) {
                this.reason.set(reason);
                for (Consumer<String> listener : listeners) {
                    try {
                        listener.accept(reason);
                    } catch (Exception e) {
                        // Ignore listener errors
                    }
                }
                listeners.clear();
            }
        }

        public boolean isAborted() {
            return aborted.get();
        }

        public String getReason() {
            return reason.get();
        }

        public void addListener(Consumer<String> listener) {
            listeners.add(listener);
        }

        public void removeListener(Consumer<String> listener) {
            listeners.remove(listener);
        }

        public void setMaxListeners(int max) {
            maxListeners.set(max);
        }

        public int getMaxListeners() {
            return maxListeners.get();
        }
    }

    /**
     * Consumer interface for abort listeners.
     */
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T value);
    }

    /**
     * Creates an AbortController with proper event listener limits set.
     */
    public static AbortController createAbortController() {
        return createAbortController(DEFAULT_MAX_LISTENERS);
    }

    public static AbortController createAbortController(int maxListeners) {
        AbortController controller = new AbortController();
        controller.setMaxListeners(maxListeners);
        return controller;
    }

    /**
     * Creates a child AbortController that aborts when its parent aborts.
     * Aborting the child does NOT affect the parent.
     */
    public static AbortController createChildAbortController(AbortController parent) {
        return createChildAbortController(parent, DEFAULT_MAX_LISTENERS);
    }

    public static AbortController createChildAbortController(AbortController parent, int maxListeners) {
        AbortController child = createAbortController(maxListeners);

        // Fast path: parent already aborted
        if (parent.isAborted()) {
            child.abort(parent.getReason());
            return child;
        }

        // Use weak references to prevent memory leaks
        WeakReference<AbortController> weakChild = new WeakReference<>(child);
        WeakReference<AbortController> weakParent = new WeakReference<>(parent);

        Consumer<String> handler = reason -> {
            AbortController c = weakChild.get();
            if (c != null) {
                c.abort(reason);
            }
        };

        parent.addListener(handler);

        // Auto-cleanup: remove parent listener when child is aborted
        child.addListener(reason -> {
            AbortController p = weakParent.get();
            if (p != null) {
                p.removeListener(handler);
            }
        });

        return child;
    }
}