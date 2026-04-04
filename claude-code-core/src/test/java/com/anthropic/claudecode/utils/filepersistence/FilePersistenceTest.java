/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.filepersistence;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Tests for FilePersistence.
 */
@DisplayName("FilePersistence Tests")
class FilePersistenceTest {

    @Test
    @DisplayName("FilePersistence isFilePersistenceEnabled returns true")
    void isFilePersistenceEnabledReturnsTrue() {
        assertTrue(FilePersistence.isFilePersistenceEnabled());
    }

    @Test
    @DisplayName("FilePersistence getPersistenceDir returns valid path")
    void getPersistenceDirReturnsValidPath() {
        Path path = FilePersistence.getPersistenceDir();

        assertNotNull(path);
        assertTrue(path.toString().contains(".claude"));
        assertTrue(path.toString().contains("persistence"));
    }

    @Test
    @DisplayName("FilePersistence runFilePersistence returns event data")
    void runFilePersistenceReturnsEventData() {
        FilePersistenceTypes.TurnStartTime turnStart = FilePersistenceTypes.TurnStartTime.now();

        CompletableFuture<FilePersistenceTypes.FilesPersistedEventData> future =
            FilePersistence.runFilePersistence(turnStart);

        FilePersistenceTypes.FilesPersistedEventData data = future.join();

        assertNotNull(data);
        assertNotNull(data.files());
        assertNotNull(data.failed());
    }

    @Test
    @DisplayName("FilePersistence clearPersistence does not throw")
    void clearPersistenceDoesNotThrow() {
        // Should not throw even if directory doesn't exist
        assertDoesNotThrow(() -> FilePersistence.clearPersistence());
    }
}