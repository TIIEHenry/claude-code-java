/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.plugins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PluginInstallationManager.
 */
class PluginInstallationManagerTest {

    private PluginInstallationManager manager;

    @BeforeEach
    void setUp() {
        manager = new PluginInstallationManager();
    }

    @Test
    @DisplayName("PluginInstallationManager getInstallationStatus returns empty initially")
    void getInstallationStatusEmpty() {
        PluginTypes.InstallationStatus status = manager.getInstallationStatus();

        assertTrue(status.marketplaces().isEmpty());
        assertTrue(status.plugins().isEmpty());
    }

    @Test
    @DisplayName("PluginInstallationManager hasPendingInstallations returns false initially")
    void hasPendingInstallationsFalse() {
        assertFalse(manager.hasPendingInstallations());
    }

    @Test
    @DisplayName("PluginInstallationManager installPlugin returns info")
    void installPlugin() throws Exception {
        PluginTypes.PluginInfo info = manager.installPlugin(
            "test-plugin",
            PluginTypes.PluginSourceType.NPM,
            "test-package"
        ).get();

        assertNotNull(info);
        assertEquals("test-plugin", info.name());
        assertEquals(PluginTypes.PluginStatus.INSTALLED, info.status());
    }

    @Test
    @DisplayName("PluginInstallationManager uninstallPlugin returns true")
    void uninstallPlugin() throws Exception {
        // First install
        manager.installPlugin("test-plugin",
            PluginTypes.PluginSourceType.LOCAL, "/path").get();

        // Then uninstall
        boolean result = manager.uninstallPlugin("test-plugin").get();
        assertTrue(result);
    }

    @Test
    @DisplayName("PluginInstallationManager performBackgroundInstallations with empty list")
    void performBackgroundInstallationsEmpty() throws Exception {
        manager.performBackgroundInstallations(List.of(), null).get();
        // Should complete without error
    }

    @Test
    @DisplayName("PluginInstallationManager performBackgroundInstallations with pending")
    void performBackgroundInstallationsWithPending() throws Exception {
        manager.performBackgroundInstallations(
            List.of("marketplace1"),
            new PluginInstallationManager.InstallationProgressCallback() {
                @Override
                public void onProgress(PluginTypes.InstallationStatus status) {}

                @Override
                public void onMarketplaceInstalled(String name) {}

                @Override
                public void onMarketplaceFailed(String name, String error) {}
            }
        ).get();

        PluginTypes.InstallationStatus status = manager.getInstallationStatus();
        assertEquals(1, status.marketplaces().size());
    }

    @Test
    @DisplayName("PluginInstallationManager shutdown does not throw")
    void shutdown() {
        manager.shutdown();
    }
}