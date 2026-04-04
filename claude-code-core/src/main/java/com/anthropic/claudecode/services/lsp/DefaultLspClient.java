/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/lsp/LSPClient.ts
 */
package com.anthropic.claudecode.services.lsp;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * LSP client interface.
 */
interface LspClient {
    /**
     * Get server capabilities.
     */
    Map<String, Object> getCapabilities();

    /**
     * Check if client is initialized.
     */
    boolean isInitialized();

    /**
     * Start the LSP server process.
     */
    CompletableFuture<Void> start(String command, List<String> args, StartOptions options);

    /**
     * Initialize the LSP connection.
     */
    CompletableFuture<InitializeResult> initialize(InitializeParams params);

    /**
     * Send a request to the server.
     */
    <T> CompletableFuture<T> sendRequest(String method, Object params);

    /**
     * Send a notification to the server.
     */
    CompletableFuture<Void> sendNotification(String method, Object params);

    /**
     * Register a notification handler.
     */
    void onNotification(String method, Consumer<Object> handler);

    /**
     * Register a request handler.
     */
    <P, R> void onRequest(String method, Function<P, R> handler);

    /**
     * Stop the client and server.
     */
    CompletableFuture<Void> stop();

    /**
     * Start options.
     */
    record StartOptions(
        Map<String, String> env,
        String cwd
    ) {}

    /**
     * Initialize params.
     */
    record InitializeParams(
        String processId,
        String clientInfo,
        String locale,
        String rootUri,
        Map<String, Object> capabilities,
        Object workspaceFolders
    ) {}

    /**
     * Initialize result.
     */
    record InitializeResult(
        Map<String, Object> capabilities,
        Object serverInfo
    ) {}
}

/**
 * Default LSP client implementation.
 */
public final class DefaultLspClient implements LspClient {
    private final String serverName;
    private final Consumer<Throwable> onCrash;

    private Process process;
    private MessageConnection connection;
    private Map<String, Object> capabilities;
    private boolean isInitialized = false;
    private boolean startFailed = false;
    private Throwable startError;
    private boolean isStopping = false;

    // Pending handlers registered before connection ready
    private final List<PendingHandler> pendingHandlers = new CopyOnWriteArrayList<>();
    private final List<PendingRequestHandler> pendingRequestHandlers = new CopyOnWriteArrayList<>();

    private record PendingHandler(String method, Consumer<Object> handler) {}
    private record PendingRequestHandler(String method, Function<Object, Object> handler) {}

    public DefaultLspClient(String serverName, Consumer<Throwable> onCrash) {
        this.serverName = serverName;
        this.onCrash = onCrash;
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public CompletableFuture<Void> start(String command, List<String> args, StartOptions options) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Build process builder
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.command().addAll(args);

                // Set environment
                if (options != null && options.env() != null) {
                    pb.environment().putAll(options.env());
                }

                // Set working directory
                if (options != null && options.cwd() != null) {
                    pb.directory(new java.io.File(options.cwd()));
                }

                // Redirect stderr to separate stream for diagnostics
                pb.redirectErrorStream(false);

                // Start process
                process = pb.start();

                // Wait for process to spawn successfully
                // In Java, ProcessBuilder.start() throws if command not found
                // so we don't need the async spawn wait like Node.js

                // Create message connection
                connection = new MessageConnection(
                    process.getInputStream(),
                    process.getOutputStream()
                );

                // Register error handlers
                connection.onError(error -> {
                    if (!isStopping) {
                        startFailed = true;
                        startError = error;
                        logError("LSP server " + serverName + " connection error: " + error.getMessage());
                    }
                });

                connection.onClose(() -> {
                    if (!isStopping) {
                        isInitialized = false;
                        logDebug("LSP server " + serverName + " connection closed");
                    }
                });

                // Start listening
                connection.listen();

                // Apply queued handlers
                for (PendingHandler handler : pendingHandlers) {
                    connection.onNotification(handler.method(), handler.handler());
                }
                pendingHandlers.clear();

                for (PendingRequestHandler handler : pendingRequestHandlers) {
                    connection.onRequest(handler.method(), handler.handler());
                }
                pendingRequestHandlers.clear();

                // Monitor process exit
                Thread monitorThread = new Thread(() -> {
                    try {
                        int exitCode = process.waitFor();
                        if (exitCode != 0 && !isStopping) {
                            isInitialized = false;
                            startFailed = false;
                            startError = null;
                            Throwable crashError = new RuntimeException(
                                "LSP server " + serverName + " crashed with exit code " + exitCode
                            );
                            logError(crashError.getMessage());
                            if (onCrash != null) {
                                onCrash.accept(crashError);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                monitorThread.setDaemon(true);
                monitorThread.start();

                logDebug("LSP client started for " + serverName);
            } catch (Exception e) {
                startFailed = true;
                startError = e;
                logError("LSP server " + serverName + " failed to start: " + e.getMessage());
                throw new RuntimeException("Failed to start LSP server", e);
            }
        });
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        if (connection == null) {
            return CompletableFuture.failedFuture(new RuntimeException("LSP client not started"));
        }

        checkStartFailed();

        return connection.sendRequest("initialize", params)
            .thenApply(result -> {
                capabilities = (Map<String, Object>) result;

                // Send initialized notification
                connection.sendNotification("initialized", Map.of());

                isInitialized = true;
                logDebug("LSP server " + serverName + " initialized");

                return new InitializeResult(capabilities, null);
            })
            .exceptionally(e -> {
                logError("LSP server " + serverName + " initialize failed: " + e.getMessage());
                throw new RuntimeException("Initialize failed", e);
            });
    }

    @Override
    public <T> CompletableFuture<T> sendRequest(String method, Object params) {
        if (connection == null) {
            return CompletableFuture.failedFuture(new RuntimeException("LSP client not started"));
        }

        checkStartFailed();

        if (!isInitialized) {
            return CompletableFuture.failedFuture(new RuntimeException("LSP server not initialized"));
        }

        @SuppressWarnings("unchecked")
        CompletableFuture<T> result = (CompletableFuture<T>) connection.sendRequest(method, params)
            .exceptionally(e -> {
                logError("LSP server " + serverName + " request " + method + " failed: " + e.getMessage());
                throw new RuntimeException("Request failed", e);
            });
        return result;
    }

    @Override
    public CompletableFuture<Void> sendNotification(String method, Object params) {
        if (connection == null) {
            return CompletableFuture.failedFuture(new RuntimeException("LSP client not started"));
        }

        checkStartFailed();

        return connection.sendNotification(method, params)
            .exceptionally(e -> {
                logDebug("Notification " + method + " failed but continuing");
                return null;
            });
    }

    @Override
    public void onNotification(String method, Consumer<Object> handler) {
        if (connection == null) {
            pendingHandlers.add(new PendingHandler(method, handler));
            logDebug("Queued notification handler for " + serverName + "." + method);
            return;
        }

        checkStartFailed();
        connection.onNotification(method, handler);
    }

    @Override
    public <P, R> void onRequest(String method, Function<P, R> handler) {
        if (connection == null) {
            pendingRequestHandlers.add(new PendingRequestHandler(
                method,
                params -> handler.apply((P) params)
            ));
            logDebug("Queued request handler for " + serverName + "." + method);
            return;
        }

        checkStartFailed();
        connection.onRequest(method, params -> handler.apply((P) params));
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            Throwable shutdownError = null;
            isStopping = true;

            try {
                if (connection != null) {
                    // Try shutdown/exit
                    try {
                        connection.sendRequest("shutdown", Map.of()).join();
                        connection.sendNotification("exit", Map.of()).join();
                    } catch (Exception e) {
                        shutdownError = e;
                        logError("LSP server " + serverName + " stop failed: " + e.getMessage());
                    }

                    // Dispose connection
                    try {
                        connection.dispose();
                    } catch (Exception e) {
                        logDebug("Connection disposal failed for " + serverName);
                    }
                    connection = null;
                }

                if (process != null) {
                    // Kill process
                    process.destroy();
                    try {
                        process.waitFor(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        process.destroyForcibly();
                    }
                    process = null;
                }

                isInitialized = false;
                capabilities = null;
                isStopping = false;

                if (shutdownError != null) {
                    startFailed = true;
                    startError = shutdownError;
                }

                logDebug("LSP client stopped for " + serverName);

                if (shutdownError != null) {
                    throw new RuntimeException("Shutdown error", shutdownError);
                }
            } catch (Exception e) {
                logError("Error stopping LSP client: " + e.getMessage());
            }
        });
    }

    private void checkStartFailed() {
        if (startFailed) {
            throw new RuntimeException(
                startError != null ? startError :
                new RuntimeException("LSP server " + serverName + " failed to start")
            );
        }
    }

    private void logError(String message) {
        System.err.println("[LSP ERROR] " + message);
    }

    private void logDebug(String message) {
        // Debug logging - would use proper logging in real implementation
    }
}