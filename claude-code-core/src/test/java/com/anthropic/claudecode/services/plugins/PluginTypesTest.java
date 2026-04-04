/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.plugins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PluginTypes.
 */
class PluginTypesTest {

    @Test
    @DisplayName("PluginTypes PluginStatus enum values")
    void pluginStatusEnum() {
        PluginTypes.PluginStatus[] statuses = PluginTypes.PluginStatus.values();
        assertEquals(5, statuses.length);
        assertEquals(PluginTypes.PluginStatus.PENDING, PluginTypes.PluginStatus.valueOf("PENDING"));
        assertEquals(PluginTypes.PluginStatus.INSTALLING, PluginTypes.PluginStatus.valueOf("INSTALLING"));
        assertEquals(PluginTypes.PluginStatus.INSTALLED, PluginTypes.PluginStatus.valueOf("INSTALLED"));
        assertEquals(PluginTypes.PluginStatus.FAILED, PluginTypes.PluginStatus.valueOf("FAILED"));
        assertEquals(PluginTypes.PluginStatus.DISABLED, PluginTypes.PluginStatus.valueOf("DISABLED"));
    }

    @Test
    @DisplayName("PluginTypes PluginSourceType enum values")
    void pluginSourceTypeEnum() {
        PluginTypes.PluginSourceType[] types = PluginTypes.PluginSourceType.values();
        assertEquals(5, types.length);
        assertEquals(PluginTypes.PluginSourceType.NPM, PluginTypes.PluginSourceType.valueOf("NPM"));
        assertEquals(PluginTypes.PluginSourceType.GITHUB, PluginTypes.PluginSourceType.valueOf("GITHUB"));
        assertEquals(PluginTypes.PluginSourceType.LOCAL, PluginTypes.PluginSourceType.valueOf("LOCAL"));
        assertEquals(PluginTypes.PluginSourceType.MARKETPLACE, PluginTypes.PluginSourceType.valueOf("MARKETPLACE"));
        assertEquals(PluginTypes.PluginSourceType.BUILTIN, PluginTypes.PluginSourceType.valueOf("BUILTIN"));
    }

    @Test
    @DisplayName("PluginTypes PluginInfo record")
    void pluginInfoRecord() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "test-plugin", "1.0.0", "A test plugin",
            PluginTypes.PluginSourceType.NPM, "npm:test-plugin",
            PluginTypes.PluginStatus.INSTALLED,
            1234567890L,
            Map.of("key", "value")
        );

        assertEquals("test-plugin", info.name());
        assertEquals("1.0.0", info.version());
        assertEquals("A test plugin", info.description());
        assertEquals(PluginTypes.PluginSourceType.NPM, info.sourceType());
        assertEquals("npm:test-plugin", info.source());
        assertEquals(PluginTypes.PluginStatus.INSTALLED, info.status());
        assertEquals(1234567890L, info.installedAt());
        assertEquals("value", info.config().get("key"));
    }

    @Test
    @DisplayName("PluginTypes PluginManifest record")
    void pluginManifestRecord() {
        PluginTypes.PluginActivation activation = new PluginTypes.PluginActivation(
            List.of("**/*.ts"), List.of("typescript"), List.of("onLoad"), true
        );

        PluginTypes.PluginManifest manifest = new PluginTypes.PluginManifest(
            "my-plugin", "2.0.0", "Description",
            "index.js", List.of("cmd1", "cmd2"), List.of("tool1"),
            Map.of("setting", true), activation
        );

        assertEquals("my-plugin", manifest.name());
        assertEquals("2.0.0", manifest.version());
        assertEquals("Description", manifest.description());
        assertEquals("index.js", manifest.main());
        assertEquals(2, manifest.commands().size());
        assertEquals(1, manifest.tools().size());
        assertTrue(manifest.config().containsKey("setting"));
        assertNotNull(manifest.activation());
    }

    @Test
    @DisplayName("PluginTypes PluginActivation record")
    void pluginActivationRecord() {
        PluginTypes.PluginActivation activation = new PluginTypes.PluginActivation(
            List.of("*.js", "*.ts"),
            List.of("javascript", "typescript"),
            List.of("onFileOpen", "onCommand"),
            true
        );

        assertEquals(2, activation.filePatterns().size());
        assertEquals(2, activation.languages().size());
        assertEquals(2, activation.triggers().size());
        assertTrue(activation.autoActivate());
    }

    @Test
    @DisplayName("PluginTypes MarketplaceInfo record")
    void marketplaceInfoRecord() {
        PluginTypes.MarketplaceInfo info = new PluginTypes.MarketplaceInfo(
            "marketplace-name", "https://example.com",
            "A marketplace", "maintainer",
            PluginTypes.PluginStatus.INSTALLED, null, 1234567890L
        );

        assertEquals("marketplace-name", info.name());
        assertEquals("https://example.com", info.url());
        assertEquals("A marketplace", info.description());
        assertEquals("maintainer", info.maintainer());
        assertEquals(PluginTypes.PluginStatus.INSTALLED, info.status());
        assertNull(info.error());
    }

    @Test
    @DisplayName("PluginTypes MarketplaceInfo with error")
    void marketplaceInfoWithError() {
        PluginTypes.MarketplaceInfo info = new PluginTypes.MarketplaceInfo(
            "name", "url", "desc", "maintainer",
            PluginTypes.PluginStatus.FAILED, "Installation failed", 0L
        );

        assertEquals(PluginTypes.PluginStatus.FAILED, info.status());
        assertEquals("Installation failed", info.error());
    }

    @Test
    @DisplayName("PluginTypes InstallationStatus record")
    void installationStatusRecord() {
        PluginTypes.MarketplaceInstallStatus marketplace = new PluginTypes.MarketplaceInstallStatus(
            "marketplace", PluginTypes.PluginStatus.INSTALLED, null
        );
        PluginTypes.PluginInstallStatus plugin = new PluginTypes.PluginInstallStatus(
            "plugin", PluginTypes.PluginStatus.PENDING, null
        );

        PluginTypes.InstallationStatus status = new PluginTypes.InstallationStatus(
            List.of(marketplace), List.of(plugin)
        );

        assertEquals(1, status.marketplaces().size());
        assertEquals(1, status.plugins().size());
    }

    @Test
    @DisplayName("PluginTypes MarketplaceInstallStatus record")
    void marketplaceInstallStatusRecord() {
        PluginTypes.MarketplaceInstallStatus status = new PluginTypes.MarketplaceInstallStatus(
            "test-marketplace", PluginTypes.PluginStatus.INSTALLING, "In progress"
        );

        assertEquals("test-marketplace", status.name());
        assertEquals(PluginTypes.PluginStatus.INSTALLING, status.status());
        assertEquals("In progress", status.error());
    }

    @Test
    @DisplayName("PluginTypes PluginInstallStatus record")
    void pluginInstallStatusRecord() {
        PluginTypes.PluginInstallStatus status = new PluginTypes.PluginInstallStatus(
            "test-plugin", PluginTypes.PluginStatus.FAILED, "Network error"
        );

        assertEquals("test-plugin", status.name());
        assertEquals(PluginTypes.PluginStatus.FAILED, status.status());
        assertEquals("Network error", status.error());
    }

    @Test
    @DisplayName("PluginTypes LoadedPlugin record")
    void loadedPluginRecord() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "loaded", "1.0", "desc",
            PluginTypes.PluginSourceType.LOCAL, "local",
            PluginTypes.PluginStatus.INSTALLED, 0L, Map.of()
        );
        PluginTypes.PluginManifest manifest = new PluginTypes.PluginManifest(
            "loaded", "1.0", "desc", "main", List.of(), List.of(), Map.of(), null
        );
        PluginTypes.RegisteredCommand cmd = new PluginTypes.RegisteredCommand(
            "cmd", "description", "plugin-source"
        );
        PluginTypes.RegisteredTool tool = new PluginTypes.RegisteredTool(
            "tool", "description", "plugin-source"
        );

        PluginTypes.LoadedPlugin plugin = new PluginTypes.LoadedPlugin(
            info, manifest, null, List.of(cmd), List.of(tool)
        );

        assertEquals("loaded", plugin.info().name());
        assertEquals("loaded", plugin.manifest().name());
        assertNull(plugin.instance());
        assertEquals(1, plugin.commands().size());
        assertEquals(1, plugin.tools().size());
    }

    @Test
    @DisplayName("PluginTypes RegisteredCommand record")
    void registeredCommandRecord() {
        PluginTypes.RegisteredCommand cmd = new PluginTypes.RegisteredCommand(
            "my-command", "My custom command", "my-plugin"
        );

        assertEquals("my-command", cmd.name());
        assertEquals("My custom command", cmd.description());
        assertEquals("my-plugin", cmd.pluginSource());
    }

    @Test
    @DisplayName("PluginTypes RegisteredTool record")
    void registeredToolRecord() {
        PluginTypes.RegisteredTool tool = new PluginTypes.RegisteredTool(
            "my-tool", "My custom tool", "my-plugin"
        );

        assertEquals("my-tool", tool.name());
        assertEquals("My custom tool", tool.description());
        assertEquals("my-plugin", tool.pluginSource());
    }
}