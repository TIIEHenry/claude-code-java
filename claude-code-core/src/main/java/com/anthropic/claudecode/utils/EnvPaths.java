/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code environment utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.file.*;

/**
 * Claude Code environment path utilities.
 */
public final class EnvPaths {
    private EnvPaths() {}

    private static final String CLAUDE_CONFIG_HOME = "CLAUDE_CONFIG_HOME";
    private static final String CLAUDE_PROJECT_DIR = "CLAUDE_PROJECT_DIR";

    /**
     * Get Claude config home directory.
     */
    public static Path getClaudeConfigHome() {
        String configHome = System.getenv(CLAUDE_CONFIG_HOME);
        if (configHome != null && !configHome.isEmpty()) {
            return Paths.get(configHome);
        }

        String home = System.getProperty("user.home");
        return Paths.get(home, ".claude");
    }

    /**
     * Get Claude projects directory.
     */
    public static Path getClaudeProjectsDir() {
        return getClaudeConfigHome().resolve("projects");
    }

    /**
     * Get Claude memory directory.
     */
    public static Path getClaudeMemoryDir() {
        return getClaudeConfigHome().resolve("memory");
    }

    /**
     * Get Claude session memory directory.
     */
    public static Path getSessionMemoryDir() {
        return getClaudeConfigHome().resolve("session-memory");
    }

    /**
     * Get Claude agent memory directory.
     */
    public static Path getAgentMemoryDir() {
        return getClaudeConfigHome().resolve("agent-memory");
    }

    /**
     * Get Claude local installation directory.
     */
    public static Path getLocalInstallDir() {
        return getClaudeConfigHome().resolve("local");
    }

    /**
     * Get Claude commands directory.
     */
    public static Path getClaudeCommandsDir() {
        return getClaudeConfigHome().resolve("commands");
    }

    /**
     * Get Claude hooks directory.
     */
    public static Path getClaudeHooksDir() {
        return getClaudeConfigHome().resolve("hooks");
    }

    /**
     * Get Claude MCP directory.
     */
    public static Path getClaudeMcpDir() {
        return getClaudeConfigHome().resolve("mcp");
    }

    /**
     * Get Claude logs directory.
     */
    public static Path getClaudeLogsDir() {
        return getClaudeConfigHome().resolve("logs");
    }

    /**
     * Get Claude settings file path.
     */
    public static Path getSettingsFile() {
        return getClaudeConfigHome().resolve("settings.json");
    }

    /**
     * Get Claude legacy settings file path.
     */
    public static Path getLegacySettingsFile() {
        return getClaudeConfigHome().resolve("settings.local.json");
    }

    /**
     * Get project-specific settings file.
     */
    public static Path getProjectSettingsFile(Path projectRoot) {
        return projectRoot.resolve(".claude").resolve("settings.local.json");
    }

    /**
     * Get project CLAUDE.md file.
     */
    public static Path getProjectClaudeMd(Path projectRoot) {
        return projectRoot.resolve("CLAUDE.md");
    }

    /**
     * Get project memory directory.
     */
    public static Path getProjectMemoryDir(Path projectRoot) {
        return projectRoot.resolve(".claude").resolve("memory");
    }

    /**
     * Get project hooks directory.
     */
    public static Path getProjectHooksDir(Path projectRoot) {
        return projectRoot.resolve(".claude").resolve("hooks");
    }

    /**
     * Get project commands directory.
     */
    public static Path getProjectCommandsDir(Path projectRoot) {
        return projectRoot.resolve(".claude").resolve("commands");
    }

    /**
     * Get scheduled tasks file.
     */
    public static Path getScheduledTasksFile() {
        return getClaudeConfigHome().resolve("scheduled_tasks.json");
    }

    /**
     * Get MCP servers file.
     */
    public static Path getMcpServersFile() {
        return getClaudeConfigHome().resolve("mcp_servers.json");
    }

    /**
     * Get current project directory from environment.
     */
    public static Optional<Path> getCurrentProjectDir() {
        String projectDir = System.getenv(CLAUDE_PROJECT_DIR);
        if (projectDir != null && !projectDir.isEmpty()) {
            return Optional.of(Paths.get(projectDir));
        }
        return Optional.empty();
    }

    /**
     * Get project sessions directory.
     */
    public static Path getProjectSessionsDir(String projectName) {
        return getClaudeProjectsDir().resolve(projectName);
    }

    /**
     * Get all Claude related directories.
     */
    public static List<Path> getAllClaudeDirs() {
        return List.of(
            getClaudeConfigHome(),
            getClaudeProjectsDir(),
            getClaudeMemoryDir(),
            getSessionMemoryDir(),
            getAgentMemoryDir(),
            getLocalInstallDir(),
            getClaudeCommandsDir(),
            getClaudeHooksDir(),
            getClaudeMcpDir(),
            getClaudeLogsDir()
        );
    }

    /**
     * Ensure all Claude directories exist.
     */
    public static void ensureClaudeDirsExist() {
        for (Path dir : getAllClaudeDirs()) {
            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Get XDG config home (following XDG Base Directory specification).
     */
    public static Path getXdgConfigHome() {
        String xdgConfig = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfig != null && !xdgConfig.isEmpty()) {
            return Paths.get(xdgConfig);
        }
        return Paths.get(System.getProperty("user.home"), ".config");
    }

    /**
     * Get XDG data home.
     */
    public static Path getXdgDataHome() {
        String xdgData = System.getenv("XDG_DATA_HOME");
        if (xdgData != null && !xdgData.isEmpty()) {
            return Paths.get(xdgData);
        }
        return Paths.get(System.getProperty("user.home"), ".local", "share");
    }

    /**
     * Get XDG cache home.
     */
    public static Path getXdgCacheHome() {
        String xdgCache = System.getenv("XDG_CACHE_HOME");
        if (xdgCache != null && !xdgCache.isEmpty()) {
            return Paths.get(xdgCache);
        }
        return Paths.get(System.getProperty("user.home"), ".cache");
    }

    /**
     * Normalize project name for storage.
     */
    public static String normalizeProjectName(Path projectPath) {
        String absolutePath = projectPath.toAbsolutePath().toString();
        // Replace path separators with dashes and remove leading slashes
        return absolutePath
            .replace("/", "-")
            .replace("\\", "-")
            .replaceAll("^-", "")
            .replaceAll("-+$", "");
    }
}