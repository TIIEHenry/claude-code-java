/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/config.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration management utilities.
 */
public final class ConfigManager {
    private ConfigManager() {}

    // Config file names
    public static final String CLAUDE_DIR = ".claude";
    public static final String CONFIG_JSON = "config.json";
    public static final String SETTINGS_JSON = "settings.json";
    public static final String CLAUDE_MD = "CLAUDE.md";

    // Cached configs
    private static final Map<String, Map<String, Object>> configCache = new ConcurrentHashMap<>();

    /**
     * Project config record.
     */
    public record ProjectConfig(
            List<String> allowedTools,
            Map<String, Object> mcpServers,
            boolean hasTrustDialogAccepted,
            String lastSessionId
    ) {
        public static ProjectConfig defaults() {
            return new ProjectConfig(
                new ArrayList<>(),
                new HashMap<>(),
                false,
                null
            );
        }
    }

    /**
     * Global config record.
     */
    public record GlobalConfig(
            String apiKey,
            String organizationId,
            String defaultModel,
            Map<String, Object> settings
    ) {
        public static GlobalConfig defaults() {
            return new GlobalConfig(null, null, null, new HashMap<>());
        }
    }

    /**
     * Get the Claude config home directory.
     */
    public static Path getClaudeConfigHome() {
        String home = System.getProperty("user.home");
        return Paths.get(home, CLAUDE_DIR);
    }

    /**
     * Get the global config file path.
     */
    public static Path getGlobalConfigPath() {
        return getClaudeConfigHome().resolve(CONFIG_JSON);
    }

    /**
     * Get the project config directory.
     */
    public static Path getProjectConfigDir(String projectRoot) {
        if (projectRoot == null) {
            projectRoot = System.getProperty("user.dir");
        }
        return Paths.get(projectRoot, CLAUDE_DIR);
    }

    /**
     * Get the project config file path.
     */
    public static Path getProjectConfigPath(String projectRoot) {
        return getProjectConfigDir(projectRoot).resolve(CONFIG_JSON);
    }

    /**
     * Get CLAUDE.md path.
     */
    public static Path getClaudeMdPath(String projectRoot) {
        return Paths.get(projectRoot, CLAUDE_MD);
    }

    /**
     * Load config from a JSON file.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadConfig(Path path) {
        if (path == null || !Files.exists(path)) {
            return new HashMap<>();
        }

        try {
            String content = Files.readString(path);
            // Simple JSON parsing - in production would use Jackson/Gson
            return parseSimpleJson(content);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    /**
     * Save config to a JSON file.
     */
    public static void saveConfig(Path path, Map<String, Object> config) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        // Ensure parent directory exists
        Files.createDirectories(path.getParent());

        // Simple JSON serialization
        String json = toJson(config);
        Files.writeString(path, json);
    }

    /**
     * Get global config.
     */
    public static GlobalConfig getGlobalConfig() {
        Path path = getGlobalConfigPath();
        Map<String, Object> config = loadConfig(path);

        return new GlobalConfig(
            (String) config.get("apiKey"),
            (String) config.get("organizationId"),
            (String) config.getOrDefault("defaultModel", "claude-sonnet-4-6"),
            config
        );
    }

    /**
     * Load global config as a map.
     */
    public static Map<String, Object> loadGlobalConfig() {
        return loadConfig(getGlobalConfigPath());
    }

    /**
     * Load user settings (~/.claude/settings.json).
     */
    public static Map<String, Object> loadUserSettings() {
        Path settingsPath = getClaudeConfigHome().resolve(SETTINGS_JSON);
        return loadConfig(settingsPath);
    }

    /**
     * Save global config from a map.
     */
    public static void saveGlobalConfig(Map<String, Object> config) {
        try {
            saveConfig(getGlobalConfigPath(), config);
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Save a single global config setting.
     */
    public static void saveGlobalConfig(String key, Object value) {
        Map<String, Object> config = loadGlobalConfig();
        config.put(key, value);
        saveGlobalConfig(config);
    }

    /**
     * Save a single user setting.
     */
    public static void saveUserSetting(String key, Object value) {
        Map<String, Object> settings = loadUserSettings();
        settings.put(key, value);
        try {
            saveConfig(getClaudeConfigHome().resolve(SETTINGS_JSON), settings);
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Get project config.
     */
    public static ProjectConfig getProjectConfig(String projectRoot) {
        Path path = getProjectConfigPath(projectRoot);
        Map<String, Object> config = loadConfig(path);

        @SuppressWarnings("unchecked")
        List<String> allowedTools = (List<String>) config.getOrDefault("allowedTools", new ArrayList<>());

        @SuppressWarnings("unchecked")
        Map<String, Object> mcpServers = (Map<String, Object>) config.getOrDefault("mcpServers", new HashMap<>());

        return new ProjectConfig(
            allowedTools,
            mcpServers,
            Boolean.TRUE.equals(config.get("hasTrustDialogAccepted")),
            (String) config.get("lastSessionId")
        );
    }

    /**
     * Clear config cache.
     */
    public static void clearCache() {
        configCache.clear();
    }

    /**
     * Simple JSON parser (basic implementation).
     */
    private static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> result = new HashMap<>();

        // Very basic parsing - production would use proper JSON library
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }

        // Remove braces
        json = json.substring(1, json.length() - 1).trim();

        // Parse key-value pairs
        int depth = 0;
        StringBuilder current = new StringBuilder();
        String currentKey = null;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;

            if (depth == 0 && c == ':') {
                currentKey = current.toString().trim().replace("\"", "");
                current = new StringBuilder();
            } else if (depth == 0 && c == ',') {
                if (currentKey != null) {
                    result.put(currentKey, parseValue(current.toString().trim()));
                }
                currentKey = null;
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Last key-value
        if (currentKey != null) {
            result.put(currentKey, parseValue(current.toString().trim()));
        }

        return result;
    }

    /**
     * Parse a JSON value.
     */
    private static Object parseValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        value = value.trim();

        // String
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        // Boolean
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;

        // Null
        if ("null".equals(value)) return null;

        // Number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * Convert map to JSON string.
     */
    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;

            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(valueToJson(entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert value to JSON.
     */
    private static String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number) return value.toString();
        if (value instanceof String) return "\"" + value + "\"";
        if (value instanceof Map) return toJson((Map<String, Object>) value);
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) value;
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append(valueToJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + value + "\"";
    }
}