/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HookHelpers.
 */
class HookHelpersTest {

    @Test
    @DisplayName("HookHelpers substituteArguments replaces $ARGUMENTS")
    void substituteArgumentsBasic() {
        String result = HookHelpers.substituteArguments("Input: $ARGUMENTS", "{\"key\":\"value\"}");

        assertEquals("Input: {\"key\":\"value\"}", result);
    }

    @Test
    @DisplayName("HookHelpers substituteArguments with array indices")
    void substituteArgumentsArrayIndices() {
        String result = HookHelpers.substituteArguments(
            "Args: $ARGUMENTS[0] and $ARGUMENTS[1]",
            "[\"first\", \"second\"]"
        );

        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
    }

    @Test
    @DisplayName("HookHelpers substituteArguments with short indices $0 $1")
    void substituteArgumentsShortIndices() {
        String result = HookHelpers.substituteArguments(
            "Args: $0 and $1",
            "[\"a\", \"b\"]"
        );

        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    @DisplayName("HookHelpers substituteArguments with object keys")
    void substituteArgumentsObjectKeys() {
        String result = HookHelpers.substituteArguments(
            "Name: $ARGUMENTS[name]",
            "{\"name\":\"test\",\"value\":123}"
        );

        assertTrue(result.contains("test"));
    }

    @Test
    @DisplayName("HookHelpers createStructuredOutputTool")
    void createStructuredOutputTool() {
        Map<String, Object> tool = HookHelpers.createStructuredOutputTool();

        assertEquals("structured_output", tool.get("name"));
        assertNotNull(tool.get("input_schema"));
        assertNotNull(tool.get("description"));
    }

    @Test
    @DisplayName("HookHelpers getSyntheticOutputToolName")
    void getSyntheticOutputToolName() {
        assertEquals("structured_output", HookHelpers.getSyntheticOutputToolName());
    }

    @Test
    @DisplayName("HookHelpers parseHookResponse success")
    void parseHookResponseSuccess() {
        HookHelpers.HookResponse response = HookHelpers.parseHookResponse("{\"ok\":true,\"reason\":\"test\"}");

        assertTrue(response.ok());
        assertEquals("test", response.reason());
    }

    @Test
    @DisplayName("HookHelpers parseHookResponse failure")
    void parseHookResponseFailure() {
        HookHelpers.HookResponse response = HookHelpers.parseHookResponse("{\"ok\":false}");

        assertFalse(response.ok());
        assertNull(response.reason());
    }

    @Test
    @DisplayName("HookHelpers parseHookResponse invalid JSON")
    void parseHookResponseInvalid() {
        HookHelpers.HookResponse response = HookHelpers.parseHookResponse("not json");

        assertFalse(response.ok());
        assertNotNull(response.reason());
    }

    @Test
    @DisplayName("HookHelpers createToolUseHookInput")
    void createToolUseHookInput() {
        String json = HookHelpers.createToolUseHookInput("bash", Map.of("command", "ls"));

        assertTrue(json.contains("\"tool_name\":\"bash\""));
        assertTrue(json.contains("command"));
    }

    @Test
    @DisplayName("HookHelpers createSessionStartHookInput")
    void createSessionStartHookInput() {
        String json = HookHelpers.createSessionStartHookInput("session-123", "/home/user");

        assertTrue(json.contains("\"session_id\":\"session-123\""));
        assertTrue(json.contains("\"cwd\":\"/home/user\""));
        assertTrue(json.contains("\"hook_event_name\":\"SessionStart\""));
    }

    @Test
    @DisplayName("HookHelpers createStopHookInput")
    void createStopHookInput() {
        String json = HookHelpers.createStopHookInput("user_requested");

        assertTrue(json.contains("\"reason\":\"user_requested\""));
        assertTrue(json.contains("\"hook_event_name\":\"Stop\""));
    }

    @Test
    @DisplayName("HookHelpers hookResponseSchema is valid")
    void hookResponseSchemaValid() {
        assertNotNull(HookHelpers.hookResponseSchema);
        assertEquals("object", HookHelpers.hookResponseSchema.getString("type"));
    }

    @Test
    @DisplayName("HookHelpers hasSuccessfulToolCall finds tool")
    void hasSuccessfulToolCallFound() {
        List<Map<String, Object>> messages = List.of(
            Map.of("content", List.of(
                Map.of("type", "tool_use", "name", "bash")
            ))
        );

        assertTrue(HookHelpers.hasSuccessfulToolCall(messages, "bash"));
    }

    @Test
    @DisplayName("HookHelpers hasSuccessfulToolCall not found")
    void hasSuccessfulToolCallNotFound() {
        List<Map<String, Object>> messages = List.of(
            Map.of("content", List.of(
                Map.of("type", "tool_use", "name", "read")
            ))
        );

        assertFalse(HookHelpers.hasSuccessfulToolCall(messages, "bash"));
    }
}