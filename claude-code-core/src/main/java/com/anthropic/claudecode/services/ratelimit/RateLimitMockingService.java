/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/rateLimitMocking.ts
 */
package com.anthropic.claudecode.services.ratelimit;

import java.util.*;
import java.net.http.*;

/**
 * Facade for rate limit header processing with mock support.
 */
public final class RateLimitMockingService {
    private RateLimitMockingService() {}

    private static volatile boolean mockLimitsActive = false;
    private static volatile Map<String, String> mockHeaders = null;
    private static volatile String headerless429Message = null;

    /**
     * Process headers, applying mocks if /mock-limits command is active.
     */
    public static Map<String, String> processRateLimitHeaders(Map<String, String> headers) {
        if (shouldProcessMockLimits()) {
            return applyMockHeaders(headers);
        }
        return headers;
    }

    /**
     * Check if we should process rate limits (either real subscriber or mock).
     */
    public static boolean shouldProcessRateLimits(boolean isSubscriber) {
        return isSubscriber || shouldProcessMockLimits();
    }

    /**
     * Check if mock rate limits should throw a 429 error.
     */
    public static ApiException checkMockRateLimitError(String currentModel, boolean isFastModeActive) {
        if (!shouldProcessMockLimits()) {
            return null;
        }

        // Check for headerless 429 message
        if (headerless429Message != null) {
            return new ApiException(429, headerless429Message, Map.of());
        }

        if (mockHeaders == null) {
            return null;
        }

        String status = mockHeaders.get("anthropic-ratelimit-unified-status");
        String overageStatus = mockHeaders.get("anthropic-ratelimit-unified-overage-status");
        String rateLimitType = mockHeaders.get("anthropic-ratelimit-unified-representative-claim");

        // Check if this is an Opus-specific rate limit
        boolean isOpusLimit = "seven_day_opus".equals(rateLimitType);
        boolean isUsingOpus = currentModel.contains("opus");

        // For Opus limits, only throw 429 if actually using Opus
        if (isOpusLimit && !isUsingOpus) {
            return null;
        }

        // Check for fast mode rate limits
        if (isMockFastModeRateLimitScenario()) {
            Map<String, String> fastModeHeaders = checkMockFastModeRateLimit(isFastModeActive);
            if (fastModeHeaders == null) {
                return null;
            }
            return new ApiException(429, "Rate limit exceeded", fastModeHeaders);
        }

        boolean shouldThrow429 = "rejected".equals(status) &&
            (overageStatus == null || "rejected".equals(overageStatus));

        if (shouldThrow429) {
            return new ApiException(429, "Rate limit exceeded", mockHeaders);
        }

        return null;
    }

    /**
     * Check if this is a mock 429 error that shouldn't be retried.
     */
    public static boolean isMockRateLimitError(ApiException error) {
        return shouldProcessMockLimits() && error.getStatusCode() == 429;
    }

    /**
     * Check if /mock-limits command is currently active.
     */
    public static boolean shouldProcessMockLimits() {
        return mockLimitsActive;
    }

    /**
     * Enable mock limits mode.
     */
    public static void enableMockLimits(Map<String, String> headers) {
        mockLimitsActive = true;
        mockHeaders = headers != null ? new HashMap<>(headers) : null;
    }

    /**
     * Disable mock limits mode.
     */
    public static void disableMockLimits() {
        mockLimitsActive = false;
        mockHeaders = null;
        headerless429Message = null;
    }

    /**
     * Set headerless 429 message for testing.
     */
    public static void setHeaderless429Message(String message) {
        headerless429Message = message;
    }

    private static Map<String, String> applyMockHeaders(Map<String, String> original) {
        if (mockHeaders == null) {
            return original;
        }
        Map<String, String> result = new HashMap<>(original);
        result.putAll(mockHeaders);
        return result;
    }

    private static boolean isMockFastModeRateLimitScenario() {
        // Check if we should mock a fast mode rate limit scenario
        String mockScenario = System.getenv("CLAUDE_CODE_MOCK_RATE_LIMIT");
        if (mockScenario == null) return false;

        // Check for specific scenario types
        return "fast_mode".equals(mockScenario) ||
               "overload".equals(mockScenario) ||
               "rate_limit".equals(mockScenario);
    }

    private static Map<String, String> checkMockFastModeRateLimit(boolean isFastModeActive) {
        if (!isFastModeActive) return null;

        String mockScenario = System.getenv("CLAUDE_CODE_MOCK_RATE_LIMIT");
        if (mockScenario == null) return null;

        // Return appropriate mock headers based on scenario
        return switch (mockScenario) {
            case "fast_mode" -> Map.of(
                "x-ratelimit-limit", "100",
                "x-ratelimit-remaining", "0",
                "x-ratelimit-reset", String.valueOf(System.currentTimeMillis() / 1000 + 60)
            );
            case "overload" -> Map.of(
                "retry-after", "30",
                "x-ratelimit-type", "overload"
            );
            case "rate_limit" -> Map.of(
                "x-ratelimit-limit", "1000",
                "x-ratelimit-remaining", "0",
                "x-ratelimit-reset", String.valueOf(System.currentTimeMillis() / 1000 + 60),
                "retry-after", "60"
            );
            default -> null;
        };
    }

    /**
     * Simple API exception for rate limiting.
     */
    public static class ApiException extends RuntimeException {
        private final int statusCode;
        private final Map<String, String> headers;

        public ApiException(int statusCode, String message, Map<String, String> headers) {
            super(message);
            this.statusCode = statusCode;
            this.headers = headers;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }
}