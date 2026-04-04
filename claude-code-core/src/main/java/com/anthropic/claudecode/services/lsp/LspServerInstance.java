/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/lsp/LSPServerInstance.ts
 */
package com.anthropic.claudecode.services.lsp;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * LSP server instance - represents a running LSP server.
 */
public final class LspServerInstance {
    private final String serverKey;
    private final String name;
    private final String command;
    private final List<String> args;
    private final Map<String, String> env;
    private final String cwd;

    private volatile LspClient client;
    private volatile LspServerManager.ServerStatus status = LspServerManager.ServerStatus.STOPPED;
    private volatile LspConfig.ScopedLspServerConfig config;

    public LspServerInstance(
            String serverKey,
            String name,
            String command,
            List<String> args,
            Map<String, String> env,
            String cwd) {
        this.serverKey = serverKey;
        this.name = name;
        this.command = command;
        this.args = args != null ? new ArrayList<>(args) : new ArrayList<>();
        this.env = env != null ? new HashMap<>(env) : new HashMap<>();
        this.cwd = cwd;
    }

    /**
     * Get server key.
     */
    public String getServerKey() {
        return serverKey;
    }

    /**
     * Get server name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get server status.
     */
    public LspServerManager.ServerStatus getStatus() {
        return status;
    }

    /**
     * Get client.
     */
    public LspClient getClient() {
        return client;
    }

    /**
     * Get config.
     */
    public LspConfig.ScopedLspServerConfig getConfig() {
        return config;
    }

    /**
     * Set config.
     */
    public void setConfig(LspConfig.ScopedLspServerConfig config) {
        this.config = config;
    }

    /**
     * Register a notification handler.
     */
    public void onNotification(String method, Consumer<Object> handler) {
        if (client != null) {
            client.onNotification(method, handler);
        }
    }

    /**
     * Start the server.
     */
    public CompletableFuture<Void> start() {
        if (status == LspServerManager.ServerStatus.RUNNING) {
            return CompletableFuture.completedFuture(null);
        }

        status = LspServerManager.ServerStatus.STARTING;

        // Create client with crash handler
        client = new DefaultLspClient(name, error -> {
            status = LspServerManager.ServerStatus.CRASHED;
        });

        // Start process
        return client.start(command, args, new LspClient.StartOptions(env, cwd))
            .thenCompose(v -> {
                // Initialize
                LspClient.InitializeParams params = new LspClient.InitializeParams(
                    String.valueOf(ProcessHandle.current().pid()),
                    "Claude Code Java",
                    "en",
                    cwd != null ? "file://" + cwd : null,
                    Map.of(
                        "textDocument", Map.of(
                            "synchronization", Map.of(
                                "dynamicRegistration", true,
                                "willSave", true,
                                "willSaveWaitUntil", true,
                                "didSave", true
                            ),
                            "completion", Map.of(
                                "dynamicRegistration", true,
                                "completionItem", Map.of(
                                    "snippetSupport", true,
                                    "commitCharactersSupport", true,
                                    "documentationFormat", List.of("markdown", "plaintext")
                                )
                            ),
                            "diagnostic", Map.of(
                                "dynamicRegistration", true,
                                "relatedInformationSupport", true
                            )
                        ),
                        "workspace", Map.of(
                            "didChangeConfiguration", Map.of("dynamicRegistration", true),
                            "didChangeWatchedFiles", Map.of("dynamicRegistration", true)
                        )
                    ),
                    null
                );

                return client.initialize(params);
            })
            .thenAccept(result -> {
                status = LspServerManager.ServerStatus.RUNNING;

                // Register diagnostic handler
                client.onNotification("textDocument/publishDiagnostics", params -> {
                    handleDiagnostics(params);
                });
            })
            .exceptionally(error -> {
                status = LspServerManager.ServerStatus.CRASHED;
                throw new RuntimeException("Failed to start LSP server: " + name, error);
            });
    }

    /**
     * Stop the server.
     */
    public CompletableFuture<Void> stop() {
        if (status == LspServerManager.ServerStatus.STOPPED ||
            status == LspServerManager.ServerStatus.CRASHED) {
            return CompletableFuture.completedFuture(null);
        }

        status = LspServerManager.ServerStatus.STOPPING;

        if (client == null) {
            status = LspServerManager.ServerStatus.STOPPED;
            return CompletableFuture.completedFuture(null);
        }

        return client.stop()
            .thenRun(() -> {
                status = LspServerManager.ServerStatus.STOPPED;
                client = null;
            })
            .exceptionally(error -> {
                status = LspServerManager.ServerStatus.STOPPED;
                client = null;
                return null;
            });
    }

    /**
     * Open a text document.
     */
    public CompletableFuture<Void> openDocument(String uri, String languageId, String content) {
        if (client == null || status != LspServerManager.ServerStatus.RUNNING) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Server not running: " + name)
            );
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("textDocument", Map.of(
            "uri", uri,
            "languageId", languageId,
            "version", 1,
            "text", content
        ));

        return client.sendNotification("textDocument/didOpen", params);
    }

    /**
     * Close a text document.
     */
    public CompletableFuture<Void> closeDocument(String uri) {
        if (client == null || status != LspServerManager.ServerStatus.RUNNING) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Server not running: " + name)
            );
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("textDocument", Map.of("uri", uri));

        return client.sendNotification("textDocument/didClose", params);
    }

    /**
     * Update a text document.
     */
    public CompletableFuture<Void> changeDocument(String uri, int version, List<TextChange> changes) {
        if (client == null || status != LspServerManager.ServerStatus.RUNNING) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Server not running: " + name)
            );
        }

        List<Map<String, Object>> changeList = new ArrayList<>();
        for (TextChange change : changes) {
            Map<String, Object> changeMap = new LinkedHashMap<>();
            changeMap.put("range", Map.of(
                "start", Map.of("line", change.range().start().line(), "character", change.range().start().character()),
                "end", Map.of("line", change.range().end().line(), "character", change.range().end().character())
            ));
            changeMap.put("text", change.text());
            changeList.add(changeMap);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("textDocument", Map.of("uri", uri, "version", version));
        params.put("contentChanges", changeList);

        return client.sendNotification("textDocument/didChange", params);
    }

    /**
     * Save a text document.
     */
    public CompletableFuture<Void> saveDocument(String uri, String text) {
        if (client == null || status != LspServerManager.ServerStatus.RUNNING) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Server not running: " + name)
            );
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("textDocument", Map.of("uri", uri));

        if (text != null) {
            params.put("text", text);
        }

        return client.sendNotification("textDocument/didSave", params);
    }

    /**
     * Get completions.
     */
    public CompletableFuture<List<CompletionItem>> getCompletions(String uri, LspDiagnosticRegistry.Position position) {
        if (client == null || status != LspServerManager.ServerStatus.RUNNING) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Server not running: " + name)
            );
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("textDocument", Map.of("uri", uri));
        params.put("position", Map.of("line", position.line(), "character", position.character()));

        return client.<Map<String, Object>>sendRequest("textDocument/completion", params)
            .thenApply(result -> {
                // Parse completion items
                List<CompletionItem> items = new ArrayList<>();
                if (result != null && result.containsKey("items")) {
                    List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) result.get("items");
                    for (Map<String, Object> item : itemMaps) {
                        items.add(new CompletionItem(
                            (String) item.get("label"),
                            (String) item.getOrDefault("kind", "text"),
                            (String) item.get("detail"),
                            (String) item.get("documentation"),
                            (String) item.get("insertText")
                        ));
                    }
                }
                return items;
            });
    }

    /**
     * Handle diagnostics notification.
     */
    private void handleDiagnostics(Object params) {
        if (params instanceof Map) {
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String uri = (String) paramsMap.get("uri");
            List<Map<String, Object>> diagnostics =
                (List<Map<String, Object>>) paramsMap.get("diagnostics");

            List<LspDiagnosticRegistry.Diagnostic> diagnosticList = new ArrayList<>();
            for (Map<String, Object> d : diagnostics) {
                LspDiagnosticRegistry.DiagnosticSeverity severity =
                    LspDiagnosticRegistry.DiagnosticSeverity.fromValue(
                        ((Number) d.getOrDefault("severity", 3)).intValue()
                    );

                Map<String, Object> rangeMap = (Map<String, Object>) d.get("range");
                Map<String, Object> startMap = (Map<String, Object>) rangeMap.get("start");
                Map<String, Object> endMap = (Map<String, Object>) rangeMap.get("end");

                LspDiagnosticRegistry.Range range = new LspDiagnosticRegistry.Range(
                    new LspDiagnosticRegistry.Position(
                        ((Number) startMap.get("line")).intValue(),
                        ((Number) startMap.get("character")).intValue()
                    ),
                    new LspDiagnosticRegistry.Position(
                        ((Number) endMap.get("line")).intValue(),
                        ((Number) endMap.get("character")).intValue()
                    )
                );

                diagnosticList.add(new LspDiagnosticRegistry.Diagnostic(
                    uri,
                    severity,
                    (String) d.get("message"),
                    (String) d.get("source"),
                    d.get("code") != null ? d.get("code").toString() : null,
                    range,
                    null
                ));
            }

            LspDiagnosticRegistry.updateDiagnostics(uri, diagnosticList);
        }
    }

    /**
     * Text change.
     */
    public record TextChange(LspDiagnosticRegistry.Range range, String text) {}

    /**
     * Completion item.
     */
    public record CompletionItem(
        String label,
        String kind,
        String detail,
        String documentation,
        String insertText
    ) {}
}