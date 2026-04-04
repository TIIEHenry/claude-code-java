/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.teammemorysync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TeamMemorySyncTypes.
 */
class TeamMemorySyncTypesTest {

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemoryContent record")
    void teamMemoryContentRecord() {
        Map<String, String> entries = Map.of("file1.md", "content1");
        Map<String, String> checksums = Map.of("file1.md", "checksum1");

        TeamMemorySyncTypes.TeamMemoryContent content = new TeamMemorySyncTypes.TeamMemoryContent(entries, checksums);

        assertEquals(entries, content.entries());
        assertEquals(checksums, content.entryChecksums());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemoryData record")
    void teamMemoryDataRecord() {
        Map<String, String> entries = Map.of();
        Map<String, String> checksums = Map.of();
        TeamMemorySyncTypes.TeamMemoryContent content = new TeamMemorySyncTypes.TeamMemoryContent(entries, checksums);

        TeamMemorySyncTypes.TeamMemoryData data = new TeamMemorySyncTypes.TeamMemoryData(
            "org-123",
            "repo-name",
            10L,
            "2024-01-01",
            "checksum-xyz",
            content
        );

        assertEquals("org-123", data.organizationId());
        assertEquals("repo-name", data.repo());
        assertEquals(10L, data.version());
        assertEquals("2024-01-01", data.lastModified());
        assertEquals("checksum-xyz", data.checksum());
        assertEquals(content, data.content());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes SkippedSecretFile record")
    void skippedSecretFileRecord() {
        TeamMemorySyncTypes.SkippedSecretFile skipped = new TeamMemorySyncTypes.SkippedSecretFile(
            "config/secrets.env",
            "AWS_SECRET_KEY",
            "AWS Secret Access Key"
        );

        assertEquals("config/secrets.env", skipped.path());
        assertEquals("AWS_SECRET_KEY", skipped.ruleId());
        assertEquals("AWS Secret Access Key", skipped.label());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemorySyncFetchResult success factory")
    void fetchResultSuccess() {
        TeamMemorySyncTypes.TeamMemoryContent content = new TeamMemorySyncTypes.TeamMemoryContent(Map.of(), Map.of());
        TeamMemorySyncTypes.TeamMemoryData data = new TeamMemorySyncTypes.TeamMemoryData(
            "org-1", "repo", 1L, "2024-01-01", "checksum", content
        );

        TeamMemorySyncTypes.TeamMemorySyncFetchResult result = TeamMemorySyncTypes.TeamMemorySyncFetchResult.success(data, "checksum");

        assertTrue(result.success());
        assertEquals(data, result.data());
        assertFalse(result.isEmpty());
        assertFalse(result.notModified());
        assertEquals("checksum", result.checksum());
        assertNull(result.error());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemorySyncFetchResult empty factory")
    void fetchResultEmpty() {
        TeamMemorySyncTypes.TeamMemorySyncFetchResult result = TeamMemorySyncTypes.TeamMemorySyncFetchResult.empty();

        assertTrue(result.success());
        assertNull(result.data());
        assertTrue(result.isEmpty());
        assertFalse(result.notModified());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemorySyncFetchResult notModified factory")
    void fetchResultNotModified() {
        TeamMemorySyncTypes.TeamMemorySyncFetchResult result = TeamMemorySyncTypes.TeamMemorySyncFetchResult.notModified("checksum-123");

        assertTrue(result.success());
        assertNull(result.data());
        assertFalse(result.isEmpty());
        assertTrue(result.notModified());
        assertEquals("checksum-123", result.checksum());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemorySyncFetchResult error factory")
    void fetchResultError() {
        TeamMemorySyncTypes.TeamMemorySyncFetchResult result = TeamMemorySyncTypes.TeamMemorySyncFetchResult.error(
            "Network timeout", "TIMEOUT", true
        );

        assertFalse(result.success());
        assertNull(result.data());
        assertEquals("Network timeout", result.error());
        assertEquals("TIMEOUT", result.errorType());
        assertTrue(result.skipRetry());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemoryHashesResult record")
    void hashesResultRecord() {
        Map<String, String> checksums = Map.of("file1", "checksum1");
        TeamMemorySyncTypes.TeamMemoryHashesResult result = new TeamMemorySyncTypes.TeamMemoryHashesResult(
            true, 5L, "main-checksum", checksums, null, null, null
        );

        assertTrue(result.success());
        assertEquals(5L, result.version());
        assertEquals("main-checksum", result.checksum());
        assertEquals(checksums, result.entryChecksums());
        assertNull(result.error());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemorySyncPushResult record")
    void pushResultRecord() {
        TeamMemorySyncTypes.SkippedSecretFile skipped = new TeamMemorySyncTypes.SkippedSecretFile(
            "secrets.env", "API_KEY", "API Key"
        );

        TeamMemorySyncTypes.TeamMemorySyncPushResult result = new TeamMemorySyncTypes.TeamMemorySyncPushResult(
            true, 5, "checksum-123", false, null, List.of(skipped), null, null
        );

        assertTrue(result.success());
        assertEquals(5, result.filesUploaded());
        assertEquals("checksum-123", result.checksum());
        assertFalse(result.conflict());
        assertNull(result.error());
        assertEquals(1, result.skippedSecrets().size());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes TeamMemorySyncUploadResult record")
    void uploadResultRecord() {
        TeamMemorySyncTypes.TeamMemorySyncUploadResult result = new TeamMemorySyncTypes.TeamMemorySyncUploadResult(
            true, "checksum-abc", "2024-01-01", false, null, null, null, null, null, null
        );

        assertTrue(result.success());
        assertEquals("checksum-abc", result.checksum());
        assertEquals("2024-01-01", result.lastModified());
        assertFalse(result.conflict());
        assertNull(result.error());
    }

    @Test
    @DisplayName("TeamMemorySyncTypes SyncState getters and setters")
    void syncState() {
        TeamMemorySyncTypes.SyncState state = new TeamMemorySyncTypes.SyncState();

        assertNull(state.getLastKnownChecksum());
        assertNull(state.getServerMaxEntries());
        assertTrue(state.getServerChecksums().isEmpty());

        state.setLastKnownChecksum("checksum-123");
        state.setServerMaxEntries(100);
        state.getServerChecksums().put("file1", "checksum-file1");

        assertEquals("checksum-123", state.getLastKnownChecksum());
        assertEquals(100, state.getServerMaxEntries());
        assertEquals("checksum-file1", state.getServerChecksums().get("file1"));
    }
}