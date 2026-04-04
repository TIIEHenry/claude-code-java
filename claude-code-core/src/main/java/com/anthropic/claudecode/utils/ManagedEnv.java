/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code managed environment utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Managed environment utilities for settings and env var handling.
 */
public final class ManagedEnv {
    private ManagedEnv() {}

    /**
     * Safe environment variables that can be applied from project settings.
     */
    public static final Set<String> SAFE_ENV_VARS = Set.of(
            "ANTHROPIC_MODEL",
            "ANTHROPIC_SMALL_FAST_MODEL",
            "CLAUDE_CODE_MAX_THINKING_TOKENS",
            "CLAUDE_CODE_DISABLE_THINKING",
            "CLAUDE_CODE_DISABLE_MEMDIR",
            "CLAUDE_CODE_DISABLE_MCP",
            "CLAUDE_CODE_DISABLE_TELEMETRY",
            "CLAUDE_CODE_DANGEROUSLY_SKIP_PERMISSIONS",
            "CLAUDE_CODE_SKIP_MCP_TRUST_DIALOG",
            "CLAUDE_CODE_DISABLE_AUTO_UPDATER",
            "CLAUDE_CODE_RESUME_ENABLED"
    );

    /**
     * Provider-managed environment variable prefixes.
     */
    public static final Set<String> PROVIDER_MANAGED_PREFIXES = Set.of(
            "ANTHROPIC_BASE_URL",
            "ANTHROPIC_API_KEY",
            "ANTHROPIC_AUTH_TOKEN",
            "CLAUDE_CODE_USE_BEDROCK",
            "CLAUDE_CODE_USE_VERTEX",
            "CLAUDE_CODE_USE_GOOGLE"
    );

    /**
     * Apply safe configuration environment variables.
     */
    public static void applySafeConfigEnvironmentVariables() {
        // Load settings and apply safe env vars
        try {
            Map<String, String> settingsEnv = loadSettingsEnv();
            Map<String, String> safeEnv = filterToSafeEnvVars(settingsEnv);

            // In Java, we cannot directly set environment variables for the current process
            // Instead, we set them as system properties for internal use
            for (Map.Entry<String, String> entry : safeEnv.entrySet()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Apply all configuration environment variables after trust is established.
     */
    public static void applyConfigEnvironmentVariables() {
        // Load settings and apply all env vars
        try {
            Map<String, String> settingsEnv = loadSettingsEnv();
            Map<String, String> filteredEnv = filterSettingsEnv(settingsEnv);

            // In Java, we cannot directly set environment variables for the current process
            // Instead, we set them as system properties for internal use
            for (Map.Entry<String, String> entry : filteredEnv.entrySet()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Load environment variables from settings.
     */
    private static Map<String, String> loadSettingsEnv() {
        Map<String, String> result = new HashMap<>();

        // Try to load from settings file
        try {
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(
                System.getProperty("user.home"),
                ".claude",
                "settings.json"
            );

            if (java.nio.file.Files.exists(settingsPath)) {
                String content = java.nio.file.Files.readString(settingsPath);
                Map<String, Object> settings = parseSimpleJson(content);

                Object envObj = settings.get("env");
                if (envObj instanceof Map envMap) {
                    for (Object key : envMap.keySet()) {
                        Object value = envMap.get(key);
                        if (key != null && value != null) {
                            result.put(key.toString(), value.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }

        return result;
    }

    /**
     * Simple JSON parser for settings.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> result = new HashMap<>();
        if (json == null || json.isBlank()) return result;

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return result;

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        // Simple parsing - handles string values only
        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && json.charAt(i) != '"') i++;
            if (i >= json.length()) break;
            i++;
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++;
                i++;
            }
            String key = json.substring(keyStart, i);
            i++;

            while (i < json.length() && json.charAt(i) != ':') i++;
            i++;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

            if (i < json.length() && json.charAt(i) == '"') {
                i++;
                int valueStart = i;
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\') i++;
                    i++;
                }
                result.put(key, json.substring(valueStart, i));
                i++;
            } else if (i < json.length() && json.charAt(i) == '{') {
                int depth = 1;
                int start = i;
                i++;
                while (i < json.length() && depth > 0) {
                    if (json.charAt(i) == '{') depth++;
                    else if (json.charAt(i) == '}') depth--;
                    i++;
                }
                result.put(key, parseSimpleJson(json.substring(start, i)));
            }

            while (i < json.length() && json.charAt(i) != ',') i++;
            if (i < json.length()) i++;
        }

        return result;
    }

    /**
     * Check if an env var is provider-managed.
     */
    public static boolean isProviderManagedEnvVar(String key) {
        if (key == null) return false;
        String upperKey = key.toUpperCase();
        for (String prefix : PROVIDER_MANAGED_PREFIXES) {
            if (upperKey.equals(prefix) || upperKey.startsWith(prefix + "_")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Filter settings env to remove dangerous vars.
     */
    public static Map<String, String> filterSettingsEnv(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>();
        boolean isHostManaged = EnvUtils.isEnvTruthy(System.getenv("CLAUDE_CODE_PROVIDER_MANAGED_BY_HOST"));

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();

            // Skip provider-managed vars when host is managing
            if (isHostManaged && isProviderManagedEnvVar(key)) {
                continue;
            }

            result.put(key, entry.getValue());
        }

        return result;
    }

    /**
     * Filter to only safe env vars.
     */
    public static Map<String, String> filterToSafeEnvVars(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String upperKey = entry.getKey().toUpperCase();
            if (SAFE_ENV_VARS.contains(upperKey)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }
}