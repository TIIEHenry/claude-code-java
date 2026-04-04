/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PermissionResult sealed interface.
 */
class PermissionResultTest {

    @Test
    @DisplayName("Allow result has correct behavior")
    void allowResultWorks() {
        PermissionResult.Allow<String> allow = PermissionResult.allow("test-input");

        assertEquals("test-input", allow.updatedInput());
        assertEquals("allow", allow.behavior());
        assertFalse(allow.userModified());
        assertNull(allow.decisionReason());
    }

    @Test
    @DisplayName("Allow with userModified works")
    void allowWithUserModifiedWorks() {
        PermissionResult.Allow<String> allow = PermissionResult.allow("modified-input", true);

        assertEquals("modified-input", allow.updatedInput());
        assertTrue(allow.userModified());
    }

    @Test
    @DisplayName("Deny result has correct behavior")
    void denyResultWorks() {
        PermissionResult.Deny deny = PermissionResult.deny("Access denied");

        assertEquals("Access denied", deny.message());
        assertEquals("deny", deny.behavior());
        assertNull(deny.decisionReason());
    }

    @Test
    @DisplayName("Ask result has correct behavior")
    void askResultWorks() {
        PermissionResult.Ask<String> ask = PermissionResult.ask("Do you want to proceed?");

        assertEquals("Do you want to proceed?", ask.message());
        assertEquals("ask", ask.behavior());
        assertNull(ask.updatedInput());
    }

    @Test
    @DisplayName("Ask with updatedInput works")
    void askWithUpdatedInputWorks() {
        PermissionResult.Ask<Integer> ask = PermissionResult.ask("Confirm value?", 42);

        assertEquals("Confirm value?", ask.message());
        assertEquals(42, ask.updatedInput());
    }

    @Test
    @DisplayName("Pattern matching on PermissionResult works")
    void patternMatchingWorks() {
        PermissionResult result = PermissionResult.allow("input");

        String behavior;
        if (result instanceof PermissionResult.Allow<?> a) {
            behavior = "allowed: " + a.updatedInput();
        } else if (result instanceof PermissionResult.Deny d) {
            behavior = "denied: " + d.message();
        } else if (result instanceof PermissionResult.Ask<?> a) {
            behavior = "ask: " + a.message();
        } else {
            behavior = "unknown";
        }

        assertEquals("allowed: input", behavior);
    }

    @Test
    @DisplayName("Pattern matching on Deny works")
    void patternMatchingDenyWorks() {
        PermissionResult result = PermissionResult.deny("No access");

        String behavior;
        if (result instanceof PermissionResult.Allow<?>) {
            behavior = "allowed";
        } else if (result instanceof PermissionResult.Deny d) {
            behavior = "denied: " + d.message();
        } else if (result instanceof PermissionResult.Ask<?>) {
            behavior = "ask";
        } else {
            behavior = "unknown";
        }

        assertEquals("denied: No access", behavior);
    }

    @Test
    @DisplayName("Pattern matching on Ask works")
    void patternMatchingAskWorks() {
        PermissionResult result = PermissionResult.ask("Please confirm");

        String behavior;
        if (result instanceof PermissionResult.Allow<?>) {
            behavior = "allowed";
        } else if (result instanceof PermissionResult.Deny) {
            behavior = "denied";
        } else if (result instanceof PermissionResult.Ask<?> a) {
            behavior = "ask: " + a.message();
        } else {
            behavior = "unknown";
        }

        assertEquals("ask: Please confirm", behavior);
    }
}