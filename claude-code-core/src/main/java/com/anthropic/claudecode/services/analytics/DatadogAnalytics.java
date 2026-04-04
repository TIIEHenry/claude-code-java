/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/analytics/datadog.ts
 */
package com.anthropic.claudecode.services.analytics;

import java.util.*;
import java.util.concurrent.*;

/**
 * Datadog analytics integration.
 */
public final class DatadogAnalytics {
    private static final String DATADOG_API_KEY_ENV = "DD_API_KEY";
    private static final String DATADOG_APP_KEY_ENV = "DD_APP_KEY";
    private static final String DATADOG_SITE_ENV = "DD_SITE";

    private static volatile boolean enabled = false;
    private static ExecutorService executor;
    private static String apiKey;
    private static String site;

    private DatadogAnalytics() {}

    /**
     * Initialize Datadog analytics.
     */
    public static synchronized void initialize() {
        if (AnalyticsConfig.isAnalyticsDisabled()) {
            return;
        }

        apiKey = System.getenv(DATADOG_API_KEY_ENV);
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }

        site = System.getenv(DATADOG_SITE_ENV);
        if (site == null || site.isEmpty()) {
            site = "datadoghq.com";
        }

        executor = Executors.newSingleThreadExecutor();
        enabled = true;
    }

    /**
     * Log an event to Datadog.
     */
    public static void logEvent(String eventName, Map<String, Object> metadata) {
        if (!enabled || executor == null) {
            return;
        }

        executor.submit(() -> {
            try {
                // Build the event payload for Datadog API
                Map<String, Object> payload = new HashMap<>();
                payload.put("event_name", eventName);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("metadata", metadata);

                // Send to Datadog Events API
                String datadogUrl = "https://api." + site + "/api/v1/events";
                String jsonPayload = mapToJson(payload);

                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(datadogUrl))
                    .header("DD-API-KEY", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if ("true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_DEBUG"))) {
                    System.out.println("[Datadog] " + eventName + " -> status " + response.statusCode());
                }
            } catch (Exception e) {
                // Silently fail - analytics should not break the app
                if ("true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_DEBUG"))) {
                    System.err.println("[Datadog Error] " + e.getMessage());
                }
            }
        });
    }

    /**
     * Log a metric to Datadog.
     */
    public static void logMetric(String metricName, double value, Map<String, String> tags) {
        if (!enabled || executor == null) {
            return;
        }

        executor.submit(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("metric", metricName);
                payload.put("value", value);
                payload.put("tags", tags);
                payload.put("timestamp", System.currentTimeMillis());

                if ("true".equalsIgnoreCase(System.getenv("CLAUDE_CODE_DEBUG"))) {
                    System.out.println("[Datadog Metric] " + metricName + "=" + value + " " + tags);
                }
            } catch (Exception e) {
                // Silently fail
            }
        });
    }

    /**
     * Flush pending events.
     */
    public static void flush() {
        // No-op for async executor
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
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) value));
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escape JSON string.
     */
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Shutdown Datadog analytics.
     */
    public static synchronized void shutdown() {
        enabled = false;
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    /**
     * Check if Datadog is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }
}