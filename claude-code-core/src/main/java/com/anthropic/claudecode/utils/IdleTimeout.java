/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code idle timeout manager
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Creates an idle timeout manager for SDK mode.
 * Automatically exits the process after the specified idle duration.
 */
public final class IdleTimeout {
    private IdleTimeout() {}

    /**
     * Create an idle timeout manager.
     */
    public static IdleTimeoutManager createIdleTimeoutManager(java.util.function.Supplier<Boolean> isIdle) {
        String delayEnv = System.getenv("CLAUDE_CODE_EXIT_AFTER_STOP_DELAY");
        Long delayMs = null;

        if (delayEnv != null) {
            try {
                long parsed = Long.parseLong(delayEnv);
                if (parsed > 0) {
                    delayMs = parsed;
                }
            } catch (NumberFormatException e) {
                // Invalid delay
            }
        }

        return new IdleTimeoutManager(isIdle, delayMs);
    }

    /**
     * Idle timeout manager.
     */
    public static class IdleTimeoutManager {
        private final java.util.function.Supplier<Boolean> isIdle;
        private final Long delayMs;
        private ScheduledFuture<?> timer;
        private long lastIdleTime;
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        private IdleTimeoutManager(java.util.function.Supplier<Boolean> isIdle, Long delayMs) {
            this.isIdle = isIdle;
            this.delayMs = delayMs;
        }

        /**
         * Start the idle timer.
         */
        public void start() {
            stop();

            if (delayMs == null || delayMs <= 0) {
                return;
            }

            lastIdleTime = System.currentTimeMillis();

            timer = executor.schedule(() -> {
                long idleDuration = System.currentTimeMillis() - lastIdleTime;
                if (isIdle.get() && idleDuration >= delayMs) {
                    Debug.log("Exiting after " + delayMs + "ms of idle time");
                    System.exit(0);
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }

        /**
         * Stop the idle timer.
         */
        public void stop() {
            if (timer != null) {
                timer.cancel(false);
                timer = null;
            }
        }
    }
}