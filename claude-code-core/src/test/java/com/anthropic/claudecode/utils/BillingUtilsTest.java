/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BillingUtils.
 */
class BillingUtilsTest {

    @BeforeEach
    void setUp() {
        BillingUtils.setMockBillingAccessOverride(null);
        AuthUtils.clearApiKey();
        AuthUtils.setAuthState(null);
    }

    @Test
    @DisplayName("BillingUtils hasConsoleBillingAccess returns false without auth")
    void hasConsoleBillingAccessNoAuth() {
        assertFalse(BillingUtils.hasConsoleBillingAccess());
    }

    @Test
    @DisplayName("BillingUtils hasClaudeAiBillingAccess returns false without subscription")
    void hasClaudeAiBillingAccessNoSubscription() {
        assertFalse(BillingUtils.hasClaudeAiBillingAccess());
    }

    @Test
    @DisplayName("BillingUtils setMockBillingAccessOverride")
    void setMockBillingAccessOverride() {
        BillingUtils.setMockBillingAccessOverride(true);
        assertTrue(BillingUtils.hasClaudeAiBillingAccess());
        
        BillingUtils.setMockBillingAccessOverride(false);
        assertFalse(BillingUtils.hasClaudeAiBillingAccess());
    }

    @Test
    @DisplayName("BillingUtils canViewCosts returns boolean")
    void canViewCosts() {
        BillingUtils.setMockBillingAccessOverride(true);
        assertTrue(BillingUtils.canViewCosts());
    }

    @Test
    @DisplayName("BillingUtils canManageBilling returns boolean")
    void canManageBilling() {
        assertFalse(BillingUtils.canManageBilling());
    }

    @Test
    @DisplayName("BillingUtils getBillingPlanType returns free by default")
    void getBillingPlanTypeDefault() {
        assertEquals("free", BillingUtils.getBillingPlanType());
    }

    @Test
    @DisplayName("BillingUtils getBillingPlanType max")
    void getBillingPlanTypeMax() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "max", "org"
        );
        AuthUtils.setAuthState(state);
        assertEquals("claude_max", BillingUtils.getBillingPlanType());
    }

    @Test
    @DisplayName("BillingUtils getBillingPlanType pro")
    void getBillingPlanTypePro() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "pro", "org"
        );
        AuthUtils.setAuthState(state);
        assertEquals("claude_pro", BillingUtils.getBillingPlanType());
    }

    @Test
    @DisplayName("BillingUtils getBillingPlanType team")
    void getBillingPlanTypeTeam() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "team", "org"
        );
        AuthUtils.setAuthState(state);
        assertEquals("claude_team", BillingUtils.getBillingPlanType());
    }

    @Test
    @DisplayName("BillingUtils getBillingPlanType enterprise")
    void getBillingPlanTypeEnterprise() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "enterprise", "org"
        );
        AuthUtils.setAuthState(state);
        assertEquals("claude_enterprise", BillingUtils.getBillingPlanType());
    }

    @Test
    @DisplayName("BillingUtils isEnterpriseSubscription false by default")
    void isEnterpriseSubscriptionDefault() {
        assertFalse(BillingUtils.isEnterpriseSubscription());
    }

    @Test
    @DisplayName("BillingUtils isEnterpriseSubscription true for team")
    void isEnterpriseSubscriptionTeam() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "team", "org"
        );
        AuthUtils.setAuthState(state);
        assertTrue(BillingUtils.isEnterpriseSubscription());
    }

    @Test
    @DisplayName("BillingUtils isConsumerSubscription false by default")
    void isConsumerSubscriptionDefault() {
        assertFalse(BillingUtils.isConsumerSubscription());
    }

    @Test
    @DisplayName("BillingUtils isConsumerSubscription true for pro")
    void isConsumerSubscriptionPro() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "pro", "org"
        );
        AuthUtils.setAuthState(state);
        assertTrue(BillingUtils.isConsumerSubscription());
    }

    @Test
    @DisplayName("BillingUtils BillingInfo record")
    void billingInfo() {
        BillingUtils.BillingInfo info = new BillingUtils.BillingInfo(
            "pro", "admin", "workspace_admin", true, true, true
        );
        assertEquals("pro", info.subscriptionType());
        assertEquals("admin", info.organizationRole());
        assertTrue(info.hasBillingAccess());
    }

    @Test
    @DisplayName("BillingUtils BillingInfo current")
    void billingInfoCurrent() {
        BillingUtils.BillingInfo info = BillingUtils.BillingInfo.current();
        assertNotNull(info);
    }
}
