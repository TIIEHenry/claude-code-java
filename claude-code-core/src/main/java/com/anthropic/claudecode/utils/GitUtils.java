/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/git.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Git utilities for repository detection and operations.
 */
public final class GitUtils {
    private GitUtils() {}

    // Cache for git roots
    private static final Map<String, Optional<String>> gitRootCache = new ConcurrentHashMap<>();

    /**
     * Find the git root by walking up the directory tree.
     * Looks for a .git directory or file (worktrees/submodules use a file).
     *
     * @param startPath Starting directory path
     * @return Git root directory, or null if not found
     */
    public static String findGitRoot(String startPath) {
        if (startPath == null || startPath.isEmpty()) {
            return null;
        }

        // Check cache
        if (gitRootCache.containsKey(startPath)) {
            return gitRootCache.get(startPath).orElse(null);
        }

        Path current = Paths.get(startPath).toAbsolutePath().normalize();
        Path root = current.getRoot();

        while (current != null && !current.equals(root)) {
            Path gitPath = current.resolve(".git");
            if (Files.exists(gitPath)) {
                String result = current.toString();
                gitRootCache.put(startPath, Optional.of(result));
                return result;
            }
            current = current.getParent();
        }

        // Check root directory
        if (root != null) {
            Path gitPath = root.resolve(".git");
            if (Files.exists(gitPath)) {
                String result = root.toString();
                gitRootCache.put(startPath, Optional.of(result));
                return result;
            }
        }

        gitRootCache.put(startPath, Optional.empty());
        return null;
    }

    /**
     * Find the canonical git repository root, resolving through worktrees.
     */
    public static String findCanonicalGitRoot(String startPath) {
        String gitRoot = findGitRoot(startPath);
        if (gitRoot == null) {
            return null;
        }

        Path gitPath = Paths.get(gitRoot, ".git");
        if (!Files.isRegularFile(gitPath)) {
            return gitRoot; // Regular repo
        }

        try {
            // In a worktree, .git is a file containing: gitdir: <path>
            String content = Files.readString(gitPath).trim();
            if (!content.startsWith("gitdir:")) {
                return gitRoot;
            }

            Path worktreeGitDir = Paths.get(gitRoot, content.substring("gitdir:".length()).trim());
            Path commonDirPath = worktreeGitDir.resolve("commondir");

            if (!Files.exists(commonDirPath)) {
                return gitRoot; // Submodule
            }

            String commonDir = Paths.get(worktreeGitDir.toString(), Files.readString(commonDirPath).trim()).toString();

            // Validate worktree structure for security
            Path worktreesDir = Paths.get(commonDir, "worktrees");
            if (!worktreeGitDir.getParent().equals(worktreesDir)) {
                return gitRoot;
            }

            // Check backlink
            Path gitdirPath = worktreeGitDir.resolve("gitdir");
            if (Files.exists(gitdirPath)) {
                String backlink = Files.readString(gitdirPath).trim();
                Path expectedBacklink = Paths.get(gitRoot, ".git").toRealPath();
                if (!Paths.get(backlink).equals(expectedBacklink)) {
                    return gitRoot;
                }
            }

            // Return the main repo directory
            if (Paths.get(commonDir).getFileName().toString().equals(".git")) {
                return Paths.get(commonDir).getParent().toString();
            }
            return commonDir;
        } catch (IOException e) {
            return gitRoot;
        }
    }

    /**
     * Check if a path is inside a git repository.
     */
    public static boolean isGitRepo(String path) {
        return findGitRoot(path) != null;
    }

    /**
     * Get the current git branch name.
     */
    public static String getCurrentBranch(String repoPath) {
        if (repoPath == null) return null;

        Path headPath = Paths.get(repoPath, ".git", "HEAD");
        if (!Files.exists(headPath)) {
            // Check for worktree
            Path gitFile = Paths.get(repoPath, ".git");
            if (Files.isRegularFile(gitFile)) {
                try {
                    String content = Files.readString(gitFile).trim();
                    if (content.startsWith("gitdir:")) {
                        headPath = Paths.get(content.substring("gitdir:".length()).trim(), "HEAD");
                    }
                } catch (IOException e) {
                    return null;
                }
            }
        }

        try {
            String headContent = Files.readString(headPath).trim();
            if (headContent.startsWith("ref: refs/heads/")) {
                return headContent.substring("ref: refs/heads/".length());
            }
            // Detached HEAD state
            return headContent.substring(0, Math.min(7, headContent.length()));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get the remote URL for a repository.
     */
    public static String getRemoteUrl(String repoPath, String remote) {
        if (repoPath == null) return null;

        try {
            Path configPath = Paths.get(repoPath, ".git", "config");
            if (!Files.exists(configPath)) {
                // Check for worktree
                Path gitFile = Paths.get(repoPath, ".git");
                if (Files.isRegularFile(gitFile)) {
                    String content = Files.readString(gitFile).trim();
                    if (content.startsWith("gitdir:")) {
                        configPath = Paths.get(content.substring("gitdir:".length()).trim(), "config");
                    }
                }
            }

            if (!Files.exists(configPath)) {
                return null;
            }

            String configContent = Files.readString(configPath);
            String urlPrefix = "[remote \"" + remote + "\"]";
            int urlIndex = configContent.indexOf(urlPrefix);
            if (urlIndex < 0) return null;

            int urlStart = configContent.indexOf("url = ", urlIndex);
            if (urlStart < 0) return null;

            urlStart += "url = ".length();
            int urlEnd = configContent.indexOf("\n", urlStart);
            if (urlEnd < 0) urlEnd = configContent.length();

            return configContent.substring(urlStart, urlEnd).trim();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get a hash of the remote URL for analytics.
     */
    public static String getRepoRemoteHash(String repoPath) {
        String url = getRemoteUrl(repoPath, "origin");
        if (url == null) {
            url = getRemoteUrl(repoPath, "upstream");
        }
        if (url == null) return null;

        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 16); // First 16 chars
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clear the git root cache.
     */
    public static void clearCache() {
        gitRootCache.clear();
    }

    /**
     * Get the default branch name for a repository.
     * Checks for refs/heads/main or refs/heads/master.
     */
    public static String getDefaultBranch(String repoPath) {
        if (repoPath == null) return null;

        Path gitDir = Paths.get(repoPath, ".git");
        if (!Files.exists(gitDir)) {
            return null;
        }

        // Check for worktree
        if (Files.isRegularFile(gitDir)) {
            try {
                String content = Files.readString(gitDir).trim();
                if (content.startsWith("gitdir:")) {
                    gitDir = Paths.get(content.substring("gitdir:".length()).trim());
                }
            } catch (IOException e) {
                return null;
            }
        }

        // Check for main branch
        Path mainRef = gitDir.resolve("refs").resolve("heads").resolve("main");
        if (Files.exists(mainRef)) {
            return "main";
        }

        // Check for master branch
        Path masterRef = gitDir.resolve("refs").resolve("heads").resolve("master");
        if (Files.exists(masterRef)) {
            return "master";
        }

        // Default to main
        return "main";
    }

    /**
     * Get remote URL with default remote.
     */
    public static String getRemoteUrl(String repoPath) {
        return getRemoteUrl(repoPath, "origin");
    }

    /**
     * Check if path is in git repo (alias for isGitRepo).
     */
    public static boolean isInGitRepo(String path) {
        return isGitRepo(path);
    }
}