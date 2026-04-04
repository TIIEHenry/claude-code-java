/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cross-project resume utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;
import java.io.File;

/**
 * Check if a log is from a different project directory.
 * For same-repo worktrees, we can resume directly without requiring cd.
 * For different projects, we generate the cd command.
 */
public final class CrossProjectResume {
    private CrossProjectResume() {}

    /**
     * Cross-project resume result.
     */
    public sealed interface CrossProjectResumeResult permits
            CrossProjectResumeResult.NotCrossProject,
            CrossProjectResumeResult.SameRepoWorktree,
            CrossProjectResumeResult.DifferentProject {

        boolean isCrossProject();

        public static final class NotCrossProject implements CrossProjectResumeResult {
            @Override public boolean isCrossProject() { return false; }
        }

        public static final class SameRepoWorktree implements CrossProjectResumeResult {
            private final String projectPath;

            public SameRepoWorktree(String projectPath) {
                this.projectPath = projectPath;
            }

            public String projectPath() { return projectPath; }
            @Override public boolean isCrossProject() { return true; }
            public boolean isSameRepoWorktree() { return true; }
        }

        public static final class DifferentProject implements CrossProjectResumeResult {
            private final String command;
            private final String projectPath;

            public DifferentProject(String command, String projectPath) {
                this.command = command;
                this.projectPath = projectPath;
            }

            public String command() { return command; }
            public String projectPath() { return projectPath; }
            @Override public boolean isCrossProject() { return true; }
            public boolean isSameRepoWorktree() { return false; }
        }
    }

    /**
     * Log option record for resume.
     */
    public record LogOption(
            String projectPath,
            String sessionId
    ) {}

    /**
     * Check if a log is from a different project directory.
     *
     * @param log The log option
     * @param showAllProjects Whether to show all projects
     * @param worktreePaths List of worktree paths
     * @param currentCwd Current working directory
     * @return Cross-project resume result
     */
    public static CrossProjectResumeResult checkCrossProjectResume(
            LogOption log,
            boolean showAllProjects,
            List<String> worktreePaths,
            String currentCwd) {

        if (!showAllProjects || log.projectPath() == null || log.projectPath().equals(currentCwd)) {
            return new CrossProjectResumeResult.NotCrossProject();
        }

        // Gate worktree detection to ants only for staged rollout
        String userType = System.getenv("USER_TYPE");
        if (!"ant".equals(userType)) {
            String command = buildResumeCommand(log.projectPath(), log.sessionId());
            return new CrossProjectResumeResult.DifferentProject(command, log.projectPath());
        }

        // Check if log.projectPath is under a worktree of the same repo
        boolean isSameRepo = worktreePaths.stream()
                .anyMatch(wt -> log.projectPath().equals(wt) ||
                        log.projectPath().startsWith(wt + File.separator));

        if (isSameRepo) {
            return new CrossProjectResumeResult.SameRepoWorktree(log.projectPath());
        }

        // Different repo - generate cd command
        String command = buildResumeCommand(log.projectPath(), log.sessionId());
        return new CrossProjectResumeResult.DifferentProject(command, log.projectPath());
    }

    /**
     * Build resume command for cross-project resume.
     */
    private static String buildResumeCommand(String projectPath, String sessionId) {
        return String.format("cd %s && claude --resume %s",
                quote(projectPath), sessionId);
    }

    /**
     * Quote a path for shell.
     */
    private static String quote(String path) {
        if (path == null || path.isEmpty()) {
            return "''";
        }
        // Simple quoting - wrap in single quotes if contains special chars
        if (path.matches(".*[\\s\"'`$\\\\;&|*?<>!()].*")) {
            return "'" + path.replace("'", "'\\''") + "'";
        }
        return path;
    }
}