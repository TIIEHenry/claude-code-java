/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Git utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/**
 * Git utilities wrapper class.
 */
public final class Git {
    private Git() {}

    /**
     * Get git executable path.
     */
    public static String gitExe() {
        return "git";
    }

    /**
     * Find git root for a path.
     */
    public static Path findGitRoot(Path path) {
        String result = GitUtils.findGitRoot(path.toString());
        return result != null ? Paths.get(result) : null;
    }

    /**
     * Get remote URL for a directory.
     */
    public static String getRemoteUrlForDir(String dir) {
        return GitUtils.getRemoteUrl(dir);
    }

    /**
     * Check if path is in a git repository.
     */
    public static boolean isInGitRepo(Path path) {
        return GitUtils.isInGitRepo(path.toString());
    }

    /**
     * Get current branch.
     */
    public static CompletableFuture<String> getCurrentBranch(Path repoPath) {
        return CompletableFuture.supplyAsync(() -> {
            return GitUtils.getCurrentBranch(repoPath.toString());
        });
    }

    /**
     * Check if current directory is in a git repository.
     */
    public static CompletableFuture<Boolean> getIsGit() {
        return CompletableFuture.supplyAsync(() -> {
            return GitUtils.isInGitRepo(System.getProperty("user.dir"));
        });
    }

    /**
     * Get current branch name.
     */
    public static CompletableFuture<String> getBranch() {
        return CompletableFuture.supplyAsync(() -> {
            return GitUtils.getCurrentBranch(System.getProperty("user.dir"));
        });
    }

    /**
     * Get default branch name (main or master).
     */
    public static CompletableFuture<String> getDefaultBranch() {
        return CompletableFuture.supplyAsync(() -> {
            String branch = GitUtils.getDefaultBranch(System.getProperty("user.dir"));
            return branch != null ? branch : "main";
        });
    }
}