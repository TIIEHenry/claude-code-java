/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/settings/types.ts
 */
package com.anthropic.claudecode.utils.settings;

import java.util.*;

/**
 * Settings types for Claude Code configuration.
 */
public final class SettingsTypes {
    private SettingsTypes() {}

    // ========== Permission Schema ==========

    /**
     * Permission mode enum.
     */
    public enum PermissionMode {
        DEFAULT("default"),
        ACCEPT_EDITS("acceptEdits"),
        BYPASS_PERMISSIONS("bypassPermissions"),
        DONT_ASK("dontAsk"),
        PLAN("plan"),
        AUTO("auto"),
        BUBBLE("bubble");

        private final String id;

        PermissionMode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static PermissionMode fromId(String id) {
            for (PermissionMode mode : values()) {
                if (mode.id.equals(id)) {
                    return mode;
                }
            }
            return DEFAULT;
        }
    }

    /**
     * Permissions schema.
     */
    public record PermissionsSchema(
        PermissionMode mode,
        List<String> allow,
        List<String> deny,
        List<String> ask
    ) {
        public static PermissionsSchema createDefault() {
            return new PermissionsSchema(
                PermissionMode.DEFAULT,
                List.of(),
                List.of(),
                List.of()
            );
        }
    }

    // ========== Hook Schema ==========

    /**
     * Hook type enum.
     */
    public enum HookType {
        PRE_TOOL_USE("preToolUse"),
        POST_TOOL_USE("postToolUse"),
        USER_PROMPT_SUBMIT("userPromptSubmit");

        private final String id;

        HookType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Hook configuration.
     */
    public record HookConfig(
        String command,
        List<String> args,
        Map<String, String> env,
        Long timeout,
        Boolean enabled
    ) {
        public static HookConfig of(String command) {
            return new HookConfig(command, List.of(), Map.of(), null, true);
        }
    }

    /**
     * Hooks schema.
     */
    public record HooksSchema(
        List<HookConfig> preToolUse,
        List<HookConfig> postToolUse,
        List<HookConfig> userPromptSubmit
    ) {
        public static HooksSchema createDefault() {
            return new HooksSchema(List.of(), List.of(), List.of());
        }
    }

    // ========== MCP Schema ==========

    /**
     * MCP transport type.
     */
    public enum McpTransportType {
        STDIO("stdio"),
        SSE("sse"),
        WEBSOCKET("websocket");

        private final String id;

        McpTransportType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * MCP server configuration.
     */
    public record McpServerSchema(
        String command,
        List<String> args,
        Map<String, String> env,
        McpTransportType transport,
        String url,
        Map<String, Object> options,
        Boolean enabled
    ) {
        public static McpServerSchema stdio(String command, List<String> args) {
            return new McpServerSchema(command, args, Map.of(), McpTransportType.STDIO, null, Map.of(), true);
        }
    }

    /**
     * MCP schema.
     */
    public record McpSchema(
        Map<String, McpServerSchema> servers,
        List<String> contextUris
    ) {
        public static McpSchema createDefault() {
            return new McpSchema(Map.of(), List.of());
        }
    }

    // ========== Auto Mode Schema ==========

    /**
     * Auto mode classifier result.
     */
    public record AutoModeConfig(
        Boolean enabled,
        Integer maxDenials,
        Integer maxConsecutiveDenials,
        Boolean fallbackToInteractive,
        List<String> allowlistedTools
    ) {
        public static AutoModeConfig createDefault() {
            return new AutoModeConfig(
                false,
                10,
                3,
                true,
                List.of()
            );
        }
    }

    // ========== Plugin Schema ==========

    /**
     * Plugin hook configuration.
     */
    public record PluginHookConfig(
        String name,
        HookType type,
        String command,
        List<String> args,
        Map<String, String> env,
        Long timeout
    ) {}

    /**
     * Plugin configuration.
     */
    public record PluginConfig(
        String name,
        String version,
        String description,
        List<PluginHookConfig> hooks,
        Map<String, Object> config
    ) {}

    // ========== Settings Schema ==========

    /**
     * Complete settings schema.
     */
    public record SettingsSchema(
        String model,
        String apiKey,
        String theme,
        PermissionMode permissionMode,
        PermissionsSchema permissions,
        HooksSchema hooks,
        McpSchema mcp,
        AutoModeConfig autoMode,
        List<String> enabledMcpServers,
        List<String> disabledMcpServers,
        Boolean autoUpdate,
        String releaseChannel,
        String installMethod,
        String preferredNotifChannel,
        Boolean hasTrustDialogAccepted,
        Boolean hasCompletedOnboarding,
        Map<String, PluginConfig> plugins,
        Map<String, Object> custom
    ) {
        public static SettingsSchema createDefault() {
            return new SettingsSchema(
                null, // model
                null, // apiKey
                "default", // theme
                PermissionMode.DEFAULT,
                PermissionsSchema.createDefault(),
                HooksSchema.createDefault(),
                McpSchema.createDefault(),
                AutoModeConfig.createDefault(),
                List.of(),
                List.of(),
                true, // autoUpdate
                "stable", // releaseChannel
                null, // installMethod
                "auto", // preferredNotifChannel
                false, // hasTrustDialogAccepted
                false, // hasCompletedOnboarding
                Map.of(),
                Map.of()
            );
        }

        /**
         * Merge with another settings schema (other takes precedence).
         */
        public SettingsSchema merge(SettingsSchema other) {
            if (other == null) return this;

            return new SettingsSchema(
                other.model != null ? other.model : this.model,
                other.apiKey != null ? other.apiKey : this.apiKey,
                other.theme != null ? other.theme : this.theme,
                other.permissionMode != null ? other.permissionMode : this.permissionMode,
                other.permissions != null ? other.permissions : this.permissions,
                other.hooks != null ? other.hooks : this.hooks,
                other.mcp != null ? other.mcp : this.mcp,
                other.autoMode != null ? other.autoMode : this.autoMode,
                other.enabledMcpServers != null ? other.enabledMcpServers : this.enabledMcpServers,
                other.disabledMcpServers != null ? other.disabledMcpServers : this.disabledMcpServers,
                other.autoUpdate != null ? other.autoUpdate : this.autoUpdate,
                other.releaseChannel != null ? other.releaseChannel : this.releaseChannel,
                other.installMethod != null ? other.installMethod : this.installMethod,
                other.preferredNotifChannel != null ? other.preferredNotifChannel : this.preferredNotifChannel,
                other.hasTrustDialogAccepted != null ? other.hasTrustDialogAccepted : this.hasTrustDialogAccepted,
                other.hasCompletedOnboarding != null ? other.hasCompletedOnboarding : this.hasCompletedOnboarding,
                other.plugins != null ? other.plugins : this.plugins,
                other.custom != null ? other.custom : this.custom
            );
        }
    }

    // ========== Setting Value ==========

    /**
     * A setting value with its source.
     */
    public record SettingValue<T>(
        T value,
        SettingSource source,
        String sourceDetail
    ) {
        public static <T> SettingValue<T> of(T value, SettingSource source) {
            return new SettingValue<>(value, source, null);
        }

        public static <T> SettingValue<T> of(T value, SettingSource source, String sourceDetail) {
            return new SettingValue<>(value, source, sourceDetail);
        }
    }
}