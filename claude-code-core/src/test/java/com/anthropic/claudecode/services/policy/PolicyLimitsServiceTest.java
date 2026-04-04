/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PolicyLimitsService.
 */
class PolicyLimitsServiceTest {

    @BeforeEach
    void setUp() {
        PolicyLimitsService.stopBackgroundPolling();
        PolicyLimitsService.clearCache();
    }

    @Test
    @DisplayName("PolicyLimitsService PolicyRestriction record")
    void policyRestrictionRecord() {
        PolicyLimitsService.PolicyRestriction restriction = new PolicyLimitsService.PolicyRestriction(
            "allow_tool_use", true, "User has permission"
        );

        assertEquals("allow_tool_use", restriction.policy());
        assertTrue(restriction.allowed());
        assertEquals("User has permission", restriction.reason());
    }

    @Test
    @DisplayName("PolicyLimitsService PolicyLimitsResponse record")
    void policyLimitsResponseRecord() {
        Map<String, PolicyLimitsService.PolicyRestriction> restrictions = Map.of(
            "policy1", new PolicyLimitsService.PolicyRestriction("policy1", true, "ok")
        );

        PolicyLimitsService.PolicyLimitsResponse response = new PolicyLimitsService.PolicyLimitsResponse(restrictions);

        assertEquals(1, response.restrictions().size());
        assertTrue(response.restrictions().get("policy1").allowed());
    }

    @Test
    @DisplayName("PolicyLimitsService PolicyLimitsFetchResult record")
    void policyLimitsFetchResultRecord() {
        PolicyLimitsService.PolicyLimitsFetchResult success = new PolicyLimitsService.PolicyLimitsFetchResult(
            true, Map.of(), null, false
        );

        assertTrue(success.success());
        assertNull(success.error());
        assertFalse(success.skipRetry());

        PolicyLimitsService.PolicyLimitsFetchResult failure = new PolicyLimitsService.PolicyLimitsFetchResult(
            false, null, "Network error", true
        );

        assertFalse(failure.success());
        assertEquals("Network error", failure.error());
        assertTrue(failure.skipRetry());
    }

    @Test
    @DisplayName("PolicyLimitsService isPolicyAllowed returns true for unknown policy")
    void isPolicyAllowedUnknown() {
        // Without any cache loaded, should fail open
        boolean result = PolicyLimitsService.isPolicyAllowed("unknown_policy");

        assertTrue(result);
    }

    @Test
    @DisplayName("PolicyLimitsService getRestrictionsFromCache returns null without cache")
    void getRestrictionsFromCacheNull() {
        Map<String, PolicyLimitsService.PolicyRestriction> restrictions = PolicyLimitsService.getRestrictionsFromCache();

        assertNull(restrictions);
    }

    @Test
    @DisplayName("PolicyLimitsService clearCache clears all state")
    void clearCache() {
        PolicyLimitsService.clearCache();

        assertNull(PolicyLimitsService.getRestrictionsFromCache());
    }

    @Test
    @DisplayName("PolicyLimitsService waitForLoad returns completed future")
    void waitForLoad() throws Exception {
        PolicyLimitsService.clearCache();

        PolicyLimitsService.waitForLoad().get();

        // Should complete without error
    }

    @Test
    @DisplayName("PolicyLimitsService loadPolicyLimits returns future")
    void loadPolicyLimits() throws Exception {
        PolicyLimitsService.loadPolicyLimits().get();

        // Should complete without error
    }

    @Test
    @DisplayName("PolicyLimitsService refreshPolicyLimits returns future")
    void refreshPolicyLimits() throws Exception {
        PolicyLimitsService.refreshPolicyLimits().get();

        // Should complete without error
    }

    @Test
    @DisplayName("PolicyLimitsService startBackgroundPolling does nothing when not eligible")
    void startBackgroundPollingNotEligible() {
        PolicyLimitsService.startBackgroundPolling();

        // Should not throw
    }

    @Test
    @DisplayName("PolicyLimitsService stopBackgroundPolling stops polling")
    void stopBackgroundPolling() {
        PolicyLimitsService.stopBackgroundPolling();

        // Should not throw
    }

    @Test
    @DisplayName("PolicyLimitsService initializeLoadingPromise")
    void initializeLoadingPromise() {
        PolicyLimitsService.initializeLoadingPromise();

        // Should not throw
        PolicyLimitsService.waitForLoad();
    }
}