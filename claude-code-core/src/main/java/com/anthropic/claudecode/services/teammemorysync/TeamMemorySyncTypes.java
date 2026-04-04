/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/teamMemorySync/types.ts
 */
package com.anthropic.claudecode.services.teammemorysync;

import java.util.*;

/**
 * Types for team memory sync service.
 */
public final class TeamMemorySyncTypes {
    private TeamMemorySyncTypes() {}

    /**
     * Content portion of team memory data - flat key-value storage.
     */
    public record TeamMemoryContent(
        Map<String, String> entries,
        Map<String, String> entryChecksums
    ) {}

    /**
     * Full response from GET /api/claude_code/team_memory
     */
    public record TeamMemoryData(
        String organizationId,
        String repo,
        long version,
        String lastModified,
        String checksum,
        TeamMemoryContent content
    ) {}

    /**
     * A file skipped during push because it contains a detected secret.
     */
    public record SkippedSecretFile(
        String path,
        String ruleId,
        String label
    ) {}

    /**
     * Result from fetching team memory.
     */
    public record TeamMemorySyncFetchResult(
        boolean success,
        TeamMemoryData data,
        boolean isEmpty,
        boolean notModified,
        String checksum,
        String error,
        boolean skipRetry,
        String errorType,
        Integer httpStatus
    ) {
        public static TeamMemorySyncFetchResult success(TeamMemoryData data, String checksum) {
            return new TeamMemorySyncFetchResult(true, data, false, false, checksum, null, false, null, null);
        }

        public static TeamMemorySyncFetchResult empty() {
            return new TeamMemorySyncFetchResult(true, null, true, false, null, null, false, null, null);
        }

        public static TeamMemorySyncFetchResult notModified(String checksum) {
            return new TeamMemorySyncFetchResult(true, null, false, true, checksum, null, false, null, null);
        }

        public static TeamMemorySyncFetchResult error(String error, String errorType, boolean skipRetry) {
            return new TeamMemorySyncFetchResult(false, null, false, false, null, error, skipRetry, errorType, null);
        }
    }

    /**
     * Lightweight metadata-only probe result.
     */
    public record TeamMemoryHashesResult(
        boolean success,
        Long version,
        String checksum,
        Map<String, String> entryChecksums,
        String error,
        String errorType,
        Integer httpStatus
    ) {}

    /**
     * Result from uploading team memory with conflict info.
     */
    public record TeamMemorySyncPushResult(
        boolean success,
        int filesUploaded,
        String checksum,
        boolean conflict,
        String error,
        List<SkippedSecretFile> skippedSecrets,
        String errorType,
        Integer httpStatus
    ) {}

    /**
     * Result from uploading team memory.
     */
    public record TeamMemorySyncUploadResult(
        boolean success,
        String checksum,
        String lastModified,
        boolean conflict,
        String error,
        String errorType,
        Integer httpStatus,
        String serverErrorCode,
        Integer serverMaxEntries,
        Integer serverReceivedEntries
    ) {}

    /**
     * Sync state for team memory.
     */
    public static final class SyncState {
        private String lastKnownChecksum;
        private final Map<String, String> serverChecksums = new HashMap<>();
        private Integer serverMaxEntries;

        public String getLastKnownChecksum() {
            return lastKnownChecksum;
        }

        public void setLastKnownChecksum(String checksum) {
            this.lastKnownChecksum = checksum;
        }

        public Map<String, String> getServerChecksums() {
            return serverChecksums;
        }

        public Integer getServerMaxEntries() {
            return serverMaxEntries;
        }

        public void setServerMaxEntries(Integer maxEntries) {
            this.serverMaxEntries = maxEntries;
        }
    }
}