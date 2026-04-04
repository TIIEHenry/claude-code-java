/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AppleTerminalBackup.
 */
class AppleTerminalBackupTest {

    @Test
    @DisplayName("AppleTerminalBackup RestoreResult sealed interface")
    void restoreResultSealedInterface() {
        AppleTerminalBackup.Restored restored = new AppleTerminalBackup.Restored();
        AppleTerminalBackup.NoBackup noBackup = new AppleTerminalBackup.NoBackup();
        AppleTerminalBackup.Failed failed = new AppleTerminalBackup.Failed("/path/to/backup");

        assertEquals("restored", restored.status());
        assertEquals("no_backup", noBackup.status());
        assertEquals("failed", failed.status());
        assertEquals("/path/to/backup", failed.backupPath());
    }

    @Test
    @DisplayName("AppleTerminalBackup getTerminalPlistPath")
    void getTerminalPlistPath() {
        Path path = AppleTerminalBackup.getTerminalPlistPath();
        assertNotNull(path);
        assertTrue(path.toString().contains("Library"));
        assertTrue(path.toString().contains("Preferences"));
        assertTrue(path.toString().endsWith("com.apple.Terminal.plist"));
    }

    @Test
    @DisplayName("AppleTerminalBackup getBackupPlistPath")
    void getBackupPlistPath() {
        Path path = AppleTerminalBackup.getBackupPlistPath();
        assertNotNull(path);
        assertTrue(path.toString().endsWith(".bak"));
    }

    @Test
    @DisplayName("AppleTerminalBackup backupTerminalPreferences returns future")
    void backupTerminalPreferencesReturnsFuture() {
        CompletableFuture<Path> future = AppleTerminalBackup.backupTerminalPreferences();
        assertNotNull(future);
    }

    @Test
    @DisplayName("AppleTerminalBackup checkAndRestoreTerminalBackup returns future")
    void checkAndRestoreTerminalBackupReturnsFuture() {
        CompletableFuture<AppleTerminalBackup.RestoreResult> future =
            AppleTerminalBackup.checkAndRestoreTerminalBackup();
        assertNotNull(future);
    }

    @Test
    @DisplayName("AppleTerminalBackup markTerminalSetupInProgress does not throw")
    void markTerminalSetupInProgressNoThrow() {
        assertDoesNotThrow(() -> AppleTerminalBackup.markTerminalSetupInProgress("/path"));
    }

    @Test
    @DisplayName("AppleTerminalBackup markTerminalSetupComplete does not throw")
    void markTerminalSetupCompleteNoThrow() {
        assertDoesNotThrow(() -> AppleTerminalBackup.markTerminalSetupComplete());
    }
}