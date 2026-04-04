/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PluginTypes.
 */
class PluginTypesTest {

    @Test
    @DisplayName("PluginTypes PluginStatus enum")
    void pluginStatusEnum() {
        PluginTypes.PluginStatus[] values = PluginTypes.PluginStatus.values();
        assertEquals(7, values.length);
        assertEquals(PluginTypes.PluginStatus.INSTALLED, PluginTypes.PluginStatus.valueOf("INSTALLED"));
        assertEquals(PluginTypes.PluginStatus.ENABLED, PluginTypes.PluginStatus.valueOf("ENABLED"));
        assertEquals(PluginTypes.PluginStatus.DISABLED, PluginTypes.PluginStatus.valueOf("DISABLED"));
        assertEquals(PluginTypes.PluginStatus.ERROR, PluginTypes.PluginStatus.valueOf("ERROR"));
    }

    @Test
    @DisplayName("PluginTypes PluginInfo record")
    void pluginInfoRecord() {
        Instant now = Instant.now();
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "plugin-123",
            "Test Plugin",
            "1.0.0",
            "A test plugin",
            "Author",
            "https://example.com",
            "https://github.com",
            List.of("test"),
            Map.of(),
            PluginTypes.PluginStatus.ENABLED,
            now,
            now,
            "/path/to/plugin"
        );

        assertEquals("plugin-123", info.id());
        assertEquals("Test Plugin", info.name());
        assertEquals("1.0.0", info.version());
        assertTrue(info.isEnabled());
        assertTrue(info.isInstalled());
    }

    @Test
    @DisplayName("PluginTypes PluginInfo isEnabled false for disabled")
    void pluginInfoIsEnabledFalse() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "id", "name", "1.0", "desc", null, null, null, null, null,
            PluginTypes.PluginStatus.DISABLED, null, null, null
        );

        assertFalse(info.isEnabled());
    }

    @Test
    @DisplayName("PluginTypes PluginInfo isInstalled false for pending")
    void pluginInfoIsInstalledFalse() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "id", "name", "1.0", "desc", null, null, null, null, null,
            PluginTypes.PluginStatus.PENDING_INSTALL, null, null, null
        );

        assertFalse(info.isInstalled());
    }

    @Test
    @DisplayName("PluginTypes PluginManifest record")
    void pluginManifestRecord() {
        PluginTypes.PluginManifest manifest = new PluginTypes.PluginManifest(
            "plugin-id",
            "Plugin Name",
            "1.0.0",
            "Description",
            "Author",
            "index.js",
            List.of("cmd1"),
            List.of(),
            List.of(),
            Map.of(),
            List.of("file:read"),
            "1.0.0",
            null
        );

        assertEquals("plugin-id", manifest.id());
        assertEquals("Plugin Name", manifest.name());
        assertEquals(1, manifest.commands().size());
    }

    @Test
    @DisplayName("PluginTypes HookDefinition record")
    void hookDefinitionRecord() {
        PluginTypes.HookDefinition hook = new PluginTypes.HookDefinition(
            "pre-command",
            "handler.js",
            10,
            true
        );

        assertEquals("pre-command", hook.event());
        assertEquals("handler.js", hook.handler());
        assertEquals(10, hook.priority());
        assertTrue(hook.enabled());
    }

    @Test
    @DisplayName("PluginTypes ToolDefinition record")
    void toolDefinitionRecord() {
        PluginTypes.ToolDefinition tool = new PluginTypes.ToolDefinition(
            "my-tool",
            "A tool",
            Map.of("type", "object"),
            "handler.js"
        );

        assertEquals("my-tool", tool.name());
        assertEquals("A tool", tool.description());
    }

    @Test
    @DisplayName("PluginTypes PluginInstallOptions defaults")
    void pluginInstallOptionsDefaults() {
        PluginTypes.PluginInstallOptions defaults = PluginTypes.PluginInstallOptions.defaults();

        assertNull(defaults.version());
        assertTrue(defaults.enableAfterInstall());
        assertTrue(defaults.initialConfig().isEmpty());
        assertFalse(defaults.forceReinstall());
    }

    @Test
    @DisplayName("PluginTypes PluginInstallOptions record")
    void pluginInstallOptionsRecord() {
        PluginTypes.PluginInstallOptions options = new PluginTypes.PluginInstallOptions(
            "1.0.0",
            false,
            Map.of("key", "value"),
            true
        );

        assertEquals("1.0.0", options.version());
        assertFalse(options.enableAfterInstall());
        assertTrue(options.forceReinstall());
    }

    @Test
    @DisplayName("PluginTypes PluginInstallResult success")
    void pluginInstallResultSuccess() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "id", "name", "1.0", "desc", null, null, null, null, null,
            PluginTypes.PluginStatus.INSTALLED, null, null, null
        );

        PluginTypes.PluginInstallResult result = PluginTypes.PluginInstallResult.success(info);

        assertTrue(result.success());
        assertEquals(info, result.plugin());
        assertNull(result.error());
    }

    @Test
    @DisplayName("PluginTypes PluginInstallResult failure")
    void pluginInstallResultFailure() {
        PluginTypes.PluginInstallResult result = PluginTypes.PluginInstallResult.failure("Install failed");

        assertFalse(result.success());
        assertNull(result.plugin());
        assertEquals("Install failed", result.error());
    }

    @Test
    @DisplayName("PluginTypes PluginSource enum")
    void pluginSourceEnum() {
        PluginTypes.PluginSource[] values = PluginTypes.PluginSource.values();
        assertEquals(5, values.length);
        assertEquals(PluginTypes.PluginSource.NPM, PluginTypes.PluginSource.valueOf("NPM"));
        assertEquals(PluginTypes.PluginSource.GITHUB, PluginTypes.PluginSource.valueOf("GITHUB"));
        assertEquals(PluginTypes.PluginSource.LOCAL, PluginTypes.PluginSource.valueOf("LOCAL"));
    }

    @Test
    @DisplayName("PluginTypes PluginDependency required")
    void pluginDependencyRequired() {
        PluginTypes.PluginDependency dep = PluginTypes.PluginDependency.required("plugin-id", "1.0.0");

        assertEquals("plugin-id", dep.pluginId());
        assertEquals("1.0.0", dep.versionRange());
        assertFalse(dep.optional());
    }

    @Test
    @DisplayName("PluginTypes PluginDependency optional")
    void pluginDependencyOptional() {
        PluginTypes.PluginDependency dep = PluginTypes.PluginDependency.optional("plugin-id", "2.0.0");

        assertTrue(dep.optional());
    }

    @Test
    @DisplayName("PluginTypes PluginConfiguration defaults")
    void pluginConfigurationDefaults() {
        PluginTypes.PluginConfiguration config = PluginTypes.PluginConfiguration.defaults("plugin-id");

        assertEquals("plugin-id", config.pluginId());
        assertTrue(config.settings().isEmpty());
        assertTrue(config.enabled());
        assertEquals(0, config.priority());
    }

    @Test
    @DisplayName("PluginTypes PluginApi record")
    void pluginApiRecord() {
        PluginTypes.PluginApi api = new PluginTypes.PluginApi(
            "1.0.0",
            List.of("tools", "hooks"),
            Map.of("base", "/api")
        );

        assertEquals("1.0.0", api.version());
        assertEquals(2, api.capabilities().size());
    }

    @Test
    @DisplayName("PluginTypes PluginPermission enum")
    void pluginPermissionEnum() {
        PluginTypes.PluginPermission[] values = PluginTypes.PluginPermission.values();
        assertEquals(7, values.length);
        assertEquals(PluginTypes.PluginPermission.FILE_READ, PluginTypes.PluginPermission.valueOf("FILE_READ"));
        assertEquals(PluginTypes.PluginPermission.FILE_WRITE, PluginTypes.PluginPermission.valueOf("FILE_WRITE"));
        assertEquals(PluginTypes.PluginPermission.NETWORK, PluginTypes.PluginPermission.valueOf("NETWORK"));
    }

    @Test
    @DisplayName("PluginTypes PluginEvent of")
    void pluginEventOf() {
        PluginTypes.PluginEvent event = PluginTypes.PluginEvent.of(
            "install",
            "plugin-123",
            Map.of("version", "1.0.0")
        );

        assertEquals("install", event.type());
        assertEquals("plugin-123", event.pluginId());
        assertNotNull(event.timestamp());
    }

    @Test
    @DisplayName("PluginTypes RegistryEntry record")
    void registryEntryRecord() {
        Instant now = Instant.now();
        PluginTypes.RegistryEntry entry = new PluginTypes.RegistryEntry(
            "plugin-id",
            "Plugin Name",
            "1.0.0",
            "Description",
            "Author",
            1000,
            4.5,
            List.of("utility"),
            now
        );

        assertEquals("plugin-id", entry.id());
        assertEquals(1000, entry.downloads());
        assertEquals(4.5, entry.rating(), 0.01);
    }
}