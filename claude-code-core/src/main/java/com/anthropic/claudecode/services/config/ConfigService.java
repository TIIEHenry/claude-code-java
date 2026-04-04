/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/config
 */
package com.anthropic.claudecode.services.config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.nio.file.*;
import java.io.*;

/**
 * Config service - Configuration management.
 */
public final class ConfigService {
    private final Path configPath;
    private final Map<String, Object> config = new ConcurrentHashMap<>();
    private final List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Create config service.
     */
    public ConfigService(Path configPath) {
        this.configPath = configPath;
        loadConfig();
    }

    /**
     * Load configuration.
     */
    private void loadConfig() {
        if (!Files.exists(configPath)) {
            return;
        }

        try {
            String content = Files.readString(configPath);
            parseConfig(content);
        } catch (Exception e) {
            // Use defaults
        }
    }

    /**
     * Parse config content.
     */
    private void parseConfig(String content) {
        // Simple parsing - key=value format
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int eq = line.indexOf('=');
            if (eq > 0) {
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                config.put(key, parseValue(value));
            }
        }
    }

    /**
     * Parse value.
     */
    private Object parseValue(String value) {
        // Boolean
        if (value.equals("true") || value.equals("false")) {
            return Boolean.parseBoolean(value);
        }

        // Number
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}

        // String
        return value;
    }

    /**
     * Save configuration.
     */
    public void saveConfig() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# Claude Code Configuration\n\n");

            for (Map.Entry<String, Object> entry : config.entrySet()) {
                sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }

            Files.writeString(configPath, sb.toString());
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Get config value.
     */
    public Optional<Object> get(String key) {
        return Optional.ofNullable(config.get(key));
    }

    /**
     * Get config value as string.
     */
    public String getString(String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Get config value as int.
     */
    public int getInt(String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return defaultValue;
    }

    /**
     * Get config value as boolean.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        return defaultValue;
    }

    /**
     * Set config value.
     */
    public void set(String key, Object value) {
        Object oldValue = config.get(key);
        config.put(key, value);

        notifyListeners(key, oldValue, value);
        saveConfig();
    }

    /**
     * Remove config value.
     */
    public void remove(String key) {
        Object oldValue = config.remove(key);
        if (oldValue != null) {
            notifyListeners(key, oldValue, null);
            saveConfig();
        }
    }

    /**
     * Check if key exists.
     */
    public boolean has(String key) {
        return config.containsKey(key);
    }

    /**
     * Get all keys.
     */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(config.keySet());
    }

    /**
     * Add listener.
     */
    public void addListener(ConfigChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(ConfigChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String key, Object oldValue, Object newValue) {
        for (ConfigChangeListener listener : listeners) {
            listener.onConfigChange(key, oldValue, newValue);
        }
    }

    /**
     * Reset to defaults.
     */
    public void resetDefaults() {
        config.clear();
        loadDefaults();
        saveConfig();
    }

    /**
     * Load defaults.
     */
    private void loadDefaults() {
        config.put("theme", "default");
        config.put("editor", "vim");
        config.put("showLineNumbers", true);
        config.put("tabWidth", 4);
        config.put("autoSave", true);
        config.put("maxHistory", 100);
        config.put("timeout", 120000);
    }

    /**
     * Config change listener interface.
     */
    public interface ConfigChangeListener {
        void onConfigChange(String key, Object oldValue, Object newValue);
    }

    /**
     * Config section record.
     */
    public record ConfigSection(
        String name,
        Map<String, Object> values
    ) {
        public Optional<Object> get(String key) {
            return Optional.ofNullable(values.get(key));
        }
    }

    /**
     * Get config as sections.
     */
    public Map<String, ConfigSection> getSections() {
        Map<String, ConfigSection> sections = new HashMap<>();
        Map<String, Map<String, Object>> sectionValues = new HashMap<>();

        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            String sectionName = "general";

            if (key.contains(".")) {
                sectionName = key.substring(0, key.indexOf('.'));
                key = key.substring(key.indexOf('.') + 1);
            }

            sectionValues.computeIfAbsent(sectionName, k -> new HashMap<>())
                .put(key, entry.getValue());
        }

        for (Map.Entry<String, Map<String, Object>> entry : sectionValues.entrySet()) {
            sections.put(entry.getKey(), new ConfigSection(entry.getKey(), entry.getValue()));
        }

        return sections;
    }
}