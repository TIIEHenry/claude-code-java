/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.settings;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for SettingsTypes.
 */
@DisplayName("SettingsTypes Tests")
class SettingsTypesTest {

    @Test
    @DisplayName("PermissionMode enum has correct values")
    void permissionModeHasCorrectValues() {
        SettingsTypes.PermissionMode[] modes = SettingsTypes.PermissionMode.values();

        assertEquals(7, modes.length);
        assertTrue(Arrays.asList(modes).contains(SettingsTypes.PermissionMode.DEFAULT));
        assertTrue(Arrays.asList(modes).contains(SettingsTypes.PermissionMode.ACCEPT_EDITS));
        assertTrue(Arrays.asList(modes).contains(SettingsTypes.PermissionMode.BYPASS_PERMISSIONS));
        assertTrue(Arrays.asList(modes).contains(SettingsTypes.PermissionMode.DONT_ASK));
        assertTrue(Arrays.asList(modes).contains(SettingsTypes.PermissionMode.PLAN));
        assertTrue(Arrays.asList(modes).contains(SettingsTypes.PermissionMode.AUTO));
        assertTrue(Arrays.asList(modes).contains(SettingsTypes.PermissionMode.BUBBLE));
    }

    @Test
    @DisplayName("PermissionMode getId works correctly")
    void permissionModeGetIdWorksCorrectly() {
        assertEquals("default", SettingsTypes.PermissionMode.DEFAULT.getId());
        assertEquals("acceptEdits", SettingsTypes.PermissionMode.ACCEPT_EDITS.getId());
        assertEquals("bypassPermissions", SettingsTypes.PermissionMode.BYPASS_PERMISSIONS.getId());
        assertEquals("dontAsk", SettingsTypes.PermissionMode.DONT_ASK.getId());
        assertEquals("plan", SettingsTypes.PermissionMode.PLAN.getId());
        assertEquals("auto", SettingsTypes.PermissionMode.AUTO.getId());
        assertEquals("bubble", SettingsTypes.PermissionMode.BUBBLE.getId());
    }

    @Test
    @DisplayName("PermissionMode fromId works correctly")
    void permissionModeFromIdWorksCorrectly() {
        assertEquals(SettingsTypes.PermissionMode.DEFAULT, SettingsTypes.PermissionMode.fromId("default"));
        assertEquals(SettingsTypes.PermissionMode.ACCEPT_EDITS, SettingsTypes.PermissionMode.fromId("acceptEdits"));
        assertEquals(SettingsTypes.PermissionMode.BYPASS_PERMISSIONS, SettingsTypes.PermissionMode.fromId("bypassPermissions"));
        assertEquals(SettingsTypes.PermissionMode.DEFAULT, SettingsTypes.PermissionMode.fromId("unknown"));
    }

    @Test
    @DisplayName("PermissionsSchema createDefault works correctly")
    void permissionsSchemaCreateDefaultWorksCorrectly() {
        SettingsTypes.PermissionsSchema perms = SettingsTypes.PermissionsSchema.createDefault();

        assertEquals(SettingsTypes.PermissionMode.DEFAULT, perms.mode());
        assertTrue(perms.allow().isEmpty());
        assertTrue(perms.deny().isEmpty());
        assertTrue(perms.ask().isEmpty());
    }

    @Test
    @DisplayName("PermissionsSchema record works correctly")
    void permissionsSchemaRecordWorksCorrectly() {
        SettingsTypes.PermissionsSchema perms = new SettingsTypes.PermissionsSchema(
            SettingsTypes.PermissionMode.ACCEPT_EDITS,
            List.of("Read", "Write"),
            List.of("Bash"),
            List.of("Edit")
        );

        assertEquals(SettingsTypes.PermissionMode.ACCEPT_EDITS, perms.mode());
        assertEquals(2, perms.allow().size());
        assertEquals(1, perms.deny().size());
        assertEquals(1, perms.ask().size());
    }

    @Test
    @DisplayName("HookType enum has correct values")
    void hookTypeHasCorrectValues() {
        SettingsTypes.HookType[] types = SettingsTypes.HookType.values();

        assertEquals(3, types.length);
        assertTrue(Arrays.asList(types).contains(SettingsTypes.HookType.PRE_TOOL_USE));
        assertTrue(Arrays.asList(types).contains(SettingsTypes.HookType.POST_TOOL_USE));
        assertTrue(Arrays.asList(types).contains(SettingsTypes.HookType.USER_PROMPT_SUBMIT));
    }

    @Test
    @DisplayName("HookConfig of factory method works correctly")
    void hookConfigOfFactoryMethodWorksCorrectly() {
        SettingsTypes.HookConfig config = SettingsTypes.HookConfig.of("echo test");

        assertEquals("echo test", config.command());
        assertTrue(config.args().isEmpty());
        assertTrue(config.env().isEmpty());
        assertNull(config.timeout());
        assertTrue(config.enabled());
    }

    @Test
    @DisplayName("HookConfig record works correctly with all parameters")
    void hookConfigRecordWorksCorrectly() {
        SettingsTypes.HookConfig config = new SettingsTypes.HookConfig(
            "npm",
            List.of("run", "lint"),
            Map.of("NODE_ENV", "test"),
            5000L,
            false
        );

        assertEquals("npm", config.command());
        assertEquals(2, config.args().size());
        assertEquals(1, config.env().size());
        assertEquals(5000L, config.timeout());
        assertFalse(config.enabled());
    }

    @Test
    @DisplayName("HooksSchema createDefault works correctly")
    void hooksSchemaCreateDefaultWorksCorrectly() {
        SettingsTypes.HooksSchema hooks = SettingsTypes.HooksSchema.createDefault();

        assertTrue(hooks.preToolUse().isEmpty());
        assertTrue(hooks.postToolUse().isEmpty());
        assertTrue(hooks.userPromptSubmit().isEmpty());
    }

    @Test
    @DisplayName("McpTransportType enum has correct values")
    void mcpTransportTypeHasCorrectValues() {
        SettingsTypes.McpTransportType[] types = SettingsTypes.McpTransportType.values();

        assertEquals(3, types.length);
        assertTrue(Arrays.asList(types).contains(SettingsTypes.McpTransportType.STDIO));
        assertTrue(Arrays.asList(types).contains(SettingsTypes.McpTransportType.SSE));
        assertTrue(Arrays.asList(types).contains(SettingsTypes.McpTransportType.WEBSOCKET));
    }

    @Test
    @DisplayName("McpServerSchema stdio factory method works correctly")
    void mcpServerSchemaStdioFactoryMethodWorksCorrectly() {
        SettingsTypes.McpServerSchema server = SettingsTypes.McpServerSchema.stdio(
            "node",
            List.of("server.js")
        );

        assertEquals("node", server.command());
        assertEquals(1, server.args().size());
        assertEquals(SettingsTypes.McpTransportType.STDIO, server.transport());
        assertTrue(server.enabled());
        assertNull(server.url());
    }

    @Test
    @DisplayName("McpSchema createDefault works correctly")
    void mcpSchemaCreateDefaultWorksCorrectly() {
        SettingsTypes.McpSchema mcp = SettingsTypes.McpSchema.createDefault();

        assertTrue(mcp.servers().isEmpty());
        assertTrue(mcp.contextUris().isEmpty());
    }

    @Test
    @DisplayName("AutoModeConfig createDefault works correctly")
    void autoModeConfigCreateDefaultWorksCorrectly() {
        SettingsTypes.AutoModeConfig autoMode = SettingsTypes.AutoModeConfig.createDefault();

        assertFalse(autoMode.enabled());
        assertEquals(10, autoMode.maxDenials());
        assertEquals(3, autoMode.maxConsecutiveDenials());
        assertTrue(autoMode.fallbackToInteractive());
        assertTrue(autoMode.allowlistedTools().isEmpty());
    }

    @Test
    @DisplayName("SettingsSchema createDefault works correctly")
    void settingsSchemaCreateDefaultWorksCorrectly() {
        SettingsTypes.SettingsSchema settings = SettingsTypes.SettingsSchema.createDefault();

        assertNull(settings.model());
        assertNull(settings.apiKey());
        assertEquals("default", settings.theme());
        assertEquals(SettingsTypes.PermissionMode.DEFAULT, settings.permissionMode());
        assertNotNull(settings.permissions());
        assertNotNull(settings.hooks());
        assertNotNull(settings.mcp());
        assertNotNull(settings.autoMode());
        assertTrue(settings.autoUpdate());
        assertEquals("stable", settings.releaseChannel());
        assertEquals("auto", settings.preferredNotifChannel());
        assertFalse(settings.hasTrustDialogAccepted());
        assertFalse(settings.hasCompletedOnboarding());
    }

    @Test
    @DisplayName("SettingsSchema merge works correctly")
    void settingsSchemaMergeWorksCorrectly() {
        SettingsTypes.SettingsSchema base = SettingsTypes.SettingsSchema.createDefault();
        SettingsTypes.SettingsSchema override = new SettingsTypes.SettingsSchema(
            "claude-opus-4-6",
            "test-key",
            "dark",
            SettingsTypes.PermissionMode.ACCEPT_EDITS,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        SettingsTypes.SettingsSchema merged = base.merge(override);

        assertEquals("claude-opus-4-6", merged.model());
        assertEquals("test-key", merged.apiKey());
        assertEquals("dark", merged.theme());
        assertEquals(SettingsTypes.PermissionMode.ACCEPT_EDITS, merged.permissionMode());
        // Other fields from base
        assertTrue(merged.autoUpdate());
        assertEquals("stable", merged.releaseChannel());
    }

    @Test
    @DisplayName("SettingsSchema merge with null returns original")
    void settingsSchemaMergeWithNullReturnsOriginal() {
        SettingsTypes.SettingsSchema settings = SettingsTypes.SettingsSchema.createDefault();

        SettingsTypes.SettingsSchema merged = settings.merge(null);

        assertEquals(settings, merged);
    }

    @Test
    @DisplayName("SettingValue of factory method works correctly")
    void settingValueOfFactoryMethodWorksCorrectly() {
        SettingsTypes.SettingValue<String> value = SettingsTypes.SettingValue.of("test", SettingSource.USER);

        assertEquals("test", value.value());
        assertEquals(SettingSource.USER, value.source());
        assertNull(value.sourceDetail());
    }

    @Test
    @DisplayName("SettingValue of with source detail works correctly")
    void settingValueOfWithSourceDetailWorksCorrectly() {
        SettingsTypes.SettingValue<String> value = SettingsTypes.SettingValue.of("test", SettingSource.PROJECT, ".claude/settings.json");

        assertEquals("test", value.value());
        assertEquals(SettingSource.PROJECT, value.source());
        assertEquals(".claude/settings.json", value.sourceDetail());
    }

    @Test
    @DisplayName("PluginHookConfig record works correctly")
    void pluginHookConfigRecordWorksCorrectly() {
        SettingsTypes.PluginHookConfig hookConfig = new SettingsTypes.PluginHookConfig(
            "pre-commit",
            SettingsTypes.HookType.PRE_TOOL_USE,
            "git",
            List.of("hook", "run"),
            Map.of("GIT_DIR", ".git"),
            10000L
        );

        assertEquals("pre-commit", hookConfig.name());
        assertEquals(SettingsTypes.HookType.PRE_TOOL_USE, hookConfig.type());
        assertEquals("git", hookConfig.command());
        assertEquals(2, hookConfig.args().size());
        assertEquals(1, hookConfig.env().size());
        assertEquals(10000L, hookConfig.timeout());
    }

    @Test
    @DisplayName("PluginConfig record works correctly")
    void pluginConfigRecordWorksCorrectly() {
        SettingsTypes.PluginConfig plugin = new SettingsTypes.PluginConfig(
            "test-plugin",
            "1.0.0",
            "Test plugin description",
            List.of(),
            Map.of("enabled", true)
        );

        assertEquals("test-plugin", plugin.name());
        assertEquals("1.0.0", plugin.version());
        assertEquals("Test plugin description", plugin.description());
        assertTrue(plugin.hooks().isEmpty());
        assertEquals(1, plugin.config().size());
    }
}