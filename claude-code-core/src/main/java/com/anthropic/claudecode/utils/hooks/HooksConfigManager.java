/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/hooksConfigManager.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.nio.file.*;
import java.util.*;
import org.json.*;

/**
 * Hooks configuration manager.
 */
public final class HooksConfigManager {
    private HooksConfigManager() {}

    private static volatile Map<String, List<HookTypes.HookCommand>> cachedConfig = null;

    /**
     * Load hooks configuration from file.
     */
    public static Map<String, List<HookTypes.HookCommand>> loadHooksConfig(String configPath) {
        if (configPath == null) {
            return new HashMap<>();
        }

        try {
            Path path = Paths.get(configPath);
            if (!Files.exists(path)) {
                return new HashMap<>();
            }

            String content = Files.readString(path);
            return parseHooksConfig(content);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Parse hooks configuration from JSON string.
     */
    public static Map<String, List<HookTypes.HookCommand>> parseHooksConfig(String json) {
        Map<String, List<HookTypes.HookCommand>> config = new HashMap<>();

        try {
            JSONObject obj = new JSONObject(json);
            JSONObject hooks = obj.optJSONObject("hooks");
            if (hooks == null) {
                return config;
            }

            for (String eventName : hooks.keySet()) {
                JSONArray hookArray = hooks.optJSONArray(eventName);
                if (hookArray == null) continue;

                List<HookTypes.HookCommand> commands = new ArrayList<>();
                for (int i = 0; i < hookArray.length(); i++) {
                    JSONObject hookObj = hookArray.optJSONObject(i);
                    if (hookObj == null) continue;

                    String type = hookObj.optString("type", "command");
                    String command = hookObj.optString("command", null);
                    String path = hookObj.optString("path", null);
                    String url = hookObj.optString("url", null);
                    Integer timeout = hookObj.has("timeout") ? hookObj.optInt("timeout") : null;

                    Map<String, String> env = new HashMap<>();
                    JSONObject envObj = hookObj.optJSONObject("env");
                    if (envObj != null) {
                        for (String key : envObj.keySet()) {
                            env.put(key, envObj.optString(key));
                        }
                    }

                    commands.add(new HookTypes.HookCommand(type, command, path, url, env, timeout));
                }
                config.put(eventName, commands);
            }
        } catch (Exception e) {
            // Return empty config on parse error
        }

        return config;
    }

    /**
     * Get cached hooks configuration.
     */
    public static Map<String, List<HookTypes.HookCommand>> getCachedConfig() {
        return cachedConfig != null ? new HashMap<>(cachedConfig) : new HashMap<>();
    }

    /**
     * Set cached hooks configuration.
     */
    public static void setCachedConfig(Map<String, List<HookTypes.HookCommand>> config) {
        cachedConfig = config != null ? new HashMap<>(config) : null;
    }

    /**
     * Clear cached configuration.
     */
    public static void clearCache() {
        cachedConfig = null;
    }

    /**
     * Get hooks for a specific event.
     */
    public static List<HookTypes.HookCommand> getHooksForEvent(
            Map<String, List<HookTypes.HookCommand>> config,
            String eventName) {
        return config.getOrDefault(eventName, new ArrayList<>());
    }

    /**
     * Check if hooks are enabled for an event.
     */
    public static boolean hasHooksForEvent(
            Map<String, List<HookTypes.HookCommand>> config,
            String eventName) {
        List<HookTypes.HookCommand> hooks = config.get(eventName);
        return hooks != null && !hooks.isEmpty();
    }
}