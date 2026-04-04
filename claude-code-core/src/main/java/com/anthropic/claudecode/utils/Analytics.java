/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code analytics utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Analytics - Simple analytics logging for Claude Code.
 */
public final class Analytics {
    private Analytics() {}

    private static volatile boolean enabled = true;
    private static final List<Map<String, Object>> eventQueue = new CopyOnWriteArrayList<>();

    /**
     * Log an analytics event.
     */
    public static void logEvent(String eventName, Map<String, Object> metadata) {
        if (!enabled) return;

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", eventName);
        event.put("timestamp", System.currentTimeMillis());
        if (metadata != null) {
            event.putAll(metadata);
        }

        eventQueue.add(event);

        // In real implementation, would flush to analytics service
        if (eventQueue.size() > 100) {
            flush();
        }
    }

    /**
     * Log a simple event.
     */
    public static void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    /**
     * Set analytics enabled status.
     */
    public static void setEnabled(boolean enabled) {
        Analytics.enabled = enabled;
    }

    /**
     * Check if analytics is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Flush queued events.
     */
    public static void flush() {
        if (eventQueue.isEmpty()) return;

        // Copy events to send
        List<Map<String, Object>> eventsToSend = new ArrayList<>(eventQueue);
        eventQueue.clear();

        // Send to analytics service asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Check if telemetry is disabled
                if ("1".equals(System.getenv("CLAUDE_CODE_DISABLE_TELEMETRY")) ||
                    "true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_DISABLE_TELEMETRY"))) {
                    return;
                }

                // Get analytics endpoint
                String analyticsUrl = System.getenv("CLAUDE_CODE_ANALYTICS_URL");
                if (analyticsUrl == null) {
                    analyticsUrl = "https://stats.anthropic.com/v1/events";
                }

                // Get API key for auth
                String apiKey = System.getenv("ANTHROPIC_API_KEY");

                // Build JSON payload
                StringBuilder payload = new StringBuilder();
                payload.append("{\"events\":[");
                for (int i = 0; i < eventsToSend.size(); i++) {
                    if (i > 0) payload.append(",");
                    payload.append(mapToJson(eventsToSend.get(i)));
                }
                payload.append("]}");

                // Send HTTP POST
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(analyticsUrl))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload.toString()));

                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.header("x-api-key", apiKey);
                }

                java.net.http.HttpRequest request = requestBuilder.build();
                httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());

            } catch (Exception e) {
                // Ignore errors in analytics
            }
        });
    }

    /**
     * Convert map to JSON string.
     */
    private static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(valueToJson(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert value to JSON.
     */
    private static String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map) {
            return mapToJson((Map<String, Object>) value);
        }
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(valueToJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(value.toString()) + "\"";
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
     * Get queued event count.
     */
    public static int getQueuedEventCount() {
        return eventQueue.size();
    }

    /**
     * Clear all queued events.
     */
    public static void clear() {
        eventQueue.clear();
    }
}