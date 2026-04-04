/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/filePersistence/types.ts
 */
package com.anthropic.claudecode.utils.filepersistence;

import java.util.*;

/**
 * Types for file persistence.
 */
public final class FilePersistenceTypes {
    private FilePersistenceTypes() {}

    public static final String OUTPUTS_SUBDIR = "outputs";
    public static final int FILE_COUNT_LIMIT = 1000;
    public static final int DEFAULT_UPLOAD_CONCURRENCY = 10;

    /**
     * Persisted file record.
     */
    public record PersistedFile(
        String filename,
        String file_id
    ) {}

    /**
     * Failed persistence record.
     */
    public record FailedPersistence(
        String filename,
        String error
    ) {}

    /**
     * Event data for files persisted.
     */
    public record FilesPersistedEventData(
        List<PersistedFile> files,
        List<FailedPersistence> failed
    ) {
        public FilesPersistedEventData() {
            this(new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Turn start timestamp.
     */
    public record TurnStartTime(long timestampMs) {
        public static TurnStartTime now() {
            return new TurnStartTime(System.currentTimeMillis());
        }
    }

    /**
     * Environment kind.
     */
    public enum EnvironmentKind {
        BYOC,
        ANTHROPIC_CLOUD
    }
}