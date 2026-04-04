/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code CA certificates config
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Config/settings-backed CA certificate configuration.
 *
 * This module populates CA cert settings from config files.
 * Split from CaCerts to avoid circular dependencies.
 */
public final class CaCertsConfig {
    private CaCertsConfig() {}

    /**
     * Apply extra CA certs from config to environment/settings.
     * Called early in init before any TLS connections are made.
     */
    public static void applyExtraCACertsFromConfig() {
        if (System.getenv("NODE_EXTRA_CA_CERTS") != null) {
            return; // Already set in environment
        }

        String configPath = getExtraCertsPathFromConfig();
        if (configPath != null) {
            // In Java, we configure CaCerts directly rather than setting env var
            CaCerts.configure(CaCerts.isUseSystemCA(), configPath);
        }
    }

    /**
     * Read extra CA certs path from config/settings.
     * Only reads from user-controlled files, not project-level settings.
     */
    private static String getExtraCertsPathFromConfig() {
        try {
            // Read from global config (~/.claude.json)
            Map<String, Object> globalConfig = ConfigManager.loadGlobalConfig();
            Map<String, String> globalEnv = getEnvFromConfig(globalConfig);

            // Read from user settings (~/.claude/settings.json)
            Map<String, Object> userSettings = ConfigManager.loadUserSettings();
            Map<String, String> settingsEnv = getEnvFromConfig(userSettings);

            // Settings override global config
            String path = null;
            if (settingsEnv != null && settingsEnv.containsKey("NODE_EXTRA_CA_CERTS")) {
                path = settingsEnv.get("NODE_EXTRA_CA_CERTS");
            } else if (globalEnv != null && globalEnv.containsKey("NODE_EXTRA_CA_CERTS")) {
                path = globalEnv.get("NODE_EXTRA_CA_CERTS");
            }

            return path;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract env map from config.
     */
    private static Map<String, String> getEnvFromConfig(Map<String, Object> config) {
        if (config == null) return null;
        Object envObj = config.get("env");
        if (envObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> envMap = (Map<String, Object>) envObj;
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, Object> e : envMap.entrySet()) {
                if (e.getValue() instanceof String) {
                    result.put(e.getKey(), (String) e.getValue());
                }
            }
            return result;
        }
        return null;
    }

    /**
     * Check if system CA is configured.
     */
    public static boolean isSystemCAConfigured() {
        try {
            Map<String, Object> userSettings = ConfigManager.loadUserSettings();
            Object useSystemCA = userSettings.get("useSystemCA");
            if (useSystemCA instanceof Boolean) {
                return (Boolean) useSystemCA;
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
}