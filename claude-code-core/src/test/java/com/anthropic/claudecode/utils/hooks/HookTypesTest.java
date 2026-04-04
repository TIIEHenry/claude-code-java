/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HookTypes.
 */
class HookTypesTest {

    @Test
    @DisplayName("HookTypes HookEvent enum values")
    void hookEventEnum() {
        HookTypes.HookEvent[] events = HookTypes.HookEvent.values();
        assertEquals(20, events.length);
    }

    @Test
    @DisplayName("HookTypes HookEvent getEventName")
    void hookEventGetEventName() {
        assertEquals("SessionStart", HookTypes.HookEvent.SESSION_START.getEventName());
        assertEquals("PreToolUse", HookTypes.HookEvent.PRE_TOOL_USE.getEventName());
        assertEquals("PostToolUse", HookTypes.HookEvent.POST_TOOL_USE.getEventName());
    }

    @Test
    @DisplayName("HookTypes HookEvent fromString")
    void hookEventFromString() {
        assertEquals(HookTypes.HookEvent.SESSION_START, HookTypes.HookEvent.fromString("SessionStart"));
        assertEquals(HookTypes.HookEvent.PRE_TOOL_USE, HookTypes.HookEvent.fromString("PreToolUse"));
        assertNull(HookTypes.HookEvent.fromString("UnknownEvent"));
    }

    @Test
    @DisplayName("HookTypes SessionStartHookInput record")
    void sessionStartHookInput() {
        HookTypes.SessionStartHookInput input = new HookTypes.SessionStartHookInput("session-1", "/home/user");
        assertEquals("session-1", input.sessionId());
        assertEquals("/home/user", input.cwd());
        assertEquals("SessionStart", input.hookEventName());
    }

    @Test
    @DisplayName("HookTypes PreToolUseHookInput record")
    void preToolUseHookInput() {
        Map<String, Object> toolInput = Map.of("command", "ls");
        HookTypes.PreToolUseHookInput input = new HookTypes.PreToolUseHookInput("Bash", toolInput);
        assertEquals("Bash", input.toolName());
        assertEquals(toolInput, input.toolInput());
        assertEquals("PreToolUse", input.hookEventName());
    }

    @Test
    @DisplayName("HookTypes PostToolUseHookInput record")
    void postToolUseHookInput() {
        Map<String, Object> toolInput = Map.of("command", "ls");
        HookTypes.PostToolUseHookInput input = new HookTypes.PostToolUseHookInput(
            "Bash", toolInput, "output", false
        );
        assertEquals("Bash", input.toolName());
        assertEquals("output", input.toolOutput());
        assertFalse(input.isError());
        assertEquals("PostToolUse", input.hookEventName());
    }

    @Test
    @DisplayName("HookTypes StopHookInput record")
    void stopHookInput() {
        HookTypes.StopHookInput input = new HookTypes.StopHookInput("user requested");
        assertEquals("user requested", input.reason());
        assertEquals("Stop", input.hookEventName());
    }

    @Test
    @DisplayName("HookTypes HookOutput success")
    void hookOutputSuccess() {
        HookTypes.HookOutput output = HookTypes.HookOutput.success("done");
        assertTrue(output.success());
        assertEquals("done", output.output());
        assertNull(output.error());
        assertEquals(0, output.exitCode());
    }

    @Test
    @DisplayName("HookTypes HookOutput error")
    void hookOutputError() {
        HookTypes.HookOutput output = HookTypes.HookOutput.error("failed");
        assertFalse(output.success());
        assertEquals("failed", output.error());
        assertNull(output.output());
        assertEquals(1, output.exitCode());
    }

    @Test
    @DisplayName("HookTypes HookOutput record")
    void hookOutputRecord() {
        HookTypes.HookOutput output = new HookTypes.HookOutput(true, "out", "err", 0);
        assertTrue(output.success());
        assertEquals("out", output.output());
        assertEquals("err", output.error());
        assertEquals(0, output.exitCode());
    }

    @Test
    @DisplayName("HookTypes HookMatcher record")
    void hookMatcherRecord() {
        HookTypes.HookMatcher matcher = new HookTypes.HookMatcher(
            "test-hook", "PreToolUse", List.of("Bash"), Map.of()
        );
        assertEquals("test-hook", matcher.hookName());
        assertEquals("PreToolUse", matcher.hookEvent());
        assertEquals(1, matcher.tools().size());
    }

    @Test
    @DisplayName("HookTypes HookCommand record")
    void hookCommandRecord() {
        HookTypes.HookCommand cmd = new HookTypes.HookCommand(
            "shell", "echo hello", null, null, Map.of("VAR", "value"), 5000
        );
        assertEquals("shell", cmd.type());
        assertEquals("echo hello", cmd.command());
        assertEquals(5000, cmd.timeout());
    }

    @Test
    @DisplayName("HookTypes HookInput interface permits correct types")
    void hookInputInterface() {
        // Test that all implementing types are valid
        HookTypes.HookInput session = new HookTypes.SessionStartHookInput("s1", "/");
        HookTypes.HookInput pre = new HookTypes.PreToolUseHookInput("Bash", Map.of());
        HookTypes.HookInput post = new HookTypes.PostToolUseHookInput("Bash", Map.of(), null, false);
        HookTypes.HookInput stop = new HookTypes.StopHookInput("done");

        assertNotNull(session);
        assertNotNull(pre);
        assertNotNull(post);
        assertNotNull(stop);
    }
}