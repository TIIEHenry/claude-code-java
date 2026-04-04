/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/diagnosticTracking
 */
package com.anthropic.claudecode.services.diagnostics;

import java.util.*;
import java.util.concurrent.*;
import java.time.*;

/**
 * Diagnostic tracking service - Track diagnostic events.
 */
public final class DiagnosticTrackingService {
    private final Map<String, DiagnosticEvent> events = new ConcurrentHashMap<>();
    private final List<DiagnosticListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Diagnostic event record.
     */
    public record DiagnosticEvent(
        String id,
        String type,
        String category,
        String message,
        Map<String, Object> data,
        Instant timestamp,
        DiagnosticSeverity severity,
        String source
    ) {
        public static DiagnosticEvent of(String type, String message) {
            return new DiagnosticEvent(
                UUID.randomUUID().toString(),
                type,
                "general",
                message,
                new HashMap<>(),
                Instant.now(),
                DiagnosticSeverity.INFO,
                "system"
            );
        }

        public DiagnosticEvent withData(String key, Object value) {
            Map<String, Object> newData = new HashMap<>(data);
            newData.put(key, value);
            return new DiagnosticEvent(id, type, category, message, newData, timestamp, severity, source);
        }

        public DiagnosticEvent withSeverity(DiagnosticSeverity sev) {
            return new DiagnosticEvent(id, type, category, message, data, timestamp, sev, source);
        }
    }

    /**
     * Diagnostic severity enum.
     */
    public enum DiagnosticSeverity {
        DEBUG(0),
        INFO(1),
        WARNING(2),
        ERROR(3),
        CRITICAL(4);

        private final int level;

        DiagnosticSeverity(int level) {
            this.level = level;
        }

        public int getLevel() { return level; }
    }

    /**
     * Track event.
     */
    public void trackEvent(String type, String message) {
        DiagnosticEvent event = DiagnosticEvent.of(type, message);
        events.put(event.id(), event);
        notifyListeners(event);
    }

    /**
     * Track event with data.
     */
    public void trackEvent(String type, String message, Map<String, Object> data) {
        DiagnosticEvent event = DiagnosticEvent.of(type, message);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            event = event.withData(entry.getKey(), entry.getValue());
        }
        events.put(event.id(), event);
        notifyListeners(event);
    }

    /**
     * Track error.
     */
    public void trackError(String type, String message, Throwable error) {
        DiagnosticEvent event = DiagnosticEvent.of(type, message)
            .withSeverity(DiagnosticSeverity.ERROR)
            .withData("exception", error.getClass().getName())
            .withData("stackTrace", getStackTrace(error));

        events.put(event.id(), event);
        notifyListeners(event);
    }

    /**
     * Get stack trace.
     */
    private String getStackTrace(Throwable error) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Get event by ID.
     */
    public DiagnosticEvent getEvent(String id) {
        return events.get(id);
    }

    /**
     * Get all events.
     */
    public List<DiagnosticEvent> getAllEvents() {
        return new ArrayList<>(events.values());
    }

    /**
     * Get events by type.
     */
    public List<DiagnosticEvent> getEventsByType(String type) {
        return events.values()
            .stream()
            .filter(e -> e.type().equals(type))
            .toList();
    }

    /**
     * Get events by severity.
     */
    public List<DiagnosticEvent> getEventsBySeverity(DiagnosticSeverity severity) {
        return events.values()
            .stream()
            .filter(e -> e.severity() == severity)
            .toList();
    }

    /**
     * Clear events.
     */
    public void clearEvents() {
        events.clear();
    }

    /**
     * Add listener.
     */
    public void addListener(DiagnosticListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(DiagnosticListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(DiagnosticEvent event) {
        for (DiagnosticListener listener : listeners) {
            listener.onDiagnostic(event);
        }
    }

    /**
     * Diagnostic listener interface.
     */
    public interface DiagnosticListener {
        void onDiagnostic(DiagnosticEvent event);
    }

    /**
     * Diagnostic summary record.
     */
    public record DiagnosticSummary(
        int totalEvents,
        Map<DiagnosticSeverity, Integer> countBySeverity,
        Map<String, Integer> countByType,
        Instant firstEvent,
        Instant lastEvent
    ) {
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("Total: ").append(totalEvents).append("\n");
            sb.append("By severity:\n");
            countBySeverity.forEach((sev, count) ->
                sb.append("  ").append(sev).append(": ").append(count).append("\n")
            );
            return sb.toString();
        }
    }

    /**
     * Get summary.
     */
    public DiagnosticSummary getSummary() {
        Map<DiagnosticSeverity, Integer> bySeverity = new HashMap<>();
        Map<String, Integer> byType = new HashMap<>();

        Instant first = null;
        Instant last = null;

        for (DiagnosticEvent event : events.values()) {
            bySeverity.merge(event.severity(), 1, Integer::sum);
            byType.merge(event.type(), 1, Integer::sum);

            if (first == null || event.timestamp().isBefore(first)) {
                first = event.timestamp();
            }
            if (last == null || event.timestamp().isAfter(last)) {
                last = event.timestamp();
            }
        }

        return new DiagnosticSummary(events.size(), bySeverity, byType, first, last);
    }

    /**
     * Shutdown service.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}