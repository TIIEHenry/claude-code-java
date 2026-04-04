/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/lsp/passiveFeedback.ts
 */
package com.anthropic.claudecode.services.lsp;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import com.anthropic.claudecode.utils.Debug;

/**
 * LSP Passive Feedback - handles diagnostic notifications from LSP servers.
 */
public final class LspPassiveFeedback {
    private LspPassiveFeedback() {}

    /**
     * Diagnostic severity enum.
     */
    public enum DiagnosticSeverity {
        ERROR("Error"),
        WARNING("Warning"),
        INFO("Info"),
        HINT("Hint");

        private final String value;

        DiagnosticSeverity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Diagnostic range.
     */
    public record DiagnosticRange(
        Position start,
        Position end
    ) {}

    /**
     * Position.
     */
    public record Position(
        int line,
        int character
    ) {}

    /**
     * Diagnostic.
     */
    public record Diagnostic(
        String message,
        DiagnosticSeverity severity,
        DiagnosticRange range,
        String source,
        String code
    ) {}

    /**
     * Diagnostic file.
     */
    public record DiagnosticFile(
        String uri,
        List<Diagnostic> diagnostics
    ) {}

    /**
     * Publish diagnostics params.
     */
    public record PublishDiagnosticsParams(
        String uri,
        List<RawDiagnostic> diagnostics
    ) {}

    /**
     * Raw diagnostic from LSP.
     */
    public record RawDiagnostic(
        String message,
        Integer severity,
        RawRange range,
        String source,
        Object code
    ) {}

    /**
     * Raw range from LSP.
     */
    public record RawRange(
        Position start,
        Position end
    ) {}

    /**
     * Handler registration result.
     */
    public record HandlerRegistrationResult(
        int totalServers,
        int successCount,
        List<RegistrationError> registrationErrors,
        Map<String, FailureTracker> diagnosticFailures
    ) {}

    /**
     * Registration error.
     */
    public record RegistrationError(
        String serverName,
        String error
    ) {}

    /**
     * Failure tracker.
     */
    public record FailureTracker(
        int count,
        String lastError
    ) {}

    /**
     * Map LSP severity to Claude diagnostic severity.
     *
     * LSP DiagnosticSeverity enum:
     * 1 = Error, 2 = Warning, 3 = Information, 4 = Hint
     */
    public static DiagnosticSeverity mapLSPSeverity(Integer lspSeverity) {
        if (lspSeverity == null) {
            return DiagnosticSeverity.ERROR;
        }

        return switch (lspSeverity) {
            case 1 -> DiagnosticSeverity.ERROR;
            case 2 -> DiagnosticSeverity.WARNING;
            case 3 -> DiagnosticSeverity.INFO;
            case 4 -> DiagnosticSeverity.HINT;
            default -> DiagnosticSeverity.ERROR;
        };
    }

    /**
     * Convert LSP diagnostics to Claude diagnostic format.
     */
    public static List<DiagnosticFile> formatDiagnosticsForAttachment(PublishDiagnosticsParams params) {
        String uri = params.uri();

        // Handle file:// URIs
        if (uri.startsWith("file://")) {
            uri = uri.substring(7);
            // On Windows, file:///C:/path becomes /C:/path - strip leading slash
            if (uri.length() > 2 && uri.charAt(0) == '/' && uri.charAt(2) == ':') {
                uri = uri.substring(1);
            }
        }

        List<Diagnostic> diagnostics = new ArrayList<>();
        for (RawDiagnostic diag : params.diagnostics()) {
            DiagnosticSeverity severity = mapLSPSeverity(diag.severity());

            DiagnosticRange range = null;
            if (diag.range() != null) {
                range = new DiagnosticRange(
                    diag.range().start(),
                    diag.range().end()
                );
            }

            String code = diag.code() != null ? String.valueOf(diag.code()) : null;

            diagnostics.add(new Diagnostic(
                diag.message(),
                severity,
                range,
                diag.source(),
                code
            ));
        }

        return List.of(new DiagnosticFile(uri, diagnostics));
    }

    /**
     * Register LSP notification handlers on all servers.
     */
    public static HandlerRegistrationResult registerLSPNotificationHandlers(LspServerManager manager) {
        Map<String, LspServerInstance> servers = manager.getAllServers();

        List<RegistrationError> registrationErrors = new ArrayList<>();
        int successCount = 0;
        Map<String, FailureTracker> diagnosticFailures = new ConcurrentHashMap<>();

        for (Map.Entry<String, LspServerInstance> entry : servers.entrySet()) {
            String serverName = entry.getKey();
            LspServerInstance serverInstance = entry.getValue();

            try {
                if (serverInstance == null) {
                    registrationErrors.add(new RegistrationError(serverName, "Server instance is null"));
                    continue;
                }

                // Register notification handler
                serverInstance.onNotification("textDocument/publishDiagnostics", params -> {
                    handleDiagnostics(serverName, params, diagnosticFailures);
                });

                Debug.logForDebugging("Registered diagnostics handler for " + serverName);
                successCount++;

            } catch (Exception e) {
                registrationErrors.add(new RegistrationError(serverName, e.getMessage()));
                Debug.logForDebugging("Failed to register diagnostics handler for " + serverName + ": " + e.getMessage());
            }
        }

        int totalServers = servers.size();
        if (!registrationErrors.isEmpty()) {
            Debug.logForDebugging("LSP notification handler registration: " + successCount + "/" + totalServers + " succeeded");
        } else {
            Debug.logForDebugging("LSP notification handlers registered successfully for all " + totalServers + " server(s)");
        }

        return new HandlerRegistrationResult(totalServers, successCount, registrationErrors, diagnosticFailures);
    }

    private static void handleDiagnostics(String serverName, Object params, Map<String, FailureTracker> diagnosticFailures) {
        Debug.logForDebugging("[PASSIVE DIAGNOSTICS] Handler invoked for " + serverName);

        try {
            // Validate params
            if (!(params instanceof Map)) {
                Debug.logForDebugging("Invalid diagnostic params from " + serverName);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;

            if (!paramsMap.containsKey("uri") || !paramsMap.containsKey("diagnostics")) {
                Debug.logForDebugging("Invalid diagnostic params from " + serverName + " (missing uri or diagnostics)");
                return;
            }

            String uri = (String) paramsMap.get("uri");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawDiagnostics = (List<Map<String, Object>>) paramsMap.get("diagnostics");

            Debug.logForDebugging("Received diagnostics from " + serverName + ": " + rawDiagnostics.size() + " diagnostic(s) for " + uri);

            // Convert to Claude format
            List<RawDiagnostic> diagnostics = new ArrayList<>();
            for (Map<String, Object> diag : rawDiagnostics) {
                diagnostics.add(new RawDiagnostic(
                    (String) diag.get("message"),
                    (Integer) diag.get("severity"),
                    parseRange(diag.get("range")),
                    (String) diag.get("source"),
                    diag.get("code")
                ));
            }

            PublishDiagnosticsParams diagnosticParams = new PublishDiagnosticsParams(uri, diagnostics);
            List<DiagnosticFile> diagnosticFiles = formatDiagnosticsForAttachment(diagnosticParams);

            if (diagnosticFiles.isEmpty() || diagnosticFiles.get(0).diagnostics().isEmpty()) {
                Debug.logForDebugging("Skipping empty diagnostics from " + serverName);
                return;
            }

            // Register diagnostics for async delivery
            LspDiagnosticRegistry.registerPendingLSPDiagnostic(serverName, diagnosticFiles);

            Debug.logForDebugging("LSP Diagnostics: Registered " + diagnosticFiles.size() + " diagnostic file(s) from " + serverName);

            // Reset failure counter
            diagnosticFailures.remove(serverName);

        } catch (Exception e) {
            Debug.logForDebugging("Error processing diagnostics from " + serverName + ": " + e.getMessage());

            // Track failures
            FailureTracker failures = diagnosticFailures.get(serverName);
            if (failures == null) {
                failures = new FailureTracker(0, "");
            }

            failures = new FailureTracker(failures.count() + 1, e.getMessage());
            diagnosticFailures.put(serverName, failures);

            if (failures.count() >= 3) {
                Debug.logForDebugging("WARNING: LSP diagnostic handler for " + serverName +
                    " has failed " + failures.count() + " times consecutively");
            }
        }
    }

    private static RawRange parseRange(Object rangeObj) {
        if (!(rangeObj instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> range = (Map<String, Object>) rangeObj;
        Object startObj = range.get("start");
        Object endObj = range.get("end");

        if (!(startObj instanceof Map) || !(endObj instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> start = (Map<String, Object>) startObj;
        @SuppressWarnings("unchecked")
        Map<String, Object> end = (Map<String, Object>) endObj;

        return new RawRange(
            new Position(
                ((Number) start.get("line")).intValue(),
                ((Number) start.get("character")).intValue()
            ),
            new Position(
                ((Number) end.get("line")).intValue(),
                ((Number) end.get("character")).intValue()
            )
        );
    }
}