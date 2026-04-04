/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/gracefulShutdown
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Graceful shutdown - Signal handlers and cleanup orchestration.
 */
public final class GracefulShutdown {
    private static final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private static final AtomicBoolean resumeHintPrinted = new AtomicBoolean(false);
    private static ScheduledFuture<?> failsafeTimer;
    private static ScheduledFuture<?> orphanCheckInterval;
    private static CompletableFuture<Void> pendingShutdown;

    private static final List<Runnable> cleanupFunctions = new CopyOnWriteArrayList<>();

    /**
     * Exit reason enum.
     */
    public enum ExitReason {
        SIGINT,
        SIGTERM,
        SIGHUP,
        ORPHAN_DETECTED,
        OTHER
    }

    /**
     * Setup graceful shutdown handlers.
     */
    public static void setupGracefulShutdown() {
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                gracefulShutdown(0, ExitReason.OTHER).join();
            } catch (Exception e) {
                // Ignore
            }
        }));

        // Signal handling would be done via sun.misc.Signal or similar
        // For portability, we use shutdown hooks primarily

        // Setup orphan detection for TTY sessions
        setupOrphanDetection();
    }

    /**
     * Add cleanup function.
     */
    public static void addCleanupFunction(Runnable cleanup) {
        cleanupFunctions.add(cleanup);
    }

    /**
     * Remove cleanup function.
     */
    public static void removeCleanupFunction(Runnable cleanup) {
        cleanupFunctions.remove(cleanup);
    }

    /**
     * Check if shutdown is in progress.
     */
    public static boolean isShuttingDown() {
        return shutdownInProgress.get();
    }

    /**
     * Reset shutdown state (for testing).
     */
    public static void resetShutdownState() {
        shutdownInProgress.set(false);
        resumeHintPrinted.set(false);
        if (failsafeTimer != null) {
            failsafeTimer.cancel(false);
            failsafeTimer = null;
        }
        pendingShutdown = null;
    }

    /**
     * Get pending shutdown for testing.
     */
    public static CompletableFuture<Void> getPendingShutdownForTesting() {
        return pendingShutdown;
    }

    /**
     * Synchronous graceful shutdown.
     */
    public static void gracefulShutdownSync(int exitCode, ExitReason reason) {
        System.exit(exitCode); // Set exit code
        pendingShutdown = gracefulShutdown(exitCode, reason)
            .exceptionally(e -> {
                cleanupTerminalModes();
                printResumeHint();
                forceExit(exitCode);
                return null;
            });
    }

    /**
     * Graceful shutdown.
     */
    public static CompletableFuture<Void> gracefulShutdown(int exitCode, ExitReason reason) {
        if (shutdownInProgress.get()) {
            return CompletableFuture.completedFuture(null);
        }
        shutdownInProgress.set(true);

        return CompletableFuture.supplyAsync(() -> {
            // Setup failsafe timer
            long sessionEndTimeoutMs = getSessionEndHookTimeoutMs();
            long failsafeDelayMs = Math.max(5000, sessionEndTimeoutMs + 3500);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            failsafeTimer = scheduler.schedule(() -> {
                cleanupTerminalModes();
                printResumeHint();
                forceExit(exitCode);
            }, failsafeDelayMs, TimeUnit.MILLISECONDS);

            // Set exit code
            System.exit(exitCode);

            // Cleanup terminal modes first
            cleanupTerminalModes();
            printResumeHint();

            // Run cleanup functions with timeout
            try {
                CompletableFuture<Void> cleanupFuture = runCleanupFunctions();
                cleanupFuture.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Timeout or error
            }

            // Execute session end hooks
            try {
                executeSessionEndHooks(reason, sessionEndTimeoutMs);
            } catch (Exception e) {
                // Ignore
            }

            // Flush analytics
            try {
                CompletableFuture.allOf(
                    shutdown1PEventLogging(),
                    shutdownDatadog()
                ).get(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Ignore
            }

            // Cancel failsafe and exit
            if (failsafeTimer != null) {
                failsafeTimer.cancel(false);
            }
            scheduler.shutdown();

            forceExit(exitCode);
            return null;
        });
    }

    /**
     * Cleanup terminal modes.
     */
    private static void cleanupTerminalModes() {
        // Disable mouse tracking, alt screen, etc.
        // In Java, this would write ANSI escape codes to stdout
        if (!System.out.getClass().getName().contains("FileOutputStream")) {
            try {
                // Write terminal reset sequences
                System.out.print("\u001b[?1000l"); // Disable mouse tracking
                System.out.print("\u001b[?1049l"); // Exit alt screen
                System.out.print("\u001b[?2004l"); // Disable bracketed paste
                System.out.print("\u001b[?25h");   // Show cursor
                System.out.flush();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Print resume hint.
     */
    private static void printResumeHint() {
        if (resumeHintPrinted.get()) {
            return;
        }

        String sessionId = getSessionId();
        if (sessionId != null && isInteractive() && !isSessionPersistenceDisabled()) {
            try {
                String hint = String.format(
                    "\nResume this session with:\nclaude --resume %s\n",
                    sessionId
                );
                System.out.print(hint);
                resumeHintPrinted.set(true);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Force exit process.
     */
    private static void forceExit(int exitCode) {
        // Clear failsafe timer
        if (failsafeTimer != null) {
            failsafeTimer.cancel(false);
            failsafeTimer = null;
        }

        try {
            System.exit(exitCode);
        } catch (Exception e) {
            // Fallback to runtime halt
            Runtime.getRuntime().halt(exitCode);
        }
    }

    /**
     * Run cleanup functions.
     */
    private static CompletableFuture<Void> runCleanupFunctions() {
        return CompletableFuture.runAsync(() -> {
            for (Runnable cleanup : cleanupFunctions) {
                try {
                    cleanup.run();
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }

    /**
     * Execute session end hooks.
     */
    private static void executeSessionEndHooks(ExitReason reason, long timeoutMs) {
        // Load and execute hooks from configuration
        try {
            java.nio.file.Path hooksPath = java.nio.file.Paths.get(
                System.getProperty("user.home"),
                ".claude",
                "settings.json"
            );

            if (!java.nio.file.Files.exists(hooksPath)) {
                return;
            }

            String content = java.nio.file.Files.readString(hooksPath);

            // Find sessionEnd hooks
            int hooksIdx = content.indexOf("\"hooks\"");
            if (hooksIdx < 0) return;

            int sessionEndIdx = content.indexOf("\"sessionEnd\"", hooksIdx);
            if (sessionEndIdx < 0) return;

            // Find the hooks array
            int arrStart = content.indexOf("[", sessionEndIdx);
            if (arrStart < 0) return;

            int depth = 1;
            int arrEnd = arrStart + 1;
            while (arrEnd < content.length() && depth > 0) {
                char c = content.charAt(arrEnd);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                arrEnd++;
            }

            String hooksArray = content.substring(arrStart, arrEnd);

            // Execute each hook command
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            int i = 0;
            while (i < hooksArray.length()) {
                int strStart = hooksArray.indexOf("\"", i);
                if (strStart < 0) break;
                int strEnd = hooksArray.indexOf("\"", strStart + 1);
                if (strEnd < 0) break;

                String hookCmd = hooksArray.substring(strStart + 1, strEnd);
                if (!hookCmd.isEmpty()) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            ProcessBuilder pb = new ProcessBuilder("sh", "-c", hookCmd);
                            pb.redirectErrorStream(true);
                            Process process = pb.start();
                            process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                            process.destroyForcibly();
                        } catch (Exception e) {
                            // Ignore hook errors
                        }
                    });
                    futures.add(future);
                }

                i = strEnd + 1;
            }

            // Wait for all hooks with overall timeout
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Timeout
            }

            scheduler.shutdown();

        } catch (Exception e) {
            // Ignore hook execution errors
        }
    }

    /**
     * Setup orphan detection.
     */
    private static void setupOrphanDetection() {
        if (System.console() != null) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            orphanCheckInterval = scheduler.scheduleAtFixedRate(() -> {
                if (System.out.checkError()) {
                    gracefulShutdown(129, ExitReason.ORPHAN_DETECTED);
                }
            }, 30, 30, TimeUnit.SECONDS);
            orphanCheckInterval.cancel(false); // Don't prevent shutdown
        }
    }

    // Helper methods
    private static long getSessionEndHookTimeoutMs() {
        String timeout = System.getenv("CLAUDE_CODE_SESSIONEND_HOOKS_TIMEOUT_MS");
        if (timeout != null) {
            try {
                return Long.parseLong(timeout);
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        return 1500; // Default 1.5s
    }

    private static String getSessionId() {
        return System.getProperty("claude.code.session.id");
    }

    private static boolean isInteractive() {
        return System.console() != null;
    }

    private static boolean isSessionPersistenceDisabled() {
        return "true".equals(System.getenv("CLAUDE_CODE_DISABLE_SESSION_PERSISTENCE"));
    }

    private static CompletableFuture<Void> shutdown1PEventLogging() {
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<Void> shutdownDatadog() {
        return CompletableFuture.completedFuture(null);
    }
}