/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bridge/bridgeApi
 */
package com.anthropic.claudecode.bridge;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.time.*;

/**
 * Bridge API client - HTTP client for bridge communication.
 */
public final class BridgeApiClient {
    private final HttpClient httpClient;
    private final String baseUrl;

    /**
     * Create bridge API client.
     */
    public BridgeApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Register bridge environment.
     */
    public CompletableFuture<EnvironmentRegistration> registerBridgeEnvironment(
        BridgeTypes.BridgeConfig config
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + "/v1/environments";

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("environment_id", config.environmentId());
                body.put("bridge_id", config.bridgeId());
                body.put("worker_type", config.workerType());
                body.put("machine_name", config.machineName());
                body.put("branch", config.branch());
                body.put("dir", config.dir());
                body.put("git_repo_url", config.gitRepoUrl());
                body.put("max_sessions", config.maxSessions());
                body.put("spawn_mode", config.spawnMode().name().toLowerCase());
                body.put("sandbox", config.sandbox());

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + BridgeConfig.getBridgeAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(serialize(body)))
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseRegistration(response.body());
                }

                throw new RuntimeException("Registration failed: " + response.statusCode());
            } catch (Exception e) {
                throw new RuntimeException("Registration failed", e);
            }
        });
    }

    /**
     * Poll for work.
     */
    public CompletableFuture<BridgeTypes.WorkResponse> pollForWork(
        String environmentId,
        String environmentSecret,
        long reclaimOlderThanMs
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + "/v1/environments/" + environmentId + "/work";

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + environmentSecret)
                    .GET();

                HttpRequest request = requestBuilder.build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 204) {
                    return null; // No work available
                }

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseWorkResponse(response.body());
                }

                throw new RuntimeException("Poll failed: " + response.statusCode());
            } catch (Exception e) {
                throw new RuntimeException("Poll failed", e);
            }
        });
    }

    /**
     * Acknowledge work.
     */
    public CompletableFuture<Void> acknowledgeWork(
        String environmentId,
        String workId,
        String sessionToken
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = baseUrl + "/v1/environments/" + environmentId + "/work/" + workId + "/ack";

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + sessionToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("Acknowledge failed: " + response.statusCode());
                }
            } catch (Exception e) {
                throw new RuntimeException("Acknowledge failed", e);
            }
        });
    }

    /**
     * Stop work.
     */
    public CompletableFuture<Void> stopWork(
        String environmentId,
        String workId,
        boolean force
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = baseUrl + "/v1/environments/" + environmentId + "/work/" + workId + "/stop";

                Map<String, Object> body = Map.of("force", force);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + BridgeConfig.getBridgeAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(serialize(body)))
                    .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new RuntimeException("Stop work failed", e);
            }
        });
    }

    /**
     * Deregister environment.
     */
    public CompletableFuture<Void> deregisterEnvironment(String environmentId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = baseUrl + "/v1/environments/" + environmentId;

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + BridgeConfig.getBridgeAccessToken())
                    .DELETE()
                    .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new RuntimeException("Deregister failed", e);
            }
        });
    }

    /**
     * Send permission response event.
     */
    public CompletableFuture<Void> sendPermissionResponseEvent(
        String sessionId,
        BridgeTypes.PermissionResponseEvent event,
        String sessionToken
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = baseUrl + "/v1/sessions/" + sessionId + "/events";

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + sessionToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(serialize(event)))
                    .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new RuntimeException("Send event failed", e);
            }
        });
    }

    /**
     * Archive session.
     */
    public CompletableFuture<Void> archiveSession(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = baseUrl + "/v1/sessions/" + sessionId + "/archive";

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + BridgeConfig.getBridgeAccessToken())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new RuntimeException("Archive failed", e);
            }
        });
    }

    /**
     * Heartbeat work.
     */
    public CompletableFuture<HeartbeatResponse> heartbeatWork(
        String environmentId,
        String workId,
        String sessionToken
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + "/v1/environments/" + environmentId + "/work/" + workId + "/heartbeat";

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + sessionToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseHeartbeat(response.body());
                }

                return new HeartbeatResponse(false, "error");
            } catch (Exception e) {
                return new HeartbeatResponse(false, "error");
            }
        });
    }

    // Helper methods
    private String serialize(Object obj) {
        // Simplified JSON serialization
        return "{}";
    }

    private EnvironmentRegistration parseRegistration(String json) {
        // Simplified parsing
        return new EnvironmentRegistration("env-id", "env-secret");
    }

    private BridgeTypes.WorkResponse parseWorkResponse(String json) {
        // Simplified parsing
        return null;
    }

    private HeartbeatResponse parseHeartbeat(String json) {
        return new HeartbeatResponse(true, "running");
    }

    /**
     * Environment registration result.
     */
    public record EnvironmentRegistration(
        String environmentId,
        String environmentSecret
    ) {}

    /**
     * Heartbeat response.
     */
    public record HeartbeatResponse(
        boolean leaseExtended,
        String state
    ) {}
}