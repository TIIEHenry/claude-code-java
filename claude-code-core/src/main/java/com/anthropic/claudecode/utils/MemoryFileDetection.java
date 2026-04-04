/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code memory file detection
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

/**
 * Memory file detection utilities.
 */
public final class MemoryFileDetection {
    private MemoryFileDetection() {}

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    /**
     * Session file type.
     */
    public enum SessionFileType {
        SESSION_MEMORY,
        SESSION_TRANSCRIPT
    }

    /**
     * Memory scope.
     */
    public enum MemoryScope {
        PERSONAL,
        TEAM
    }

    /**
     * Detect session file type from path.
     */
    public static SessionFileType detectSessionFileType(String filePath) {
        String configDir = EnvUtils.getClaudeConfigHomeDir();
        String normalized = toComparable(filePath);
        String configDirCmp = toComparable(configDir);

        if (!normalized.startsWith(configDirCmp)) {
            return null;
        }

        if (normalized.contains("/session-memory/") && normalized.endsWith(".md")) {
            return SessionFileType.SESSION_MEMORY;
        }

        if (normalized.contains("/projects/") && normalized.endsWith(".jsonl")) {
            return SessionFileType.SESSION_TRANSCRIPT;
        }

        return null;
    }

    /**
     * Detect session pattern type from glob pattern.
     */
    public static SessionFileType detectSessionPatternType(String pattern) {
        String normalized = pattern.replace("\\", "/");

        if (normalized.contains("session-memory") &&
            (normalized.contains(".md") || normalized.endsWith("*"))) {
            return SessionFileType.SESSION_MEMORY;
        }

        if (normalized.contains(".jsonl") ||
            (normalized.contains("projects") && normalized.contains("*.jsonl"))) {
            return SessionFileType.SESSION_TRANSCRIPT;
        }

        return null;
    }

    /**
     * Check if path is auto-managed memory file.
     */
    public static boolean isAutoManagedMemoryFile(String filePath) {
        SessionFileType sessionType = detectSessionFileType(filePath);
        if (sessionType != null) {
            return true;
        }

        String normalized = toComparable(filePath);

        // Check for agent memory
        if (normalized.contains("/agent-memory/") || normalized.contains("/agent-memory-local/")) {
            return true;
        }

        return false;
    }

    /**
     * Check if directory is memory directory.
     */
    public static boolean isMemoryDirectory(String dirPath) {
        String normalized = toComparable(dirPath);

        // Check for agent memory directories
        if (normalized.contains("/agent-memory/") || normalized.contains("/agent-memory-local/")) {
            return true;
        }

        String configDir = EnvUtils.getClaudeConfigHomeDir();
        String configDirCmp = toComparable(configDir);

        if (!normalized.startsWith(configDirCmp)) {
            return false;
        }

        if (normalized.contains("/session-memory/")) {
            return true;
        }

        if (normalized.contains("/projects/")) {
            return true;
        }

        if (normalized.contains("/memory/")) {
            return true;
        }

        return false;
    }

    /**
     * Check if shell command targets memory files.
     */
    public static boolean isShellCommandTargetingMemory(String command) {
        String configDir = EnvUtils.getClaudeConfigHomeDir();
        String commandCmp = toComparable(command);

        if (!commandCmp.contains(toComparable(configDir))) {
            return false;
        }

        // Extract absolute path tokens
        Pattern pathPattern = Pattern.compile("(?:[A-Za-z]:[/\\\\]|/)[^\\s'\"]+");
        Matcher matcher = pathPattern.matcher(command);

        while (matcher.find()) {
            String path = matcher.group().replaceAll("[,;|&>]+$", "");

            if (IS_WINDOWS) {
                path = WindowsPaths.posixPathToWindowsPath(path);
            }

            if (isAutoManagedMemoryFile(path) || isMemoryDirectory(path)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get memory scope for path.
     */
    public static MemoryScope memoryScopeForPath(String filePath) {
        String normalized = toComparable(filePath);

        if (normalized.contains("/team/")) {
            return MemoryScope.TEAM;
        }

        if (isAutoManagedMemoryFile(filePath)) {
            return MemoryScope.PERSONAL;
        }

        return null;
    }

    /**
     * Check if pattern is auto-managed memory pattern.
     */
    public static boolean isAutoManagedMemoryPattern(String pattern) {
        if (detectSessionPatternType(pattern) != null) {
            return true;
        }

        String normalized = pattern.replace("\\", "/");
        return normalized.contains("agent-memory/") || normalized.contains("agent-memory-local/");
    }

    /**
     * Convert path to comparable form (forward slashes, lowercase on Windows).
     */
    private static String toComparable(String path) {
        if (path == null) return "";
        String posix = path.replace("\\", "/");
        return IS_WINDOWS ? posix.toLowerCase() : posix;
    }
}