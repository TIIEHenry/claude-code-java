/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/settings/constants.ts
 */
package com.anthropic.claudecode.utils.settings;

/**
 * Setting source enum.
 *
 * Defines the hierarchy of where settings can come from.
 * Lower ordinal = higher priority.
 */
public enum SettingSource {
    FLAG("flag", "Flag"),
    LOCAL("local", "Local"),
    PROJECT("project", "Project"),
    USER("user", "User"),
    POLICY("policy", "Policy");

    private final String id;
    private final String displayName;

    SettingSource(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse a setting sources flag string.
     * Format: "user,project" or "user" etc.
     */
    public static SettingSource[] parseSettingSourcesFlag(String flag) {
        if (flag == null || flag.isEmpty()) {
            return SETTING_SOURCES.clone();
        }

        String[] parts = flag.split(",");
        SettingSource[] result = new SettingSource[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim().toLowerCase();
            switch (part) {
                case "flag" -> result[i] = FLAG;
                case "local" -> result[i] = LOCAL;
                case "project" -> result[i] = PROJECT;
                case "user" -> result[i] = USER;
                case "policy" -> result[i] = POLICY;
                default -> throw new IllegalArgumentException("Unknown setting source: " + part);
            }
        }
        return result;
    }

    /**
     * Get the source display name for a value.
     */
    public static String getSourceDisplayName(SettingSource source, String value) {
        if (value == null || value.isEmpty()) {
            return source.getDisplayName();
        }
        return source.getDisplayName() + " (" + value + ")";
    }

    /**
     * Default setting sources in priority order.
     */
    public static final SettingSource[] SETTING_SOURCES = values();
}