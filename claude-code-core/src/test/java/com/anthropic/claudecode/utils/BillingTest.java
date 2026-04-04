/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Billing.
 */
class BillingTest {

    @BeforeEach
    void setUp() {
        Billing.setPlan(Billing.BillingPlan.FREE);
        Billing.resetUsage();
    }

    @Test
    @DisplayName("Billing BillingPlan enum values")
    void billingPlanEnum() {
        Billing.BillingPlan[] plans = Billing.BillingPlan.values();
        assertEquals(4, plans.length);
    }

    @Test
    @DisplayName("Billing BillingPlan getName")
    void billingPlanGetName() {
        assertEquals("free", Billing.BillingPlan.FREE.getName());
        assertEquals("pro", Billing.BillingPlan.PRO.getName());
        assertEquals("team", Billing.BillingPlan.TEAM.getName());
        assertEquals("enterprise", Billing.BillingPlan.ENTERPRISE.getName());
    }

    @Test
    @DisplayName("Billing BillingPlan getMaxUsers")
    void billingPlanGetMaxUsers() {
        assertEquals(0, Billing.BillingPlan.FREE.getMaxUsers());
        assertEquals(100, Billing.BillingPlan.PRO.getMaxUsers());
        assertEquals(-1, Billing.BillingPlan.ENTERPRISE.getMaxUsers());
    }

    @Test
    @DisplayName("Billing BillingPlan getMaxTokens")
    void billingPlanGetMaxTokens() {
        assertEquals(0, Billing.BillingPlan.FREE.getMaxTokens());
        assertEquals(1000000, Billing.BillingPlan.PRO.getMaxTokens());
        assertEquals(-1, Billing.BillingPlan.ENTERPRISE.getMaxTokens());
    }

    @Test
    @DisplayName("Billing BillingPlan isUnlimited")
    void billingPlanIsUnlimited() {
        assertFalse(Billing.BillingPlan.FREE.isUnlimited());
        assertFalse(Billing.BillingPlan.PRO.isUnlimited());
        assertTrue(Billing.BillingPlan.ENTERPRISE.isUnlimited());
    }

    @Test
    @DisplayName("Billing UsageStats empty")
    void usageStatsEmpty() {
        Billing.UsageStats stats = Billing.UsageStats.empty();
        assertEquals(0, stats.inputTokens());
        assertEquals(0, stats.outputTokens());
        assertEquals(0, stats.totalTokens());
        assertEquals(0, stats.apiCalls());
    }

    @Test
    @DisplayName("Billing UsageStats addInput")
    void usageStatsAddInput() {
        Billing.UsageStats stats = Billing.UsageStats.empty();
        stats = stats.addInput(100);
        assertEquals(100, stats.inputTokens());
        assertEquals(100, stats.totalTokens());
        assertEquals(1, stats.apiCalls());
    }

    @Test
    @DisplayName("Billing UsageStats addOutput")
    void usageStatsAddOutput() {
        Billing.UsageStats stats = Billing.UsageStats.empty();
        stats = stats.addOutput(50);
        assertEquals(50, stats.outputTokens());
        assertEquals(50, stats.totalTokens());
    }

    @Test
    @DisplayName("Billing UsageStats getCostEstimate")
    void usageStatsGetCostEstimate() {
        Billing.UsageStats stats = Billing.UsageStats.empty();
        stats = stats.addInput(1000).addOutput(1000);
        // Input: 1000 * 0.003/1000 = 0.003
        // Output: 1000 * 0.015/1000 = 0.015
        // Total: 0.018
        assertEquals(0.018, stats.getCostEstimate(), 0.001);
    }

    @Test
    @DisplayName("Billing getPlan default FREE")
    void getPlanDefault() {
        assertEquals(Billing.BillingPlan.FREE, Billing.getPlan());
    }

    @Test
    @DisplayName("Billing setPlan")
    void setPlan() {
        Billing.setPlan(Billing.BillingPlan.PRO);
        assertEquals(Billing.BillingPlan.PRO, Billing.getPlan());
    }

    @Test
    @DisplayName("Billing getUsage initially empty")
    void getUsageInitiallyEmpty() {
        Billing.UsageStats usage = Billing.getUsage();
        assertEquals(0, usage.totalTokens());
    }

    @Test
    @DisplayName("Billing recordUsage updates stats")
    void recordUsage() {
        Billing.recordUsage(100, 50);
        Billing.UsageStats usage = Billing.getUsage();
        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(150, usage.totalTokens());
    }

    @Test
    @DisplayName("Billing isWithinLimits true for enterprise")
    void isWithinLimitsEnterprise() {
        Billing.setPlan(Billing.BillingPlan.ENTERPRISE);
        Billing.recordUsage(1000000, 1000000);
        assertTrue(Billing.isWithinLimits());
    }

    @Test
    @DisplayName("Billing isWithinLimits true when under limit")
    void isWithinLimitsUnder() {
        Billing.setPlan(Billing.BillingPlan.PRO);
        Billing.recordUsage(100, 50);
        assertTrue(Billing.isWithinLimits());
    }

    @Test
    @DisplayName("Billing isWithinLimits false when over limit")
    void isWithinLimitsOver() {
        Billing.setPlan(Billing.BillingPlan.FREE);
        Billing.recordUsage(100, 50);
        // FREE plan has max 0 tokens
        assertFalse(Billing.isWithinLimits());
    }

    @Test
    @DisplayName("Billing getRemainingTokens for enterprise")
    void getRemainingTokensEnterprise() {
        Billing.setPlan(Billing.BillingPlan.ENTERPRISE);
        assertEquals(Long.MAX_VALUE, Billing.getRemainingTokens());
    }

    @Test
    @DisplayName("Billing getRemainingTokens for limited plan")
    void getRemainingTokensLimited() {
        Billing.setPlan(Billing.BillingPlan.PRO);
        Billing.recordUsage(100, 50);
        assertEquals(1000000 - 150, Billing.getRemainingTokens());
    }

    @Test
    @DisplayName("Billing resetUsage clears stats")
    void resetUsage() {
        Billing.recordUsage(100, 50);
        Billing.resetUsage();
        assertEquals(0, Billing.getUsage().totalTokens());
    }

    @Test
    @DisplayName("Billing BillingSummary record")
    void billingSummary() {
        Billing.BillingSummary summary = new Billing.BillingSummary(
            Billing.BillingPlan.PRO,
            Billing.UsageStats.empty(),
            0.0,
            1000000L,
            true
        );
        assertEquals(Billing.BillingPlan.PRO, summary.plan());
        assertTrue(summary.withinLimits());
    }

    @Test
    @DisplayName("Billing BillingSummary format")
    void billingSummaryFormat() {
        Billing.UsageStats stats = Billing.UsageStats.empty().addInput(1000);
        Billing.BillingSummary summary = new Billing.BillingSummary(
            Billing.BillingPlan.PRO,
            stats,
            0.003,
            999000L,
            true
        );
        String formatted = summary.format();
        assertTrue(formatted.contains("pro"));
        assertTrue(formatted.contains("1000"));
    }

    @Test
    @DisplayName("Billing getSummary returns summary")
    void getSummary() {
        Billing.BillingSummary summary = Billing.getSummary();
        assertNotNull(summary);
        assertEquals(Billing.getPlan(), summary.plan());
    }
}
