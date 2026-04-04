/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/plugins
 */
package com.anthropic.claudecode.services.plugins;

import java.util.*;
import java.util.concurrent.*;

/**
 * Plugin types - Type definitions for plugin system.
 */
public final class PluginTypes {

    /**
     * Plugin status enum.
     */
    public enum PluginStatus {
        PENDING,
        INSTALLING,
        INSTALLED,
        FAILED,
        DISABLED
    }

    /**
     * Plugin source type.
     */
    public enum PluginSourceType {
        NPM,
        GITHUB,
        LOCAL,
        MARKETPLACE,
        BUILTIN
    }

    /**
     * Plugin info.
     */
    public record PluginInfo(
        String name,
        String version,
        String description,
        PluginSourceType sourceType,
        String source,
        PluginStatus status,
        long installedAt,
        Map<String, Object> config
    ) {}

    /**
     * Plugin manifest.
     */
    public record PluginManifest(
        String name,
        String version,
        String description,
        String main,
        List<String> commands,
        List<String> tools,
        Map<String, Object> config,
        PluginActivation activation
    ) {}

    /**
     * Plugin activation settings.
     */
    public record PluginActivation(
        List<String> filePatterns,
        List<String> languages,
        List<String> triggers,
        boolean autoActivate
    ) {}

    /**
     * Marketplace info.
     */
    public record MarketplaceInfo(
        String name,
        String url,
        String description,
        String maintainer,
        PluginStatus status,
        String error,
        long lastUpdated
    ) {}

    /**
     * Installation status.
     */
    public record InstallationStatus(
        List<MarketplaceInstallStatus> marketplaces,
        List<PluginInstallStatus> plugins
    ) {}

    /**
     * Marketplace install status.
     */
    public record MarketplaceInstallStatus(
        String name,
        PluginStatus status,
        String error
    ) {}

    /**
     * Plugin install status.
     */
    public record PluginInstallStatus(
        String name,
        PluginStatus status,
        String error
    ) {}

    /**
     * Loaded plugin.
     */
    public record LoadedPlugin(
        PluginInfo info,
        PluginManifest manifest,
        Object instance,
        List<RegisteredCommand> commands,
        List<RegisteredTool> tools
    ) {}

    /**
     * Registered command from plugin.
     */
    public record RegisteredCommand(
        String name,
        String description,
        String pluginSource
    ) {}

    /**
     * Registered tool from plugin.
     */
    public record RegisteredTool(
        String name,
        String description,
        String pluginSource
    ) {}
}