/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/hookHelpers.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.function.Consumer;
import org.json.*;

/**
 * Helper utilities for hooks.
 */
public final class HookHelpers {
    private HookHelpers() {}

    private static final String SYNTHETIC_OUTPUT_TOOL_NAME = "structured_output";

    /**
     * Schema for hook responses.
     */
    public static final JSONObject hookResponseSchema = new JSONObject()
        .put("type", "object")
        .put("properties", new JSONObject()
            .put("ok", new JSONObject()
                .put("type", "boolean")
                .put("description", "Whether the condition was met"))
            .put("reason", new JSONObject()
                .put("type", "string")
                .put("description", "Reason, if the condition was not met")))
        .put("required", List.of("ok"))
        .put("additionalProperties", false);

    /**
     * Add hook input JSON to prompt, substituting $ARGUMENTS placeholder.
     * Supports indexed arguments like $ARGUMENTS[0], $ARGUMENTS[1], or $0, $1.
     */
    public static String addArgumentsToPrompt(String prompt, String jsonInput) {
        return substituteArguments(prompt, jsonInput);
    }

    /**
     * Substitute arguments in prompt.
     */
    public static String substituteArguments(String prompt, String jsonInput) {
        // Replace $ARGUMENTS with entire JSON
        String result = prompt.replace("$ARGUMENTS", jsonInput);

        // Replace indexed arguments $ARGUMENTS[0], $ARGUMENTS[1], etc.
        try {
            Object parsed = new JSONTokener(jsonInput).nextValue();
            if (parsed instanceof JSONArray arr) {
                for (int i = 0; i < arr.length(); i++) {
                    result = result.replace("$ARGUMENTS[" + i + "]",
                        arr.optString(i, ""));
                    result = result.replace("$" + i,
                        arr.optString(i, ""));
                }
            } else if (parsed instanceof JSONObject obj) {
                // Handle object arguments by key
                for (String key : obj.keySet()) {
                    result = result.replace("$ARGUMENTS[" + key + "]",
                        obj.optString(key, ""));
                }
            }
        } catch (Exception e) {
            // If parsing fails, just use raw JSON
        }

        return result;
    }

    /**
     * Create a StructuredOutput tool configured for hook responses.
     */
    public static Map<String, Object> createStructuredOutputTool() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", SYNTHETIC_OUTPUT_TOOL_NAME);
        tool.put("input_schema", hookResponseSchema);
        tool.put("description", "Return verification result");
        return tool;
    }

    /**
     * Get the synthetic output tool name.
     */
    public static String getSyntheticOutputToolName() {
        return SYNTHETIC_OUTPUT_TOOL_NAME;
    }

    /**
     * Check if messages have a successful tool call.
     */
    public static boolean hasSuccessfulToolCall(
            List<Map<String, Object>> messages,
            String toolName) {

        for (Map<String, Object> msg : messages) {
            Object content = msg.get("content");
            if (content instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        String type = (String) map.get("type");
                        if ("tool_use".equals(type)) {
                            String name = (String) map.get("name");
                            if (toolName.equals(name)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Register a function hook that enforces structured output.
     */
    public static void registerStructuredOutputEnforcement(
            String sessionId,
            Consumer<SessionHooks.SessionStore> setAppState) {

        SessionHooks.addFunctionHook(
            setAppState,
            sessionId,
            "Stop",
            "",
            messages -> hasSuccessfulToolCall(messages, SYNTHETIC_OUTPUT_TOOL_NAME),
            "You MUST call the " + SYNTHETIC_OUTPUT_TOOL_NAME +
                " tool to complete this request. Call this tool now.",
            5000,
            null
        );
    }

    /**
     * Parse hook response from JSON.
     */
    public static HookResponse parseHookResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            boolean ok = obj.optBoolean("ok", false);
            String reason = obj.optString("reason", null);
            return new HookResponse(ok, reason);
        } catch (Exception e) {
            return new HookResponse(false, "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Hook response record.
     */
    public record HookResponse(boolean ok, String reason) {}

    /**
     * Create hook input JSON for tool use.
     */
    public static String createToolUseHookInput(
            String toolName,
            Map<String, Object> toolInput) {

        JSONObject obj = new JSONObject();
        obj.put("tool_name", toolName);
        obj.put("tool_input", new JSONObject(toolInput));
        return obj.toString();
    }

    /**
     * Create hook input JSON for session start.
     */
    public static String createSessionStartHookInput(
            String sessionId,
            String cwd) {

        JSONObject obj = new JSONObject();
        obj.put("session_id", sessionId);
        obj.put("cwd", cwd);
        obj.put("hook_event_name", "SessionStart");
        return obj.toString();
    }

    /**
     * Create hook input JSON for stop event.
     */
    public static String createStopHookInput(String reason) {
        JSONObject obj = new JSONObject();
        obj.put("reason", reason);
        obj.put("hook_event_name", "Stop");
        return obj.toString();
    }
}