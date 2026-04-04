/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code managed environment utilities
 */
package com.anthropic.claudecode.utils.env;

import java.util.*;
import java.util.concurrent.*;

/**
 * Managed environment utilities for secure environment variable handling.
 *
 * This module handles the filtering and application of environment variables
 * from various settings sources, with special handling for SSH tunneling
 * and provider-managed environments.
 */
public final class ManagedEnv {
    private ManagedEnv() {}

    // Safe environment variables that can be applied from project settings
    private static final Set<String> SAFE_ENV_VARS = Set.of(
            "CLAUDE_CODE_DISABLE_TELEMETRY",
            "CLAUDE_CODE_ENABLE_TELEMETRY",
            "CLAUDE_CODE_TELEMETRY_URL",
            "ANTHROPIC_LOG_LEVEL",
            "DEBUG",
            "LOG_LEVEL"
    );

    // Provider-managed environment variables that should be stripped when host owns routing
    private static final Set<String> PROVIDER_MANAGED_ENV_VARS = Set.of(
            "ANTHROPIC_BASE_URL",
            "ANTHROPIC_MODEL",
            "CLAUDE_CODE_USE_BEDROCK",
            "CLAUDE_CODE_USE_VERTEX"
    );

    // Trusted setting sources whose env vars can be applied before trust dialog
    private static final Set<String> TRUSTED_SETTING_SOURCES = Set.of(
            "userSettings",
            "flagSettings",
            "policySettings"
    );

    // CCD spawn env keys - captured before any settings.env is applied
    private static volatile Set<String> ccdSpawnEnvKeys = null;

    /**
     * Filter SSH tunnel variables from an env map.
     */
    public static Map<String, String> withoutSSHTunnelVars(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return new HashMap<>();
        }

        if (System.getenv("ANTHROPIC_UNIX_SOCKET") == null) {
            return env;
        }

        Map<String, String> result = new HashMap<>(env);
        result.remove("ANTHROPIC_UNIX_SOCKET");
        result.remove("ANTHROPIC_BASE_URL");
        result.remove("ANTHROPIC_API_KEY");
        result.remove("ANTHROPIC_AUTH_TOKEN");
        result.remove("CLAUDE_CODE_OAUTH_TOKEN");
        return result;
    }

    /**
     * Filter provider-managed variables when host owns routing.
     */
    public static Map<String, String> withoutHostManagedProviderVars(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return new HashMap<>();
        }

        if (!isEnvTruthy(System.getenv("CLAUDE_CODE_PROVIDER_MANAGED_BY_HOST"))) {
            return env;
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (!isProviderManagedEnvVar(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Filter CCD spawn env keys.
     */
    public static Map<String, String> withoutCcdSpawnEnvKeys(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return new HashMap<>();
        }

        Set<String> spawnKeys = ccdSpawnEnvKeys;
        if (spawnKeys == null) {
            return env;
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (!spawnKeys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Compose all strip filters for settings-sourced env.
     */
    public static Map<String, String> filterSettingsEnv(Map<String, String> env) {
        return withoutCcdSpawnEnvKeys(
                withoutHostManagedProviderVars(withoutSSHTunnelVars(env))
        );
    }

    /**
     * Apply safe config environment variables from trusted sources.
     */
    public static void applySafeConfigEnvironmentVariables() {
        // Capture CCD spawn-env keys before any settings.env is applied
        if (ccdSpawnEnvKeys == null) {
            String entrypoint = System.getenv("CLAUDE_CODE_ENTRYPOINT");
            if ("claude-desktop".equals(entrypoint)) {
                ccdSpawnEnvKeys = new HashSet<>(System.getenv().keySet());
            } else {
                ccdSpawnEnvKeys = Collections.emptySet();
            }
        }

        // Apply global config env with filtering
        Map<String, String> globalEnv = getGlobalConfigEnv();
        applyEnvWithFilter(globalEnv);

        // Apply env vars from trusted setting sources (excluding policySettings first)
        for (String source : TRUSTED_SETTING_SOURCES) {
            if ("policySettings".equals(source)) continue;
            if (!isSettingSourceEnabled(source)) continue;

            Map<String, String> sourceEnv = getSettingsForSource(source);
            if (sourceEnv != null) {
                applyEnvWithFilter(sourceEnv);
            }
        }

        // Apply policy settings env
        Map<String, String> policyEnv = getSettingsForSource("policySettings");
        if (policyEnv != null) {
            applyEnvWithFilter(policyEnv);
        }

        // Apply only safe env vars from fully-merged settings
        Map<String, String> mergedEnv = getMergedSettingsEnv();
        if (mergedEnv != null) {
            Map<String, String> filtered = filterSettingsEnv(mergedEnv);
            for (Map.Entry<String, String> entry : filtered.entrySet()) {
                if (SAFE_ENV_VARS.contains(entry.getKey().toUpperCase())) {
                    setEnv(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Apply all environment variables from settings after trust is established.
     */
    public static void applyConfigEnvironmentVariables() {
        Map<String, String> globalEnv = getGlobalConfigEnv();
        if (globalEnv != null) {
            applyEnvWithFilter(globalEnv);
        }

        Map<String, String> mergedEnv = getMergedSettingsEnv();
        if (mergedEnv != null) {
            applyEnvWithFilter(mergedEnv);
        }
    }

    /**
     * Apply env vars with filter to System properties (as Java env proxy).
     */
    private static void applyEnvWithFilter(Map<String, String> env) {
        if (env == null || env.isEmpty()) return;

        Map<String, String> filtered = filterSettingsEnv(env);
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            setEnv(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Set environment variable (using System properties as proxy in Java).
     */
    private static void setEnv(String key, String value) {
        System.setProperty(key, value);
    }

    /**
     * Check if an env var is truthy.
     */
    public static boolean isEnvTruthy(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "yes".equalsIgnoreCase(value);
    }

    /**
     * Check if an env var is provider-managed.
     */
    public static boolean isProviderManagedEnvVar(String key) {
        return PROVIDER_MANAGED_ENV_VARS.contains(key.toUpperCase());
    }

    /**
     * Check if a setting source is enabled.
     */
    public static boolean isSettingSourceEnabled(String source) {
        // In Java implementation, default to true
        return true;
    }

    /**
     * Get global config env.
     */
    private static Map<String, String> getGlobalConfigEnv() {
        Map<String, String> env = new HashMap<>();
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path configPath = java.nio.file.Paths.get(home, ".claude", "config.json");

            if (java.nio.file.Files.exists(configPath)) {
                String content = java.nio.file.Files.readString(configPath);
                // Parse environment section
                int envIdx = content.indexOf("\"env\"");
                if (envIdx >= 0) {
                    int objStart = content.indexOf("{", envIdx);
                    int objEnd = content.indexOf("}", objStart);
                    if (objStart >= 0 && objEnd > objStart) {
                        String obj = content.substring(objStart + 1, objEnd);
                        // Parse key-value pairs
                        int i = 0;
                        while (i < obj.length()) {
                            int keyStart = obj.indexOf("\"", i);
                            if (keyStart < 0) break;
                            int keyEnd = obj.indexOf("\"", keyStart + 1);
                            if (keyEnd < 0) break;

                            String key = obj.substring(keyStart + 1, keyEnd);

                            int valStart = obj.indexOf("\"", keyEnd + 1);
                            if (valStart < 0) break;
                            int valEnd = obj.indexOf("\"", valStart + 1);
                            if (valEnd < 0) break;

                            String value = obj.substring(valStart + 1, valEnd);
                            env.put(key, value);

                            i = valEnd + 1;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Return empty map on error
        }
        return env;
    }

    /**
     * Get settings for source.
     */
    private static Map<String, String> getSettingsForSource(String source) {
        Map<String, String> settings = new HashMap<>();
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(home, ".claude", "settings.json");

            if (java.nio.file.Files.exists(settingsPath)) {
                String content = java.nio.file.Files.readString(settingsPath);
                // Parse environment variables from settings
                int envIdx = content.indexOf("\"env\"");
                if (envIdx >= 0) {
                    int objStart = content.indexOf("{", envIdx);
                    int objEnd = content.indexOf("}", objStart);
                    if (objStart >= 0 && objEnd > objStart) {
                        String obj = content.substring(objStart + 1, objEnd);
                        // Parse key-value pairs
                        int i = 0;
                        while (i < obj.length()) {
                            int keyStart = obj.indexOf("\"", i);
                            if (keyStart < 0) break;
                            int keyEnd = obj.indexOf("\"", keyStart + 1);
                            if (keyEnd < 0) break;

                            String key = obj.substring(keyStart + 1, keyEnd);

                            int valStart = obj.indexOf("\"", keyEnd + 1);
                            if (valStart < 0) break;
                            int valEnd = obj.indexOf("\"", valStart + 1);
                            if (valEnd < 0) break;

                            String value = obj.substring(valStart + 1, valEnd);
                            settings.put(key, value);

                            i = valEnd + 1;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Return empty map on error
        }
        return settings;
    }

    /**
     * Get merged settings env.
     */
    private static Map<String, String> getMergedSettingsEnv() {
        Map<String, String> merged = new HashMap<>();

        // Start with global config
        merged.putAll(getGlobalConfigEnv());

        // Override with source-specific settings
        for (String source : TRUSTED_SETTING_SOURCES) {
            Map<String, String> sourceSettings = getSettingsForSource(source);
            merged.putAll(sourceSettings);
        }

        return merged;
    }

    /**
     * Get safe env vars set.
     */
    public static Set<String> getSafeEnvVars() {
        return SAFE_ENV_VARS;
    }

    /**
     * Get provider managed env vars set.
     */
    public static Set<String> getProviderManagedEnvVars() {
        return PROVIDER_MANAGED_ENV_VARS;
    }

    /**
     * Get trusted setting sources.
     */
    public static Set<String> getTrustedSettingSources() {
        return TRUSTED_SETTING_SOURCES;
    }
}