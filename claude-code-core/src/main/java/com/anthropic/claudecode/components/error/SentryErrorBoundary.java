/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/SentryErrorBoundary
 */
package com.anthropic.claudecode.components.error;

import java.util.*;
import java.util.concurrent.*;
import java.time.*;

/**
 * Sentry error boundary - Error handling and reporting.
 */
public final class SentryErrorBoundary {
    private static volatile boolean enabled = true;
    private static volatile String dsn = null;
    private static volatile String environment = "production";
    private static volatile String release = "1.0.0";
    private static final List<ErrorListener> listeners = new CopyOnWriteArrayList<>();
    private static final CircularBuffer<ErrorEvent> recentErrors = new CircularBuffer<>(100);
    private static final CircularBuffer<Breadcrumb> breadcrumbs = new CircularBuffer<>(100);
    private static volatile Map<String, Object> userContext = new ConcurrentHashMap<>();

    /**
     * Error event record.
     */
    public record ErrorEvent(
        String eventId,
        String message,
        String type,
        String stackTrace,
        Map<String, Object> context,
        Map<String, String> tags,
        Instant timestamp,
        String user,
        String sessionId,
        Severity severity
    ) {
        public static ErrorEvent of(Throwable error) {
            return new ErrorEvent(
                UUID.randomUUID().toString(),
                error.getMessage(),
                error.getClass().getName(),
                getStackTrace(error),
                new HashMap<>(),
                new HashMap<>(),
                Instant.now(),
                null,
                null,
                Severity.ERROR
            );
        }

        public ErrorEvent withContext(String key, Object value) {
            Map<String, Object> newContext = new HashMap<>(context);
            newContext.put(key, value);
            return new ErrorEvent(eventId, message, type, stackTrace, newContext, tags, timestamp, user, sessionId, severity);
        }

        public ErrorEvent withTag(String key, String value) {
            Map<String, String> newTags = new HashMap<>(tags);
            newTags.put(key, value);
            return new ErrorEvent(eventId, message, type, stackTrace, context, newTags, timestamp, user, sessionId, severity);
        }

        private static String getStackTrace(Throwable error) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : error.getStackTrace()) {
                sb.append(element.toString()).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Severity enum.
     */
    public enum Severity {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        FATAL
    }

    /**
     * Initialize Sentry.
     */
    public static void init(String dsn, String environment, String release) {
        SentryErrorBoundary.dsn = dsn;
        SentryErrorBoundary.environment = environment;
        SentryErrorBoundary.release = release;
    }

    /**
     * Set enabled.
     */
    public static void setEnabled(boolean enabled) {
        SentryErrorBoundary.enabled = enabled;
    }

    /**
     * Capture exception.
     */
    public static String captureException(Throwable error) {
        if (!enabled) return null;

        ErrorEvent event = ErrorEvent.of(error);
        event = event.withTag("environment", environment)
                     .withTag("release", release);

        recentErrors.add(event);
        notifyListeners(event);

        // Would send to Sentry
        if (dsn != null) {
            sendToSentry(event);
        }

        return event.eventId();
    }

    /**
     * Capture message.
     */
    public static String captureMessage(String message, Severity severity) {
        if (!enabled) return null;

        ErrorEvent event = new ErrorEvent(
            UUID.randomUUID().toString(),
            message,
            "message",
            null,
            new HashMap<>(),
            Map.of("environment", environment, "release", release),
            Instant.now(),
            null,
            null,
            severity
        );

        recentErrors.add(event);
        notifyListeners(event);

        if (dsn != null) {
            sendToSentry(event);
        }

        return event.eventId();
    }

    /**
     * Add breadcrumb.
     */
    public static void addBreadcrumb(String message, String category) {
        Breadcrumb breadcrumb = new Breadcrumb(message, category, Instant.now());
        breadcrumbs.add(breadcrumb);
    }

    /**
     * Set user context.
     */
    public static void setUser(String userId, Map<String, Object> userData) {
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put("id", userId);
        if (userData != null) {
            context.putAll(userData);
        }
        userContext = context;
    }

    /**
     * Get user context.
     */
    public static Map<String, Object> getUserContext() {
        return new HashMap<>(userContext);
    }

    /**
     * Add listener.
     */
    public static void addListener(ErrorListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public static void removeListener(ErrorListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(ErrorEvent event) {
        for (ErrorListener listener : listeners) {
            listener.onError(event);
        }
    }

    private static void sendToSentry(ErrorEvent event) {
        if (dsn == null || dsn.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Parse DSN to get Sentry endpoint
                SentryEndpoint endpoint = parseDsn(dsn);

                // Build Sentry event payload
                String payload = buildSentryPayload(event);

                // Send to Sentry API
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(endpoint.url))
                    .header("Content-Type", "application/json")
                    .header("X-Sentry-Auth", buildAuthHeader(endpoint, event.eventId()))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                    .build();

                httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                // Silently fail - error reporting should not break the app
            }
        });
    }

    /**
     * Parse DSN into endpoint components.
     */
    private static SentryEndpoint parseDsn(String dsn) {
        // DSN format: https://key@host/project_id
        try {
            java.net.URI uri = java.net.URI.create(dsn);
            String host = uri.getHost();
            String key = uri.getUserInfo();
            String path = uri.getPath();
            // Project ID is the last part of the path
            String projectId = path.substring(path.lastIndexOf('/') + 1);

            return new SentryEndpoint(
                "https://" + host + "/api/" + projectId + "/envelope/",
                key,
                projectId
            );
        } catch (Exception e) {
            return new SentryEndpoint(dsn, "", "");
        }
    }

    /**
     * Build Sentry auth header.
     */
    private static String buildAuthHeader(SentryEndpoint endpoint, String eventId) {
        return "Sentry sentry_version=7," +
               "sentry_client=claude-code-java/1.0," +
               "sentry_key=" + endpoint.key + "," +
               "sentry_time=" + System.currentTimeMillis();
    }

    /**
     * Build Sentry event payload.
     */
    private static String buildSentryPayload(ErrorEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"event_id\":\"").append(event.eventId()).append("\",");
        sb.append("\"timestamp\":").append(event.timestamp().getEpochSecond()).append(",");
        sb.append("\"platform\":\"java\",");
        sb.append("\"environment\":\"").append(escapeJson(environment)).append("\",");
        sb.append("\"release\":\"").append(escapeJson(release)).append("\",");

        // Message
        if (event.message() != null) {
            sb.append("\"message\":\"").append(escapeJson(event.message())).append("\",");
        }

        // Exception
        if (event.type() != null) {
            sb.append("\"exception\":{\"values\":[{");
            sb.append("\"type\":\"").append(escapeJson(event.type())).append("\",");
            sb.append("\"value\":\"").append(escapeJson(event.message() != null ? event.message() : "")).append("\"");
            if (event.stackTrace() != null) {
                sb.append(",\"stacktrace\":{\"frames\":[]}");
            }
            sb.append("}]},");
        }

        // Tags
        if (!event.tags().isEmpty()) {
            sb.append("\"tags\":{");
            boolean first = true;
            for (Map.Entry<String, String> tag : event.tags().entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(tag.getKey())).append("\":\"").append(escapeJson(tag.getValue())).append("\"");
                first = false;
            }
            sb.append("},");
        }

        // User context
        if (!userContext.isEmpty()) {
            sb.append("\"user\":{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : userContext.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"").append(escapeJson(String.valueOf(entry.getValue()))).append("\"");
                first = false;
            }
            sb.append("},");
        }

        // Breadcrumbs
        List<Breadcrumb> bcs = breadcrumbs.toList();
        if (!bcs.isEmpty()) {
            sb.append("\"breadcrumbs\":[");
            for (int i = 0; i < bcs.size(); i++) {
                if (i > 0) sb.append(",");
                Breadcrumb b = bcs.get(i);
                sb.append("{\"message\":\"").append(escapeJson(b.message)).append("\",");
                sb.append("\"category\":\"").append(escapeJson(b.category)).append("\",");
                sb.append("\"timestamp\":\"").append(b.timestamp).append("\"}");
            }
            sb.append("],");
        }

        // Remove trailing comma
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }

        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Sentry endpoint record.
     */
    private record SentryEndpoint(String url, String key, String projectId) {}

    /**
     * Breadcrumb record.
     */
    private record Breadcrumb(String message, String category, Instant timestamp) {}

    /**
     * Get recent errors.
     */
    public static List<ErrorEvent> getRecentErrors() {
        return recentErrors.toList();
    }

    /**
     * Error listener interface.
     */
    public interface ErrorListener {
        void onError(ErrorEvent event);
    }

    /**
     * Circular buffer implementation.
     */
    private static class CircularBuffer<T> {
        private final T[] buffer;
        private int head = 0;
        private int size = 0;

        @SuppressWarnings("unchecked")
        CircularBuffer(int capacity) {
            this.buffer = (T[]) new Object[capacity];
        }

        void add(T item) {
            buffer[head] = item;
            head = (head + 1) % buffer.length;
            if (size < buffer.length) size++;
        }

        List<T> toList() {
            List<T> result = new ArrayList<>();
            int start = head - size;
            if (start < 0) start += buffer.length;

            for (int i = 0; i < size; i++) {
                int idx = (start + i) % buffer.length;
                if (buffer[idx] != null) {
                    result.add(buffer[idx]);
                }
            }

            return result;
        }
    }

    /**
     * Error boundary config.
     */
    public record ErrorBoundaryConfig(
        boolean enabled,
        String dsn,
        String environment,
        String release,
        int maxBreadcrumbs,
        double sampleRate,
        boolean attachStacktrace
    ) {
        public static ErrorBoundaryConfig defaultConfig() {
            return new ErrorBoundaryConfig(true, null, "production", "1.0.0", 100, 1.0, true);
        }
    }
}