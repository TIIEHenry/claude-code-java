/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/lsp/LSPDiagnosticRegistry.ts
 */
package com.anthropic.claudecode.services.lsp;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * LSP diagnostic registry for collecting and managing diagnostics.
 */
public final class LspDiagnosticRegistry {
    private LspDiagnosticRegistry() {}

    /**
     * Diagnostic severity levels.
     */
    public enum DiagnosticSeverity {
        ERROR(1),
        WARNING(2),
        INFORMATION(3),
        HINT(4);

        private final int value;
        DiagnosticSeverity(int value) { this.value = value; }
        public int getValue() { return value; }

        public static DiagnosticSeverity fromValue(int value) {
            return switch (value) {
                case 1 -> ERROR;
                case 2 -> WARNING;
                case 3 -> INFORMATION;
                case 4 -> HINT;
                default -> INFORMATION;
            };
        }
    }

    /**
     * Diagnostic from LSP server.
     */
    public record Diagnostic(
        String uri,           // file URI
        DiagnosticSeverity severity,
        String message,
        String source,        // diagnostic source (e.g., "typescript")
        String code,          // diagnostic code
        Range range,          // location in file
        List<DiagnosticRelatedInfo> relatedInformation
    ) {}

    /**
     * Range in a file.
     */
    public record Range(Position start, Position end) {}

    /**
     * Position in a file.
     */
    public record Position(int line, int character) {}

    /**
     * Related diagnostic information.
     */
    public record DiagnosticRelatedInfo(
        String uri,
        Range range,
        String message
    ) {}

    /**
     * Diagnostic change event.
     */
    public record DiagnosticChangeEvent(
        String uri,
        List<Diagnostic> diagnostics
    ) {}

    // Registry state
    private static final ConcurrentHashMap<String, List<Diagnostic>> diagnosticsByUri =
        new ConcurrentHashMap<>();

    private static final List<Consumer<DiagnosticChangeEvent>> changeListeners =
        new CopyOnWriteArrayList<>();

    /**
     * Register a change listener.
     */
    public static void addChangeListener(Consumer<DiagnosticChangeEvent> listener) {
        changeListeners.add(listener);
    }

    /**
     * Remove a change listener.
     */
    public static void removeChangeListener(Consumer<DiagnosticChangeEvent> listener) {
        changeListeners.remove(listener);
    }

    /**
     * Update diagnostics for a URI.
     */
    public static void updateDiagnostics(String uri, List<Diagnostic> newDiagnostics) {
        List<Diagnostic> previous = diagnosticsByUri.put(uri, newDiagnostics);

        // Notify listeners
        DiagnosticChangeEvent event = new DiagnosticChangeEvent(uri, newDiagnostics);
        for (Consumer<DiagnosticChangeEvent> listener : changeListeners) {
            listener.accept(event);
        }
    }

    /**
     * Get diagnostics for a URI.
     */
    public static List<Diagnostic> getDiagnostics(String uri) {
        return diagnosticsByUri.getOrDefault(uri, Collections.emptyList());
    }

    /**
     * Get all diagnostics.
     */
    public static Map<String, List<Diagnostic>> getAllDiagnostics() {
        return new HashMap<>(diagnosticsByUri);
    }

    /**
     * Clear diagnostics for a URI.
     */
    public static void clearDiagnostics(String uri) {
        diagnosticsByUri.remove(uri);
        DiagnosticChangeEvent event = new DiagnosticChangeEvent(uri, Collections.emptyList());
        for (Consumer<DiagnosticChangeEvent> listener : changeListeners) {
            listener.accept(event);
        }
    }

    /**
     * Clear all diagnostics.
     */
    public static void clearAllDiagnostics() {
        for (String uri : new HashSet<>(diagnosticsByUri.keySet())) {
            clearDiagnostics(uri);
        }
    }

    /**
     * Register pending LSP diagnostic (from passive feedback).
     */
    public static void registerPendingLSPDiagnostic(String serverName, List<LspPassiveFeedback.DiagnosticFile> files) {
        for (LspPassiveFeedback.DiagnosticFile file : files) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            for (LspPassiveFeedback.Diagnostic d : file.diagnostics()) {
                DiagnosticSeverity severity = DiagnosticSeverity.fromValue(
                    d.severity() == LspPassiveFeedback.DiagnosticSeverity.ERROR ? 1 :
                    d.severity() == LspPassiveFeedback.DiagnosticSeverity.WARNING ? 2 :
                    d.severity() == LspPassiveFeedback.DiagnosticSeverity.INFO ? 3 : 4
                );

                Range range = null;
                if (d.range() != null) {
                    range = new Range(
                        new Position(d.range().start().line(), d.range().start().character()),
                        new Position(d.range().end().line(), d.range().end().character())
                    );
                }

                diagnostics.add(new Diagnostic(
                    file.uri(),
                    severity,
                    d.message(),
                    d.source(),
                    d.code(),
                    range,
                    null
                ));
            }
            updateDiagnostics(file.uri(), diagnostics);
        }
    }

    /**
     * Get error count.
     */
    public static int getErrorCount() {
        int count = 0;
        for (List<Diagnostic> diagnostics : diagnosticsByUri.values()) {
            for (Diagnostic d : diagnostics) {
                if (d.severity() == DiagnosticSeverity.ERROR) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Get warning count.
     */
    public static int getWarningCount() {
        int count = 0;
        for (List<Diagnostic> diagnostics : diagnosticsByUri.values()) {
            for (Diagnostic d : diagnostics) {
                if (d.severity() == DiagnosticSeverity.WARNING) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Format diagnostics as string.
     */
    public static String formatDiagnostics(String uri) {
        List<Diagnostic> diagnostics = getDiagnostics(uri);
        if (diagnostics.isEmpty()) {
            return "No diagnostics for " + uri;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Diagnostics for ").append(uri).append(":\n");

        for (Diagnostic d : diagnostics) {
            sb.append(String.format("  [%s] %s",
                d.severity().name().toLowerCase(),
                d.message()));

            if (d.range() != null) {
                sb.append(String.format(" (line %d, col %d)",
                    d.range().start().line(),
                    d.range().start().character()));
            }

            if (d.source() != null) {
                sb.append(" [").append(d.source()).append("]");
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}