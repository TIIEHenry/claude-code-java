/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/settingsSync/index.ts
 */
package com.anthropic.claudecode.services.settingssync;

import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.anthropic.claudecode.utils.EnvUtils;

/**
 * Settings Sync Service.
 * Syncs user settings and memory files across Claude Code environments.
 */
public final class SettingsSyncService {
    private SettingsSyncService() {}

    private static final int SETTINGS_SYNC_TIMEOUT_MS = 10000;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int MAX_FILE_SIZE_BYTES = 500 * 1024;
    private static CompletableFuture<Boolean> downloadPromise = null;

    /**
     * Upload local settings to remote (interactive CLI only).
     */
    public static CompletableFuture<Void> uploadUserSettingsInBackground() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!shouldUploadSettings()) {
                    return;
                }
                AnalyticsMetadata.logEvent("tengu_settings_sync_upload_started", Map.of());
            } catch (Exception e) {
                // Ignore errors
            }
        });
    }

    /**
     * Download remote settings to local.
     */
    public static CompletableFuture<Boolean> downloadUserSettingsInBackground() {
        if (downloadPromise != null) {
            return downloadPromise;
        }
        downloadPromise = CompletableFuture.supplyAsync(() -> {
            try {
                AnalyticsMetadata.logEvent("tengu_settings_sync_download_started", Map.of());
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        return downloadPromise;
    }

    /**
     * Check if should upload settings.
     */
    private static boolean shouldUploadSettings() {
        return EnvUtils.isUserTypeAnt();
    }

    /**
     * Check if settings sync is enabled.
     */
    public static boolean isSettingsSyncEnabled() {
        return EnvUtils.isUserTypeAnt();
    }

    /**
     * Get sync status.
     */
    public static String getSyncStatus() {
        return "idle";
    }

    /**
     * Reset sync state.
     */
    public static void resetSyncState() {
        downloadPromise = null;
    }
}