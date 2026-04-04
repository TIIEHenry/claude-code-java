/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/config/settings
 */
package com.anthropic.claudecode.services.config;

import java.util.*;
import java.nio.file.*;

/**
 * Settings - User settings management.
 */
public final class Settings {
    private static volatile Settings instance = null;
    private final ConfigService configService;
    private final Path settingsPath;

    /**
     * Settings key constants.
     */
    public static final class Keys {
        public static final String THEME = "theme";
        public static final String EDITOR = "editor";
        public static final String SHOW_LINE_NUMBERS = "showLineNumbers";
        public static final String TAB_WIDTH = "tabWidth";
        public static final String AUTO_SAVE = "autoSave";
        public static final String MAX_HISTORY = "maxHistory";
        public static final String TIMEOUT_MS = "timeoutMs";
        public static final String API_KEY = "apiKey";
        public static final String DEFAULT_MODEL = "defaultModel";
        public static final String PERMISSION_MODE = "permissionMode";
        public static final String FAST_MODE = "fastMode";
        public static final String BROWSER = "browser";
        public static final String SHELL = "shell";
        public static final String LOG_LEVEL = "logLevel";
        public static final String MCP_ENABLED = "mcpEnabled";
        public static final String PLUGINS_ENABLED = "pluginsEnabled";
        public static final String UPDATE_CHANNEL = "updateChannel";
        public static final String LANGUAGE = "language";
        public static final String CODE_STYLE = "codeStyle";
        public static final String AUTO_FORMAT = "autoFormat";
        public static final String VIM_MODE = "vimMode";
        public static final String SHOW_HIDDEN_FILES = "showHiddenFiles";
    }

    /**
     * Get settings instance.
     */
    public static Settings getInstance() {
        if (instance == null) {
            Path settingsPath = Paths.get(
                System.getProperty("user.home"),
                ".claude",
                "settings"
            );
            instance = new Settings(settingsPath);
        }
        return instance;
    }

    /**
     * Create settings.
     */
    private Settings(Path path) {
        this.settingsPath = path;
        this.configService = new ConfigService(path.resolve("settings.conf"));
    }

    /**
     * Get theme.
     */
    public String getTheme() {
        return configService.getString(Keys.THEME, "default");
    }

    /**
     * Set theme.
     */
    public void setTheme(String theme) {
        configService.set(Keys.THEME, theme);
    }

    /**
     * Get editor.
     */
    public String getEditor() {
        return configService.getString(Keys.EDITOR, "vim");
    }

    /**
     * Set editor.
     */
    public void setEditor(String editor) {
        configService.set(Keys.EDITOR, editor);
    }

    /**
     * Check vim mode.
     */
    public boolean isVimMode() {
        return configService.getBoolean(Keys.VIM_MODE, false);
    }

    /**
     * Set vim mode.
     */
    public void setVimMode(boolean enabled) {
        configService.set(Keys.VIM_MODE, enabled);
    }

    /**
     * Get tab width.
     */
    public int getTabWidth() {
        return configService.getInt(Keys.TAB_WIDTH, 4);
    }

    /**
     * Set tab width.
     */
    public void setTabWidth(int width) {
        configService.set(Keys.TAB_WIDTH, width);
    }

    /**
     * Check auto save.
     */
    public boolean isAutoSave() {
        return configService.getBoolean(Keys.AUTO_SAVE, true);
    }

    /**
     * Set auto save.
     */
    public void setAutoSave(boolean enabled) {
        configService.set(Keys.AUTO_SAVE, enabled);
    }

    /**
     * Get default model.
     */
    public String getDefaultModel() {
        return configService.getString(Keys.DEFAULT_MODEL, "claude-opus-4-6");
    }

    /**
     * Set default model.
     */
    public void setDefaultModel(String model) {
        configService.set(Keys.DEFAULT_MODEL, model);
    }

    /**
     * Get permission mode.
     */
    public String getPermissionMode() {
        return configService.getString(Keys.PERMISSION_MODE, "default");
    }

    /**
     * Set permission mode.
     */
    public void setPermissionMode(String mode) {
        configService.set(Keys.PERMISSION_MODE, mode);
    }

    /**
     * Check fast mode.
     */
    public boolean isFastMode() {
        return configService.getBoolean(Keys.FAST_MODE, false);
    }

    /**
     * Set fast mode.
     */
    public void setFastMode(boolean enabled) {
        configService.set(Keys.FAST_MODE, enabled);
    }

    /**
     * Get timeout.
     */
    public int getTimeout() {
        return configService.getInt(Keys.TIMEOUT_MS, 120000);
    }

    /**
     * Set timeout.
     */
    public void setTimeout(int timeoutMs) {
        configService.set(Keys.TIMEOUT_MS, timeoutMs);
    }

    /**
     * Get max history.
     */
    public int getMaxHistory() {
        return configService.getInt(Keys.MAX_HISTORY, 100);
    }

    /**
     * Set max history.
     */
    public void setMaxHistory(int max) {
        configService.set(Keys.MAX_HISTORY, max);
    }

    /**
     * Check MCP enabled.
     */
    public boolean isMcpEnabled() {
        return configService.getBoolean(Keys.MCP_ENABLED, true);
    }

    /**
     * Set MCP enabled.
     */
    public void setMcpEnabled(boolean enabled) {
        configService.set(Keys.MCP_ENABLED, enabled);
    }

    /**
     * Get all settings.
     */
    public Map<String, Object> getAllSettings() {
        Map<String, Object> all = new HashMap<>();
        for (String key : configService.getKeys()) {
            all.put(key, configService.get(key).orElse(null));
        }
        return all;
    }

    /**
     * Reset to defaults.
     */
    public void resetDefaults() {
        configService.resetDefaults();
    }

    /**
     * Settings profile record.
     */
    public record SettingsProfile(
        String name,
        String description,
        Map<String, Object> settings
    ) {
        public static SettingsProfile defaultProfile() {
            Map<String, Object> settings = new HashMap<>();
            settings.put(Keys.THEME, "default");
            settings.put(Keys.EDITOR, "vim");
            settings.put(Keys.VIM_MODE, false);
            settings.put(Keys.TAB_WIDTH, 4);
            settings.put(Keys.AUTO_SAVE, true);
            settings.put(Keys.DEFAULT_MODEL, "claude-opus-4-6");
            return new SettingsProfile("default", "Default settings", settings);
        }

        public static SettingsProfile minimalProfile() {
            Map<String, Object> settings = new HashMap<>();
            settings.put(Keys.PERMISSION_MODE, "accept_edits");
            settings.put(Keys.FAST_MODE, true);
            return new SettingsProfile("minimal", "Minimal settings for fast operation", settings);
        }
    }

    /**
     * Apply profile.
     */
    public void applyProfile(SettingsProfile profile) {
        for (Map.Entry<String, Object> entry : profile.settings().entrySet()) {
            configService.set(entry.getKey(), entry.getValue());
        }
    }
}