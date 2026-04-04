/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code settings constants
 */
package com.anthropic.claudecode.utils.settings;

import java.util.*;

/**
 * Settings constants for Claude Code configuration.
 */
public final class SettingsConstants {
    private SettingsConstants() {}

    /**
     * All possible sources where settings can come from.
     * Order matters - later sources override earlier ones.
     */
    public static final List<String> SETTING_SOURCES = List.of(
            "userSettings",     // User settings (global)
            "projectSettings",  // Project settings (shared per-directory)
            "localSettings",    // Local settings (gitignored)
            "flagSettings",     // Flag settings (from --settings flag)
            "policySettings"    // Policy settings (managed-settings.json or remote)
    );

    /**
     * Setting source enum.
     */
    public enum SettingSource {
        USER_SETTINGS("userSettings"),
        PROJECT_SETTINGS("projectSettings"),
        LOCAL_SETTINGS("localSettings"),
        FLAG_SETTINGS("flagSettings"),
        POLICY_SETTINGS("policySettings");

        private final String id;

        SettingSource(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static SettingSource fromId(String id) {
            for (SettingSource source : values()) {
                if (source.id.equals(id)) {
                    return source;
                }
            }
            return null;
        }
    }

    /**
     * Get setting source name.
     */
    public static String getSettingSourceName(SettingSource source) {
        return switch (source) {
            case USER_SETTINGS -> "user";
            case PROJECT_SETTINGS -> "project";
            case LOCAL_SETTINGS -> "project, gitignored";
            case FLAG_SETTINGS -> "cli flag";
            case POLICY_SETTINGS -> "managed";
        };
    }

    /**
     * Get short display name for a setting source.
     */
    public static String getSourceDisplayName(SettingSource source) {
        return switch (source) {
            case USER_SETTINGS -> "User";
            case PROJECT_SETTINGS -> "Project";
            case LOCAL_SETTINGS -> "Local";
            case FLAG_SETTINGS -> "Flag";
            case POLICY_SETTINGS -> "Managed";
        };
    }

    /**
     * Get display name for a setting source (lowercase).
     */
    public static String getSettingSourceDisplayNameLowercase(SettingSource source) {
        return switch (source) {
            case USER_SETTINGS -> "user settings";
            case PROJECT_SETTINGS -> "shared project settings";
            case LOCAL_SETTINGS -> "project local settings";
            case FLAG_SETTINGS -> "command line arguments";
            case POLICY_SETTINGS -> "enterprise managed settings";
        };
    }

    /**
     * Get display name for a setting source (capitalized).
     */
    public static String getSettingSourceDisplayNameCapitalized(SettingSource source) {
        return switch (source) {
            case USER_SETTINGS -> "User settings";
            case PROJECT_SETTINGS -> "Shared project settings";
            case LOCAL_SETTINGS -> "Project local settings";
            case FLAG_SETTINGS -> "Command line arguments";
            case POLICY_SETTINGS -> "Enterprise managed settings";
        };
    }

    /**
     * Parse setting sources flag.
     */
    public static List<SettingSource> parseSettingSourcesFlag(String flag) {
        if (flag == null || flag.isEmpty()) {
            return List.of();
        }

        List<SettingSource> result = new ArrayList<>();
        String[] names = flag.split(",");

        for (String name : names) {
            String trimmed = name.trim();
            switch (trimmed) {
                case "user" -> result.add(SettingSource.USER_SETTINGS);
                case "project" -> result.add(SettingSource.PROJECT_SETTINGS);
                case "local" -> result.add(SettingSource.LOCAL_SETTINGS);
                default -> throw new IllegalArgumentException(
                        "Invalid setting source: " + trimmed + ". Valid options are: user, project, local");
            }
        }

        return result;
    }

    /**
     * Get enabled setting sources with policy/flag always included.
     */
    public static List<SettingSource> getEnabledSettingSources() {
        Set<SettingSource> result = new LinkedHashSet<>();

        // Add allowed sources (would come from bootstrap state in real impl)
        result.add(SettingSource.USER_SETTINGS);
        result.add(SettingSource.PROJECT_SETTINGS);
        result.add(SettingSource.LOCAL_SETTINGS);

        // Always include policy and flag settings
        result.add(SettingSource.POLICY_SETTINGS);
        result.add(SettingSource.FLAG_SETTINGS);

        return new ArrayList<>(result);
    }

    /**
     * Check if a specific source is enabled.
     */
    public static boolean isSettingSourceEnabled(SettingSource source) {
        return getEnabledSettingSources().contains(source);
    }

    /**
     * Editable setting sources (excludes policySettings and flagSettings).
     */
    public static final List<SettingSource> EDITABLE_SOURCES = List.of(
            SettingSource.LOCAL_SETTINGS,
            SettingSource.PROJECT_SETTINGS,
            SettingSource.USER_SETTINGS
    );

    /**
     * Sources where permission rules can be saved, in display order.
     */
    public static final List<SettingSource> SOURCES = List.of(
            SettingSource.LOCAL_SETTINGS,
            SettingSource.PROJECT_SETTINGS,
            SettingSource.USER_SETTINGS
    );

    /**
     * The JSON Schema URL for Claude Code settings.
     */
    public static final String CLAUDE_CODE_SETTINGS_SCHEMA_URL =
            "https://json.schemastore.org/claude-code-settings.json";
}