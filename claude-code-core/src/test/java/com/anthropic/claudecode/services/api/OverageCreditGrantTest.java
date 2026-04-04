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
 * Tests for OverageCreditGrant.
 */
class OverageCreditGrantTest {

    @BeforeEach
    void setUp() {
        OverageCreditGrant.reset();
    }

    @Test
    @DisplayName("OverageCreditGrant isOverageEnabled returns false initially")
    void isOverageEnabledInitial() {
        assertFalse(OverageCreditGrant.isOverageEnabled());
    }

    @Test
    @DisplayName("OverageCreditGrant setOverageEnabled and get")
    void setOverageEnabled() {
        OverageCreditGrant.setOverageEnabled(true);
        assertTrue(OverageCreditGrant.isOverageEnabled());

        OverageCreditGrant.setOverageEnabled(false);
        assertFalse(OverageCreditGrant.isOverageEnabled());
    }

    @Test
    @DisplayName("OverageCreditGrant getDisabledReason returns null initially")
    void getDisabledReasonInitial() {
        assertNull(OverageCreditGrant.getDisabledReason());
    }

    @Test
    @DisplayName("OverageCreditGrant setDisabledReason")
    void setDisabledReason() {
        OverageCreditGrant.setDisabledReason("limit_exceeded");

        assertEquals("limit_exceeded", OverageCreditGrant.getDisabledReason());
        assertFalse(OverageCreditGrant.isOverageEnabled());
    }

    @Test
    @DisplayName("OverageCreditGrant getCreditBalance returns zero initially")
    void getCreditBalanceInitial() {
        assertEquals(0.0, OverageCreditGrant.getCreditBalance(), 0.001);
    }

    @Test
    @DisplayName("OverageCreditGrant setCreditBalance")
    void setCreditBalance() {
        OverageCreditGrant.setCreditBalance(100.0);

        assertEquals(100.0, OverageCreditGrant.getCreditBalance(), 0.001);
    }

    @Test
    @DisplayName("OverageCreditGrant addCredits")
    void addCredits() {
        OverageCreditGrant.setCreditBalance(50.0);
        OverageCreditGrant.addCredits(25.0);

        assertEquals(75.0, OverageCreditGrant.getCreditBalance(), 0.001);
    }

    @Test
    @DisplayName("OverageCreditGrant useCredits succeeds with sufficient balance")
    void useCreditsSufficient() {
        OverageCreditGrant.setCreditBalance(100.0);

        boolean result = OverageCreditGrant.useCredits(30.0);

        assertTrue(result);
        assertEquals(70.0, OverageCreditGrant.getCreditBalance(), 0.001);
    }

    @Test
    @DisplayName("OverageCreditGrant useCredits fails with insufficient balance")
    void useCreditsInsufficient() {
        OverageCreditGrant.setCreditBalance(10.0);

        boolean result = OverageCreditGrant.useCredits(30.0);

        assertFalse(result);
        assertEquals(10.0, OverageCreditGrant.getCreditBalance(), 0.001);
    }

    @Test
    @DisplayName("OverageCreditGrant hasCredits returns true when sufficient")
    void hasCreditsSufficient() {
        OverageCreditGrant.setCreditBalance(100.0);

        assertTrue(OverageCreditGrant.hasCredits(50.0));
        assertTrue(OverageCreditGrant.hasCredits(100.0));
    }

    @Test
    @DisplayName("OverageCreditGrant hasCredits returns false when insufficient")
    void hasCreditsInsufficient() {
        OverageCreditGrant.setCreditBalance(10.0);

        assertFalse(OverageCreditGrant.hasCredits(50.0));
    }

    @Test
    @DisplayName("OverageCreditGrant updateFromHeaders sets overage status")
    void updateFromHeadersStatus() {
        Map<String, String> headers = Map.of(
            "anthropic-ratelimit-unified-overage-status", "allowed"
        );

        OverageCreditGrant.updateFromHeaders(headers);

        assertTrue(OverageCreditGrant.isOverageEnabled());
    }

    @Test
    @DisplayName("OverageCreditGrant updateFromHeaders sets disabled reason")
    void updateFromHeadersReason() {
        Map<String, String> headers = Map.of(
            "anthropic-ratelimit-unified-overage-status", "disabled",
            "anthropic-ratelimit-unified-overage-disabled-reason", "policy_violation"
        );

        OverageCreditGrant.updateFromHeaders(headers);

        assertFalse(OverageCreditGrant.isOverageEnabled());
        assertEquals("policy_violation", OverageCreditGrant.getDisabledReason());
    }

    @Test
    @DisplayName("OverageCreditGrant getOverageStatus returns all data")
    void getOverageStatus() {
        OverageCreditGrant.setCreditBalance(50.0);
        OverageCreditGrant.setOverageEnabled(true);

        Map<String, Object> status = OverageCreditGrant.getOverageStatus();

        assertTrue((Boolean) status.get("enabled"));
        assertEquals(50.0, (Double) status.get("creditBalance"), 0.001);
    }

    @Test
    @DisplayName("OverageCreditGrant getOverageStatus includes disabledReason")
    void getOverageStatusWithReason() {
        OverageCreditGrant.setDisabledReason("test_reason");

        Map<String, Object> status = OverageCreditGrant.getOverageStatus();

        assertEquals("test_reason", status.get("disabledReason"));
    }

    @Test
    @DisplayName("OverageCreditGrant reset clears all state")
    void reset() {
        OverageCreditGrant.setCreditBalance(100.0);
        OverageCreditGrant.setOverageEnabled(true);
        OverageCreditGrant.setDisabledReason("some_reason");

        OverageCreditGrant.reset();

        assertEquals(0.0, OverageCreditGrant.getCreditBalance(), 0.001);
        assertFalse(OverageCreditGrant.isOverageEnabled());
        assertNull(OverageCreditGrant.getDisabledReason());
    }

    @Test
    @DisplayName("OverageCreditGrant enabling clears disabled reason")
    void enablingClearsReason() {
        OverageCreditGrant.setDisabledReason("some_reason");
        OverageCreditGrant.setOverageEnabled(true);

        assertNull(OverageCreditGrant.getDisabledReason());
    }
}