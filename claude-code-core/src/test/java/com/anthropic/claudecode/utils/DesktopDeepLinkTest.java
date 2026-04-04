/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DesktopDeepLink.
 */
class DesktopDeepLinkTest {

    @Test
    @DisplayName("DesktopDeepLink DesktopInstallStatus.NotInstalled")
    void notInstalled() {
        DesktopDeepLink.DesktopInstallStatus.NotInstalled status =
            new DesktopDeepLink.DesktopInstallStatus.NotInstalled();
        assertEquals("not-installed", status.status());
    }

    @Test
    @DisplayName("DesktopDeepLink DesktopInstallStatus.VersionTooOld")
    void versionTooOld() {
        DesktopDeepLink.DesktopInstallStatus.VersionTooOld status =
            new DesktopDeepLink.DesktopInstallStatus.VersionTooOld("1.0.0");
        assertEquals("version-too-old", status.status());
        assertEquals("1.0.0", status.version());
    }

    @Test
    @DisplayName("DesktopDeepLink DesktopInstallStatus.Ready")
    void ready() {
        DesktopDeepLink.DesktopInstallStatus.Ready status =
            new DesktopDeepLink.DesktopInstallStatus.Ready("1.1.0");
        assertEquals("ready", status.status());
        assertEquals("1.1.0", status.version());
    }

    @Test
    @DisplayName("DesktopDeepLink DesktopOpenResult record")
    void desktopOpenResult() {
        DesktopDeepLink.DesktopOpenResult result = new DesktopDeepLink.DesktopOpenResult(
            true, null, "claude://resume?session=id"
        );
        assertTrue(result.success());
        assertNull(result.error());
        assertEquals("claude://resume?session=id", result.deepLinkUrl());
    }

    @Test
    @DisplayName("DesktopDeepLink buildDesktopDeepLink")
    void buildDesktopDeepLink() {
        String link = DesktopDeepLink.buildDesktopDeepLink("session-123", "/home/user");
        assertNotNull(link);
        assertTrue(link.contains("session-123"));
        assertTrue(link.contains("resume"));
    }

    @Test
    @DisplayName("DesktopDeepLink isDesktopInstalled returns future")
    void isDesktopInstalledReturnsFuture() {
        CompletableFuture<Boolean> future = DesktopDeepLink.isDesktopInstalled();
        assertNotNull(future);
    }

    @Test
    @DisplayName("DesktopDeepLink getDesktopVersion returns future")
    void getDesktopVersionReturnsFuture() {
        CompletableFuture<String> future = DesktopDeepLink.getDesktopVersion();
        assertNotNull(future);
    }

    @Test
    @DisplayName("DesktopDeepLink getDesktopInstallStatus returns future")
    void getDesktopInstallStatusReturnsFuture() {
        CompletableFuture<DesktopDeepLink.DesktopInstallStatus> future =
            DesktopDeepLink.getDesktopInstallStatus();
        assertNotNull(future);
    }

    @Test
    @DisplayName("DesktopDeepLink openCurrentSessionInDesktop returns future")
    void openCurrentSessionInDesktopReturnsFuture() {
        CompletableFuture<DesktopDeepLink.DesktopOpenResult> future =
            DesktopDeepLink.openCurrentSessionInDesktop("session-id", "/cwd");
        assertNotNull(future);
    }
}