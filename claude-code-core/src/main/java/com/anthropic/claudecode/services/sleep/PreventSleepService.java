/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/preventSleep.ts
 */
package com.anthropic.claudecode.services.sleep;

import java.util.concurrent.*;
import com.anthropic.claudecode.utils.Debug;

/**
 * Prevents macOS from sleeping while Claude is working.
 *
 * Uses the built-in `caffeinate` command to create a power assertion.
 * Only runs on macOS - no-op on other platforms.
 */
public final class PreventSleepService {
    private PreventSleepService() {}

    // Caffeinate timeout in seconds - process auto-exits after this
    private static final int CAFFEINATE_TIMEOUT_SECONDS = 300; // 5 minutes

    // Restart interval - restart before expiry
    private static final long RESTART_INTERVAL_MS = 4 * 60 * 1000;

    private static Process caffeinateProcess = null;
    private static ScheduledFuture<?> restartFuture = null;
    private static int refCount = 0;
    private static boolean cleanupRegistered = false;

    private static final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    /**
     * Increment the reference count and start preventing sleep if needed.
     */
    public static synchronized void startPreventSleep() {
        refCount++;

        if (refCount == 1) {
            spawnCaffeinate();
            startRestartInterval();
        }
    }

    /**
     * Decrement the reference count and allow sleep if no more work pending.
     */
    public static synchronized void stopPreventSleep() {
        if (refCount > 0) {
            refCount--;
        }

        if (refCount == 0) {
            stopRestartInterval();
            killCaffeinate();
        }
    }

    /**
     * Force stop preventing sleep, regardless of reference count.
     */
    public static synchronized void forceStopPreventSleep() {
        refCount = 0;
        stopRestartInterval();
        killCaffeinate();
    }

    private static void startRestartInterval() {
        // Only run on macOS
        if (!isMacOS()) {
            return;
        }

        // Already running
        if (restartFuture != null) {
            return;
        }

        restartFuture = scheduler.scheduleAtFixedRate(() -> {
            if (refCount > 0) {
                Debug.logForDebugging("Restarting caffeinate to maintain sleep prevention");
                killCaffeinate();
                spawnCaffeinate();
            }
        }, RESTART_INTERVAL_MS, RESTART_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private static void stopRestartInterval() {
        if (restartFuture != null) {
            restartFuture.cancel(false);
            restartFuture = null;
        }
    }

    private static void spawnCaffeinate() {
        // Only run on macOS
        if (!isMacOS()) {
            return;
        }

        // Already running
        if (caffeinateProcess != null) {
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "caffeinate",
                "-i", // Prevent idle sleep
                "-t", String.valueOf(CAFFEINATE_TIMEOUT_SECONDS)
            );
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);

            caffeinateProcess = pb.start();

            Debug.logForDebugging("Started caffeinate to prevent sleep");
        } catch (Exception e) {
            Debug.logForDebugging("caffeinate spawn error: " + e.getMessage());
            caffeinateProcess = null;
        }
    }

    private static void killCaffeinate() {
        if (caffeinateProcess != null) {
            Process proc = caffeinateProcess;
            caffeinateProcess = null;
            try {
                proc.destroyForcibly();
                Debug.logForDebugging("Stopped caffeinate, allowing sleep");
            } catch (Exception e) {
                // Process may have already exited
            }
        }
    }

    private static boolean isMacOS() {
        return "Mac OS X".equals(System.getProperty("os.name")) ||
               "Darwin".equals(System.getProperty("os.name"));
    }

    /**
     * Shutdown the scheduler.
     */
    public static void shutdown() {
        forceStopPreventSleep();
        scheduler.shutdown();
    }
}