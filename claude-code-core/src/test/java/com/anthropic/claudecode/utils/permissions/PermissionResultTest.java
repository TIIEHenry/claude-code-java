/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.permissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PermissionResult.
 */
class PermissionResultTest {

    @Test
    @DisplayName("PermissionResult Behavior enum values")
    void behaviorEnum() {
        PermissionResult.Behavior[] behaviors = PermissionResult.Behavior.values();
        assertEquals(4, behaviors.length);
        assertEquals(PermissionResult.Behavior.ALLOW, PermissionResult.Behavior.valueOf("ALLOW"));
        assertEquals(PermissionResult.Behavior.DENY, PermissionResult.Behavior.valueOf("DENY"));
        assertEquals(PermissionResult.Behavior.ASK, PermissionResult.Behavior.valueOf("ASK"));
        assertEquals(PermissionResult.Behavior.PASSTHROUGH, PermissionResult.Behavior.valueOf("PASSTHROUGH"));
    }

    @Test
    @DisplayName("PermissionResult Behavior getId")
    void behaviorGetId() {
        assertEquals("allow", PermissionResult.Behavior.ALLOW.getId());
        assertEquals("deny", PermissionResult.Behavior.DENY.getId());
        assertEquals("ask", PermissionResult.Behavior.ASK.getId());
        assertEquals("passthrough", PermissionResult.Behavior.PASSTHROUGH.getId());
    }

    @Test
    @DisplayName("PermissionResult Allow record")
    void allowRecord() {
        PermissionResult.Allow allow = new PermissionResult.Allow(null, null);
        assertEquals(PermissionResult.Behavior.ALLOW, allow.behavior());
        assertNull(allow.message());
        assertNull(allow.updatedInput());
        assertNull(allow.decisionReason());
    }

    @Test
    @DisplayName("PermissionResult Allow of")
    void allowOf() {
        PermissionResult.Allow allow = PermissionResult.Allow.of();
        assertNotNull(allow);
        assertEquals(PermissionResult.Behavior.ALLOW, allow.behavior());
    }

    @Test
    @DisplayName("PermissionResult Allow withInput")
    void allowWithInput() {
        Map<String, Object> input = Map.of("key", "value");
        PermissionResult.Allow allow = PermissionResult.Allow.withInput(input);
        assertEquals(input, allow.updatedInput());
    }

    @Test
    @DisplayName("PermissionResult Deny record")
    void denyRecord() {
        PermissionResult.Deny deny = new PermissionResult.Deny("Access denied", null, false);
        assertEquals(PermissionResult.Behavior.DENY, deny.behavior());
        assertEquals("Access denied", deny.message());
        assertFalse(deny.interrupt());
    }

    @Test
    @DisplayName("PermissionResult Deny of")
    void denyOf() {
        PermissionResult.Deny deny = PermissionResult.Deny.of("Denied");
        assertEquals("Denied", deny.message());
    }

    @Test
    @DisplayName("PermissionResult Deny withReason")
    void denyWithReason() {
        PermissionDecisionReason reason = new PermissionDecisionReason.Rule(null);
        PermissionResult.Deny deny = PermissionResult.Deny.withReason("Denied", reason);
        assertEquals("Denied", deny.message());
        assertEquals(reason, deny.decisionReason());
    }

    @Test
    @DisplayName("PermissionResult Ask record")
    void askRecord() {
        PermissionResult.Ask ask = new PermissionResult.Ask("Confirm?", null, null);
        assertEquals(PermissionResult.Behavior.ASK, ask.behavior());
        assertEquals("Confirm?", ask.message());
    }

    @Test
    @DisplayName("PermissionResult Ask of")
    void askOf() {
        PermissionResult.Ask ask = PermissionResult.Ask.of("Please confirm");
        assertEquals("Please confirm", ask.message());
    }

    @Test
    @DisplayName("PermissionResult Ask withSuggestions")
    void askWithSuggestions() {
        List<PermissionSuggestion> suggestions = List.of(
            PermissionSuggestion.allowTool("Bash")
        );
        PermissionResult.Ask ask = PermissionResult.Ask.withSuggestions("Confirm?", suggestions);
        assertEquals(suggestions, ask.suggestions());
    }

    @Test
    @DisplayName("PermissionResult Passthrough record")
    void passthroughRecord() {
        PermissionResult.Passthrough pt = new PermissionResult.Passthrough("No rule", null);
        assertEquals(PermissionResult.Behavior.PASSTHROUGH, pt.behavior());
        assertEquals("No rule", pt.message());
    }

    @Test
    @DisplayName("PermissionResult Passthrough of")
    void passthroughOf() {
        PermissionResult.Passthrough pt = PermissionResult.Passthrough.of("Continuing");
        assertEquals("Continuing", pt.message());
    }

    @Test
    @DisplayName("PermissionDecisionReason Rule record")
    void decisionReasonRule() {
        PermissionDecisionReason.Rule reason = new PermissionDecisionReason.Rule(null);
        assertNull(reason.rule());
    }

    @Test
    @DisplayName("PermissionDecisionReason Hook record")
    void decisionReasonHook() {
        PermissionDecisionReason.Hook reason = new PermissionDecisionReason.Hook("hook-name", "test");
        assertEquals("hook-name", reason.hookName());
        assertEquals("test", reason.reason());
    }

    @Test
    @DisplayName("PermissionDecisionReason Classifier record")
    void decisionReasonClassifier() {
        PermissionDecisionReason.Classifier reason = new PermissionDecisionReason.Classifier("auto", "approved");
        assertEquals("auto", reason.classifier());
        assertEquals("approved", reason.reason());
    }

    @Test
    @DisplayName("PermissionDecisionReason Mode record")
    void decisionReasonMode() {
        PermissionDecisionReason.Mode reason = new PermissionDecisionReason.Mode("accept");
        assertEquals("accept", reason.mode());
    }

    @Test
    @DisplayName("PermissionDecisionReason SafetyCheck record")
    void decisionReasonSafetyCheck() {
        PermissionDecisionReason.SafetyCheck reason = new PermissionDecisionReason.SafetyCheck("danger", true);
        assertEquals("danger", reason.reason());
        assertTrue(reason.classifierApprovable());
    }

    @Test
    @DisplayName("PermissionSuggestion allowTool")
    void permissionSuggestionAllowTool() {
        PermissionSuggestion suggestion = PermissionSuggestion.allowTool("Bash");
        assertEquals("allow", suggestion.type());
        assertEquals("Allow Bash", suggestion.description());
        assertEquals("Bash", suggestion.data().get("tool"));
    }

    @Test
    @DisplayName("PermissionSuggestion denyTool")
    void permissionSuggestionDenyTool() {
        PermissionSuggestion suggestion = PermissionSuggestion.denyTool("Bash");
        assertEquals("deny", suggestion.type());
        assertEquals("Deny Bash", suggestion.description());
    }
}