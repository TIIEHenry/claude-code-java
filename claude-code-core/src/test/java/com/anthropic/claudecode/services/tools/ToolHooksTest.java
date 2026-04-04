/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolHooks.
 */
class ToolHooksTest {

    @Test
    @DisplayName("ToolHooks HookType enum values")
    void hookTypeEnum() {
        ToolHooks.HookType[] types = ToolHooks.HookType.values();
        assertEquals(4, types.length);
        assertEquals(ToolHooks.HookType.PRE_TOOL_USE, ToolHooks.HookType.valueOf("PRE_TOOL_USE"));
        assertEquals(ToolHooks.HookType.POST_TOOL_USE, ToolHooks.HookType.valueOf("POST_TOOL_USE"));
        assertEquals(ToolHooks.HookType.POST_TOOL_USE_FAILURE, ToolHooks.HookType.valueOf("POST_TOOL_USE_FAILURE"));
        assertEquals(ToolHooks.HookType.PERMISSION_DENIED, ToolHooks.HookType.valueOf("PERMISSION_DENIED"));
    }

    @Test
    @DisplayName("ToolHooks MessageResult")
    void messageResult() {
        ToolHooks.MessageResult result = new ToolHooks.MessageResult("test message");

        assertEquals("test message", result.getMessage());
    }

    @Test
    @DisplayName("ToolHooks BlockingResult")
    void blockingResult() {
        ToolHooks.BlockingResult result = new ToolHooks.BlockingResult(
            "error message", true, "stop reason"
        );

        assertEquals("error message", result.getBlockingError());
        assertTrue(result.shouldPreventContinuation());
        assertEquals("stop reason", result.getStopReason());
    }

    @Test
    @DisplayName("ToolHooks PermissionResult")
    void permissionResult() {
        ToolHooks.PermissionResult result = new ToolHooks.PermissionResult(
            "allow", "updated input", "message", "reason"
        );

        assertEquals("allow", result.getBehavior());
        assertEquals("updated input", result.getUpdatedInput());
        assertEquals("message", result.getMessage());
        assertEquals("reason", result.getDecisionReason());
    }

    @Test
    @DisplayName("ToolHooks ContextResult")
    void contextResult() {
        ToolHooks.ContextResult result = new ToolHooks.ContextResult(
            List.of("context1", "context2"), "output"
        );

        assertEquals(2, result.getAdditionalContexts().size());
        assertEquals("output", result.getUpdatedMCPToolOutput());
    }

    @Test
    @DisplayName("ToolHooks HookExecutionContext record")
    void hookExecutionContext() {
        ToolHooks.HookExecutionContext context = new ToolHooks.HookExecutionContext(
            "bash", "tool-123", "input", "output",
            "ctx", "auto", false
        );

        assertEquals("bash", context.toolName());
        assertEquals("tool-123", context.toolUseId());
        assertEquals("input", context.input());
        assertEquals("output", context.output());
        assertEquals("ctx", context.toolUseContext());
        assertEquals("auto", context.permissionMode());
        assertFalse(context.isAbortSignal());
    }

    @Test
    @DisplayName("ToolHooks PreToolUseHookResult record")
    void preToolUseHookResult() {
        ToolHooks.PreToolUseHookResult result = new ToolHooks.PreToolUseHookResult(
            "message", "error", true, "stop",
            "allow", "input", "source", "reason", List.of("ctx")
        );

        assertEquals("message", result.message());
        assertEquals("error", result.blockingError());
        assertTrue(result.preventContinuation());
        assertEquals("stop", result.stopReason());
        assertEquals("allow", result.permissionBehavior());
        assertEquals("input", result.updatedInput());
        assertEquals("source", result.hookSource());
        assertEquals("reason", result.hookPermissionDecisionReason());
        assertEquals(1, result.additionalContexts().size());
    }

    @Test
    @DisplayName("ToolHooks PostToolUseHookResult record")
    void postToolUseHookResult() {
        ToolHooks.PostToolUseHookResult result = new ToolHooks.PostToolUseHookResult(
            "message", "error", true, "stop",
            List.of("ctx"), "output"
        );

        assertEquals("message", result.message());
        assertEquals("error", result.blockingError());
        assertTrue(result.preventContinuation());
        assertEquals("stop", result.stopReason());
        assertEquals(1, result.additionalContexts().size());
        assertEquals("output", result.updatedMCPToolOutput());
    }

    @Test
    @DisplayName("ToolHooks PermissionDecision isAllowed")
    void permissionDecisionIsAllowed() {
        ToolHooks.PermissionDecision allow = new ToolHooks.PermissionDecision("allow", "input", "msg", "reason");
        assertTrue(allow.isAllowed());
        assertFalse(allow.isDenied());
        assertFalse(allow.isAsk());
    }

    @Test
    @DisplayName("ToolHooks PermissionDecision isDenied")
    void permissionDecisionIsDenied() {
        ToolHooks.PermissionDecision deny = new ToolHooks.PermissionDecision("deny", "input", "msg", "reason");
        assertFalse(deny.isAllowed());
        assertTrue(deny.isDenied());
        assertFalse(deny.isAsk());
    }

    @Test
    @DisplayName("ToolHooks PermissionDecision isAsk")
    void permissionDecisionIsAsk() {
        ToolHooks.PermissionDecision ask = new ToolHooks.PermissionDecision("ask", "input", null, null);
        assertFalse(ask.isAllowed());
        assertFalse(ask.isDenied());
        assertTrue(ask.isAsk());
    }

    @Test
    @DisplayName("ToolHooks addListener and removeListener")
    void addRemoveListener() {
        ToolHooks hooks = new ToolHooks();
        ToolHooks.ToolHookListener listener = new TestListener();

        hooks.addListener(listener);
        hooks.removeListener(listener);

        // Should not throw
    }

    @Test
    @DisplayName("ToolHooks executePreToolHooks returns empty list")
    void executePreToolHooksEmpty() throws Exception {
        ToolHooks hooks = new ToolHooks();
        ToolHooks.HookExecutionContext context = new ToolHooks.HookExecutionContext(
            "bash", "id", null, null, null, "auto", false
        );

        List<ToolHooks.PreToolUseHookResult> results = hooks.executePreToolHooks(context).get();

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("ToolHooks executePostToolHooks returns empty list")
    void executePostToolHooksEmpty() throws Exception {
        ToolHooks hooks = new ToolHooks();
        ToolHooks.HookExecutionContext context = new ToolHooks.HookExecutionContext(
            "bash", "id", null, null, null, "auto", false
        );

        List<ToolHooks.PostToolUseHookResult> results = hooks.executePostToolHooks(context).get();

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("ToolHooks executePostToolUseFailureHooks returns empty list")
    void executePostToolUseFailureHooksEmpty() throws Exception {
        ToolHooks hooks = new ToolHooks();
        ToolHooks.HookExecutionContext context = new ToolHooks.HookExecutionContext(
            "bash", "id", null, null, null, "auto", false
        );

        List<ToolHooks.PostToolUseHookResult> results = hooks.executePostToolUseFailureHooks(context, "error").get();

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("ToolHooks resolveHookPermissionDecision with allow")
    void resolveHookPermissionDecisionAllow() {
        ToolHooks hooks = new ToolHooks();
        ToolHooks.PermissionResult pr = new ToolHooks.PermissionResult("allow", "new input", "msg", "reason");

        ToolHooks.PermissionDecision decision = hooks.resolveHookPermissionDecision(pr, "tool", "input", null, null, null, "id");

        assertTrue(decision.isAllowed());
        assertEquals("new input", decision.input());
    }

    @Test
    @DisplayName("ToolHooks resolveHookPermissionDecision with deny")
    void resolveHookPermissionDecisionDeny() {
        ToolHooks hooks = new ToolHooks();
        ToolHooks.PermissionResult pr = new ToolHooks.PermissionResult("deny", null, "denied", "reason");

        ToolHooks.PermissionDecision decision = hooks.resolveHookPermissionDecision(pr, "tool", "input", null, null, null, "id");

        assertTrue(decision.isDenied());
        assertEquals("input", decision.input());
    }

    @Test
    @DisplayName("ToolHooks resolveHookPermissionDecision with other result")
    void resolveHookPermissionDecisionOther() {
        ToolHooks hooks = new ToolHooks();
        ToolHooks.MessageResult mr = new ToolHooks.MessageResult("message");

        ToolHooks.PermissionDecision decision = hooks.resolveHookPermissionDecision(mr, "tool", "input", null, null, null, "id");

        assertTrue(decision.isAsk());
    }

    @Test
    @DisplayName("ToolHooks timing constants")
    void timingConstants() {
        assertEquals(500, ToolHooks.HOOK_TIMING_DISPLAY_THRESHOLD_MS);
        assertEquals(2000, ToolHooks.SLOW_PHASE_LOG_THRESHOLD_MS);
    }

    // Test listener implementation
    private static class TestListener implements ToolHooks.ToolHookListener {
        @Override
        public void onPreToolUse(ToolHooks.HookExecutionContext context, java.util.function.Consumer<ToolHooks.PreToolUseHookResult> resultConsumer) {}

        @Override
        public void onPostToolUse(ToolHooks.HookExecutionContext context, java.util.function.Consumer<ToolHooks.PostToolUseHookResult> resultConsumer) {}

        @Override
        public void onPostToolUseFailure(ToolHooks.HookExecutionContext context, String error, java.util.function.Consumer<ToolHooks.PostToolUseHookResult> resultConsumer) {}
    }
}