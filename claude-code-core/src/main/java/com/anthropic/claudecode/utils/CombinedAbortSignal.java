/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/combinedAbortSignal.ts
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Creates a combined AbortSignal that aborts when the input signal aborts,
 * an optional second signal aborts, or an optional timeout elapses.
 */
public final class CombinedAbortSignal {
    private CombinedAbortSignal() {}

    /**
     * Options for combined abort signal.
     */
    public record Options(
        AbortControllerUtil.AbortController signalB,
        Integer timeoutMs
    ) {}

    /**
     * Result of combined abort signal creation.
     */
    public record Result(
        AbortControllerUtil.AbortController signal,
        Runnable cleanup
    ) {}

    /**
     * Creates a combined AbortSignal that aborts when the input signal aborts,
     * an optional second signal aborts, or an optional timeout elapses.
     */
    public static Result createCombinedAbortSignal(
            AbortControllerUtil.AbortController signal,
            Options opts) {

        AbortControllerUtil.AbortController signalB = opts != null ? opts.signalB() : null;
        Integer timeoutMs = opts != null ? opts.timeoutMs() : null;

        AbortControllerUtil.AbortController combined = AbortControllerUtil.createAbortController();

        // Fast path: either signal already aborted
        if ((signal != null && signal.isAborted()) ||
            (signalB != null && signalB.isAborted())) {
            String reason = signal != null && signal.isAborted()
                ? signal.getReason()
                : (signalB != null ? signalB.getReason() : null);
            combined.abort(reason);
            return new Result(combined, () -> {});
        }

        AtomicReference<ScheduledFuture<?>> timer = new AtomicReference<>(null);
        AtomicBoolean cleaned = new AtomicBoolean(false);

        AbortControllerUtil.Consumer<String> abortCombined = reason -> {
            ScheduledFuture<?> t = timer.get();
            if (t != null) {
                t.cancel(false);
            }
            combined.abort(reason);
        };

        // Set up timeout if provided
        if (timeoutMs != null) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            timer.set(scheduler.schedule(() -> {
                abortCombined.accept("timeout");
                scheduler.shutdown();
            }, timeoutMs, TimeUnit.MILLISECONDS));
        }

        // Add listeners to signals
        if (signal != null) {
            signal.addListener(abortCombined);
        }
        if (signalB != null) {
            signalB.addListener(abortCombined);
        }

        Runnable cleanup = () -> {
            if (cleaned.compareAndSet(false, true)) {
                ScheduledFuture<?> t = timer.get();
                if (t != null) {
                    t.cancel(false);
                }
                if (signal != null) {
                    signal.removeListener(abortCombined);
                }
                if (signalB != null) {
                    signalB.removeListener(abortCombined);
                }
            }
        };

        return new Result(combined, cleanup);
    }

    /**
     * Create combined abort signal with just timeout.
     */
    public static Result createCombinedAbortSignal(int timeoutMs) {
        return createCombinedAbortSignal(null, new Options(null, timeoutMs));
    }

    /**
     * Create combined abort signal with parent signal.
     */
    public static Result createCombinedAbortSignal(AbortControllerUtil.AbortController signal) {
        return createCombinedAbortSignal(signal, null);
    }

    /**
     * Create combined abort signal with parent signal and timeout.
     */
    public static Result createCombinedAbortSignal(
            AbortControllerUtil.AbortController signal,
            int timeoutMs) {
        return createCombinedAbortSignal(signal, new Options(null, timeoutMs));
    }
}