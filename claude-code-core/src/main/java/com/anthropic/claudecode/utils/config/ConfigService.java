/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/config.ts config service
 */
package com.anthropic.claudecode.utils.config;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Configuration service for managing global and project configs.
 */
public final class ConfigService {
    private ConfigService() {}

    private static volatile GlobalConfig globalConfig = null;
    private static volatile ProjectConfig projectConfig = null;
    private static volatile Path globalConfigPath = null;
    private static volatile Path projectConfigPath = null;

    /**
     * Get the global config file path.
     */
    public static Path getGlobalConfigPath() {
        if (globalConfigPath == null) {
            String configDir = System.getenv("CLAUDE_CONFIG_DIR");
            if (configDir != null && !configDir.isEmpty()) {
                globalConfigPath = Paths.get(configDir, ".claude.json");
            } else {
                globalConfigPath = Paths.get(System.getProperty("user.home"), ".claude.json");
            }
        }
        return globalConfigPath;
    }

    /**
     * Get the project config file path.
     */
    public static Path getProjectConfigPath() {
        if (projectConfigPath == null) {
            projectConfigPath = Paths.get(System.getProperty("user.dir"), ".claude", "settings.json");
        }
        return projectConfigPath;
    }

    /**
     * Get global config (cached).
     */
    public static GlobalConfig getGlobalConfig() {
        if (globalConfig == null) {
            globalConfig = loadGlobalConfig();
        }
        return globalConfig;
    }

    /**
     * Get project config (cached).
     */
    public static ProjectConfig getProjectConfig() {
        if (projectConfig == null) {
            projectConfig = loadProjectConfig();
        }
        return projectConfig;
    }

    /**
     * Load global config from disk.
     */
    private static GlobalConfig loadGlobalConfig() {
        Path path = getGlobalConfigPath();
        try {
            if (Files.exists(path)) {
                String content = Files.readString(path);
                return parseGlobalConfig(content);
            }
        } catch (Exception e) {
            // Return defaults on error
        }
        return GlobalConfig.createDefault();
    }

    /**
     * Load project config from disk.
     */
    private static ProjectConfig loadProjectConfig() {
        Path path = getProjectConfigPath();
        try {
            if (Files.exists(path)) {
                String content = Files.readString(path);
                return parseProjectConfig(content);
            }
        } catch (Exception e) {
            // Return defaults on error
        }
        return ProjectConfig.createDefault();
    }

    /**
     * Parse global config from JSON string.
     */
    private static GlobalConfig parseGlobalConfig(String json) {
        // Simple parsing - in production would use Jackson/Gson
        return GlobalConfig.createDefault();
    }

    /**
     * Parse project config from JSON string.
     */
    private static ProjectConfig parseProjectConfig(String json) {
        // Simple parsing - in production would use Jackson/Gson
        return ProjectConfig.createDefault();
    }

    /**
     * Save global config to disk.
     */
    public static void saveGlobalConfig(GlobalConfig config) {
        globalConfig = config;
        // In production, would serialize to JSON and write
    }

    /**
     * Save project config to disk.
     */
    public static void saveProjectConfig(ProjectConfig config) {
        projectConfig = config;
        // In production, would serialize to JSON and write
    }

    /**
     * Clear config caches.
     */
    public static void clearCaches() {
        globalConfig = null;
        projectConfig = null;
        globalConfigPath = null;
        projectConfigPath = null;
    }

    /**
     * Get the Claude config home directory.
     */
    public static String getClaudeConfigHomeDir() {
        String configDir = System.getenv("CLAUDE_CONFIG_DIR");
        if (configDir != null && !configDir.isEmpty()) {
            return configDir;
        }
        return Paths.get(System.getProperty("user.home"), ".claude").toString();
    }

    /**
     * Get the teams directory.
     */
    public static String getTeamsDir() {
        return Paths.get(getClaudeConfigHomeDir(), "teams").toString();
    }

    /**
     * Get the skills directory.
     */
    public static String getSkillsDir() {
        return Paths.get(getClaudeConfigHomeDir(), "skills").toString();
    }

    /**
     * Get the hooks directory.
     */
    public static String getHooksDir() {
        return Paths.get(getClaudeConfigHomeDir(), "hooks").toString();
    }

    /**
     * Get the sessions directory.
     */
    public static String getSessionsDir() {
        return Paths.get(getClaudeConfigHomeDir(), "sessions").toString();
    }

    /**
     * Check if trust dialog has been accepted.
     */
    public static boolean hasTrustDialogAccepted() {
        GlobalConfig global = getGlobalConfig();
        return global.hasTrustDialogAccepted() != null && global.hasTrustDialogAccepted();
    }

    /**
     * Check if onboarding has been completed.
     */
    public static boolean hasCompletedOnboarding() {
        GlobalConfig global = getGlobalConfig();
        return global.hasCompletedOnboarding() != null && global.hasCompletedOnboarding();
    }

    /**
     * Get the configured model.
     */
    public static String getConfiguredModel() {
        GlobalConfig global = getGlobalConfig();
        return global.model();
    }

    /**
     * Get the preferred notification channel.
     */
    public static String getPreferredNotifChannel() {
        GlobalConfig global = getGlobalConfig();
        return global.preferredNotifChannel();
    }
}