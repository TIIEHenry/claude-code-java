/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code context.ts
 */
package com.anthropic.claudecode.services.context;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * System context service.
 *
 * Provides context prepended to each conversation, cached for the session duration.
 */
public final class SystemContextService {
    private SystemContextService() {}

    // Cache
    private static volatile Map<String, String> cachedSystemContext = null;
    private static volatile Map<String, String> cachedUserContext = null;
    private static volatile String systemPromptInjection = null;

    // Constants
    private static final int MAX_STATUS_CHARS = 2000;

    /**
     * Get system prompt injection (for cache breaking).
     */
    public static String getSystemPromptInjection() {
        return systemPromptInjection;
    }

    /**
     * Set system prompt injection.
     */
    public static void setSystemPromptInjection(String value) {
        systemPromptInjection = value;
        // Clear caches
        cachedSystemContext = null;
        cachedUserContext = null;
    }

    /**
     * Get git status.
     */
    public static CompletableFuture<String> getGitStatus() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isGitRepository()) {
                return null;
            }

            try {
                String branch = getCurrentBranch();
                String mainBranch = getDefaultBranch();
                String status = getGitStatusShort();
                String log = getRecentCommits(5);
                String userName = getGitUserName();

                // Truncate status if needed
                String truncatedStatus = status;
                if (status != null && status.length() > MAX_STATUS_CHARS) {
                    truncatedStatus = status.substring(0, MAX_STATUS_CHARS) +
                        "\n... (truncated because it exceeds 2k characters. If you need more information, run \"git status\" using BashTool)";
                }

                StringBuilder sb = new StringBuilder();
                sb.append("This is the git status at the start of the conversation. Note that this status is a snapshot in time, and will not update during the conversation.\n\n");
                sb.append("Current branch: ").append(branch != null ? branch : "unknown").append("\n\n");
                sb.append("Main branch (you will usually use this for PRs): ").append(mainBranch != null ? mainBranch : "main").append("\n\n");
                if (userName != null && !userName.isEmpty()) {
                    sb.append("Git user: ").append(userName).append("\n\n");
                }
                sb.append("Status:\n").append(truncatedStatus != null && !truncatedStatus.isEmpty() ? truncatedStatus : "(clean)").append("\n\n");
                sb.append("Recent commits:\n").append(log != null ? log : "");

                return sb.toString();
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Get system context.
     */
    public static CompletableFuture<Map<String, String>> getSystemContext() {
        if (cachedSystemContext != null) {
            return CompletableFuture.completedFuture(cachedSystemContext);
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> context = new LinkedHashMap<>();

            // Skip git status in remote mode
            String remote = System.getenv("CLAUDE_CODE_REMOTE");
            if (!"true".equalsIgnoreCase(remote)) {
                String gitStatus = getGitStatus().join();
                if (gitStatus != null) {
                    context.put("gitStatus", gitStatus);
                }
            }

            // Add cache breaker if set
            if (systemPromptInjection != null) {
                context.put("cacheBreaker", "[CACHE_BREAKER: " + systemPromptInjection + "]");
            }

            cachedSystemContext = context;
            return context;
        });
    }

    /**
     * Get user context.
     */
    public static CompletableFuture<Map<String, String>> getUserContext() {
        if (cachedUserContext != null) {
            return CompletableFuture.completedFuture(cachedUserContext);
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> context = new LinkedHashMap<>();

            // Add current date
            String date = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());
            context.put("currentDate", "Today's date is " + date + ".");

            // Check for CLAUDE.md content
            String claudeMd = getClaudeMdContent();
            if (claudeMd != null && !claudeMd.isEmpty()) {
                context.put("claudeMd", claudeMd);
            }

            cachedUserContext = context;
            return context;
        });
    }

    /**
     * Clear context caches.
     */
    public static void clearCaches() {
        cachedSystemContext = null;
        cachedUserContext = null;
    }

    // ─── Git helpers ──────────────────────────────────────────────────────

    private static boolean isGitRepository() {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        return Files.exists(cwd.resolve(".git"));
    }

    private static String getCurrentBranch() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "branch", "--show-current");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.waitFor();
                if (process.exitValue() == 0 && line != null) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private static String getDefaultBranch() {
        // Try to get from remote
        String[] branches = {"main", "master", "develop"};
        for (String branch : branches) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "git", "rev-parse", "--verify", branch);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();
                if (process.exitValue() == 0) {
                    return branch;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return "main";
    }

    private static String getGitStatusShort() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "git", "--no-optional-locks", "status", "--short");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            process.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String getRecentCommits(int count) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "git", "--no-optional-locks", "log", "--oneline", "-n", String.valueOf(count));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            process.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String getGitUserName() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "config", "user.name");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.waitFor();
                if (process.exitValue() == 0 && line != null) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    // ─── CLAUDE.md helpers ─────────────────────────────────────────────────

    private static String getClaudeMdContent() {
        // Check if disabled
        if ("true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_DISABLE_CLAUDE_MDS"))) {
            return null;
        }

        Path cwd = Paths.get(System.getProperty("user.dir"));

        // Check for CLAUDE.md
        Path claudeMd = cwd.resolve("CLAUDE.md");
        if (Files.exists(claudeMd)) {
            try {
                return Files.readString(claudeMd);
            } catch (IOException e) {
                // Ignore
            }
        }

        // Check for .claude/CLAUDE.md
        Path dotClaude = cwd.resolve(".claude/CLAUDE.md");
        if (Files.exists(dotClaude)) {
            try {
                return Files.readString(dotClaude);
            } catch (IOException e) {
                // Ignore
            }
        }

        return null;
    }
}