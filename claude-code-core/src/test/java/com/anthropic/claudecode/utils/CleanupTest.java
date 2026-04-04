/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Cleanup.
 */
class CleanupTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Cleanup CleanupResult record")
    void cleanupResultRecord() {
        Cleanup.CleanupResult result = new Cleanup.CleanupResult(5, 2);
        assertEquals(5, result.messages());
        assertEquals(2, result.errors());
    }

    @Test
    @DisplayName("Cleanup CleanupResult add")
    void cleanupResultAdd() {
        Cleanup.CleanupResult r1 = new Cleanup.CleanupResult(5, 2);
        Cleanup.CleanupResult r2 = new Cleanup.CleanupResult(3, 1);
        Cleanup.CleanupResult sum = r1.add(r2);
        assertEquals(8, sum.messages());
        assertEquals(3, sum.errors());
    }

    @Test
    @DisplayName("Cleanup getCutoffDate")
    void getCutoffDate() {
        Instant cutoff = Cleanup.getCutoffDate();
        Instant expected = Instant.now().minus(30, ChronoUnit.DAYS);
        // Should be approximately 30 days ago
        assertTrue(cutoff.isBefore(Instant.now().minus(29, ChronoUnit.DAYS)));
        assertTrue(cutoff.isAfter(Instant.now().minus(31, ChronoUnit.DAYS)));
    }

    @Test
    @DisplayName("Cleanup convertFileNameToDate valid")
    void convertFileNameToDateValid() {
        String filename = "2024-01-15T10-30-45-123Z.jsonl";
        Instant date = Cleanup.convertFileNameToDate(filename);
        assertNotNull(date);
        // Should parse to valid instant
        assertTrue(date.isAfter(Instant.parse("2024-01-01T00:00:00Z")));
    }

    @Test
    @DisplayName("Cleanup convertFileNameToDate null")
    void convertFileNameToDateNull() {
        assertNull(Cleanup.convertFileNameToDate(null));
    }

    @Test
    @DisplayName("Cleanup convertFileNameToDate empty")
    void convertFileNameToDateEmpty() {
        assertNull(Cleanup.convertFileNameToDate(""));
    }

    @Test
    @DisplayName("Cleanup convertFileNameToDate invalid")
    void convertFileNameToDateInvalid() {
        assertNull(Cleanup.convertFileNameToDate("invalid-filename"));
    }

    @Test
    @DisplayName("Cleanup cleanupOldFilesInDirectory empty dir")
    void cleanupOldFilesInDirectoryEmpty() throws Exception {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        Cleanup.CleanupResult result = Cleanup.cleanupOldFilesInDirectory(
            emptyDir, Instant.now()
        );
        assertEquals(0, result.messages());
        assertEquals(0, result.errors());
    }

    @Test
    @DisplayName("Cleanup cleanupOldFilesInDirectory nonexistent")
    void cleanupOldFilesInDirectoryNonexistent() {
        Cleanup.CleanupResult result = Cleanup.cleanupOldFilesInDirectory(
            tempDir.resolve("nonexistent"), Instant.now()
        );
        assertEquals(0, result.messages());
        assertEquals(0, result.errors());
    }

    @Test
    @DisplayName("Cleanup cleanupOldFilesInDirectory deletes old files")
    void cleanupOldFilesInDirectoryDeletesOld() throws Exception {
        Path file = tempDir.resolve("2024-01-01T00-00-00-000Z.jsonl");
        Files.writeString(file, "test content");

        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        Cleanup.CleanupResult result = Cleanup.cleanupOldFilesInDirectory(tempDir, cutoff);

        assertEquals(1, result.messages());
        assertFalse(Files.exists(file));
    }

    @Test
    @DisplayName("Cleanup cleanupOldFilesInDirectory keeps new files")
    void cleanupOldFilesInDirectoryKeepsNew() throws Exception {
        // Use a date string format that will parse to a recent date (not in the future)
        String recentDate = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS)
            .toString().replace(":", "-").replace(".", "-");
        Path file = tempDir.resolve(recentDate + ".jsonl");
        Files.writeString(file, "test content");

        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        Cleanup.CleanupResult result = Cleanup.cleanupOldFilesInDirectory(tempDir, cutoff);

        assertEquals(0, result.messages());
        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("Cleanup deleteIfOld deletes old")
    void deleteIfOldDeletes() throws Exception {
        Path file = tempDir.resolve("old.txt");
        Files.writeString(file, "content");
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.from(
            Instant.now().minus(40, ChronoUnit.DAYS)
        ));

        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        boolean deleted = Cleanup.deleteIfOld(file, cutoff);

        assertTrue(deleted);
        assertFalse(Files.exists(file));
    }

    @Test
    @DisplayName("Cleanup deleteIfOld keeps new")
    void deleteIfOldKeeps() throws Exception {
        Path file = tempDir.resolve("new.txt");
        Files.writeString(file, "content");

        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        boolean deleted = Cleanup.deleteIfOld(file, cutoff);

        assertFalse(deleted);
        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("Cleanup deleteIfOld nonexistent")
    void deleteIfOldNonexistent() throws Exception {
        Path file = tempDir.resolve("nonexistent.txt");
        Instant cutoff = Instant.now();

        boolean deleted = Cleanup.deleteIfOld(file, cutoff);
        assertFalse(deleted);
    }

    @Test
    @DisplayName("Cleanup tryRmdir empty")
    void tryRmdirEmpty() throws Exception {
        Path dir = tempDir.resolve("emptyDir");
        Files.createDirectories(dir);

        boolean removed = Cleanup.tryRmdir(dir);
        assertTrue(removed);
        assertFalse(Files.exists(dir));
    }

    @Test
    @DisplayName("Cleanup tryRmdir nonexistent")
    void tryRmdirNonexistent() {
        Path dir = tempDir.resolve("nonexistentDir");
        boolean removed = Cleanup.tryRmdir(dir);
        assertTrue(removed); // deleteIfExists returns true even if not exists
    }

    @Test
    @DisplayName("Cleanup tryRmdir nonempty")
    void tryRmdirNonempty() throws Exception {
        Path dir = tempDir.resolve("nonemptyDir");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("file.txt"), "content");

        boolean removed = Cleanup.tryRmdir(dir);
        assertFalse(removed);
        assertTrue(Files.exists(dir));
    }

    @Test
    @DisplayName("Cleanup cleanupOldDebugLogs nonexistent")
    void cleanupOldDebugLogsNonexistent() {
        Cleanup.CleanupResult result = Cleanup.cleanupOldDebugLogs(
            tempDir.resolve("nonexistent")
        );
        assertEquals(0, result.messages());
        assertEquals(0, result.errors());
    }

    @Test
    @DisplayName("Cleanup cleanupOldPlanFiles nonexistent")
    void cleanupOldPlanFilesNonexistent() {
        Cleanup.CleanupResult result = Cleanup.cleanupOldPlanFiles(
            tempDir.resolve("nonexistent")
        );
        assertEquals(0, result.messages());
        assertEquals(0, result.errors());
    }

    @Test
    @DisplayName("Cleanup cleanupFilesWithExtension nonexistent")
    void cleanupFilesWithExtensionNonexistent() {
        Cleanup.CleanupResult result = Cleanup.cleanupFilesWithExtension(
            tempDir.resolve("nonexistent"), ".txt"
        );
        assertEquals(0, result.messages());
        assertEquals(0, result.errors());
    }

    @Test
    @DisplayName("Cleanup cleanupFilesWithExtension deletes old")
    void cleanupFilesWithExtensionDeletesOld() throws Exception {
        Path file = tempDir.resolve("test.md");
        Files.writeString(file, "content");
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.from(
            Instant.now().minus(40, ChronoUnit.DAYS)
        ));

        Cleanup.CleanupResult result = Cleanup.cleanupFilesWithExtension(tempDir, ".md");

        assertTrue(result.messages() >= 1);
        assertFalse(Files.exists(file));
    }

    @Test
    @DisplayName("Cleanup runBackgroundCleanup")
    void runBackgroundCleanup() {
        CompletableFuture<Cleanup.CleanupResult> future = Cleanup.runBackgroundCleanup();
        assertDoesNotThrow(() -> future.get(5, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Cleanup cleanupOldSessionFiles nonexistent")
    void cleanupOldSessionFilesNonexistent() {
        Cleanup.CleanupResult result = Cleanup.cleanupOldSessionFiles(
            tempDir.resolve("nonexistent")
        );
        assertEquals(0, result.messages());
        assertEquals(0, result.errors());
    }
}