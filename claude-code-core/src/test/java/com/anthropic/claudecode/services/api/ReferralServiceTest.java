/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReferralService.
 */
class ReferralServiceTest {

    @BeforeEach
    void setUp() {
        ReferralService.clear();
    }

    @Test
    @DisplayName("ReferralService getReferralCode returns null initially")
    void getReferralCodeInitial() {
        assertNull(ReferralService.getReferralCode());
    }

    @Test
    @DisplayName("ReferralService setReferralCode and get")
    void setReferralCode() {
        ReferralService.setReferralCode("CODE123");

        assertEquals("CODE123", ReferralService.getReferralCode());
    }

    @Test
    @DisplayName("ReferralService getReferrerId returns null initially")
    void getReferrerIdInitial() {
        assertNull(ReferralService.getReferrerId());
    }

    @Test
    @DisplayName("ReferralService setReferrerId and get")
    void setReferrerId() {
        ReferralService.setReferrerId("referrer-456");

        assertEquals("referrer-456", ReferralService.getReferrerId());
    }

    @Test
    @DisplayName("ReferralService getReferralTimestamp returns null initially")
    void getReferralTimestampInitial() {
        assertNull(ReferralService.getReferralTimestamp());
    }

    @Test
    @DisplayName("ReferralService recordReferral sets all fields")
    void recordReferral() {
        ReferralService.recordReferral("MYCODE", "user-789");

        assertEquals("MYCODE", ReferralService.getReferralCode());
        assertEquals("user-789", ReferralService.getReferrerId());
        assertNotNull(ReferralService.getReferralTimestamp());
    }

    @Test
    @DisplayName("ReferralService hasReferral returns false initially")
    void hasReferralInitial() {
        assertFalse(ReferralService.hasReferral());
    }

    @Test
    @DisplayName("ReferralService hasReferral returns true with code")
    void hasReferralWithCode() {
        ReferralService.setReferralCode("CODE");
        assertTrue(ReferralService.hasReferral());
    }

    @Test
    @DisplayName("ReferralService hasReferral returns true with referrerId")
    void hasReferralWithReferrerId() {
        ReferralService.setReferrerId("referrer");
        assertTrue(ReferralService.hasReferral());
    }

    @Test
    @DisplayName("ReferralService clear removes all data")
    void clear() {
        ReferralService.recordReferral("CODE", "referrer");

        ReferralService.clear();

        assertNull(ReferralService.getReferralCode());
        assertNull(ReferralService.getReferrerId());
        assertNull(ReferralService.getReferralTimestamp());
        assertFalse(ReferralService.hasReferral());
    }

    @Test
    @DisplayName("ReferralService getReferralInfo returns empty map initially")
    void getReferralInfoInitial() {
        Map<String, Object> info = ReferralService.getReferralInfo();

        assertTrue(info.isEmpty());
    }

    @Test
    @DisplayName("ReferralService getReferralInfo returns all data")
    void getReferralInfo() {
        ReferralService.recordReferral("CODE123", "user-456");

        Map<String, Object> info = ReferralService.getReferralInfo();

        assertEquals(3, info.size());
        assertEquals("CODE123", info.get("referralCode"));
        assertEquals("user-456", info.get("referrerId"));
        assertNotNull(info.get("timestamp"));
    }

    @Test
    @DisplayName("ReferralService getReferralInfo with only code")
    void getReferralInfoCodeOnly() {
        ReferralService.setReferralCode("ONLYCODE");

        Map<String, Object> info = ReferralService.getReferralInfo();

        assertEquals(1, info.size());
        assertEquals("ONLYCODE", info.get("referralCode"));
    }

    @Test
    @DisplayName("ReferralService getReferralInfo with only referrerId")
    void getReferralInfoReferrerOnly() {
        ReferralService.setReferrerId("only-referrer");

        Map<String, Object> info = ReferralService.getReferralInfo();

        assertEquals(1, info.size());
        assertEquals("only-referrer", info.get("referrerId"));
    }

    @Test
    @DisplayName("ReferralService timestamp is reasonable")
    void timestampReasonable() {
        long before = System.currentTimeMillis();
        ReferralService.recordReferral("CODE", "referrer");
        long after = System.currentTimeMillis();

        Long timestamp = ReferralService.getReferralTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp >= before);
        assertTrue(timestamp <= after);
    }
}