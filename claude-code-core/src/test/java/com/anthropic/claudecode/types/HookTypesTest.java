/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HookTypes.
 */
class HookTypesTest {

    @Test
    @DisplayName("HookTypes HookEvent enum values")
    void hookEventEnum() {
        HookTypes.HookEvent[] events = HookTypes.HookEvent.values();
        assertEquals(15, events.length);
    }

    @Test
    @DisplayName("HookTypes HookEvent getValue")
    void hookEventGetValue() {
        assertEquals("PreToolUse", HookTypes.HookEvent.PRE_TOOL_USE.getValue());
        assertEquals("PostToolUse", HookTypes.HookEvent.POST_TOOL_USE.getValue());
        assertEquals("SessionStart", HookTypes.HookEvent.SESSION_START.getValue());
    }

    @Test
    @DisplayName("HookTypes HookEvent fromString")
    void hookEventFromString() {
        assertEquals(HookTypes.HookEvent.PRE_TOOL_USE, HookTypes.HookEvent.fromString("PreToolUse"));
        assertEquals(HookTypes.HookEvent.SESSION_START, HookTypes.HookEvent.fromString("SessionStart"));
        assertNull(HookTypes.HookEvent.fromString("InvalidEvent"));
    }

    @Test
    @DisplayName("HookTypes HookOutcome enum values")
    void hookOutcomeEnum() {
        HookTypes.HookOutcome[] outcomes = HookTypes.HookOutcome.values();
        assertEquals(4, outcomes.length);
        assertEquals(HookTypes.HookOutcome.SUCCESS, HookTypes.HookOutcome.valueOf("SUCCESS"));
        assertEquals(HookTypes.HookOutcome.BLOCKING, HookTypes.HookOutcome.valueOf("BLOCKING"));
        assertEquals(HookTypes.HookOutcome.NON_BLOCKING_ERROR, HookTypes.HookOutcome.valueOf("NON_BLOCKING_ERROR"));
        assertEquals(HookTypes.HookOutcome.CANCELLED, HookTypes.HookOutcome.valueOf("CANCELLED"));
    }

    @Test
    @DisplayName("HookTypes PermissionBehavior enum values")
    void permissionBehaviorEnum() {
        HookTypes.PermissionBehavior[] behaviors = HookTypes.PermissionBehavior.values();
        assertEquals(4, behaviors.length);
        assertEquals(HookTypes.PermissionBehavior.ASK, HookTypes.PermissionBehavior.valueOf("ASK"));
        assertEquals(HookTypes.PermissionBehavior.DENY, HookTypes.PermissionBehavior.valueOf("DENY"));
        assertEquals(HookTypes.PermissionBehavior.ALLOW, HookTypes.PermissionBehavior.valueOf("ALLOW"));
        assertEquals(HookTypes.PermissionBehavior.PASSTHROUGH, HookTypes.PermissionBehavior.valueOf("PASSTHROUGH"));
    }

    @Test
    @DisplayName("HookTypes HookBlockingError record")
    void hookBlockingErrorRecord() {
        HookTypes.HookBlockingError error = new HookTypes.HookBlockingError(
            "Access denied", "bash"
        );
        assertEquals("Access denied", error.blockingError());
        assertEquals("bash", error.command());
    }

    @Test
    @DisplayName("HookTypes PermissionRequestResult.Allow record")
    void permissionRequestResultAllow() {
        HookTypes.PermissionRequestResult.Allow allow = new HookTypes.PermissionRequestResult.Allow(
            Map.of("key", "value"), List.of()
        );
        assertTrue(allow instanceof HookTypes.PermissionRequestResult);
        assertEquals(1, allow.updatedInput().size());
        assertEquals(0, allow.updatedPermissions().size());
    }

    @Test
    @DisplayName("HookTypes PermissionRequestResult.Deny record")
    void permissionRequestResultDeny() {
        HookTypes.PermissionRequestResult.Deny deny = new HookTypes.PermissionRequestResult.Deny(
            "Not allowed", true
        );
        assertTrue(deny instanceof HookTypes.PermissionRequestResult);
        assertEquals("Not allowed", deny.message());
        assertTrue(deny.interrupt());
    }

    @Test
    @DisplayName("HookTypes PermissionUpdate record")
    void permissionUpdateRecord() {
        HookTypes.PermissionUpdate update = new HookTypes.PermissionUpdate(
            "Bash", "allow", "Safe command"
        );
        assertEquals("Bash", update.toolName());
        assertEquals("allow", update.behavior());
        assertEquals("Safe command", update.reason());
    }

    @Test
    @DisplayName("HookTypes HookResult success factory")
    void hookResultSuccess() {
        HookTypes.HookResult result = HookTypes.HookResult.success();
        assertNull(result.message());
        assertNull(result.blockingError());
        assertEquals(HookTypes.HookOutcome.SUCCESS, result.outcome());
        assertFalse(result.preventContinuation());
        assertFalse(result.retry());
    }

    @Test
    @DisplayName("HookTypes HookResult blocking factory")
    void hookResultBlocking() {
        HookTypes.HookResult result = HookTypes.HookResult.blocking(
            "Error occurred", "mycommand"
        );
        assertNotNull(result.blockingError());
        assertEquals("Error occurred", result.blockingError().blockingError());
        assertEquals("mycommand", result.blockingError().command());
        assertEquals(HookTypes.HookOutcome.BLOCKING, result.outcome());
        assertTrue(result.preventContinuation());
    }

    @Test
    @DisplayName("HookTypes HookResult full record")
    void hookResultRecord() {
        HookTypes.HookResult result = new HookTypes.HookResult(
            "msg", "sysMsg", null, HookTypes.HookOutcome.SUCCESS,
            false, null, null, null, null, null, null, null, null, true
        );

        assertEquals("msg", result.message());
        assertEquals("sysMsg", result.systemMessage());
        assertEquals(HookTypes.HookOutcome.SUCCESS, result.outcome());
        assertTrue(result.retry());
    }

    @Test
    @DisplayName("HookTypes AggregatedHookResult empty factory")
    void aggregatedHookResultEmpty() {
        HookTypes.AggregatedHookResult result = HookTypes.AggregatedHookResult.empty();
        assertNull(result.message());
        assertEquals(0, result.blockingErrors().size());
        assertFalse(result.preventContinuation());
        assertEquals(0, result.additionalContexts().size());
        assertFalse(result.retry());
    }

    @Test
    @DisplayName("HookTypes AggregatedHookResult full record")
    void aggregatedHookResultRecord() {
        HookTypes.HookBlockingError error = new HookTypes.HookBlockingError("err", "cmd");
        HookTypes.AggregatedHookResult result = new HookTypes.AggregatedHookResult(
            "msg", List.of(error), true, "stop", "reason",
            HookTypes.PermissionBehavior.ALLOW, List.of("ctx"),
            "initial", Map.of("k", "v"), null, null, true
        );

        assertEquals("msg", result.message());
        assertEquals(1, result.blockingErrors().size());
        assertTrue(result.preventContinuation());
        assertEquals("stop", result.stopReason());
        assertEquals(HookTypes.PermissionBehavior.ALLOW, result.permissionBehavior());
        assertEquals(1, result.additionalContexts().size());
        assertTrue(result.retry());
    }

    @Test
    @DisplayName("HookTypes HookInput record")
    void hookInputRecord() {
        HookTypes.HookInput input = new HookTypes.HookInput(
            HookTypes.HookEvent.PRE_TOOL_USE, Map.of("tool", "Bash")
        );
        assertEquals(HookTypes.HookEvent.PRE_TOOL_USE, input.event());
        assertEquals("Bash", input.data().get("tool"));
    }

    @Test
    @DisplayName("HookTypes HookJSONOutput.Sync record")
    void hookJsonOutputSync() {
        HookTypes.HookJSONOutput.Sync sync = new HookTypes.HookJSONOutput.Sync(
            true, false, null, "allow", "test", "system"
        );

        assertTrue(sync.continueAfter());
        assertFalse(sync.suppressOutput());
        assertEquals("allow", sync.decision());
        assertEquals("test", sync.reason());
        assertEquals("system", sync.systemMessage());
        assertTrue(sync instanceof HookTypes.HookJSONOutput);
    }

    @Test
    @DisplayName("HookTypes HookJSONOutput.Async record")
    void hookJsonOutputAsync() {
        HookTypes.HookJSONOutput.Async async = new HookTypes.HookJSONOutput.Async(5000);
        assertEquals(5000, async.asyncTimeout());
        assertTrue(async instanceof HookTypes.HookJSONOutput);
    }

    @Test
    @DisplayName("HookTypes isSyncHookJSONOutput true for sync")
    void isSyncHookJSONOutputTrue() {
        HookTypes.HookJSONOutput.Sync sync = new HookTypes.HookJSONOutput.Sync(
            true, false, null, null, null, null
        );
        assertTrue(HookTypes.isSyncHookJSONOutput(sync));
        assertFalse(HookTypes.isAsyncHookJSONOutput(sync));
    }

    @Test
    @DisplayName("HookTypes isAsyncHookJSONOutput true for async")
    void isAsyncHookJSONOutputTrue() {
        HookTypes.HookJSONOutput.Async async = new HookTypes.HookJSONOutput.Async(1000);
        assertTrue(HookTypes.isAsyncHookJSONOutput(async));
        assertFalse(HookTypes.isSyncHookJSONOutput(async));
    }
}