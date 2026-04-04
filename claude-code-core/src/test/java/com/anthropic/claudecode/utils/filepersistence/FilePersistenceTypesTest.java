/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.filepersistence;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for FilePersistenceTypes.
 */
@DisplayName("FilePersistenceTypes Tests")
class FilePersistenceTypesTest {

    @Test
    @DisplayName("FilePersistenceTypes constants are correct")
    void constantsAreCorrect() {
        assertEquals("outputs", FilePersistenceTypes.OUTPUTS_SUBDIR);
        assertEquals(1000, FilePersistenceTypes.FILE_COUNT_LIMIT);
        assertEquals(10, FilePersistenceTypes.DEFAULT_UPLOAD_CONCURRENCY);
    }

    @Test
    @DisplayName("PersistedFile record works correctly")
    void persistedFileRecordWorksCorrectly() {
        FilePersistenceTypes.PersistedFile file = new FilePersistenceTypes.PersistedFile(
            "test.txt",
            "file-123"
        );

        assertEquals("test.txt", file.filename());
        assertEquals("file-123", file.file_id());
    }

    @Test
    @DisplayName("FailedPersistence record works correctly")
    void failedPersistenceRecordWorksCorrectly() {
        FilePersistenceTypes.FailedPersistence failed = new FilePersistenceTypes.FailedPersistence(
            "error.txt",
            "Upload failed"
        );

        assertEquals("error.txt", failed.filename());
        assertEquals("Upload failed", failed.error());
    }

    @Test
    @DisplayName("FilesPersistedEventData default constructor creates empty lists")
    void filesPersistedEventDataDefaultConstructorCreatesEmptyLists() {
        FilePersistenceTypes.FilesPersistedEventData data = new FilePersistenceTypes.FilesPersistedEventData();

        assertTrue(data.files().isEmpty());
        assertTrue(data.failed().isEmpty());
    }

    @Test
    @DisplayName("FilesPersistedEventData record works correctly")
    void filesPersistedEventDataRecordWorksCorrectly() {
        List<FilePersistenceTypes.PersistedFile> files = List.of(
            new FilePersistenceTypes.PersistedFile("a.txt", "id-a"),
            new FilePersistenceTypes.PersistedFile("b.txt", "id-b")
        );
        List<FilePersistenceTypes.FailedPersistence> failed = List.of(
            new FilePersistenceTypes.FailedPersistence("c.txt", "Error")
        );

        FilePersistenceTypes.FilesPersistedEventData data = new FilePersistenceTypes.FilesPersistedEventData(
            files, failed
        );

        assertEquals(2, data.files().size());
        assertEquals(1, data.failed().size());
    }

    @Test
    @DisplayName("TurnStartTime record works correctly")
    void turnStartTimeRecordWorksCorrectly() {
        long timestamp = System.currentTimeMillis();
        FilePersistenceTypes.TurnStartTime turnStart = new FilePersistenceTypes.TurnStartTime(timestamp);

        assertEquals(timestamp, turnStart.timestampMs());
    }

    @Test
    @DisplayName("TurnStartTime now factory method works correctly")
    void turnStartTimeNowFactoryMethodWorksCorrectly() {
        FilePersistenceTypes.TurnStartTime turnStart = FilePersistenceTypes.TurnStartTime.now();

        assertTrue(turnStart.timestampMs() > 0);
        assertTrue(turnStart.timestampMs() <= System.currentTimeMillis());
    }

    @Test
    @DisplayName("EnvironmentKind enum has correct values")
    void environmentKindEnumHasCorrectValues() {
        FilePersistenceTypes.EnvironmentKind[] kinds = FilePersistenceTypes.EnvironmentKind.values();

        assertEquals(2, kinds.length);
        assertTrue(Arrays.asList(kinds).contains(FilePersistenceTypes.EnvironmentKind.BYOC));
        assertTrue(Arrays.asList(kinds).contains(FilePersistenceTypes.EnvironmentKind.ANTHROPIC_CLOUD));
    }
}