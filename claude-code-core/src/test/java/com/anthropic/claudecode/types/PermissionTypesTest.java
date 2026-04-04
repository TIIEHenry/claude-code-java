/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PermissionTypes.
 */
class PermissionTypesTest {

    @Test
    @DisplayName("PermissionTypes PermissionMode enum values")
    void permissionModeEnum() {
        PermissionTypes.PermissionMode[] modes = PermissionTypes.PermissionMode.values();
        assertEquals(7, modes.length);
    }

    @Test
    @DisplayName("PermissionTypes PermissionMode getValue")
    void permissionModeGetValue() {
        assertEquals("default", PermissionTypes.PermissionMode.DEFAULT.getValue());
        assertEquals("acceptEdits", PermissionTypes.PermissionMode.ACCEPT_EDITS.getValue());
        assertEquals("plan", PermissionTypes.PermissionMode.PLAN.getValue());
        assertEquals("auto", PermissionTypes.PermissionMode.AUTO.getValue());
    }

    @Test
    @DisplayName("PermissionTypes PermissionMode fromValue")
    void permissionModeFromValue() {
        assertEquals(PermissionTypes.PermissionMode.DEFAULT, PermissionTypes.PermissionMode.fromValue("default"));
        assertEquals(PermissionTypes.PermissionMode.PLAN, PermissionTypes.PermissionMode.fromValue("plan"));
        assertEquals(PermissionTypes.PermissionMode.DEFAULT, PermissionTypes.PermissionMode.fromValue("invalid"));
    }

    @Test
    @DisplayName("PermissionTypes PermissionAction enum values")
    void permissionActionEnum() {
        PermissionTypes.PermissionAction[] actions = PermissionTypes.PermissionAction.values();
        assertEquals(5, actions.length);
    }

    @Test
    @DisplayName("PermissionTypes PermissionSource enum values")
    void permissionSourceEnum() {
        PermissionTypes.PermissionSource[] sources = PermissionTypes.PermissionSource.values();
        assertEquals(5, sources.length);
    }

    @Test
    @DisplayName("PermissionTypes PermissionOption allow")
    void permissionOptionAllow() {
        PermissionTypes.PermissionOption option = PermissionTypes.PermissionOption.allow();

        assertEquals("allow", option.id());
        assertEquals("Allow", option.label());
        assertEquals(PermissionTypes.PermissionAction.ALLOW, option.action());
    }

    @Test
    @DisplayName("PermissionTypes PermissionOption deny")
    void permissionOptionDeny() {
        PermissionTypes.PermissionOption option = PermissionTypes.PermissionOption.deny();

        assertEquals("deny", option.id());
        assertEquals("Deny", option.label());
        assertEquals(PermissionTypes.PermissionAction.DENY, option.action());
    }

    @Test
    @DisplayName("PermissionTypes PermissionOption allowAlways")
    void permissionOptionAllowAlways() {
        PermissionTypes.PermissionOption option = PermissionTypes.PermissionOption.allowAlways();

        assertEquals("allow_always", option.id());
        assertEquals(PermissionTypes.PermissionAction.ALLOW_ALWAYS, option.action());
    }

    @Test
    @DisplayName("PermissionTypes PermissionRule matches exact")
    void permissionRuleMatchesExact() {
        PermissionTypes.PermissionRule rule = new PermissionTypes.PermissionRule(
            "id", "bash", PermissionTypes.PermissionAction.ALLOW,
            "desc", "test", false, Instant.now()
        );

        assertTrue(rule.matches("bash"));
        assertFalse(rule.matches("bash-edit"));
        assertFalse(rule.matches("other"));
    }

    @Test
    @DisplayName("PermissionTypes PermissionRule matches wildcard prefix")
    void permissionRuleMatchesPrefix() {
        PermissionTypes.PermissionRule rule = new PermissionTypes.PermissionRule(
            "id", "bash*", PermissionTypes.PermissionAction.ALLOW,
            "desc", "test", false, Instant.now()
        );

        assertTrue(rule.matches("bash"));
        assertTrue(rule.matches("bash-edit"));
        assertTrue(rule.matches("bash-run"));
        assertFalse(rule.matches("other"));
    }

    @Test
    @DisplayName("PermissionTypes PermissionRule matches wildcard suffix")
    void permissionRuleMatchesSuffix() {
        PermissionTypes.PermissionRule rule = new PermissionTypes.PermissionRule(
            "id", "*-tool", PermissionTypes.PermissionAction.ALLOW,
            "desc", "test", false, Instant.now()
        );

        assertTrue(rule.matches("bash-tool"));
        assertTrue(rule.matches("file-tool"));
        assertFalse(rule.matches("tool"));
        assertFalse(rule.matches("other"));
    }

    @Test
    @DisplayName("PermissionTypes PermissionRule matches all")
    void permissionRuleMatchesAll() {
        PermissionTypes.PermissionRule rule = new PermissionTypes.PermissionRule(
            "id", "*", PermissionTypes.PermissionAction.ALLOW,
            "desc", "test", false, Instant.now()
        );

        assertTrue(rule.matches("anything"));
        assertTrue(rule.matches("bash"));
        assertTrue(rule.matches(""));
    }

    @Test
    @DisplayName("PermissionTypes PermissionContext forTool")
    void permissionContextForTool() {
        PermissionTypes.PermissionContext ctx = PermissionTypes.PermissionContext.forTool(
            "Bash", Map.of("command", "ls")
        );

        assertEquals("Bash", ctx.toolName());
        assertEquals("ls", ctx.params().get("command"));
    }

    @Test
    @DisplayName("PermissionTypes PermissionContext forFile")
    void permissionContextForFile() {
        PermissionTypes.PermissionContext ctx = PermissionTypes.PermissionContext.forFile(
            "read", "/home/user/file.txt"
        );

        assertEquals("file", ctx.toolName());
        assertEquals("read", ctx.operation());
        assertEquals("/home/user/file.txt", ctx.resource());
    }

    @Test
    @DisplayName("PermissionTypes PermissionDecision record")
    void permissionDecisionRecord() {
        PermissionTypes.PermissionDecision decision = new PermissionTypes.PermissionDecision(
            "dec-1", "rule-1", PermissionTypes.PermissionAction.ALLOW,
            "User approved", Instant.now(), "user"
        );

        assertEquals("dec-1", decision.id());
        assertEquals("rule-1", decision.ruleId());
        assertEquals(PermissionTypes.PermissionAction.ALLOW, decision.action());
        assertEquals("User approved", decision.reason());
    }
}