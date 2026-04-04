/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api
 */
package com.anthropic.claudecode.services.api;

import java.util.*;

/**
 * API Client configuration.
 */
public record ApiClientConfig(
    String apiKey,
    String baseUrl,
    int timeoutMs,
    int maxRetries,
    Map<String, String> defaultHeaders
) {
    public static final String DEFAULT_BASE_URL = "https://coding.dashscope.aliyuncs.com/v1";
    public static final int DEFAULT_TIMEOUT_MS = 120000;
    public static final int DEFAULT_MAX_RETRIES = 3;

    public ApiClientConfig {
        if (baseUrl == null) baseUrl = DEFAULT_BASE_URL;
        if (timeoutMs <= 0) timeoutMs = DEFAULT_TIMEOUT_MS;
        if (maxRetries <= 0) maxRetries = DEFAULT_MAX_RETRIES;
        if (defaultHeaders == null) defaultHeaders = Map.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private int timeoutMs = DEFAULT_TIMEOUT_MS;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private Map<String, String> defaultHeaders = new HashMap<>();

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }
        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder defaultHeaders(Map<String, String> headers) { this.defaultHeaders = headers; return this; }
        public Builder addHeader(String key, String value) { this.defaultHeaders.put(key, value); return this; }

        public ApiClientConfig build() {
            return new ApiClientConfig(apiKey, baseUrl, timeoutMs, maxRetries, Collections.unmodifiableMap(defaultHeaders));
        }
    }
}