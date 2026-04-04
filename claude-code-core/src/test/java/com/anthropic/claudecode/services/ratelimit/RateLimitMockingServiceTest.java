/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.ratelimit;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for RateLimitMockingService.
 */
@DisplayName("RateLimitMockingService Tests")
class RateLimitMockingServiceTest {

    @AfterEach
    void tearDown() {
        RateLimitMockingService.disableMockLimits();
    }

    @Test
    @DisplayName("RateLimitMockingService processes headers without mock")
    void processesHeadersWithoutMock() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-ratelimit-limit", "1000");
        headers.put("x-ratelimit-remaining", "500");

        Map<String, String> processed = RateLimitMockingService.processRateLimitHeaders(headers);

        assertEquals("1000", processed.get("x-ratelimit-limit"));
        assertEquals("500", processed.get("x-ratelimit-remaining"));
    }

    @Test
    @DisplayName("RateLimitMockingService applies mock headers when enabled")
    void appliesMockHeadersWhenEnabled() {
        Map<String, String> mockHeaders = new HashMap<>();
        mockHeaders.put("x-ratelimit-remaining", "0");
        mockHeaders.put("anthropic-ratelimit-unified-status", "rejected");

        RateLimitMockingService.enableMockLimits(mockHeaders);

        Map<String, String> original = new HashMap<>();
        original.put("x-ratelimit-remaining", "100");

        Map<String, String> processed = RateLimitMockingService.processRateLimitHeaders(original);

        assertEquals("0", processed.get("x-ratelimit-remaining"));
        assertEquals("rejected", processed.get("anthropic-ratelimit-unified-status"));
    }

    @Test
    @DisplayName("RateLimitMockingService shouldProcessMockLimits returns correct state")
    void shouldProcessMockLimitsReturnsCorrectState() {
        assertFalse(RateLimitMockingService.shouldProcessMockLimits());

        RateLimitMockingService.enableMockLimits(null);
        assertTrue(RateLimitMockingService.shouldProcessMockLimits());

        RateLimitMockingService.disableMockLimits();
        assertFalse(RateLimitMockingService.shouldProcessMockLimits());
    }

    @Test
    @DisplayName("RateLimitMockingService shouldProcessRateLimits works correctly")
    void shouldProcessRateLimitsWorksCorrectly() {
        // When subscriber
        assertTrue(RateLimitMockingService.shouldProcessRateLimits(true));

        // When not subscriber and no mock
        assertFalse(RateLimitMockingService.shouldProcessRateLimits(false));

        // When mock is enabled
        RateLimitMockingService.enableMockLimits(null);
        assertTrue(RateLimitMockingService.shouldProcessRateLimits(false));
    }

    @Test
    @DisplayName("RateLimitMockingService checkMockRateLimitError returns null when disabled")
    void checkMockRateLimitErrorReturnsNullWhenDisabled() {
        assertNull(RateLimitMockingService.checkMockRateLimitError("claude-sonnet-4-6", false));
    }

    @Test
    @DisplayName("RateLimitMockingService checkMockRateLimitError with headerless 429")
    void checkMockRateLimitErrorWithHeaderless429() {
        RateLimitMockingService.enableMockLimits(null);
        RateLimitMockingService.setHeaderless429Message("Rate limit exceeded");

        RateLimitMockingService.ApiException error =
            RateLimitMockingService.checkMockRateLimitError("claude-sonnet-4-6", false);

        assertNotNull(error);
        assertEquals(429, error.getStatusCode());
        assertEquals("Rate limit exceeded", error.getMessage());
    }

    @Test
    @DisplayName("RateLimitMockingService checkMockRateLimitError with rejected status")
    void checkMockRateLimitErrorWithRejectedStatus() {
        Map<String, String> mockHeaders = new HashMap<>();
        mockHeaders.put("anthropic-ratelimit-unified-status", "rejected");

        RateLimitMockingService.enableMockLimits(mockHeaders);

        RateLimitMockingService.ApiException error =
            RateLimitMockingService.checkMockRateLimitError("claude-sonnet-4-6", false);

        assertNotNull(error);
        assertEquals(429, error.getStatusCode());
    }

    @Test
    @DisplayName("RateLimitMockingService ApiException has correct properties")
    void apiExceptionHasCorrectProperties() {
        Map<String, String> headers = new HashMap<>();
        headers.put("retry-after", "60");

        RateLimitMockingService.ApiException error =
            new RateLimitMockingService.ApiException(429, "Rate limit exceeded", headers);

        assertEquals(429, error.getStatusCode());
        assertEquals("Rate limit exceeded", error.getMessage());
        assertEquals("60", error.getHeaders().get("retry-after"));
    }

    @Test
    @DisplayName("RateLimitMockingService isMockRateLimitError works correctly")
    void isMockRateLimitErrorWorksCorrectly() {
        RateLimitMockingService.ApiException error =
            new RateLimitMockingService.ApiException(429, "Error", Map.of());

        // When mock disabled
        RateLimitMockingService.disableMockLimits();
        assertFalse(RateLimitMockingService.isMockRateLimitError(error));

        // When mock enabled
        RateLimitMockingService.enableMockLimits(null);
        assertTrue(RateLimitMockingService.isMockRateLimitError(error));

        // Not a 429 error
        RateLimitMockingService.ApiException notRateLimit =
            new RateLimitMockingService.ApiException(500, "Server error", Map.of());
        assertFalse(RateLimitMockingService.isMockRateLimitError(notRateLimit));
    }
}