/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tmux socket utilities
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.CompletableFuture;

/**
 * TMUX socket isolation utilities.
 *
 * This module manages an isolated tmux socket for Claude's operations.
 * Without isolation, Claude could accidentally affect the user's tmux sessions.
 *
 * NOTE: This Java implementation is a simplified version that provides
 * the core socket management functionality. Full tmux command execution
 * would require integration with the Bash tool.
 */
public final class TmuxSocket {
    private TmuxSocket() {}

    private static final String TMUX_COMMAND = "tmux";
    private static final String CLAUDE_SOCKET_PREFIX = "claude";

    // Socket state
    private static volatile String socketName = null;
    private static volatile String socketPath = null;
    private static volatile Long serverPid = null;
    private static volatile boolean isInitializing = false;
    private static volatile boolean tmuxAvailable = false;
    private static volatile boolean tmuxAvailabilityChecked = false;
    private static volatile boolean tmuxToolUsed = false;

    /**
     * Gets the socket name for Claude's isolated tmux session.
     * Format: claude-<PID>
     */
    public static String getClaudeSocketName() {
        if (socketName == null) {
            socketName = CLAUDE_SOCKET_PREFIX + "-" + ProcessHandle.current().pid();
        }
        return socketName;
    }

    /**
     * Gets the socket path if the socket has been initialized.
     */
    public static String getClaudeSocketPath() {
        return socketPath;
    }

    /**
     * Sets socket info after initialization.
     */
    public static void setClaudeSocketInfo(String path, long pid) {
        socketPath = path;
        serverPid = pid;
    }

    /**
     * Returns whether the socket has been initialized.
     */
    public static boolean isSocketInitialized() {
        return socketPath != null && serverPid != null;
    }

    /**
     * Gets the TMUX environment variable value for Claude's isolated socket.
     * Format: "socket_path,server_pid,pane_index"
     */
    public static String getClaudeTmuxEnv() {
        if (socketPath == null || serverPid == null) {
            return null;
        }
        return socketPath + "," + serverPid + ",0";
    }

    /**
     * Checks if tmux is available on this system.
     */
    public static CompletableFuture<Boolean> checkTmuxAvailable() {
        if (tmuxAvailabilityChecked) {
            return CompletableFuture.completedFuture(tmuxAvailable);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("which", TMUX_COMMAND);
                Process p = pb.start();
                int exitCode = p.waitFor();
                tmuxAvailable = exitCode == 0;
                tmuxAvailabilityChecked = true;
                return tmuxAvailable;
            } catch (Exception e) {
                tmuxAvailable = false;
                tmuxAvailabilityChecked = true;
                return false;
            }
        });
    }

    /**
     * Returns the cached tmux availability status.
     */
    public static boolean isTmuxAvailable() {
        return tmuxAvailabilityChecked && tmuxAvailable;
    }

    /**
     * Marks that the Tmux tool has been used at least once.
     */
    public static void markTmuxToolUsed() {
        tmuxToolUsed = true;
    }

    /**
     * Returns whether the Tmux tool has been used at least once.
     */
    public static boolean hasTmuxToolBeenUsed() {
        return tmuxToolUsed;
    }

    /**
     * Ensures the socket is initialized with a tmux session.
     */
    public static CompletableFuture<Void> ensureSocketInitialized() {
        if (isSocketInitialized()) {
            return CompletableFuture.completedFuture(null);
        }

        return checkTmuxAvailable().thenCompose(available -> {
            if (!available) {
                return CompletableFuture.completedFuture(null);
            }
            return doInitialize();
        });
    }

    /**
     * Initialize the socket.
     */
    private static CompletableFuture<Void> doInitialize() {
        return CompletableFuture.runAsync(() -> {
            String socket = getClaudeSocketName();

            try {
                // Create a new session with custom socket
                ProcessBuilder pb = new ProcessBuilder(
                        TMUX_COMMAND, "-L", socket,
                        "new-session", "-d", "-s", "base",
                        "-e", "CLAUDE_CODE_SKIP_PROMPT_HISTORY=true"
                );
                Process p = pb.start();
                int exitCode = p.waitFor();

                if (exitCode != 0) {
                    // Check if session already exists
                    ProcessBuilder checkPb = new ProcessBuilder(
                            TMUX_COMMAND, "-L", socket,
                            "has-session", "-t", "base"
                    );
                    Process checkP = checkPb.start();
                    int checkExit = checkP.waitFor();

                    if (checkExit != 0) {
                        throw new RuntimeException("Failed to create tmux session");
                    }
                }

                // Get socket path and server PID
                ProcessBuilder infoPb = new ProcessBuilder(
                        TMUX_COMMAND, "-L", socket,
                        "display-message", "-p", "#{socket_path},#{pid}"
                );
                Process infoP = infoPb.start();
                String output = new java.io.BufferedReader(
                        new java.io.InputStreamReader(infoP.getInputStream()))
                        .lines().collect(java.util.stream.Collectors.joining("\n"));
                infoP.waitFor();

                String[] parts = output.trim().split(",");
                if (parts.length == 2) {
                    String path = parts[0];
                    long pid = Long.parseLong(parts[1]);
                    setClaudeSocketInfo(path, pid);
                } else {
                    // Fallback path
                    String fallbackPath = "/tmp/tmux-" + getUid() + "/" + socket;
                    setClaudeSocketInfo(fallbackPath, ProcessHandle.current().pid());
                }
            } catch (Exception e) {
                Debug.log("Failed to initialize tmux socket: " + e.getMessage(), "warn");
            }
        });
    }

    /**
     * Kill the tmux server for Claude's socket.
     */
    public static CompletableFuture<Void> killTmuxServer() {
        String socket = getClaudeSocketName();
        Debug.log("Killing tmux server for socket: " + socket, "debug");

        return CompletableFuture.runAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        TMUX_COMMAND, "-L", socket, "kill-server"
                );
                Process p = pb.start();
                p.waitFor();
            } catch (Exception e) {
                Debug.log("Failed to kill tmux server: " + e.getMessage(), "warn");
            }
        });
    }

    /**
     * Get the UID (user ID) for the current process.
     */
    private static long getUid() {
        // Java doesn't have direct access to UID, use fallback
        return 0;
    }

    /**
     * Reset socket state (for testing).
     */
    public static void resetSocketState() {
        socketName = null;
        socketPath = null;
        serverPid = null;
        isInitializing = false;
        tmuxAvailable = false;
        tmuxAvailabilityChecked = false;
        tmuxToolUsed = false;
    }

    /**
     * Register cleanup handler for graceful shutdown.
     */
    public static void registerCleanup() {
        // Register with cleanup registry
        CleanupRegistry.register("tmux-socket", () -> {
            killTmuxServer();
        });
    }
}