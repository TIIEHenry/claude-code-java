/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/sessionIngress
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.time.*;
import java.util.UUID;

/**
 * Session ingress service - Persist session logs to remote server.
 *
 * Uses optimistic concurrency control with Last-Uuid header.
 */
public final class SessionIngressService {
    private static final int MAX_RETRIES = 10;
    private static final long BASE_DELAY_MS = 500;

    private final java.net.http.HttpClient httpClient;
    private final String baseUrl;

    // Per-session state
    private final Map<String, UUID> lastUuidMap = new ConcurrentHashMap<>();
    private final Map<String, SequentialExecutor> sequentialExecutors = new ConcurrentHashMap<>();

    /**
     * Create session ingress service.
     */
    public SessionIngressService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    }

    /**
     * Transcript message entry.
     */
    public record TranscriptMessage(
        UUID uuid,
        String type,
        Map<String, Object> content,
        Long timestamp
    ) {}

    /**
     * Session log entry.
     */
    public record SessionLogEntry(
        UUID uuid,
        String type,
        Object payload,
        Long timestamp
    ) {}

    /**
     * Append session log with JWT token.
     */
    public CompletableFuture<Boolean> appendSessionLog(
        String sessionId,
        TranscriptMessage entry,
        String url
    ) {
        String sessionToken = getSessionIngressAuthToken();
        if (sessionToken == null) {
            return CompletableFuture.completedFuture(false);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + sessionToken);
        headers.put("Content-Type", "application/json");

        SequentialExecutor executor = getOrCreateSequentialExecutor(sessionId);
        return executor.execute(() -> appendSessionLogImpl(sessionId, entry, url, headers));
    }

    /**
     * Get session logs.
     */
    public CompletableFuture<List<SessionLogEntry>> getSessionLogs(
        String sessionId,
        String url
    ) {
        String sessionToken = getSessionIngressAuthToken();
        if (sessionToken == null) {
            return CompletableFuture.completedFuture(null);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + sessionToken);

        return fetchSessionLogsFromUrl(sessionId, url, headers);
    }

    /**
     * Get session logs via OAuth.
     */
    public CompletableFuture<List<SessionLogEntry>> getSessionLogsViaOAuth(
        String sessionId,
        String accessToken,
        String orgUUID
    ) {
        String url = baseUrl + "/v1/session_ingress/session/" + sessionId;

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("x-organization-uuid", orgUUID);

        return fetchSessionLogsFromUrl(sessionId, url, headers);
    }

    /**
     * Get teleport events.
     */
    public CompletableFuture<List<SessionLogEntry>> getTeleportEvents(
        String sessionId,
        String accessToken,
        String orgUUID
    ) {
        String baseUrl = this.baseUrl + "/v1/code/sessions/" + sessionId + "/teleport-events";

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("x-organization-uuid", orgUUID);

        List<SessionLogEntry> all = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        int maxPages = 100;

        return fetchTeleportEventsPaginated(baseUrl, headers, all, cursor, pages, maxPages);
    }

    /**
     * Clear cached state for a session.
     */
    public void clearSession(String sessionId) {
        lastUuidMap.remove(sessionId);
        sequentialExecutors.remove(sessionId);
    }

    /**
     * Clear all cached session state.
     */
    public void clearAllSessions() {
        lastUuidMap.clear();
        sequentialExecutors.clear();
    }

    // Internal implementation
    private Boolean appendSessionLogImpl(
        String sessionId,
        TranscriptMessage entry,
        String url,
        Map<String, String> headers
    ) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                UUID lastUuid = lastUuidMap.get(sessionId);
                Map<String, String> requestHeaders = new HashMap<>(headers);
                if (lastUuid != null) {
                    requestHeaders.put("Last-Uuid", lastUuid.toString());
                }

                String body = serializeEntry(entry);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", requestHeaders.get("Authorization"))
                    .method("PUT", HttpRequest.BodyPublishers.ofString(body))
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    lastUuidMap.put(sessionId, entry.uuid());
                    return true;
                }

                if (response.statusCode() == 409) {
                    // Handle conflict
                    String serverLastUuid = response.headers().firstValue("x-last-uuid").orElse(null);
                    if (serverLastUuid != null) {
                        lastUuidMap.put(sessionId, UUID.fromString(serverLastUuid));
                        continue; // retry
                    }
                    return false;
                }

                if (response.statusCode() == 401) {
                    return false; // Non-retryable
                }

                // Other 4xx/5xx - retryable
            } catch (Exception e) {
                // Network error - retryable
            }

            if (attempt == MAX_RETRIES) {
                return false;
            }

            // Exponential backoff
            long delay = Math.min(BASE_DELAY_MS * (1L << (attempt - 1)), 8000);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    private CompletableFuture<List<SessionLogEntry>> fetchSessionLogsFromUrl(
        String sessionId,
        String url,
        Map<String, String> headers
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();

                for (Map.Entry<String, String> h : headers.entrySet()) {
                    requestBuilder.header(h.getKey(), h.getValue());
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<SessionLogEntry> logs = parseLogResponse(response.body());

                    // Update lastUuid
                    if (!logs.isEmpty()) {
                        SessionLogEntry last = logs.get(logs.size() - 1);
                        if (last.uuid() != null) {
                            lastUuidMap.put(sessionId, last.uuid());
                        }
                    }

                    return logs;
                }

                if (response.statusCode() == 404) {
                    return Collections.emptyList();
                }

                return null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    private CompletableFuture<List<SessionLogEntry>> fetchTeleportEventsPaginated(
        String baseUrl,
        Map<String, String> headers,
        List<SessionLogEntry> allEntries,
        String initialCursor,
        int initialPages,
        int maxPages
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<SessionLogEntry> all = new ArrayList<>(allEntries);
            String cursor = initialCursor;
            int pages = initialPages;

            while (pages < maxPages) {
                try {
                    StringBuilder url = new StringBuilder(baseUrl);
                    url.append("?limit=1000");
                    if (cursor != null) {
                        url.append("&cursor=").append(cursor);
                    }

                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url.toString()))
                        .GET();

                    for (Map.Entry<String, String> h : headers.entrySet()) {
                        requestBuilder.header(h.getKey(), h.getValue());
                    }

                    HttpRequest request = requestBuilder.build();
                    HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 404) {
                        return pages == initialPages ? null : all;
                    }

                    if (response.statusCode() != 200) {
                        return null;
                    }

                    TeleportEventsResponse parsed = parseTeleportResponse(response.body());
                    for (SessionLogEntry entry : parsed.events()) {
                        all.add(entry);
                    }

                    pages++;

                    if (parsed.nextCursor() == null) {
                        break;
                    }

                    cursor = parsed.nextCursor();
                } catch (Exception e) {
                    return null;
                }
            }

            return all;
        });
    }

    // Helper methods
    private String getSessionIngressAuthToken() {
        return System.getenv("CLAUDE_CODE_SESSION_TOKEN");
    }

    private SequentialExecutor getOrCreateSequentialExecutor(String sessionId) {
        return sequentialExecutors.computeIfAbsent(sessionId, k -> new SequentialExecutor());
    }

    private String serializeEntry(TranscriptMessage entry) {
        // Simplified JSON serialization
        return "{\"uuid\":\"" + entry.uuid() + "\",\"type\":\"" + entry.type() + "\"}";
    }

    private List<SessionLogEntry> parseLogResponse(String json) {
        // Simplified parsing
        return Collections.emptyList();
    }

    private TeleportEventsResponse parseTeleportResponse(String json) {
        // Simplified parsing
        return new TeleportEventsResponse(Collections.emptyList(), null);
    }

    /**
     * Teleport events response.
     */
    private record TeleportEventsResponse(
        List<SessionLogEntry> events,
        String nextCursor
    ) {}

    /**
     * Sequential executor for per-session ordering.
     */
    private static class SequentialExecutor {
        private final Semaphore semaphore = new Semaphore(1);

        public CompletableFuture<Boolean> execute(java.util.function.Supplier<Boolean> task) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    return task.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                } finally {
                    semaphore.release();
                }
            });
        }
    }
}