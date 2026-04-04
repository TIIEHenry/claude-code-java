/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.settingssync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SettingsSyncTypes.
 */
class SettingsSyncTypesTest {

    @Test
    @DisplayName("SettingsSyncTypes UserSyncContent record")
    void userSyncContentRecord() {
        Map<String, String> entries = Map.of("key1", "value1", "key2", "value2");
        SettingsSyncTypes.UserSyncContent content = new SettingsSyncTypes.UserSyncContent(entries);

        assertEquals(entries, content.entries());
    }

    @Test
    @DisplayName("SettingsSyncTypes UserSyncData record")
    void userSyncDataRecord() {
        Map<String, String> entries = Map.of("setting", "value");
        SettingsSyncTypes.UserSyncContent content = new SettingsSyncTypes.UserSyncContent(entries);
        SettingsSyncTypes.UserSyncData data = new SettingsSyncTypes.UserSyncData(
            "user-123",
            5L,
            "2024-01-01T00:00:00Z",
            "checksum-abc",
            content
        );

        assertEquals("user-123", data.userId());
        assertEquals(5L, data.version());
        assertEquals("2024-01-01T00:00:00Z", data.lastModified());
        assertEquals("checksum-abc", data.checksum());
        assertEquals(content, data.content());
    }

    @Test
    @DisplayName("SettingsSyncTypes SettingsSyncFetchResult success factory")
    void settingsSyncFetchResultSuccess() {
        Map<String, String> entries = Map.of();
        SettingsSyncTypes.UserSyncContent content = new SettingsSyncTypes.UserSyncContent(entries);
        SettingsSyncTypes.UserSyncData data = new SettingsSyncTypes.UserSyncData(
            "user-1", 1L, "2024-01-01", "checksum", content
        );

        SettingsSyncTypes.SettingsSyncFetchResult result = SettingsSyncTypes.SettingsSyncFetchResult.success(data);

        assertTrue(result.success());
        assertEquals(data, result.data());
        assertFalse(result.isEmpty());
        assertNull(result.error());
        assertFalse(result.skipRetry());
    }

    @Test
    @DisplayName("SettingsSyncTypes SettingsSyncFetchResult empty factory")
    void settingsSyncFetchResultEmpty() {
        SettingsSyncTypes.SettingsSyncFetchResult result = SettingsSyncTypes.SettingsSyncFetchResult.empty();

        assertTrue(result.success());
        assertNull(result.data());
        assertTrue(result.isEmpty());
        assertNull(result.error());
    }

    @Test
    @DisplayName("SettingsSyncTypes SettingsSyncFetchResult error factory")
    void settingsSyncFetchResultError() {
        SettingsSyncTypes.SettingsSyncFetchResult result = SettingsSyncTypes.SettingsSyncFetchResult.error("Network error", true);

        assertFalse(result.success());
        assertNull(result.data());
        assertFalse(result.isEmpty());
        assertEquals("Network error", result.error());
        assertTrue(result.skipRetry());
    }

    @Test
    @DisplayName("SettingsSyncTypes SettingsSyncUploadResult success factory")
    void settingsSyncUploadResultSuccess() {
        SettingsSyncTypes.SettingsSyncUploadResult result = SettingsSyncTypes.SettingsSyncUploadResult.success("checksum-123", "2024-01-01");

        assertTrue(result.success());
        assertEquals("checksum-123", result.checksum());
        assertEquals("2024-01-01", result.lastModified());
        assertNull(result.error());
    }

    @Test
    @DisplayName("SettingsSyncTypes SettingsSyncUploadResult error factory")
    void settingsSyncUploadResultError() {
        SettingsSyncTypes.SettingsSyncUploadResult result = SettingsSyncTypes.SettingsSyncUploadResult.error("Upload failed");

        assertFalse(result.success());
        assertNull(result.checksum());
        assertNull(result.lastModified());
        assertEquals("Upload failed", result.error());
    }

    @Test
    @DisplayName("SettingsSyncTypes SyncKeys constants")
    void syncKeysConstants() {
        assertEquals("~/.claude/settings.json", SettingsSyncTypes.SyncKeys.USER_SETTINGS);
        assertEquals("~/.claude/CLAUDE.md", SettingsSyncTypes.SyncKeys.USER_MEMORY);
    }

    @Test
    @DisplayName("SettingsSyncTypes SyncKeys projectSettings")
    void syncKeysProjectSettings() {
        String key = SettingsSyncTypes.SyncKeys.projectSettings("proj-123");

        assertEquals("projects/proj-123/.claude/settings.local.json", key);
    }

    @Test
    @DisplayName("SettingsSyncTypes SyncKeys projectMemory")
    void syncKeysProjectMemory() {
        String key = SettingsSyncTypes.SyncKeys.projectMemory("proj-456");

        assertEquals("projects/proj-456/CLAUDE.local.md", key);
    }
}