/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code abort controller utilities
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.*;
import java.lang.ref.*;

/**
 * Abort controller utilities with proper listener limits and child controllers.
 * Java equivalent of Node.js AbortController with memory-safe parent-child propagation.
 */
public final class AbortControllerUtils {
    private AbortControllerUtils() {}

    /**
     * Default max listeners for standard operations.
     */
    public static final int DEFAULT_MAX_LISTENERS = 50;

    /**
     * Abort signal interface for Java.
     */
    public sealed interface AbortSignal permits AbortSignalImpl {
        boolean isAborted();
        Object getReason();
        void addListener(AbortListener listener);
        void removeListener(AbortListener listener);
    }

    /**
     * Abort listener functional interface.
     */
    @FunctionalInterface
    public interface AbortListener {
        void onAbort(Object reason);
    }

    /**
     * Abort signal implementation.
     */
    public static final class AbortSignalImpl implements AbortSignal {
        private volatile boolean aborted = false;
        private volatile Object reason = null;
        private final CopyOnWriteArrayList<AbortListener> listeners = new CopyOnWriteArrayList<>();
        private final int maxListeners;

        AbortSignalImpl(int maxListeners) {
            this.maxListeners = maxListeners;
        }

        @Override
        public boolean isAborted() {
            return aborted;
        }

        @Override
        public Object getReason() {
            return reason;
        }

        @Override
        public void addListener(AbortListener listener) {
            if (listeners.size() < maxListeners) {
                listeners.add(listener);
            }
        }

        @Override
        public void removeListener(AbortListener listener) {
            listeners.remove(listener);
        }

        void abort(Object reason) {
            if (!aborted) {
                this.aborted = true;
                this.reason = reason;
                for (AbortListener listener : listeners) {
                    try {
                        listener.onAbort(reason);
                    } catch (Exception e) {
                        // Ignore listener errors
                    }
                }
            }
        }
    }

    /**
     * Abort controller for Java.
     */
    public static final class AbortController {
        private final AbortSignalImpl signal;

        public AbortController() {
            this(DEFAULT_MAX_LISTENERS);
        }

        public AbortController(int maxListeners) {
            this.signal = new AbortSignalImpl(maxListeners);
        }

        public AbortSignal getSignal() {
            return signal;
        }

        public void abort() {
            abort(null);
        }

        public void abort(Object reason) {
            signal.abort(reason);
        }
    }

    /**
     * Create an AbortController with proper listener limits.
     */
    public static AbortController createAbortController(int maxListeners) {
        return new AbortController(maxListeners);
    }

    public static AbortController createAbortController() {
        return createAbortController(DEFAULT_MAX_LISTENERS);
    }

    /**
     * Propagates abort from parent to child using weak references.
     */
    private static class PropagateAbortHandler implements AbortListener {
        private final WeakReference<AbortController> weakParent;
        private final WeakReference<AbortController> weakChild;

        PropagateAbortHandler(AbortController parent, AbortController child) {
            this.weakParent = new WeakReference<>(parent);
            this.weakChild = new WeakReference<>(child);
        }

        @Override
        public void onAbort(Object reason) {
            AbortController child = weakChild.get();
            if (child != null && !child.getSignal().isAborted()) {
                child.abort(reason);
            }
        }
    }

    /**
     * Removes abort handler from parent signal when child is aborted.
     */
    private static class CleanupHandler implements AbortListener {
        private final WeakReference<AbortSignal> weakParentSignal;
        private final WeakReference<AbortListener> weakHandler;

        CleanupHandler(AbortSignal parentSignal, AbortListener handler) {
            this.weakParentSignal = new WeakReference<>(parentSignal);
            this.weakHandler = new WeakReference<>(handler);
        }

        @Override
        public void onAbort(Object reason) {
            AbortSignal parentSignal = weakParentSignal.get();
            AbortListener handler = weakHandler.get();
            if (parentSignal != null && handler != null) {
                parentSignal.removeListener(handler);
            }
        }
    }

    /**
     * Creates a child AbortController that aborts when its parent aborts.
     * Aborting the child does NOT affect the parent.
     *
     * Memory-safe: Uses WeakRef so parent doesn't retain abandoned children.
     */
    public static AbortController createChildAbortController(AbortController parent, int maxListeners) {
        AbortController child = createAbortController(maxListeners);

        // Fast path: parent already aborted
        if (parent.getSignal().isAborted()) {
            child.abort(parent.getSignal().getReason());
            return child;
        }

        // Set up propagation handler
        PropagateAbortHandler propagateHandler = new PropagateAbortHandler(parent, child);
        parent.getSignal().addListener(propagateHandler);

        // Auto-cleanup when child is aborted
        CleanupHandler cleanupHandler = new CleanupHandler(parent.getSignal(), propagateHandler);
        child.getSignal().addListener(cleanupHandler);

        return child;
    }

    public static AbortController createChildAbortController(AbortController parent) {
        return createChildAbortController(parent, DEFAULT_MAX_LISTENERS);
    }

    /**
     * Combined abort signal that aborts when any of the sources abort.
     */
    public static AbortSignal combineAbortSignals(AbortSignal... signals) {
        AbortController combined = createAbortController(signals.length * 2);

        for (AbortSignal signal : signals) {
            if (signal.isAborted()) {
                combined.abort(signal.getReason());
                return combined.getSignal();
            }

            signal.addListener(reason -> {
                if (!combined.getSignal().isAborted()) {
                    combined.abort(reason);
                }
            });
        }

        return combined.getSignal();
    }

    /**
     * Timeout abort signal that aborts after specified duration.
     */
    public static AbortSignal timeoutAbortSignal(long timeoutMs) {
        AbortController controller = createAbortController();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(() -> {
            if (!controller.getSignal().isAborted()) {
                controller.abort(new TimeoutException("Operation timed out after " + timeoutMs + "ms"));
            }
            scheduler.shutdown();
        }, timeoutMs, TimeUnit.MILLISECONDS);

        return controller.getSignal();
    }

    /**
     * Check if a signal is aborted and throw if so.
     */
    public static void checkAborted(AbortSignal signal) throws AbortException {
        if (signal != null && signal.isAborted()) {
            throw new AbortException(signal.getReason());
        }
    }

    /**
     * Abort exception.
     */
    public static final class AbortException extends RuntimeException {
        private final Object reason;

        public AbortException(Object reason) {
            super("Operation was aborted", reason instanceof Throwable t ? t : null);
            this.reason = reason;
        }

        public Object getReason() {
            return reason;
        }
    }
}