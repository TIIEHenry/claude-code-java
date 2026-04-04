/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RateLimitService.
 */
class RateLimitServiceTest {

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitService();
        service.resetAll();
    }

    @Test
    @DisplayName("RateLimitService RateLimit requests factory")
    void rateLimitRequests() {
        RateLimitService.RateLimit limit = RateLimitService.RateLimit.requests("test", 100, 60000);

        assertEquals("test", limit.name());
        assertEquals(100, limit.maxRequests());
        assertEquals(0, limit.maxTokens());
        assertEquals(60000, limit.windowMs());
        assertEquals("requests", limit.scope());
    }

    @Test
    @DisplayName("RateLimitService RateLimit tokens factory")
    void rateLimitTokens() {
        RateLimitService.RateLimit limit = RateLimitService.RateLimit.tokens("test", 1000, 60000);

        assertEquals("test", limit.name());
        assertEquals(0, limit.maxRequests());
        assertEquals(1000, limit.maxTokens());
        assertEquals(60000, limit.windowMs());
        assertEquals("tokens", limit.scope());
    }

    @Test
    @DisplayName("RateLimitService default limits registered")
    void defaultLimits() {
        assertNotNull(service.getStatus("api_requests"));
        assertNotNull(service.getStatus("api_tokens"));
    }

    @Test
    @DisplayName("RateLimitService isAllowed returns true initially")
    void isAllowedInitial() {
        assertTrue(service.isAllowed("api_requests", 1));
    }

    @Test
    @DisplayName("RateLimitService getAvailable returns max initially")
    void getAvailableInitial() {
        int available = service.getAvailable("api_requests");
        assertTrue(available > 0);
    }

    @Test
    @DisplayName("RateLimitService getAvailable unknown limit returns MAX_VALUE")
    void getAvailableUnknown() {
        assertEquals(Integer.MAX_VALUE, service.getAvailable("unknown_limit"));
    }

    @Test
    @DisplayName("RateLimitService isAllowed unknown limit returns true")
    void isAllowedUnknown() {
        assertTrue(service.isAllowed("unknown_limit", 100));
    }

    @Test
    @DisplayName("RateLimitService registerLimit")
    void registerLimit() {
        RateLimitService.RateLimit limit = RateLimitService.RateLimit.requests("custom", 50, 30000);
        service.registerLimit(limit);

        int available = service.getAvailable("custom");
        assertTrue(available > 0 && available <= 50);
    }

    @Test
    @DisplayName("RateLimitService getWaitTime returns 0 initially")
    void getWaitTimeInitial() {
        assertEquals(0, service.getWaitTime("api_requests", 1));
    }

    @Test
    @DisplayName("RateLimitService getWaitTime unknown limit returns 0")
    void getWaitTimeUnknown() {
        assertEquals(0, service.getWaitTime("unknown_limit", 100));
    }

    @Test
    @DisplayName("RateLimitService RateLimitStatus format")
    void rateLimitStatusFormat() {
        RateLimitService.RateLimitStatus status = service.getStatus("api_requests");

        String formatted = status.format();
        assertTrue(formatted.contains("api_requests"));
        assertTrue(formatted.contains("/"));
    }

    @Test
    @DisplayName("RateLimitService RateLimitStatus fields")
    void rateLimitStatusFields() {
        RateLimitService.RateLimitStatus status = service.getStatus("api_requests");

        assertNotNull(status.name());
        assertTrue(status.limit() > 0);
        assertTrue(status.remaining() > 0);
        assertTrue(status.resetMs() > 0);
        assertFalse(status.limited());
    }

    @Test
    @DisplayName("RateLimitService getAllStatuses returns all")
    void getAllStatuses() {
        var statuses = service.getAllStatuses();

        assertTrue(statuses.containsKey("api_requests"));
        assertTrue(statuses.containsKey("api_tokens"));
    }

    @Test
    @DisplayName("RateLimitService resetLimit")
    void resetLimit() {
        service.resetLimit("api_requests");

        assertTrue(service.isAllowed("api_requests", 1));
    }

    @Test
    @DisplayName("RateLimitService resetAll")
    void resetAll() {
        service.resetAll();

        assertTrue(service.isAllowed("api_requests", 1));
        assertTrue(service.isAllowed("api_tokens", 1));
    }

    @Test
    @DisplayName("RateLimitService RateLimitExceededException")
    void rateLimitExceededException() {
        RateLimitService.RateLimitExceededException ex = new RateLimitService.RateLimitExceededException(
            "test_limit", 5000
        );

        assertEquals("test_limit", ex.getLimitName());
        assertEquals(5000, ex.getRetryAfterMs());
        assertTrue(ex.getMessage().contains("test_limit"));
    }

    @Test
    @DisplayName("RateLimitService waitForAllow does not throw when available")
    void waitForAllowAvailable() throws InterruptedException {
        service.waitForAllow("api_requests", 1);

        // Should complete without error
    }

    @Test
    @DisplayName("RateLimitService unknown limit status")
    void unknownLimitStatus() {
        RateLimitService.RateLimitStatus status = service.getStatus("nonexistent");

        assertEquals("nonexistent", status.name());
        assertEquals(Integer.MAX_VALUE, status.limit());
        assertEquals(Integer.MAX_VALUE, status.remaining());
        assertFalse(status.limited());
    }
}