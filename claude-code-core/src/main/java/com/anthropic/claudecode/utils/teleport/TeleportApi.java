/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/teleport/api
 */
package com.anthropic.claudecode.utils.teleport;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.time.*;
import java.util.UUID;
import com.anthropic.claudecode.utils.SlowOperations;

/**
 * Teleport API utilities - Session API interactions for remote sessions.
 */
public final class TeleportApi {
    private static final String CCR_BYOC_BETA = "ccr-byoc-2025-07-29";
    private static final int[] RETRY_DELAYS = {2000, 4000, 8000, 16000};
    private static final int MAX_RETRIES = RETRY_DELAYS.length;

    private final HttpClient httpClient;
    private final String baseUrl;

    /**
     * Create teleport API client.
     */
    public TeleportApi(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    /**
     * Session status enum.
     */
    public enum SessionStatus {
        REQUIRES_ACTION,
        RUNNING,
        IDLE,
        ARCHIVED
    }

    /**
     * Code session status enum.
     */
    public enum CodeSessionStatus {
        IDLE,
        WORKING,
        WAITING,
        COMPLETED,
        ARCHIVED,
        CANCELLED,
        REJECTED
    }

    /**
     * Git source record.
     */
    public record GitSource(
        String url,
        String revision,
        Boolean allowUnrestrictedGitPush
    ) {}

    /**
     * Session context record.
     */
    public record SessionContext(
        List<Object> sources,
        String cwd,
        List<Object> outcomes,
        String customSystemPrompt,
        String appendSystemPrompt,
        String model,
        String seedBundleFileId,
        GitHubPR githubPr,
        Boolean reuseOutcomeBranches
    ) {}

    /**
     * GitHub PR record.
     */
    public record GitHubPR(
        String owner,
        String repo,
        int number
    ) {}

    /**
     * Session resource record.
     */
    public record SessionResource(
        String type,
        String id,
        String title,
        SessionStatus sessionStatus,
        String environmentId,
        String createdAt,
        String updatedAt,
        SessionContext sessionContext
    ) {}

    /**
     * Code session record.
     */
    public record CodeSession(
        String id,
        String title,
        String description,
        CodeSessionStatus status,
        RepoInfo repo,
        List<String> turns,
        String createdAt,
        String updatedAt
    ) {}

    /**
     * Repo info record.
     */
    public record RepoInfo(
        String name,
        OwnerInfo owner,
        String defaultBranch
    ) {}

    /**
     * Owner info record.
     */
    public record OwnerInfo(
        String login
    ) {}

    /**
     * Check if error is transient.
     */
    public static boolean isTransientNetworkError(Throwable error) {
        if (error instanceof java.net.SocketException) {
            return true;
        }
        if (error instanceof java.net.SocketTimeoutException) {
            return true;
        }
        if (error instanceof java.net.UnknownHostException) {
            return false; // DNS failure - not transient
        }
        return false;
    }

    /**
     * GET with retry.
     */
    public <T> CompletableFuture<T> getWithRetry(
        String url,
        Map<String, String> headers,
        Class<T> responseType
    ) {
        return getWithRetry(url, headers, responseType, 0);
    }

    private <T> CompletableFuture<T> getWithRetry(
        String url,
        Map<String, String> headers,
        Class<T> responseType,
        int attempt
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();

                for (Map.Entry<String, String> h : headers.entrySet()) {
                    requestBuilder.header(h.getKey(), h.getValue());
                }

                HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() >= 500 && attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAYS[attempt]);
                    return getWithRetry(url, headers, responseType, attempt + 1).join();
                }

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseResponse(response.body(), responseType);
                }

                throw new RuntimeException("Request failed: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            } catch (Exception e) {
                if (isTransientNetworkError(e) && attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAYS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    return getWithRetry(url, headers, responseType, attempt + 1).join();
                }
                throw new RuntimeException("Request failed", e);
            }
        });
    }

    /**
     * Prepare API request.
     */
    public CompletableFuture<ApiCredentials> prepareApiRequest() {
        return CompletableFuture.supplyAsync(() -> {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                throw new RuntimeException(
                    "Claude Code web sessions require authentication. Please run /login."
                );
            }

            String orgUUID = getOrgUUID();
            if (orgUUID == null) {
                throw new RuntimeException("Unable to get organization UUID");
            }

            return new ApiCredentials(accessToken, orgUUID);
        });
    }

    /**
     * Fetch code sessions from Sessions API.
     */
    public CompletableFuture<List<CodeSession>> fetchCodeSessions() {
        return prepareApiRequest().thenCompose(creds -> {
            String url = baseUrl + "/v1/sessions";

            Map<String, String> headers = getOAuthHeaders(creds.accessToken());
            headers.put("anthropic-beta", CCR_BYOC_BETA);
            headers.put("x-organization-uuid", creds.orgUUID());

            return getWithRetry(url, headers, Map.class)
                .thenApply(response -> parseSessionList(response));
        });
    }

    /**
     * Fetch single session.
     */
    public CompletableFuture<SessionResource> fetchSession(String sessionId) {
        return prepareApiRequest().thenCompose(creds -> {
            String url = baseUrl + "/v1/sessions/" + sessionId;

            Map<String, String> headers = getOAuthHeaders(creds.accessToken());
            headers.put("anthropic-beta", CCR_BYOC_BETA);
            headers.put("x-organization-uuid", creds.orgUUID());

            return getWithRetry(url, headers, SessionResource.class);
        });
    }

    /**
     * Send event to remote session.
     */
    public CompletableFuture<Boolean> sendEventToRemoteSession(
        String sessionId,
        Object messageContent
    ) {
        return prepareApiRequest().thenCompose(creds -> {
            String url = baseUrl + "/v1/sessions/" + sessionId + "/events";

            Map<String, String> headers = getOAuthHeaders(creds.accessToken());
            headers.put("anthropic-beta", CCR_BYOC_BETA);
            headers.put("x-organization-uuid", creds.orgUUID());

            Map<String, Object> userEvent = new LinkedHashMap<>();
            userEvent.put("uuid", UUID.randomUUID().toString());
            userEvent.put("session_id", sessionId);
            userEvent.put("type", "user");
            userEvent.put("parent_tool_use_id", null);
            userEvent.put("message", Map.of(
                "role", "user",
                "content", messageContent
            ));

            return post(url, headers, Map.of("events", List.of(userEvent)))
                .thenApply(response -> response >= 200 && response < 300);
        });
    }

    /**
     * Update session title.
     */
    public CompletableFuture<Boolean> updateSessionTitle(String sessionId, String title) {
        return prepareApiRequest().thenCompose(creds -> {
            String url = baseUrl + "/v1/sessions/" + sessionId;

            Map<String, String> headers = getOAuthHeaders(creds.accessToken());
            headers.put("anthropic-beta", CCR_BYOC_BETA);
            headers.put("x-organization-uuid", creds.orgUUID());

            return patch(url, headers, Map.of("title", title))
                .thenApply(response -> response >= 200 && response < 300);
        });
    }

    /**
     * Get OAuth headers.
     */
    public Map<String, String> getOAuthHeaders(String accessToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("Content-Type", "application/json");
        headers.put("anthropic-version", "2023-06-01");
        return headers;
    }

    // Helper methods
    private CompletableFuture<Integer> post(String url, Map<String, String> headers, Object body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(SlowOperations.jsonStringify(body)));

                for (Map.Entry<String, String> h : headers.entrySet()) {
                    requestBuilder.header(h.getKey(), h.getValue());
                }

                HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
                );

                return response.statusCode();
            } catch (Exception e) {
                throw new RuntimeException("POST failed", e);
            }
        });
    }

    private CompletableFuture<Integer> patch(String url, Map<String, String> headers, Object body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(SlowOperations.jsonStringify(body)));

                for (Map.Entry<String, String> h : headers.entrySet()) {
                    requestBuilder.header(h.getKey(), h.getValue());
                }

                HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
                );

                return response.statusCode();
            } catch (Exception e) {
                throw new RuntimeException("PATCH failed", e);
            }
        });
    }

    private String getAccessToken() {
        return System.getenv("CLAUDE_CODE_ACCESS_TOKEN");
    }

    private String getOrgUUID() {
        return System.getenv("CLAUDE_CODE_ORG_UUID");
    }

    @SuppressWarnings("unchecked")
    private <T> T parseResponse(String body, Class<T> type) {
        // Simplified - would use Jackson in production
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<CodeSession> parseSessionList(Map<String, Object> response) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<CodeSession> sessions = new ArrayList<>();

        for (Map<String, Object> session : data) {
            sessions.add(new CodeSession(
                (String) session.get("id"),
                (String) session.getOrDefault("title", "Untitled"),
                "",
                CodeSessionStatus.IDLE,
                null,
                Collections.emptyList(),
                (String) session.get("created_at"),
                (String) session.get("updated_at")
            ));
        }

        return sessions;
    }

    /**
     * API credentials record.
     */
    public record ApiCredentials(
        String accessToken,
        String orgUUID
    ) {}
}