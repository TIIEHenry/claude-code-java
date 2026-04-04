/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiClientConfig.
 */
class ApiClientConfigTest {

    @Test
    @DisplayName("ApiClientConfig constants")
    void constants() {
        assertEquals("https://api.anthropic.com", ApiClientConfig.DEFAULT_BASE_URL);
        assertEquals(120000, ApiClientConfig.DEFAULT_TIMEOUT_MS);
        assertEquals(3, ApiClientConfig.DEFAULT_MAX_RETRIES);
    }

    @Test
    @DisplayName("ApiClientConfig record with all fields")
    void recordFields() {
        Map<String, String> headers = Map.of("X-Custom", "value");
        ApiClientConfig config = new ApiClientConfig(
            "test-key",
            "https://custom.api.com",
            60000,
            5,
            headers
        );

        assertEquals("test-key", config.apiKey());
        assertEquals("https://custom.api.com", config.baseUrl());
        assertEquals(60000, config.timeoutMs());
        assertEquals(5, config.maxRetries());
        assertEquals(headers, config.defaultHeaders());
    }

    @Test
    @DisplayName("ApiClientConfig uses defaults for null values")
    void usesDefaults() {
        ApiClientConfig config = new ApiClientConfig(
            "key",
            null,
            0,
            0,
            null
        );

        assertEquals(ApiClientConfig.DEFAULT_BASE_URL, config.baseUrl());
        assertEquals(ApiClientConfig.DEFAULT_TIMEOUT_MS, config.timeoutMs());
        assertEquals(ApiClientConfig.DEFAULT_MAX_RETRIES, config.maxRetries());
        assertTrue(config.defaultHeaders().isEmpty());
    }

    @Test
    @DisplayName("ApiClientConfig builder creates config")
    void builder() {
        ApiClientConfig config = ApiClientConfig.builder()
            .apiKey("my-key")
            .baseUrl("https://test.com")
            .timeoutMs(30000)
            .maxRetries(2)
            .build();

        assertEquals("my-key", config.apiKey());
        assertEquals("https://test.com", config.baseUrl());
        assertEquals(30000, config.timeoutMs());
        assertEquals(2, config.maxRetries());
    }

    @Test
    @DisplayName("ApiClientConfig builder addHeader")
    void builderAddHeader() {
        ApiClientConfig config = ApiClientConfig.builder()
            .apiKey("key")
            .addHeader("X-Header-1", "value1")
            .addHeader("X-Header-2", "value2")
            .build();

        assertEquals("value1", config.defaultHeaders().get("X-Header-1"));
        assertEquals("value2", config.defaultHeaders().get("X-Header-2"));
    }

    @Test
    @DisplayName("ApiClientConfig builder defaultHeaders")
    void builderDefaultHeaders() {
        Map<String, String> headers = Map.of("X-Auth", "token");
        ApiClientConfig config = ApiClientConfig.builder()
            .apiKey("key")
            .defaultHeaders(headers)
            .build();

        assertEquals("token", config.defaultHeaders().get("X-Auth"));
    }

    @Test
    @DisplayName("ApiClientConfig builder returns immutable headers")
    void immutableHeaders() {
        ApiClientConfig config = ApiClientConfig.builder()
            .apiKey("key")
            .addHeader("X-Test", "value")
            .build();

        assertThrows(UnsupportedOperationException.class, () ->
            config.defaultHeaders().put("new", "value")
        );
    }
}