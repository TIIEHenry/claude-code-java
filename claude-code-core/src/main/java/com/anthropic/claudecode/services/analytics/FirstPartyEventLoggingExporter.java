/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/analytics/firstPartyEventLoggingExporter
 */
package com.anthropic.claudecode.services.analytics;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.time.*;

/**
 * First party event logging exporter - Exports events to internal API.
 */
public final class FirstPartyEventLoggingExporter {
    private final int maxBatchSize;
    private final boolean skipAuth;
    private final int maxAttempts;
    private final String path;
    private final String baseUrl;
    private final java.util.function.Supplier<Boolean> isKilled;

    private final HttpClient httpClient;
    private final List<Map<String, Object>> batch;
    private final ScheduledExecutorService scheduler;

    /**
     * Create exporter.
     */
    public FirstPartyEventLoggingExporter(
        int maxBatchSize,
        Boolean skipAuth,
        Integer maxAttempts,
        String path,
        String baseUrl,
        java.util.function.Supplier<Boolean> isKilled
    ) {
        this.maxBatchSize = maxBatchSize > 0 ? maxBatchSize : 200;
        this.skipAuth = skipAuth != null && skipAuth;
        this.maxAttempts = maxAttempts != null ? maxAttempts : 3;
        this.path = path != null ? path : "/api/event_logging/batch";
        this.baseUrl = baseUrl != null ? baseUrl : getOAuthBaseUrl();
        this.isKilled = isKilled;

        this.batch = new ArrayList<>();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Export a batch of events.
     */
    public CompletableFuture<Void> export(List<Map<String, Object>> events) {
        if (isKilled.get()) {
            return CompletableFuture.completedFuture(null);
        }

        if (events.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                try {
                    String json = serializeEvents(events);

                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json));

                    if (!skipAuth) {
                        String auth = getAuthHeader();
                        if (auth != null) {
                            requestBuilder.header("Authorization", auth);
                        }
                    }

                    HttpRequest request = requestBuilder.build();
                    HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return null;
                    }

                    if (response.statusCode() >= 400 && response.statusCode() < 500) {
                        // Client error - don't retry
                        return null;
                    }

                    // Server error - retry with backoff
                    Thread.sleep(1000 * (attempt + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                } catch (Exception e) {
                    // Retry
                    try {
                        Thread.sleep(1000 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }

            // All retries failed - write to disk for later retry
            writeToDisk(events);
            return null;
        });
    }

    /**
     * Serialize events to JSON.
     */
    private String serializeEvents(List<Map<String, Object>> events) {
        StringBuilder sb = new StringBuilder("{\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializeEvent(events.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Serialize single event.
     */
    private String serializeEvent(Map<String, Object> event) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : event.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(serializeValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serialize value.
     */
    private String serializeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map) {
            return serializeEvent((Map<String, Object>) value);
        }
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(serializeValue(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    /**
     * Escape JSON string.
     */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Write events to disk for later retry.
     */
    private void writeToDisk(List<Map<String, Object>> events) {
        try {
            java.nio.file.Path batchPath = getCurrentBatchFilePath();
            java.nio.file.Files.createDirectories(batchPath.getParent());

            // Append events to existing batch file
            String json = serializeEvents(events);
            if (java.nio.file.Files.exists(batchPath)) {
                String existing = java.nio.file.Files.readString(batchPath);
                // Merge with existing events
                json = mergeEventJson(existing, json);
            }
            java.nio.file.Files.writeString(batchPath, json);
        } catch (Exception e) {
            // Silently fail - logging should not break the app
        }
    }

    /**
     * Get current batch file path.
     */
    private java.nio.file.Path getCurrentBatchFilePath() {
        String home = System.getProperty("user.home");
        return java.nio.file.Paths.get(home, ".claude", "pending-events.json");
    }

    /**
     * Merge event JSON strings.
     */
    private String mergeEventJson(String existing, String newJson) {
        try {
            // Extract events arrays from both JSON strings
            int existingStart = existing.indexOf("\"events\":");
            int newStart = newJson.indexOf("\"events\":");

            if (existingStart < 0 || newStart < 0) return newJson;

            int existingArrStart = existing.indexOf("[", existingStart);
            int newArrStart = newJson.indexOf("[", newStart);

            if (existingArrStart < 0 || newArrStart < 0) return newJson;

            // Find matching closing braces
            int existingDepth = 1;
            int existingArrEnd = existingArrStart + 1;
            while (existingArrEnd < existing.length() && existingDepth > 0) {
                char c = existing.charAt(existingArrEnd);
                if (c == '[') existingDepth++;
                else if (c == ']') existingDepth--;
                existingArrEnd++;
            }

            int newDepth = 1;
            int newArrEnd = newArrStart + 1;
            while (newArrEnd < newJson.length() && newDepth > 0) {
                char c = newJson.charAt(newArrEnd);
                if (c == '[') newDepth++;
                else if (c == ']') newDepth--;
                newArrEnd++;
            }

            String existingEvents = existing.substring(existingArrStart + 1, existingArrEnd - 1);
            String newEvents = newJson.substring(newArrStart + 1, newArrEnd - 1);

            // Merge arrays
            String merged = existingEvents.trim();
            if (!merged.isEmpty() && !newEvents.trim().isEmpty()) {
                merged += ",";
            }
            merged += newEvents;

            return "{\"events\":[" + merged + "]}";
        } catch (Exception e) {
            return newJson;
        }
    }

    /**
     * Get OAuth base URL.
     */
    private String getOAuthBaseUrl() {
        String url = System.getenv("CLAUDE_CODE_API_URL");
        return url != null ? url : "https://api.claude.ai";
    }

    /**
     * Get auth header.
     */
    private String getAuthHeader() {
        String token = System.getenv("CLAUDE_CODE_ACCESS_TOKEN");
        return token != null ? "Bearer " + token : null;
    }

    /**
     * Shutdown exporter.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}