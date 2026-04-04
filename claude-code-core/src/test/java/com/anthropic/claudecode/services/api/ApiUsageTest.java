/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiUsage.
 */
class ApiUsageTest {

    @Test
    @DisplayName("ApiUsage RateLimit record")
    void rateLimitRecord() {
        ApiUsage.RateLimit limit = new ApiUsage.RateLimit(75.0, "2024-01-01T00:00:00Z");
        assertEquals(75.0, limit.utilization());
        assertEquals("2024-01-01T00:00:00Z", limit.resetsAt());
    }

    @Test
    @DisplayName("ApiUsage RateLimit with null values")
    void rateLimitWithNulls() {
        ApiUsage.RateLimit limit = new ApiUsage.RateLimit(null, null);
        assertNull(limit.utilization());
        assertNull(limit.resetsAt());
    }

    @Test
    @DisplayName("ApiUsage ExtraUsage record")
    void extraUsageRecord() {
        ApiUsage.ExtraUsage usage = new ApiUsage.ExtraUsage(
            true, 100.0, 50.0, 50.0
        );
        assertTrue(usage.isEnabled());
        assertEquals(100.0, usage.monthlyLimit());
        assertEquals(50.0, usage.usedCredits());
        assertEquals(50.0, usage.utilization());
    }

    @Test
    @DisplayName("ApiUsage Utilization record")
    void utilizationRecord() {
        ApiUsage.RateLimit fiveHour = new ApiUsage.RateLimit(30.0, null);
        ApiUsage.RateLimit sevenDay = new ApiUsage.RateLimit(60.0, null);
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            fiveHour, sevenDay, null, null, null, null
        );

        assertEquals(30.0, utilization.fiveHour().utilization());
        assertEquals(60.0, utilization.sevenDay().utilization());
        assertNull(utilization.sevenDayOpus());
        assertNull(utilization.sevenDaySonnet());
    }

    @Test
    @DisplayName("ApiUsage isApproachingLimit false for null utilization")
    void isApproachingLimitNull() {
        assertFalse(ApiUsage.isApproachingLimit(null, 80.0));
    }

    @Test
    @DisplayName("ApiUsage isApproachingLimit true when above threshold")
    void isApproachingLimitTrue() {
        ApiUsage.RateLimit limit = new ApiUsage.RateLimit(90.0, null);
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            limit, null, null, null, null, null
        );

        assertTrue(ApiUsage.isApproachingLimit(utilization, 80.0));
    }

    @Test
    @DisplayName("ApiUsage isApproachingLimit false when below threshold")
    void isApproachingLimitFalse() {
        ApiUsage.RateLimit limit = new ApiUsage.RateLimit(50.0, null);
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            limit, null, null, null, null, null
        );

        assertFalse(ApiUsage.isApproachingLimit(utilization, 80.0));
    }

    @Test
    @DisplayName("ApiUsage isApproachingLimit checks all limits")
    void isApproachingLimitMultiple() {
        ApiUsage.RateLimit fiveHour = new ApiUsage.RateLimit(30.0, null);
        ApiUsage.RateLimit sevenDay = new ApiUsage.RateLimit(85.0, null); // Above threshold
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            fiveHour, sevenDay, null, null, null, null
        );

        assertTrue(ApiUsage.isApproachingLimit(utilization, 80.0));
    }

    @Test
    @DisplayName("ApiUsage getMostRestrictiveLimit returns highest utilization")
    void getMostRestrictiveLimit() {
        ApiUsage.RateLimit fiveHour = new ApiUsage.RateLimit(30.0, null);
        ApiUsage.RateLimit sevenDay = new ApiUsage.RateLimit(70.0, null);
        ApiUsage.RateLimit sevenDayOpus = new ApiUsage.RateLimit(90.0, null);
        // Order: fiveHour, sevenDay, sevenDayOauthApps, sevenDayOpus, sevenDaySonnet, extraUsage
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            fiveHour, sevenDay, null, sevenDayOpus, null, null
        );

        ApiUsage.RateLimit mostRestrictive = ApiUsage.getMostRestrictiveLimit(utilization);
        assertNotNull(mostRestrictive);
        assertEquals(90.0, mostRestrictive.utilization());
    }

    @Test
    @DisplayName("ApiUsage getMostRestrictiveLimit returns null for empty utilization")
    void getMostRestrictiveLimitNull() {
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            null, null, null, null, null, null
        );

        assertNull(ApiUsage.getMostRestrictiveLimit(utilization));
    }

    @Test
    @DisplayName("ApiUsage getMostRestrictiveLimit null input")
    void getMostRestrictiveLimitNullInput() {
        assertNull(ApiUsage.getMostRestrictiveLimit(null));
    }

    @Test
    @DisplayName("ApiUsage formatUtilization null input")
    void formatUtilizationNull() {
        String formatted = ApiUsage.formatUtilization(null);
        assertEquals("No usage data available", formatted);
    }

    @Test
    @DisplayName("ApiUsage formatUtilization with limits")
    void formatUtilizationWithLimits() {
        ApiUsage.RateLimit fiveHour = new ApiUsage.RateLimit(30.0, "2024-01-01");
        ApiUsage.RateLimit sevenDay = new ApiUsage.RateLimit(60.0, null);
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            fiveHour, sevenDay, null, null, null, null
        );

        String formatted = ApiUsage.formatUtilization(utilization);
        assertTrue(formatted.contains("Claude AI Usage"));
        assertTrue(formatted.contains("Weekly"));
        assertTrue(formatted.contains("Session"));
        assertTrue(formatted.contains("60%"));
        assertTrue(formatted.contains("30%"));
    }

    @Test
    @DisplayName("ApiUsage formatUtilization with extra usage")
    void formatUtilizationWithExtraUsage() {
        ApiUsage.RateLimit fiveHour = new ApiUsage.RateLimit(30.0, null);
        ApiUsage.ExtraUsage extraUsage = new ApiUsage.ExtraUsage(
            true, 100.0, 50.0, 50.0
        );
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            fiveHour, null, null, null, null, extraUsage
        );

        String formatted = ApiUsage.formatUtilization(utilization);
        assertTrue(formatted.contains("Extra Usage"));
        assertTrue(formatted.contains("50%"));
        assertTrue(formatted.contains("$100"));
    }

    @Test
    @DisplayName("ApiUsage formatUtilization with opus limits")
    void formatUtilizationWithOpus() {
        ApiUsage.RateLimit sevenDayOpus = new ApiUsage.RateLimit(80.0, null);
        ApiUsage.RateLimit sevenDaySonnet = new ApiUsage.RateLimit(40.0, null);
        ApiUsage.Utilization utilization = new ApiUsage.Utilization(
            null, null, null, sevenDayOpus, sevenDaySonnet, null
        );

        String formatted = ApiUsage.formatUtilization(utilization);
        assertTrue(formatted.contains("Opus Weekly"));
        assertTrue(formatted.contains("Sonnet Weekly"));
        assertTrue(formatted.contains("80%"));
        assertTrue(formatted.contains("40%"));
    }

    @Test
    @DisplayName("ApiUsage fetchUtilization returns CompletableFuture")
    void fetchUtilization() {
        // ApiClient requires ApiClientConfig, use mock/null check
        var future = ApiUsage.fetchUtilization(null);
        assertNotNull(future);

        var utilization = future.join();
        assertNotNull(utilization);
    }
}