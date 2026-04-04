/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/plugins/PluginInstallationManager
 */
package com.anthropic.claudecode.services.plugins;

import java.util.*;
import java.util.concurrent.*;

/**
 * Plugin installation manager - Background plugin and marketplace installation.
 */
public final class PluginInstallationManager {
    private final ExecutorService executor;
    private final Map<String, CompletableFuture<PluginTypes.PluginInfo>> installations;
    private final List<PluginTypes.MarketplaceInstallStatus> marketplaceStatuses;
    private final List<PluginTypes.PluginInstallStatus> pluginStatuses;

    public PluginInstallationManager() {
        this.executor = Executors.newCachedThreadPool();
        this.installations = new ConcurrentHashMap<>();
        this.marketplaceStatuses = new CopyOnWriteArrayList<>();
        this.pluginStatuses = new CopyOnWriteArrayList<>();
    }

    /**
     * Perform background plugin startup checks and installations.
     */
    public CompletableFuture<Void> performBackgroundInstallations(
        List<String> declaredMarketplaces,
        InstallationProgressCallback callback
    ) {
        // Compute diff for initial UI status
        List<String> pending = computePendingInstallations(declaredMarketplaces);

        if (pending.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Initialize pending status
        for (String name : pending) {
            marketplaceStatuses.add(new PluginTypes.MarketplaceInstallStatus(
                name,
                PluginTypes.PluginStatus.PENDING,
                null
            ));
        }

        // Notify callback
        if (callback != null) {
            callback.onProgress(getInstallationStatus());
        }

        // Install each marketplace
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String name : pending) {
            futures.add(installMarketplace(name, callback));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Install a marketplace.
     */
    private CompletableFuture<Void> installMarketplace(
        String name,
        InstallationProgressCallback callback
    ) {
        return CompletableFuture.runAsync(() -> {
            updateMarketplaceStatus(name, PluginTypes.PluginStatus.INSTALLING, null);

            try {
                // Simulate installation
                Thread.sleep(1000);

                // Mark as installed
                updateMarketplaceStatus(name, PluginTypes.PluginStatus.INSTALLED, null);

                if (callback != null) {
                    callback.onMarketplaceInstalled(name);
                    callback.onProgress(getInstallationStatus());
                }
            } catch (Exception e) {
                updateMarketplaceStatus(name, PluginTypes.PluginStatus.FAILED, e.getMessage());

                if (callback != null) {
                    callback.onMarketplaceFailed(name, e.getMessage());
                    callback.onProgress(getInstallationStatus());
                }
            }
        }, executor);
    }

    /**
     * Install a plugin.
     */
    public CompletableFuture<PluginTypes.PluginInfo> installPlugin(
        String name,
        PluginTypes.PluginSourceType sourceType,
        String source
    ) {
        return installations.computeIfAbsent(name, k ->
            CompletableFuture.supplyAsync(() -> {
                pluginStatuses.add(new PluginTypes.PluginInstallStatus(
                    name,
                    PluginTypes.PluginStatus.INSTALLING,
                    null
                ));

                try {
                    // Simulate plugin installation
                    Thread.sleep(500);

                    PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
                        name,
                        "1.0.0",
                        "Plugin: " + name,
                        sourceType,
                        source,
                        PluginTypes.PluginStatus.INSTALLED,
                        System.currentTimeMillis(),
                        Collections.emptyMap()
                    );

                    updatePluginStatus(name, PluginTypes.PluginStatus.INSTALLED, null);
                    return info;
                } catch (Exception e) {
                    updatePluginStatus(name, PluginTypes.PluginStatus.FAILED, e.getMessage());
                    throw new CompletionException(e);
                } finally {
                    installations.remove(name);
                }
            }, executor)
        );
    }

    /**
     * Uninstall a plugin.
     */
    public CompletableFuture<Boolean> uninstallPlugin(String name) {
        return CompletableFuture.supplyAsync(() -> {
            // Remove plugin
            pluginStatuses.removeIf(p -> p.name().equals(name));
            return true;
        }, executor);
    }

    /**
     * Get installation status.
     */
    public PluginTypes.InstallationStatus getInstallationStatus() {
        return new PluginTypes.InstallationStatus(
            new ArrayList<>(marketplaceStatuses),
            new ArrayList<>(pluginStatuses)
        );
    }

    /**
     * Check if any installations are pending.
     */
    public boolean hasPendingInstallations() {
        return marketplaceStatuses.stream()
            .anyMatch(s -> s.status() == PluginTypes.PluginStatus.PENDING ||
                          s.status() == PluginTypes.PluginStatus.INSTALLING) ||
            pluginStatuses.stream()
            .anyMatch(s -> s.status() == PluginTypes.PluginStatus.PENDING ||
                          s.status() == PluginTypes.PluginStatus.INSTALLING);
    }

    private void updateMarketplaceStatus(String name, PluginTypes.PluginStatus status, String error) {
        marketplaceStatuses.removeIf(s -> s.name().equals(name));
        marketplaceStatuses.add(new PluginTypes.MarketplaceInstallStatus(name, status, error));
    }

    private void updatePluginStatus(String name, PluginTypes.PluginStatus status, String error) {
        pluginStatuses.removeIf(s -> s.name().equals(name));
        pluginStatuses.add(new PluginTypes.PluginInstallStatus(name, status, error));
    }

    private List<String> computePendingInstallations(List<String> declared) {
        Set<String> installed = new HashSet<>();
        for (PluginTypes.MarketplaceInstallStatus status : marketplaceStatuses) {
            if (status.status() == PluginTypes.PluginStatus.INSTALLED) {
                installed.add(status.name());
            }
        }

        List<String> pending = new ArrayList<>();
        for (String name : declared) {
            if (!installed.contains(name)) {
                pending.add(name);
            }
        }
        return pending;
    }

    /**
     * Shutdown the manager.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    /**
     * Installation progress callback.
     */
    public interface InstallationProgressCallback {
        void onProgress(PluginTypes.InstallationStatus status);
        void onMarketplaceInstalled(String name);
        void onMarketplaceFailed(String name, String error);
    }
}