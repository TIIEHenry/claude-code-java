/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/client.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * API Client for Claude API interactions.
 */
public class ApiClient {
    private final ApiClientConfig config;
    private final HttpClient httpClient;
    private volatile boolean closed = false;

    public ApiClient(ApiClientConfig config) {
        this.config = config;
        this.httpClient = new HttpClient(config);
    }

    /**
     * Send a message to the API.
     */
    public CompletableFuture<ApiResponse> sendMessage(ApiRequest request) {
        if (closed) {
            return CompletableFuture.failedFuture(new ApiException("Client is closed"));
        }
        return httpClient.post("/v1/messages", request);
    }

    /**
     * Stream a message from the API.
     */
    public CompletableFuture<ApiStreamingResponse> streamMessage(ApiRequest request) {
        if (closed) {
            return CompletableFuture.failedFuture(new ApiException("Client is closed"));
        }
        return httpClient.streamPost("/v1/messages", request);
    }

    /**
     * Close the client.
     */
    public void close() {
        closed = true;
        httpClient.close();
    }

    /**
     * Check if the client is closed.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Get the configuration.
     */
    public ApiClientConfig getConfig() {
        return config;
    }

    /**
     * Create a default API client.
     */
    public static ApiClient create(String apiKey) {
        return new ApiClient(ApiClientConfig.builder()
            .apiKey(apiKey)
            .build());
    }
}