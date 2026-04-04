/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HttpUtils.
 */
class HttpUtilsTest {

    @Test
    @DisplayName("HttpUtils getUserAgent returns string")
    void getUserAgent() {
        String userAgent = HttpUtils.getUserAgent();
        assertNotNull(userAgent);
        assertTrue(userAgent.contains("claude-cli"));
    }

    @Test
    @DisplayName("HttpUtils getMCPUserAgent returns string")
    void getMCPUserAgent() {
        String userAgent = HttpUtils.getMCPUserAgent();
        assertNotNull(userAgent);
        assertTrue(userAgent.contains("claude-code"));
    }

    @Test
    @DisplayName("HttpUtils getWebFetchUserAgent returns string")
    void getWebFetchUserAgent() {
        String userAgent = HttpUtils.getWebFetchUserAgent();
        assertNotNull(userAgent);
        assertTrue(userAgent.contains("Claude-User"));
    }

    @Test
    @DisplayName("HttpUtils AuthHeaders record")
    void authHeadersRecord() {
        HttpUtils.AuthHeaders headers = new HttpUtils.AuthHeaders(
            Map.of("key", "value"),
            null
        );

        assertEquals(1, headers.headers().size());
        assertNull(headers.error());
    }

    @Test
    @DisplayName("HttpUtils AuthHeaders of")
    void authHeadersOf() {
        HttpUtils.AuthHeaders headers = HttpUtils.AuthHeaders.of(Map.of("x-api-key", "test"));
        assertEquals(1, headers.headers().size());
        assertNull(headers.error());
    }

    @Test
    @DisplayName("HttpUtils AuthHeaders error")
    void authHeadersError() {
        HttpUtils.AuthHeaders headers = HttpUtils.AuthHeaders.error("No API key");
        assertTrue(headers.headers().isEmpty());
        assertEquals("No API key", headers.error());
    }

    @Test
    @DisplayName("HttpUtils getAuthHeaders returns result")
    void getAuthHeaders() {
        HttpUtils.AuthHeaders headers = HttpUtils.getAuthHeaders();
        // May have headers or error depending on env vars
        assertTrue(headers.headers() != null || headers.error() != null);
    }

    @Test
    @DisplayName("HttpUtils buildHeaders")
    void buildHeaders() {
        Map<String, String> headers = HttpUtils.buildHeaders();
        assertTrue(headers.containsKey("User-Agent"));
        assertTrue(headers.containsKey("Content-Type"));
    }

    @Test
    @DisplayName("HttpUtils buildHeaders with additional headers")
    void buildHeadersWithAdditional() {
        Map<String, String> extra = Map.of("X-Custom", "value");
        Map<String, String> headers = HttpUtils.buildHeaders(extra);

        assertTrue(headers.containsKey("User-Agent"));
        assertTrue(headers.containsKey("X-Custom"));
        assertEquals("value", headers.get("X-Custom"));
    }

    @Test
    @DisplayName("HttpUtils buildHeaders with null map")
    void buildHeadersWithNull() {
        Map<String, String> headers = HttpUtils.buildHeaders((Map<String, String>) null);
        assertTrue(headers.containsKey("User-Agent"));
    }
}