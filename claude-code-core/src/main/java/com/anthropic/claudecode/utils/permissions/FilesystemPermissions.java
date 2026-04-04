/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/filesystem.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Filesystem permission utilities.
 *
 * Provides constants and utilities for filesystem-related permission checks.
 */
public final class FilesystemPermissions {
    private FilesystemPermissions() {}

    /**
     * Dangerous files that should be protected from auto-editing.
     * These files can be used for code execution or data exfiltration.
     */
    public static final Set<String> DANGEROUS_FILES = Set.of(
        ".gitconfig",
        ".gitmodules",
        ".bashrc",
        ".bash_profile",
        ".zshrc",
        ".zprofile",
        ".profile",
        ".ripgreprc",
        ".mcp.json",
        ".claude.json"
    );

    /**
     * Dangerous directories that should be protected from auto-editing.
     * These directories contain sensitive configuration or executable files.
     */
    public static final Set<String> DANGEROUS_DIRECTORIES = Set.of(
        ".git",
        ".vscode",
        ".idea",
        ".claude"
    );

    /**
     * Normalize a path for case-insensitive comparison.
     * This prevents bypassing security checks using mixed-case paths.
     */
    public static String normalizeCaseForComparison(String path) {
        return path != null ? path.toLowerCase() : null;
    }

    /**
     * Check if a file is considered dangerous.
     */
    public static boolean isDangerousFile(String filename) {
        if (filename == null) return false;
        String name = filename;
        int lastSep = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (lastSep >= 0) {
            name = filename.substring(lastSep + 1);
        }
        return DANGEROUS_FILES.contains(name.toLowerCase());
    }

    /**
     * Check if a directory is considered dangerous.
     */
    public static boolean isDangerousDirectory(String dirname) {
        if (dirname == null) return false;
        String name = dirname;
        int lastSep = Math.max(dirname.lastIndexOf('/'), dirname.lastIndexOf('\\'));
        if (lastSep >= 0) {
            name = dirname.substring(lastSep + 1);
        }
        return DANGEROUS_DIRECTORIES.contains(name.toLowerCase());
    }

    /**
     * Check if a path is inside a dangerous directory.
     */
    public static boolean isInsideDangerousDirectory(String path) {
        if (path == null) return false;
        String normalized = normalizeCaseForComparison(path.replace('\\', '/'));
        for (String dir : DANGEROUS_DIRECTORIES) {
            if (normalized.contains("/" + dir + "/") ||
                normalized.endsWith("/" + dir) ||
                normalized.startsWith(dir + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get Claude temp directory.
     */
    public static String getClaudeTempDir() {
        String tempDir = System.getenv("CLAUDE_TEMP_DIR");
        if (tempDir != null && !tempDir.isEmpty()) {
            return tempDir;
        }
        return System.getProperty("java.io.tmpdir") + "/claude-code";
    }

    /**
     * Get Claude temp directory name.
     */
    public static String getClaudeTempDirName() {
        return "claude-code";
    }

    /**
     * Get session temp directory.
     */
    public static String getSessionTempDir(String sessionId) {
        return getClaudeTempDir() + "/sessions/" + sessionId;
    }

    /**
     * Get tool results directory.
     */
    public static String getToolResultsDir(String sessionId) {
        return getSessionTempDir(sessionId) + "/tool-results";
    }
}