/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/config.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

/**
 * Configuration utilities.
 */
public class Config {
    private final Map<String, Object> config = new ConcurrentHashMap<>();
    private final Path configPath;

    public Config() {
        this.configPath = Paths.get(System.getProperty("user.home"), ".claude", "config.json");
    }

    public Config(Path configPath) {
        this.configPath = configPath;
    }

    /**
     * Get a string value.
     */
    public Optional<String> getString(String key) {
        Object value = config.get(key);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
    }

    /**
     * Get a string value with default.
     */
    public String getString(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }

    /**
     * Get an integer value.
     */
    public Optional<Integer> getInteger(String key) {
        Object value = config.get(key);
        return value instanceof Number ? Optional.of(((Number) value).intValue()) : Optional.empty();
    }

    /**
     * Get a boolean value.
     */
    public Optional<Boolean> getBoolean(String key) {
        Object value = config.get(key);
        return value instanceof Boolean ? Optional.of((Boolean) value) : Optional.empty();
    }

    /**
     * Set a value.
     */
    public void set(String key, Object value) {
        config.put(key, value);
    }

    /**
     * Remove a key.
     */
    public void remove(String key) {
        config.remove(key);
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
    public Set<String> keys() {
        return new HashSet<>(config.keySet());
    }

    /**
     * Get the config path.
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Clear all config.
     */
    public void clear() {
        config.clear();
    }

    /**
     * Get global config.
     */
    public static Config getGlobalConfig() {
        return new Config();
    }
}