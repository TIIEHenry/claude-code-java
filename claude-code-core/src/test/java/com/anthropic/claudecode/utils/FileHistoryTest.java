/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileHistory.
 */
class FileHistoryTest {

    @BeforeEach
    void setUp() {
        FileHistory.setFileHistoryEnabled(true);
    }

    @Test
    @DisplayName("FileHistory FileHistoryBackup record")
    void fileHistoryBackupRecord() {
        FileHistory.FileHistoryBackup backup = new FileHistory.FileHistoryBackup(
            "backup-file", 1, new Date()
        );
        assertEquals("backup-file", backup.backupFileName());
        assertEquals(1, backup.version());
        assertNotNull(backup.backupTime());
    }

    @Test
    @DisplayName("FileHistory FileHistorySnapshot record")
    void fileHistorySnapshotRecord() {
        FileHistory.FileHistorySnapshot snapshot = new FileHistory.FileHistorySnapshot(
            "msg-id", Map.of(), new Date()
        );
        assertEquals("msg-id", snapshot.messageId());
        assertNotNull(snapshot.trackedFileBackups());
        assertNotNull(snapshot.timestamp());
    }

    @Test
    @DisplayName("FileHistory FileHistoryState createEmpty")
    void fileHistoryStateCreateEmpty() {
        FileHistory.FileHistoryState state = FileHistory.FileHistoryState.createEmpty();
        assertEquals(0, state.snapshots().size());
        assertEquals(0, state.trackedFiles().size());
        assertEquals(0, state.snapshotSequence());
    }

    @Test
    @DisplayName("FileHistory FileHistoryState record")
    void fileHistoryStateRecord() {
        FileHistory.FileHistoryState state = new FileHistory.FileHistoryState(
            new ArrayList<>(),
            new HashSet<>(),
            5
        );
        assertEquals(5, state.snapshotSequence());
    }

    @Test
    @DisplayName("FileHistory DiffStats record")
    void diffStatsRecord() {
        FileHistory.DiffStats stats = new FileHistory.DiffStats(
            List.of("file1.txt", "file2.txt"), 10, 5
        );
        assertEquals(2, stats.filesChanged().size());
        assertEquals(10, stats.insertions());
        assertEquals(5, stats.deletions());
    }

    @Test
    @DisplayName("FileHistory fileHistoryEnabled true by default")
    void fileHistoryEnabledDefault() {
        FileHistory.setFileHistoryEnabled(true);
        assertTrue(FileHistory.fileHistoryEnabled());
    }

    @Test
    @DisplayName("FileHistory setFileHistoryEnabled false")
    void setFileHistoryEnabledFalse() {
        FileHistory.setFileHistoryEnabled(false);
        assertFalse(FileHistory.fileHistoryEnabled());
        // Reset
        FileHistory.setFileHistoryEnabled(true);
    }

    @Test
    @DisplayName("FileHistory fileHistoryTrackEdit returns future")
    void fileHistoryTrackEditReturnsFuture() {
        FileHistory.FileHistoryState state = FileHistory.FileHistoryState.createEmpty();
        CompletableFuture<Void> future = FileHistory.fileHistoryTrackEdit(
            state, "/tmp/test.txt", "msg-id"
        );
        assertNotNull(future);
    }

    @Test
    @DisplayName("FileHistory fileHistoryMakeSnapshot returns future")
    void fileHistoryMakeSnapshotReturnsFuture() {
        FileHistory.FileHistoryState state = FileHistory.FileHistoryState.createEmpty();
        CompletableFuture<Void> future = FileHistory.fileHistoryMakeSnapshot(state, "msg-id");
        assertNotNull(future);
    }

    @Test
    @DisplayName("FileHistory fileHistoryRewind returns future")
    void fileHistoryRewindReturnsFuture() {
        FileHistory.FileHistoryState state = FileHistory.FileHistoryState.createEmpty();
        CompletableFuture<Void> future = FileHistory.fileHistoryRewind(state, "msg-id");
        assertNotNull(future);
    }

    @Test
    @DisplayName("FileHistory fileHistoryCanRestore false for empty state")
    void fileHistoryCanRestoreEmpty() {
        FileHistory.FileHistoryState state = FileHistory.FileHistoryState.createEmpty();
        assertFalse(FileHistory.fileHistoryCanRestore(state, "msg-id"));
    }

    @Test
    @DisplayName("FileHistory fileHistoryCanRestore true with matching snapshot")
    void fileHistoryCanRestoreWithSnapshot() {
        FileHistory.FileHistorySnapshot snapshot = new FileHistory.FileHistorySnapshot(
            "msg-123", Map.of(), new Date()
        );
        FileHistory.FileHistoryState state = new FileHistory.FileHistoryState(
            List.of(snapshot), new HashSet<>(), 1
        );
        assertTrue(FileHistory.fileHistoryCanRestore(state, "msg-123"));
    }

    @Test
    @DisplayName("FileHistory fileHistoryGetDiffStats returns future")
    void fileHistoryGetDiffStatsReturnsFuture() {
        FileHistory.FileHistoryState state = FileHistory.FileHistoryState.createEmpty();
        CompletableFuture<FileHistory.DiffStats> future = FileHistory.fileHistoryGetDiffStats(state, "msg-id");
        assertNotNull(future);
    }

    @Test
    @DisplayName("FileHistory fileHistoryHasAnyChanges returns future")
    void fileHistoryHasAnyChangesReturnsFuture() {
        FileHistory.FileHistoryState state = FileHistory.FileHistoryState.createEmpty();
        CompletableFuture<Boolean> future = FileHistory.fileHistoryHasAnyChanges(state, "msg-id");
        assertNotNull(future);
    }
}