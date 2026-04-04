/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/settingsSync/types.ts
 */
package com.anthropic.claudecode.services.settingssync;

import java.util.Map;

/**
 * Types for settings sync service.
 */
public final class SettingsSyncTypes {
    private SettingsSyncTypes() {}

    /**
     * User sync content - flat key-value storage.
     */
    public record UserSyncContent(
        Map<String, String> entries
    ) {}

    /**
     * Full response from GET /api/claude_code/user_settings
     */
    public record UserSyncData(
        String userId,
        long version,
        String lastModified,
        String checksum,
        UserSyncContent content
    ) {}

    /**
     * Result from fetching user settings.
     */
    public record SettingsSyncFetchResult(
        boolean success,
        UserSyncData data,
        boolean isEmpty, // true if 404 (no data exists)
        String error,
        boolean skipRetry
    ) {
        public static SettingsSyncFetchResult success(UserSyncData data) {
            return new SettingsSyncFetchResult(true, data, false, null, false);
        }

        public static SettingsSyncFetchResult empty() {
            return new SettingsSyncFetchResult(true, null, true, null, false);
        }

        public static SettingsSyncFetchResult error(String error, boolean skipRetry) {
            return new SettingsSyncFetchResult(false, null, false, error, skipRetry);
        }
    }

    /**
     * Result from uploading user settings.
     */
    public record SettingsSyncUploadResult(
        boolean success,
        String checksum,
        String lastModified,
        String error
    ) {
        public static SettingsSyncUploadResult success(String checksum, String lastModified) {
            return new SettingsSyncUploadResult(true, checksum, lastModified, null);
        }

        public static SettingsSyncUploadResult error(String error) {
            return new SettingsSyncUploadResult(false, null, null, error);
        }
    }

    /**
     * Keys used for sync entries.
     */
    public static final class SyncKeys {
        public static final String USER_SETTINGS = "~/.claude/settings.json";
        public static final String USER_MEMORY = "~/.claude/CLAUDE.md";

        public static String projectSettings(String projectId) {
            return "projects/" + projectId + "/.claude/settings.local.json";
        }

        public static String projectMemory(String projectId) {
            return "projects/" + projectId + "/CLAUDE.local.md";
        }
    }
}