/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/plugin
 */
package com.anthropic.claudecode.types;

import java.util.*;
import java.time.*;

/**
 * Plugin types - Plugin type definitions.
 */
public final class PluginTypes {

    /**
     * Plugin status enum.
     */
    public enum PluginStatus {
        INSTALLED,
        ENABLED,
        DISABLED,
        ERROR,
        PENDING_INSTALL,
        PENDING_UPDATE,
        PENDING_UNINSTALL
    }

    /**
     * Plugin info record.
     */
    public record PluginInfo(
        String id,
        String name,
        String version,
        String description,
        String author,
        String homepage,
        String repository,
        List<String> keywords,
        Map<String, Object> config,
        PluginStatus status,
        Instant installedAt,
        Instant updatedAt,
        String installPath
    ) {
        public boolean isEnabled() {
            return status == PluginStatus.ENABLED;
        }

        public boolean isInstalled() {
            return status == PluginStatus.INSTALLED ||
                   status == PluginStatus.ENABLED ||
                   status == PluginStatus.DISABLED;
        }
    }

    /**
     * Plugin manifest record.
     */
    public record PluginManifest(
        String id,
        String name,
        String version,
        String description,
        String author,
        String main,
        List<String> commands,
        List<HookDefinition> hooks,
        List<ToolDefinition> tools,
        Map<String, Object> configuration,
        List<String> permissions,
        String minimumVersion,
        String maximumVersion
    ) {}

    /**
     * Hook definition record.
     */
    public record HookDefinition(
        String event,
        String handler,
        int priority,
        boolean enabled
    ) {}

    /**
     * Tool definition record.
     */
    public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> inputSchema,
        String handler
    ) {}

    /**
     * Plugin install options record.
     */
    public record PluginInstallOptions(
        String version,
        boolean enableAfterInstall,
        Map<String, Object> initialConfig,
        boolean forceReinstall
    ) {
        public static PluginInstallOptions defaults() {
            return new PluginInstallOptions(null, true, new HashMap<>(), false);
        }
    }

    /**
     * Plugin install result record.
     */
    public record PluginInstallResult(
        boolean success,
        PluginInfo plugin,
        String error,
        List<String> warnings
    ) {
        public static PluginInstallResult success(PluginInfo plugin) {
            return new PluginInstallResult(true, plugin, null, Collections.emptyList());
        }

        public static PluginInstallResult failure(String error) {
            return new PluginInstallResult(false, null, error, Collections.emptyList());
        }
    }

    /**
     * Plugin source enum.
     */
    public enum PluginSource {
        NPM,
        GITHUB,
        LOCAL,
        MARKETPLACE,
        BUILTIN
    }

    /**
     * Plugin dependency record.
     */
    public record PluginDependency(
        String pluginId,
        String versionRange,
        boolean optional
    ) {
        public static PluginDependency required(String id, String version) {
            return new PluginDependency(id, version, false);
        }

        public static PluginDependency optional(String id, String version) {
            return new PluginDependency(id, version, true);
        }
    }

    /**
     * Plugin configuration record.
     */
    public record PluginConfiguration(
        String pluginId,
        Map<String, Object> settings,
        boolean enabled,
        int priority
    ) {
        public static PluginConfiguration defaults(String pluginId) {
            return new PluginConfiguration(pluginId, new HashMap<>(), true, 0);
        }
    }

    /**
     * Plugin API record.
     */
    public record PluginApi(
        String version,
        List<String> capabilities,
        Map<String, String> endpoints
    ) {}

    /**
     * Plugin permission enum.
     */
    public enum PluginPermission {
        FILE_READ,
        FILE_WRITE,
        NETWORK,
        SHELL_EXECUTE,
        CLIPBOARD,
        SYSTEM_INFO,
        USER_INPUT
    }

    /**
     * Plugin event record.
     */
    public record PluginEvent(
        String type,
        String pluginId,
        Map<String, Object> data,
        Instant timestamp
    ) {
        public static PluginEvent of(String type, String pluginId, Map<String, Object> data) {
            return new PluginEvent(type, pluginId, data, Instant.now());
        }
    }

    /**
     * Plugin registry entry record.
     */
    public record RegistryEntry(
        String id,
        String name,
        String latestVersion,
        String description,
        String author,
        int downloads,
        double rating,
        List<String> tags,
        Instant publishedAt
    ) {}
}