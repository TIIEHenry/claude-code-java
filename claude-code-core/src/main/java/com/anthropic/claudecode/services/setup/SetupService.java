/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code setup.ts
 */
package com.anthropic.claudecode.services.setup;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Setup service for CLI initialization.
 *
 * Handles session initialization, worktree creation, terminal backup restoration,
 * and other setup tasks that must run before the first query.
 */
public final class SetupService {
    private SetupService() {}

    private static volatile boolean initialized = false;
    private static volatile String cwd = System.getProperty("user.dir");
    private static volatile String projectRoot = null;
    private static volatile String sessionId = java.util.UUID.randomUUID().toString();

    /**
     * Setup options.
     */
    public record SetupOptions(
        String cwd,
        PermissionMode permissionMode,
        boolean allowDangerouslySkipPermissions,
        boolean worktreeEnabled,
        String worktreeName,
        boolean tmuxEnabled,
        String customSessionId,
        Integer worktreePRNumber,
        String messagingSocketPath
    ) {
        public SetupOptions(String cwd) {
            this(cwd, PermissionMode.DEFAULT, false, false, null, false, null, null, null);
        }
    }

    /**
     * Permission mode enum.
     */
    public enum PermissionMode {
        DEFAULT,
        ACCEPT_EDITS,
        BYPASS_PERMISSIONS,
        PLAN,
        AUTO
    }

    /**
     * Run setup.
     */
    public static CompletableFuture<Void> setup(SetupOptions options) {
        return CompletableFuture.runAsync(() -> {
            if (initialized) {
                return;
            }

            // Set CWD
            if (options.cwd() != null) {
                cwd = options.cwd();
            }

            // Set custom session ID if provided
            if (options.customSessionId() != null) {
                sessionId = options.customSessionId();
            }

            // Find project root
            projectRoot = findProjectRoot(cwd);

            // Handle worktree creation if requested
            if (options.worktreeEnabled()) {
                handleWorktreeCreation(options);
            }

            // Initialize session memory
            initSessionMemory();

            // Check permission mode safety
            validatePermissionMode(options);

            initialized = true;
        });
    }

    /**
     * Get current working directory.
     */
    public static String getCwd() {
        return cwd;
    }

    /**
     * Set current working directory.
     */
    public static void setCwd(String newCwd) {
        cwd = newCwd;
    }

    /**
     * Get project root.
     */
    public static String getProjectRoot() {
        return projectRoot != null ? projectRoot : cwd;
    }

    /**
     * Set project root.
     */
    public static void setProjectRoot(String root) {
        projectRoot = root;
    }

    /**
     * Get session ID.
     */
    public static String getSessionId() {
        return sessionId;
    }

    /**
     * Switch to a different session.
     */
    public static void switchSession(String newSessionId) {
        sessionId = newSessionId;
    }

    /**
     * Check if setup has completed.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Reset setup state (for testing).
     */
    public static void reset() {
        initialized = false;
        cwd = System.getProperty("user.dir");
        projectRoot = null;
        sessionId = java.util.UUID.randomUUID().toString();
    }

    // ─── Private helpers ──────────────────────────────────────────────────

    private static String findProjectRoot(String dir) {
        Path path = Paths.get(dir);

        // Walk up looking for project markers
        while (path != null) {
            // Check for git
            if (Files.exists(path.resolve(".git"))) {
                return path.toString();
            }

            // Check for other project markers
            if (Files.exists(path.resolve("pom.xml")) ||
                Files.exists(path.resolve("build.gradle")) ||
                Files.exists(path.resolve("package.json")) ||
                Files.exists(path.resolve("Cargo.toml"))) {
                return path.toString();
            }

            path = path.getParent();
        }

        return dir;
    }

    private static void handleWorktreeCreation(SetupOptions options) {
        // Check if in git repo
        boolean inGit = isGitRepository(cwd);

        if (!inGit) {
            System.err.println("Error: Can only use --worktree in a git repository");
            System.exit(1);
        }

        // Generate worktree name
        String slug = options.worktreePRNumber() != null
            ? "pr-" + options.worktreePRNumber()
            : (options.worktreeName() != null ? options.worktreeName() : "session-" + sessionId.substring(0, 8));

        // Create worktree (placeholder - would call git worktree add)
        String worktreePath = createWorktree(slug);
        if (worktreePath != null) {
            cwd = worktreePath;
            projectRoot = worktreePath;
        }
    }

    private static boolean isGitRepository(String dir) {
        return Files.exists(Paths.get(dir).resolve(".git"));
    }

    private static String createWorktree(String slug) {
        // Run git worktree add command
        Path worktreePath = Paths.get(cwd, ".claude", "worktrees", slug);
        try {
            Files.createDirectories(worktreePath.getParent());

            // Run git worktree add
            ProcessBuilder pb = new ProcessBuilder(
                "git", "worktree", "add",
                worktreePath.toString(),
                "-b", "worktree/" + slug
            );
            pb.directory(new java.io.File(cwd));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

            if (process.exitValue() == 0) {
                return worktreePath.toString();
            } else {
                // If git worktree fails, just create the directory
                Files.createDirectories(worktreePath);
                return worktreePath.toString();
            }
        } catch (Exception e) {
            // Fallback to just creating directory
            try {
                Files.createDirectories(worktreePath);
                return worktreePath.toString();
            } catch (IOException ex) {
                System.err.println("Error creating worktree: " + ex.getMessage());
                return null;
            }
        }
    }

    private static void initSessionMemory() {
        // Initialize session memory hooks for session persistence
        try {
            // Create session memory directory
            Path sessionDir = Paths.get(cwd, ".claude", "sessions");
            Files.createDirectories(sessionDir);

            // Register shutdown hook to save session state
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Path sessionFile = sessionDir.resolve("last-session.json");
                    String sessionData = String.format(
                        "{\"endedAt\":\"%s\",\"cwd\":\"%s\"}",
                        java.time.Instant.now().toString(),
                        cwd
                    );
                    Files.writeString(sessionFile, sessionData);
                } catch (Exception e) {
                    // Ignore errors on shutdown
                }
            }));
        } catch (Exception e) {
            System.err.println("Error initializing session memory: " + e.getMessage());
        }
    }

    private static void validatePermissionMode(SetupOptions options) {
        if (options.permissionMode() == PermissionMode.BYPASS_PERMISSIONS ||
            options.allowDangerouslySkipPermissions()) {

            // Check if running as root on Unix
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win")) {
                // On Unix, check for root
                // In Java, there's no direct way to check UID, so we check environment
                String user = System.getProperty("user.name");
                if ("root".equals(user) && !isSandboxed()) {
                    System.err.println("--dangerously-skip-permissions cannot be used with root/sudo privileges for security reasons");
                    System.exit(1);
                }
            }
        }
    }

    private static boolean isSandboxed() {
        // Check for common sandbox indicators
        return "true".equalsIgnoreCase(System.getenv("IS_SANDBOX")) ||
               "true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_BUBBLEWRAP")) ||
               Files.exists(Paths.get("/.dockerenv"));
    }
}