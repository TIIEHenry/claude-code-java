/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/plugins/pluginOperations
 */
package com.anthropic.claudecode.services.plugins;

import java.util.*;
import java.util.concurrent.*;

/**
 * Plugin operations - Plugin management operations.
 */
public final class PluginOperations {
    private final PluginInstallationManager installationManager;
    private final Map<String, PluginTypes.LoadedPlugin> loadedPlugins;

    public PluginOperations(PluginInstallationManager installationManager) {
        this.installationManager = installationManager;
        this.loadedPlugins = new ConcurrentHashMap<>();
    }

    /**
     * List all available plugins.
     */
    public List<PluginTypes.PluginInfo> listPlugins() {
        List<PluginTypes.PluginInfo> plugins = new ArrayList<>();
        for (PluginTypes.LoadedPlugin loaded : loadedPlugins.values()) {
            plugins.add(loaded.info());
        }
        return plugins;
    }

    /**
     * Get a plugin by name.
     */
    public PluginTypes.LoadedPlugin getPlugin(String name) {
        return loadedPlugins.get(name);
    }

    /**
     * Install a plugin from npm.
     */
    public CompletableFuture<PluginTypes.PluginInfo> installFromNpm(String packageName) {
        return installationManager.installPlugin(
            packageName,
            PluginTypes.PluginSourceType.NPM,
            packageName
        );
    }

    /**
     * Install a plugin from GitHub.
     */
    public CompletableFuture<PluginTypes.PluginInfo> installFromGitHub(String repo) {
        return installationManager.installPlugin(
            repo,
            PluginTypes.PluginSourceType.GITHUB,
            "https://github.com/" + repo
        );
    }

    /**
     * Install a plugin from local path.
     */
    public CompletableFuture<PluginTypes.PluginInfo> installFromLocal(String path) {
        return installationManager.installPlugin(
            path,
            PluginTypes.PluginSourceType.LOCAL,
            path
        );
    }

    /**
     * Uninstall a plugin.
     */
    public CompletableFuture<Boolean> uninstall(String name) {
        // First unload
        unloadPlugin(name);

        // Then uninstall
        return installationManager.uninstallPlugin(name);
    }

    /**
     * Enable a plugin.
     */
    public void enablePlugin(String name) {
        PluginTypes.LoadedPlugin plugin = loadedPlugins.get(name);
        if (plugin != null) {
            // Update status to installed (enabled)
            PluginTypes.PluginInfo newInfo = new PluginTypes.PluginInfo(
                plugin.info().name(),
                plugin.info().version(),
                plugin.info().description(),
                plugin.info().sourceType(),
                plugin.info().source(),
                PluginTypes.PluginStatus.INSTALLED,
                plugin.info().installedAt(),
                plugin.info().config()
            );

            loadedPlugins.put(name, new PluginTypes.LoadedPlugin(
                newInfo,
                plugin.manifest(),
                plugin.instance(),
                plugin.commands(),
                plugin.tools()
            ));
        }
    }

    /**
     * Disable a plugin.
     */
    public void disablePlugin(String name) {
        PluginTypes.LoadedPlugin plugin = loadedPlugins.get(name);
        if (plugin != null) {
            PluginTypes.PluginInfo newInfo = new PluginTypes.PluginInfo(
                plugin.info().name(),
                plugin.info().version(),
                plugin.info().description(),
                plugin.info().sourceType(),
                plugin.info().source(),
                PluginTypes.PluginStatus.DISABLED,
                plugin.info().installedAt(),
                plugin.info().config()
            );

            loadedPlugins.put(name, new PluginTypes.LoadedPlugin(
                newInfo,
                plugin.manifest(),
                plugin.instance(),
                plugin.commands(),
                plugin.tools()
            ));
        }
    }

    /**
     * Load a plugin.
     */
    public void loadPlugin(String name, PluginTypes.LoadedPlugin plugin) {
        loadedPlugins.put(name, plugin);
    }

    /**
     * Unload a plugin.
     */
    public void unloadPlugin(String name) {
        loadedPlugins.remove(name);
    }

    /**
     * Reload all plugins.
     */
    public CompletableFuture<Void> reloadAll() {
        return CompletableFuture.runAsync(() -> {
            List<String> names = new ArrayList<>(loadedPlugins.keySet());
            loadedPlugins.clear();

            // Re-load each plugin
            for (String name : names) {
                // Implementation would reload the plugin
            }
        });
    }

    /**
     * Get all registered commands from plugins.
     */
    public List<PluginTypes.RegisteredCommand> getAllCommands() {
        List<PluginTypes.RegisteredCommand> commands = new ArrayList<>();
        for (PluginTypes.LoadedPlugin plugin : loadedPlugins.values()) {
            if (plugin.info().status() != PluginTypes.PluginStatus.DISABLED) {
                commands.addAll(plugin.commands());
            }
        }
        return commands;
    }

    /**
     * Get all registered tools from plugins.
     */
    public List<PluginTypes.RegisteredTool> getAllTools() {
        List<PluginTypes.RegisteredTool> tools = new ArrayList<>();
        for (PluginTypes.LoadedPlugin plugin : loadedPlugins.values()) {
            if (plugin.info().status() != PluginTypes.PluginStatus.DISABLED) {
                tools.addAll(plugin.tools());
            }
        }
        return tools;
    }

    /**
     * Check if a plugin is enabled.
     */
    public boolean isPluginEnabled(String name) {
        PluginTypes.LoadedPlugin plugin = loadedPlugins.get(name);
        return plugin != null && plugin.info().status() != PluginTypes.PluginStatus.DISABLED;
    }

    /**
     * Get plugin count.
     */
    public int getPluginCount() {
        return loadedPlugins.size();
    }

    /**
     * Get enabled plugin count.
     */
    public int getEnabledPluginCount() {
        return (int) loadedPlugins.values().stream()
            .filter(p -> p.info().status() != PluginTypes.PluginStatus.DISABLED)
            .count();
    }
}