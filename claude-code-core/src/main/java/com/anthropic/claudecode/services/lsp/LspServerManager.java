/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/lsp/LSPServerManager.ts
 */
package com.anthropic.claudecode.services.lsp;

import java.util.*;
import java.util.concurrent.*;

/**
 * LSP server manager for managing multiple LSP server instances.
 */
public final class LspServerManager {
    private LspServerManager() {}

    private static final ConcurrentHashMap<String, LspServerInstance> servers =
        new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, String> fileExtensionToServer =
        new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, String> languageToServer =
        new ConcurrentHashMap<>();

    /**
     * Server status.
     */
    public enum ServerStatus {
        STOPPED,
        STARTING,
        RUNNING,
        CRASHED,
        STOPPING
    }

    /**
     * Start an LSP server.
     */
    public static CompletableFuture<LspServerInstance> startServer(
            String serverKey,
            LspConfig.ScopedLspServerConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if already running
            LspServerInstance existing = servers.get(serverKey);
            if (existing != null && existing.getStatus() == ServerStatus.RUNNING) {
                return existing;
            }

            // Create new instance
            LspServerInstance instance = new LspServerInstance(
                serverKey,
                config.name(),
                config.command(),
                config.args(),
                config.env(),
                config.cwd()
            );

            servers.put(serverKey, instance);

            // Map file extensions and languages
            if (config.fileExtensions() != null) {
                for (String ext : config.fileExtensions()) {
                    fileExtensionToServer.put(ext, serverKey);
                }
            }
            if (config.languages() != null) {
                for (String lang : config.languages()) {
                    languageToServer.put(lang, serverKey);
                }
            }

            // Start the server
            instance.start();

            return instance;
        });
    }

    /**
     * Stop an LSP server.
     */
    public static CompletableFuture<Void> stopServer(String serverKey) {
        LspServerInstance instance = servers.get(serverKey);
        if (instance == null) {
            return CompletableFuture.completedFuture(null);
        }

        return instance.stop().thenRun(() -> {
            servers.remove(serverKey);

            // Remove mappings
            LspConfig.ScopedLspServerConfig config = instance.getConfig();
            if (config != null && config.fileExtensions() != null) {
                for (String ext : config.fileExtensions()) {
                    fileExtensionToServer.remove(ext, serverKey);
                }
            }
            if (config != null && config.languages() != null) {
                for (String lang : config.languages()) {
                    languageToServer.remove(lang, serverKey);
                }
            }
        });
    }

    /**
     * Get server by key.
     */
    public static LspServerInstance getServer(String serverKey) {
        return servers.get(serverKey);
    }

    /**
     * Get server for file extension.
     */
    public static LspServerInstance getServerForFile(String filePath) {
        String extension = getFileExtension(filePath);
        String serverKey = fileExtensionToServer.get(extension);
        if (serverKey != null) {
            return servers.get(serverKey);
        }
        return null;
    }

    /**
     * Get server for language.
     */
    public static LspServerInstance getServerForLanguage(String languageId) {
        String serverKey = languageToServer.get(languageId);
        if (serverKey != null) {
            return servers.get(serverKey);
        }
        return null;
    }

    /**
     * Get all running servers.
     */
    public static List<LspServerInstance> getRunningServers() {
        return servers.values().stream()
            .filter(s -> s.getStatus() == ServerStatus.RUNNING)
            .toList();
    }

    /**
     * Get all servers.
     */
    public static Map<String, LspServerInstance> getAllServers() {
        return new HashMap<>(servers);
    }

    /**
     * Stop all servers.
     */
    public static CompletableFuture<Void> stopAllServers() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String key : new HashSet<>(servers.keySet())) {
            futures.add(stopServer(key));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Restart a crashed server.
     */
    public static CompletableFuture<LspServerInstance> restartServer(String serverKey) {
        LspServerInstance instance = servers.get(serverKey);
        if (instance == null) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Server not found: " + serverKey)
            );
        }

        return instance.stop()
            .thenCompose(v -> {
                LspConfig.ScopedLspServerConfig config = instance.getConfig();
                if (config == null) {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("No config for server: " + serverKey)
                    );
                }
                return startServer(serverKey, config);
            });
    }

    private static String getFileExtension(String filePath) {
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex > 0) {
            return filePath.substring(dotIndex);
        }
        return "";
    }
}