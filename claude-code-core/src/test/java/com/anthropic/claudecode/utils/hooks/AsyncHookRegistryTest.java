/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AsyncHookRegistry.
 */
class AsyncHookRegistryTest {

    @BeforeEach
    void setUp() {
        AsyncHookRegistry.clearAllAsyncHooks();
    }

    @Test
    @DisplayName("AsyncHookRegistry PendingAsyncHook record")
    void pendingAsyncHookRecord() {
        AsyncHookRegistry.PendingAsyncHook hook = new AsyncHookRegistry.PendingAsyncHook(
            "proc-123", "hook-456", "testHook", "PreToolUse",
            "bash", "plugin-1", System.currentTimeMillis(), 15000,
            "echo test", false, null, () -> {}
        );

        assertEquals("proc-123", hook.processId());
        assertEquals("hook-456", hook.hookId());
        assertEquals("testHook", hook.hookName());
        assertEquals("PreToolUse", hook.hookEvent());
        assertEquals("bash", hook.toolName());
        assertEquals(15000, hook.timeout());
    }

    @Test
    @DisplayName("AsyncHookRegistry ShellResult record")
    void shellResultRecord() {
        AsyncHookRegistry.ShellResult result = new AsyncHookRegistry.ShellResult(0, "output", "error");

        assertEquals(0, result.code());
        assertEquals("output", result.stdout());
        assertEquals("error", result.stderr());
    }

    @Test
    @DisplayName("AsyncHookRegistry SyncHookJSONOutput record")
    void syncHookJsonOutputRecord() {
        AsyncHookRegistry.SyncHookJSONOutput output = new AsyncHookRegistry.SyncHookJSONOutput(
            true, "reason", "{\"modified\":true}", Map.of("extra", "data")
        );

        assertTrue(output.blockTool());
        assertEquals("reason", output.blockReason());
        assertEquals("{\"modified\":true}", output.modifiedToolInputJSON());
        assertEquals("data", output.additionalFields().get("extra"));
    }

    @Test
    @DisplayName("AsyncHookRegistry AsyncHookJSONOutput record")
    void asyncHookJsonOutputRecord() {
        AsyncHookRegistry.AsyncHookJSONOutput output = new AsyncHookRegistry.AsyncHookJSONOutput(
            true, 30000, Map.of("key", "value")
        );

        assertTrue(output.async());
        assertEquals(30000, output.asyncTimeout());
        assertEquals("value", output.additionalFields().get("key"));
    }

    @Test
    @DisplayName("AsyncHookRegistry OutputResult record")
    void outputResultRecord() {
        AsyncHookRegistry.OutputResult result = new AsyncHookRegistry.OutputResult("out", "err", "combined");

        assertEquals("out", result.stdout());
        assertEquals("err", result.stderr());
        assertEquals("combined", result.output());
    }

    @Test
    @DisplayName("AsyncHookRegistry getPendingAsyncHooks returns empty initially")
    void getPendingAsyncHooksEmpty() {
        assertTrue(AsyncHookRegistry.getPendingAsyncHooks().isEmpty());
    }

    @Test
    @DisplayName("AsyncHookRegistry clearAllAsyncHooks")
    void clearAllAsyncHooks() {
        AsyncHookRegistry.clearAllAsyncHooks();
        assertTrue(AsyncHookRegistry.getPendingAsyncHooks().isEmpty());
    }

    @Test
    @DisplayName("AsyncHookRegistry checkForAsyncHookResponses returns empty")
    void checkForAsyncHookResponsesEmpty() throws Exception {
        var responses = AsyncHookRegistry.checkForAsyncHookResponses().get();
        assertNotNull(responses);
    }

    @Test
    @DisplayName("AsyncHookRegistry finalizePendingAsyncHooks completes")
    void finalizePendingAsyncHooks() throws Exception {
        AsyncHookRegistry.finalizePendingAsyncHooks().get();
        assertTrue(AsyncHookRegistry.getPendingAsyncHooks().isEmpty());
    }

    @Test
    @DisplayName("AsyncHookRegistry removeDeliveredAsyncHooks does nothing on empty")
    void removeDeliveredAsyncHooksEmpty() {
        AsyncHookRegistry.removeDeliveredAsyncHooks(java.util.List.of("nonexistent"));
        assertTrue(AsyncHookRegistry.getPendingAsyncHooks().isEmpty());
    }

    @Test
    @DisplayName("AsyncHookRegistry HookResponse record")
    void hookResponseRecord() {
        AsyncHookRegistry.SyncHookJSONOutput syncOutput = new AsyncHookRegistry.SyncHookJSONOutput(
            false, null, null, Map.of()
        );

        AsyncHookRegistry.HookResponse response = new AsyncHookRegistry.HookResponse(
            "proc-1", syncOutput, "hookName", "PreToolUse",
            "bash", "plugin-1", "stdout", "stderr", 0
        );

        assertEquals("proc-1", response.processId());
        assertEquals("hookName", response.hookName());
        assertEquals("PreToolUse", response.hookEvent());
        assertEquals("bash", response.toolName());
        assertEquals(0, response.exitCode());
    }
}