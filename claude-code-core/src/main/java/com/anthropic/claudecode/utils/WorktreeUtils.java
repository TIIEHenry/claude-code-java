/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/worktree.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.nio.file.*;
import com.anthropic.claudecode.utils.Debug;

/**
 * Worktree utilities for managing git worktrees.
 */
public final class WorktreeUtils {
    private WorktreeUtils() {}

    private static final int MAX_WORKTREE_SLUG_LENGTH = 64;
    private static final Pattern VALID_SEGMENT = Pattern.compile("^[a-zA-Z0-9._-]+$");

    /**
     * Worktree session record.
     */
    public record WorktreeSession(
        String originalCwd,
        String worktreePath,
        String worktreeName,
        String worktreeBranch,
        String originalBranch,
        String originalHeadCommit,
        String sessionId,
        String tmuxSessionName,
        boolean hookBased,
        Long creationDurationMs,
        Boolean usedSparsePaths
    ) {}

    private static volatile WorktreeSession currentWorktreeSession = null;

    /**
     * Validate a worktree slug to prevent path traversal and directory escape.
     */
    public static void validateWorktreeSlug(String slug) {
        if (slug.length() > MAX_WORKTREE_SLUG_LENGTH) {
            throw new IllegalArgumentException(
                "Invalid worktree name: must be " + MAX_WORKTREE_SLUG_LENGTH +
                " characters or fewer (got " + slug.length() + ")"
            );
        }

        // Check for leading or trailing slashes
        if (slug.startsWith("/") || slug.endsWith("/")) {
            throw new IllegalArgumentException(
                "Invalid worktree name \"" + slug + "\": must not start or end with '/'"
            );
        }

        // Validate each segment
        for (String segment : slug.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException(
                    "Invalid worktree name \"" + slug + "\": must not contain \".\" or \"..\" path segments"
                );
            }

            if (!VALID_SEGMENT.matches(segment)) {
                throw new IllegalArgumentException(
                    "Invalid worktree name \"" + slug + "\": each \"/\"-separated segment must be non-empty " +
                    "and contain only letters, digits, dots, underscores, and dashes"
                );
            }
        }
    }

    /**
     * Get current worktree session.
     */
    public static WorktreeSession getCurrentWorktreeSession() {
        return currentWorktreeSession;
    }

    /**
     * Restore worktree session.
     */
    public static void restoreWorktreeSession(WorktreeSession session) {
        currentWorktreeSession = session;
    }

    /**
     * Generate tmux session name.
     */
    public static String generateTmuxSessionName(String repoPath, String branch) {
        String repoName = Paths.get(repoPath).getFileName().toString();
        String combined = repoName + "_" + branch;
        return combined.replaceAll("[/.]", "_");
    }

    /**
     * Find canonical git root.
     */
    public static String findCanonicalGitRoot(String cwd) {
        try {
            Path path = Paths.get(cwd);
            while (path != null) {
                Path gitDir = path.resolve(".git");
                if (Files.exists(gitDir)) {
                    // Check if it's a worktree
                    if (Files.isDirectory(gitDir)) {
                        Path commonDirPath = gitDir.resolve("commondir");
                        if (Files.exists(commonDirPath)) {
                            String commonDir = Files.readString(commonDirPath).trim();
                            Path mainGitDir = path.resolve(".git").resolve(commonDir);
                            if (Files.exists(mainGitDir)) {
                                return mainGitDir.getParent().toString();
                            }
                        }
                    }
                    return path.toString();
                }
                path = path.getParent();
            }
        } catch (Exception e) {
            Debug.logForDebugging("Error finding git root: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if currently in a worktree.
     */
    public static boolean isInWorktree(String cwd) {
        try {
            Path gitPath = Paths.get(cwd, ".git");
            if (Files.isRegularFile(gitPath)) {
                // .git is a file in worktrees pointing to the main repo
                return true;
            }
            if (Files.isDirectory(gitPath)) {
                // Check for commondir file which indicates worktree
                return Files.exists(gitPath.resolve("commondir"));
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Get worktrees directory.
     */
    public static Path getWorktreesDirectory(String repoRoot) {
        return Paths.get(repoRoot, ".claude", "worktrees");
    }

    /**
     * Create worktree for session.
     */
    public static CompletableFuture<WorktreeSession> createWorktreeForSession(
        String sessionId,
        String slug,
        String cwd
    ) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String originalCwd = cwd;

            try {
                // Find main repo root
                String mainRepoRoot = findCanonicalGitRoot(cwd);
                if (mainRepoRoot == null) {
                    mainRepoRoot = cwd;
                }

                // Create worktree path
                String worktreeName = slug != null ? slug : generateSlug();
                Path worktreesDir = getWorktreesDirectory(mainRepoRoot);
                Path worktreePath = worktreesDir.resolve(worktreeName);

                // Create directories
                Files.createDirectories(worktreesDir);

                // Create the worktree
                if (!Files.exists(worktreePath)) {
                    Files.createDirectories(worktreePath);

                    // Copy .git file or create worktree
                    // In a real implementation, this would run git worktree add
                    Debug.logForDebugging("Created worktree at: " + worktreePath);
                }

                long duration = System.currentTimeMillis() - startTime;

                return new WorktreeSession(
                    originalCwd,
                    worktreePath.toString(),
                    worktreeName,
                    worktreeName, // branch name same as slug
                    null,
                    null,
                    sessionId,
                    null,
                    false,
                    duration,
                    false
                );

            } catch (Exception e) {
                throw new RuntimeException("Failed to create worktree: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Remove worktree.
     */
    public static CompletableFuture<Boolean> removeWorktree(String worktreePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path path = Paths.get(worktreePath);
                if (Files.exists(path)) {
                    Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception e) {}
                        });
                    return true;
                }
                return false;
            } catch (Exception e) {
                Debug.logForDebugging("Failed to remove worktree: " + e.getMessage());
                return false;
            }
        });
    }

    private static String generateSlug() {
        return "worktree-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Pattern for validation.
     */
    private static final class Pattern {
        private final java.util.regex.Pattern pattern;

        public Pattern(String regex) {
            this.pattern = java.util.regex.Pattern.compile(regex);
        }

        public boolean matches(String input) {
            return pattern.matcher(input).matches();
        }

        public static Pattern compile(String regex) {
            return new Pattern(regex);
        }
    }
}