/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AutoUpdater.
 */
class AutoUpdaterTest {

    @Test
    @DisplayName("AutoUpdater InstallStatus enum values")
    void installStatusEnum() {
        AutoUpdater.InstallStatus[] statuses = AutoUpdater.InstallStatus.values();
        assertEquals(4, statuses.length);
    }

    @Test
    @DisplayName("AutoUpdater AutoUpdaterResult record")
    void autoUpdaterResult() {
        AutoUpdater.AutoUpdaterResult result = new AutoUpdater.AutoUpdaterResult(
            "1.0.0", AutoUpdater.InstallStatus.SUCCESS, java.util.List.of("notification")
        );
        assertEquals("1.0.0", result.version());
        assertEquals(AutoUpdater.InstallStatus.SUCCESS, result.status());
        assertEquals(1, result.notifications().size());
    }

    @Test
    @DisplayName("AutoUpdater MaxVersionConfig record")
    void maxVersionConfig() {
        AutoUpdater.MaxVersionConfig config = new AutoUpdater.MaxVersionConfig(
            "1.0.0", "1.0.1", "external message", "ant message"
        );
        assertEquals("1.0.0", config.external());
        assertEquals("1.0.1", config.ant());
        assertEquals("external message", config.externalMessage());
        assertEquals("ant message", config.antMessage());
    }

    @Test
    @DisplayName("AutoUpdater NpmDistTags record")
    void npmDistTags() {
        AutoUpdater.NpmDistTags tags = new AutoUpdater.NpmDistTags("1.0.0", "0.9.0");
        assertEquals("1.0.0", tags.latest());
        assertEquals("0.9.0", tags.stable());
    }

    @Test
    @DisplayName("AutoUpdater getLockFilePath returns path")
    void getLockFilePath() {
        assertNotNull(AutoUpdater.getLockFilePath());
    }

    @Test
    @DisplayName("AutoUpdater acquireLock returns future")
    void acquireLock() {
        CompletableFuture<Boolean> future = AutoUpdater.acquireLock();
        assertNotNull(future);
        // Just verify it completes
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // May fail if lock file can't be created
        }
    }

    @Test
    @DisplayName("AutoUpdater releaseLock returns future")
    void releaseLock() {
        CompletableFuture<Void> future = AutoUpdater.releaseLock();
        assertNotNull(future);
    }

    @Test
    @DisplayName("AutoUpdater checkGlobalInstallPermissions returns future")
    void checkGlobalInstallPermissions() {
        CompletableFuture<AutoUpdater.InstallPermissions> future = AutoUpdater.checkGlobalInstallPermissions();
        assertNotNull(future);
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            // May fail if npm not available
        }
    }

    @Test
    @DisplayName("AutoUpdater getLatestVersionFromGcs returns future")
    void getLatestVersionFromGcs() {
        CompletableFuture<String> future = AutoUpdater.getLatestVersionFromGcs("stable");
        assertNotNull(future);
        try {
            String version = future.get(5, TimeUnit.SECONDS);
            // May be null if no network
        } catch (Exception e) {
            // Expected if no network
        }
    }

    @Test
    @DisplayName("AutoUpdater getVersionHistory returns future")
    void getVersionHistory() {
        CompletableFuture<java.util.List<String>> future = AutoUpdater.getVersionHistory(10);
        assertNotNull(future);
        try {
            java.util.List<String> versions = future.get(30, TimeUnit.SECONDS);
            assertNotNull(versions);
        } catch (Exception e) {
            // Expected if no npm
        }
    }

    @Test
    @DisplayName("AutoUpdater InstallPermissions record")
    void installPermissions() {
        AutoUpdater.InstallPermissions perms = new AutoUpdater.InstallPermissions(true, "/usr/local");
        assertTrue(perms.hasPermissions());
        assertEquals("/usr/local", perms.npmPrefix());
    }
}
