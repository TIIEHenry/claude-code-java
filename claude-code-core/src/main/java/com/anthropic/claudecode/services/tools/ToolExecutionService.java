/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tools/toolExecution.ts
 */
package com.anthropic.claudecode.services.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.types.MessageTypes;
import com.anthropic.claudecode.utils.MessageUtils;
import com.anthropic.claudecode.message.ContentBlock;
import com.anthropic.claudecode.hooks.HookManager;
import com.anthropic.claudecode.hooks.HookContext;

/**
 * Tool execution logic.
 */
public final class ToolExecutionService {
    private ToolExecutionService() {}

    /** Minimum total hook duration (ms) to show inline timing summary */
    public static final long HOOK_TIMING_DISPLAY_THRESHOLD_MS = 500;
    /** Log a debug warning when hooks/permission-decision block for this long */
    public static final long SLOW_PHASE_LOG_THRESHOLD_MS = 2000;

    /**
     * Message update lazy record.
     */
    public record MessageUpdate<M extends MessageTypes.Message>(
        M message,
        ContextModifier contextModifier
    ) {}

    /**
     * Context modifier record.
     */
    public record ContextModifier(
        String toolUseID,
        Function<ToolUseContext, ToolUseContext> modifyContext
    ) {}

    /**
     * MCP server type enum.
     */
    public enum McpServerType {
        STDIO, SSE, HTTP, WS, SDK, SSE_IDE, WS_IDE, CLAUDEAI_PROXY
    }

    /**
     * Convert MessageTypes.AssistantMessage to com.anthropic.claudecode.AssistantMessage.
     */
    private static AssistantMessage convertAssistantMessage(MessageTypes.AssistantMessage msg) {
        List<ContentBlock> contentBlocks = new ArrayList<>();
        for (Map<String, Object> block : msg.content()) {
            String type = (String) block.get("type");
            if ("text".equals(type)) {
                contentBlocks.add(new ContentBlock.Text((String) block.get("text")));
            } else if ("tool_use".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> input = (Map<String, Object>) block.get("input");
                contentBlocks.add(new ContentBlock.ToolUse(
                    (String) block.get("id"),
                    (String) block.get("name"),
                    input
                ));
            }
        }
        return new AssistantMessage(
            msg.id(),
            java.time.Instant.now(),
            "assistant",
            contentBlocks,
            msg.stopReason(),
            new AssistantMessage.Usage(0, 0, 0, 0)
        );
    }

    /**
     * Classify a tool execution error into a telemetry-safe string.
     */
    public static String classifyToolError(Throwable error) {
        if (error instanceof TelemetrySafeException) {
            return ((TelemetrySafeException) error).getTelemetryMessage().substring(0, Math.min(200, ((TelemetrySafeException) error).getTelemetryMessage().length()));
        }
        if (error != null && error.getClass().getName() != null && !error.getClass().getName().equals("Error") && error.getClass().getName().length() > 3) {
            return error.getClass().getName().substring(0, Math.min(60, error.getClass().getName().length()));
        }
        return "Error";
    }

    /**
     * Run tool use.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Iterator<MessageUpdate> runToolUse(
        ToolUseBlock toolUse,
        MessageTypes.AssistantMessage assistantMessage,
        CanUseToolFn canUseTool,
        ToolUseContext toolUseContext
    ) {
        String toolName = toolUse.name();
        List<Tool<?, ?, ?>> toolsWithType = toolUseContext.options().tools();
        // Find tool - need to cast to raw Tool for compatibility
        Tool tool = findToolByNameTyped(toolsWithType, toolName);

        // Check deprecated alias
        if (tool == null) {
            Tool fallbackTool = findToolByNameTyped(getAllBaseTools(), toolName);
            if (fallbackTool != null && fallbackTool.aliases().contains(toolName)) {
                tool = fallbackTool;
            }
        }

        String messageId = assistantMessage.id();
        String requestId = assistantMessage.id(); // Use id as requestId

        // Check if the tool exists
        if (tool == null) {
            List<MessageUpdate> errorList = Collections.singletonList(
                new MessageUpdate<>(
                    MessageUtils.createUserMessage(
                        "<tool_use_error>Error: No such tool available: " + toolName + "</tool_use_error>",
                        toolUse.id(),
                        assistantMessage.uuid()
                    ),
                    null
                )
            );
            return errorList.iterator();
        }

        // Run the tool
        List<MessageUpdate> results = new ArrayList<>();
        try {
            List<MessageUpdate> toolResults = checkPermissionsAndCallTool(
                tool,
                toolUse.id(),
                toolUse.input(),
                toolUseContext,
                canUseTool,
                assistantMessage,
                messageId,
                requestId,
                null,
                null,
                progress -> {}
            );
            results.addAll(toolResults);
        } catch (Exception error) {
            String errorMsg = error.getMessage();
            results.add(new MessageUpdate<>(
                MessageUtils.createUserMessage(
                    "<tool_use_error>Error calling tool (" + tool.name() + "): " + errorMsg + "</tool_use_error>",
                    toolUse.id(),
                    assistantMessage.uuid()
                ),
                null
            ));
        }

        return results.iterator();
    }

    /**
     * Check permissions and call tool.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<MessageUpdate> checkPermissionsAndCallTool(
        Tool tool,
        String toolUseID,
        Map<String, Object> input,
        ToolUseContext toolUseContext,
        CanUseToolFn canUseTool,
        MessageTypes.AssistantMessage assistantMessage,
        String messageId,
        String requestId,
        McpServerType mcpServerType,
        String mcpServerBaseUrl,
        Consumer<ToolProgress> onToolProgress
    ) {
        List<MessageUpdate> resultingMessages = new ArrayList<>();

        // Convert assistant message to the expected type for Tool.call
        AssistantMessage convertedAssistantMessage = convertAssistantMessage(assistantMessage);

        // Validate input types
        Object parsedInput = tool.getInputSchema().parse(input);
        if (parsedInput == null) {
            return Collections.singletonList(new MessageUpdate<>(
                MessageUtils.createUserMessage(
                    "<tool_use_error>InputValidationError: Invalid input</tool_use_error>",
                    toolUseID,
                    assistantMessage.uuid()
                ),
                null
            ));
        }

        // Validate input values
        Object validationResult = tool.validateInput(parsedInput, toolUseContext).join();
        ValidationResult isValidCall = (ValidationResult) validationResult;
        if (isValidCall != null && !isValidCall.isSuccess()) {
            String errorMsg = isValidCall.message();
            return Collections.singletonList(new MessageUpdate<>(
                MessageUtils.createUserMessage(
                    "<tool_use_error>" + isValidCall.message() + "</tool_use_error>",
                    toolUseID,
                    assistantMessage.uuid()
                ),
                null
            ));
        }

        // Run pre-tool hooks
        PermissionResult hookPermissionResult = null;
        boolean shouldPreventContinuation = false;
        String stopReason = null;

        for (HookResult result : runPreToolUseHooks(
            toolUseContext,
            tool,
            parsedInput,
            toolUseID,
            messageId,
            requestId,
            mcpServerType,
            mcpServerBaseUrl
        )) {
            if (result.message() != null) {
                resultingMessages.add(new MessageUpdate<>(result.message(), null));
            }
            if (result.permissionResult() != null) {
                hookPermissionResult = result.permissionResult();
            }
            if (result.preventContinuation()) {
                shouldPreventContinuation = true;
            }
            if (result.stopReason() != null) {
                stopReason = result.stopReason();
            }
        }

        // Check permissions
        PermissionDecision decision = resolveHookPermissionDecision(
            hookPermissionResult,
            tool,
            parsedInput,
            toolUseContext,
            canUseTool,
            convertedAssistantMessage,
            toolUseID
        );

        if (decision.behavior() != PermissionDecision.Behavior.ALLOW) {
            String errorMessage = decision.message();
            if (shouldPreventContinuation && errorMessage == null) {
                errorMessage = "Execution stopped by PreToolUse hook" + (stopReason != null ? ": " + stopReason : "");
            }
            resultingMessages.add(new MessageUpdate<>(
                MessageUtils.createUserMessage(
                    errorMessage != null ? errorMessage : "Permission denied",
                    toolUseID,
                    assistantMessage.uuid()
                ),
                null
            ));
            return resultingMessages;
        }

        // Use updated input from permissions if provided
        if (decision.updatedInput() != null) {
            parsedInput = decision.updatedInput();
        }

        // Call the tool
        long startTime = System.currentTimeMillis();
        try {
            ToolResult result = (ToolResult) tool.call(
                parsedInput,
                toolUseContext.withToolUseId(toolUseID),
                canUseTool,
                convertedAssistantMessage,
                progress -> onToolProgress.accept(new ToolProgress(toolUseID, progress))
            ).join();
            long durationMs = System.currentTimeMillis() - startTime;

            // Add tool result
            resultingMessages.add(new MessageUpdate<>(
                MessageUtils.createUserMessage(
                    result.data() != null ? result.data().toString() : "",
                    toolUseID,
                    assistantMessage.uuid()
                ),
                result.contextModifier() != null ?
                    new ContextModifier(toolUseID, result.contextModifier()) : null
            ));

            // Run post-tool hooks
            for (MessageUpdate hookResult : runPostToolUseHooks(
                toolUseContext,
                tool,
                toolUseID,
                messageId,
                parsedInput,
                result.data(),
                requestId,
                mcpServerType,
                mcpServerBaseUrl
            )) {
                resultingMessages.add(hookResult);
            }

            // Check for prevent continuation
            if (shouldPreventContinuation) {
                resultingMessages.add(new MessageUpdate<>(
                    MessageUtils.createAttachmentMessage(
                        "hook_stopped_continuation",
                        stopReason != null ? stopReason : "Execution stopped by hook",
                        toolUseID
                    ),
                    null
                ));
            }

            return resultingMessages;
        } catch (Exception error) {
            long durationMs = System.currentTimeMillis() - startTime;
            String content = error.getMessage();

            // Run post-tool failure hooks
            for (MessageUpdate hookResult : runPostToolUseFailureHooks(
                toolUseContext,
                tool,
                toolUseID,
                messageId,
                parsedInput,
                content,
                error instanceof AbortException,
                requestId,
                mcpServerType,
                mcpServerBaseUrl
            )) {
                resultingMessages.add(hookResult);
            }

            resultingMessages.add(new MessageUpdate<>(
                MessageUtils.createUserMessage(
                    "<tool_use_error>" + content + "</tool_use_error>",
                    toolUseID,
                    assistantMessage.uuid()
                ),
                null
            ));

            return resultingMessages;
        }
    }

    /**
     * Run pre-tool use hooks.
     */
    private static List<HookResult> runPreToolUseHooks(
        ToolUseContext toolUseContext,
        Tool tool,
        Object processedInput,
        String toolUseID,
        String messageId,
        String requestId,
        McpServerType mcpServerType,
        String mcpServerBaseUrl
    ) {
        // Get hook manager from context
        List<HookResult> results = new ArrayList<>();
        if (toolUseContext == null) return results;

        try {
            Map<String, Object> appState = toolUseContext.getAppState().apply(null);
            if (appState == null) return results;

            Object hookManagerObj = appState.get("hookManager");
            if (hookManagerObj instanceof HookManager hookManager) {
                // Build hook context using builder
                HookContext context = HookContext.builder()
                    .event("pre_tool_use")
                    .toolName(tool.name())
                    .input(processedInput)
                    .metadata("tool_use_id", toolUseID)
                    .metadata("message_id", messageId)
                    .metadata("request_id", requestId)
                    .metadata("mcp_server_type", mcpServerType != null ? mcpServerType.name() : null)
                    .metadata("mcp_server_url", mcpServerBaseUrl)
                    .build();

                // Trigger hook
                hookManager.trigger("pre_tool_use", context);

                // Extract results from context metadata
                Object hookResult = context.metadata().get("result");
                if (hookResult instanceof HookResult) {
                    results.add((HookResult) hookResult);
                } else if (hookResult instanceof List) {
                    for (Object r : (List<?>) hookResult) {
                        if (r instanceof HookResult) results.add((HookResult) r);
                    }
                }
            }
        } catch (Exception e) {
            // Hook execution failed, continue with empty results
        }
        return results;
    }

    /**
     * Run post-tool use hooks.
     */
    private static List<MessageUpdate> runPostToolUseHooks(
        ToolUseContext toolUseContext,
        Tool tool,
        String toolUseID,
        String messageId,
        Object processedInput,
        Object toolOutput,
        String requestId,
        McpServerType mcpServerType,
        String mcpServerBaseUrl
    ) {
        List<MessageUpdate> updates = new ArrayList<>();
        if (toolUseContext == null) return updates;

        try {
            Map<String, Object> appState = toolUseContext.getAppState().apply(null);
            if (appState == null) return updates;

            Object hookManagerObj = appState.get("hookManager");
            if (hookManagerObj instanceof HookManager hookManager) {
                // Build hook context using builder
                HookContext context = HookContext.builder()
                    .event("post_tool_use")
                    .toolName(tool.name())
                    .input(processedInput)
                    .output(toolOutput)
                    .metadata("tool_use_id", toolUseID)
                    .metadata("message_id", messageId)
                    .metadata("request_id", requestId)
                    .metadata("mcp_server_type", mcpServerType != null ? mcpServerType.name() : null)
                    .metadata("mcp_server_url", mcpServerBaseUrl)
                    .build();

                // Trigger hook
                hookManager.trigger("post_tool_use", context);

                // Extract message updates from context metadata
                Object result = context.metadata().get("result");
                if (result instanceof MessageUpdate) {
                    updates.add((MessageUpdate) result);
                } else if (result instanceof List) {
                    for (Object u : (List<?>) result) {
                        if (u instanceof MessageUpdate) updates.add((MessageUpdate) u);
                    }
                }
            }
        } catch (Exception e) {
            // Hook execution failed
        }
        return updates;
    }

    /**
     * Run post-tool use failure hooks.
     */
    private static List<MessageUpdate> runPostToolUseFailureHooks(
        ToolUseContext toolUseContext,
        Tool tool,
        String toolUseID,
        String messageId,
        Object processedInput,
        String error,
        boolean isInterrupt,
        String requestId,
        McpServerType mcpServerType,
        String mcpServerBaseUrl
    ) {
        List<MessageUpdate> updates = new ArrayList<>();
        if (toolUseContext == null) return updates;

        try {
            Map<String, Object> appState = toolUseContext.getAppState().apply(null);
            if (appState == null) return updates;

            Object hookManagerObj = appState.get("hookManager");
            if (hookManagerObj instanceof HookManager hookManager) {
                // Build hook context using builder
                HookContext context = HookContext.builder()
                    .event("post_tool_use_failure")
                    .toolName(tool.name())
                    .input(processedInput)
                    .error(new RuntimeException(error))
                    .metadata("tool_use_id", toolUseID)
                    .metadata("message_id", messageId)
                    .metadata("request_id", requestId)
                    .metadata("is_interrupt", isInterrupt)
                    .metadata("mcp_server_type", mcpServerType != null ? mcpServerType.name() : null)
                    .metadata("mcp_server_url", mcpServerBaseUrl)
                    .build();

                // Trigger hook
                hookManager.trigger("post_tool_use_failure", context);

                // Extract message updates from context metadata
                Object result = context.metadata().get("result");
                if (result instanceof MessageUpdate) {
                    updates.add((MessageUpdate) result);
                } else if (result instanceof List) {
                    for (Object u : (List<?>) result) {
                        if (u instanceof MessageUpdate) updates.add((MessageUpdate) u);
                    }
                }
            }
        } catch (Exception e) {
            // Hook execution failed
        }
        return updates;
    }

    /**
     * Resolve hook permission decision.
     */
    private static PermissionDecision resolveHookPermissionDecision(
        PermissionResult hookPermissionResult,
        Tool tool,
        Object input,
        ToolUseContext toolUseContext,
        CanUseToolFn canUseTool,
        AssistantMessage assistantMessage,
        String toolUseID
    ) {
        if (hookPermissionResult != null && hookPermissionResult.behavior() == PermissionResult.Behavior.ALLOW) {
            Object hookInput = hookPermissionResult.updatedInput() != null ?
                hookPermissionResult.updatedInput() : input;

            // Hook allow skips the interactive prompt, but deny/ask rules still apply
            PermissionResult ruleCheck = checkRuleBasedPermissions(tool, hookInput, toolUseContext);
            if (ruleCheck == null) {
                return new PermissionDecision(PermissionDecision.Behavior.ALLOW, hookInput, null, null, null);
            }
            if (ruleCheck.behavior() == PermissionResult.Behavior.DENY) {
                return new PermissionDecision(PermissionDecision.Behavior.DENY, hookInput, ruleCheck.message(), null, null);
            }
        }

        if (hookPermissionResult != null && hookPermissionResult.behavior() == PermissionResult.Behavior.DENY) {
            return new PermissionDecision(PermissionDecision.Behavior.DENY, input, hookPermissionResult.message(), null, null);
        }

        // No hook decision - normal permission flow
        try {
            com.anthropic.claudecode.permission.PermissionResult result = canUseTool.apply(tool, input, toolUseContext, assistantMessage, toolUseID).get();
            // Convert PermissionResult to PermissionDecision
            if (result instanceof com.anthropic.claudecode.permission.PermissionResult.Allow<?> allow) {
                return new PermissionDecision(PermissionDecision.Behavior.ALLOW, allow.updatedInput(), null, allow.userModified(), null);
            } else if (result instanceof com.anthropic.claudecode.permission.PermissionResult.Deny deny) {
                return new PermissionDecision(PermissionDecision.Behavior.DENY, input, deny.message(), null, null);
            } else if (result instanceof com.anthropic.claudecode.permission.PermissionResult.Ask<?> ask) {
                return new PermissionDecision(PermissionDecision.Behavior.ASK, ask.updatedInput(), ask.message(), null, null);
            }
            return new PermissionDecision(PermissionDecision.Behavior.ALLOW, input, null, null, null);
        } catch (Exception e) {
            return new PermissionDecision(PermissionDecision.Behavior.DENY, input, e.getMessage(), null, null);
        }
    }

    private static PermissionResult checkRuleBasedPermissions(Tool tool, Object input, ToolUseContext context) {
        // Check permission rules from settings
        try {
            // Load settings from file
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(
                System.getProperty("user.home"), ".claude", "settings.json"
            );

            if (!java.nio.file.Files.exists(settingsPath)) {
                return null; // No settings file, no rules to check
            }

            String content = java.nio.file.Files.readString(settingsPath);

            // Find permissions section
            int permIdx = content.indexOf("\"permissions\"");
            if (permIdx < 0) return null;

            // Find the array
            int arrStart = content.indexOf("[", permIdx);
            if (arrStart < 0) return null;

            int depth = 1;
            int arrEnd = arrStart + 1;
            while (arrEnd < content.length() && depth > 0) {
                char c = content.charAt(arrEnd);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                arrEnd++;
            }

            String permArray = content.substring(arrStart, arrEnd);

            // Check each rule
            int i = 0;
            while (i < permArray.length()) {
                int ruleStart = permArray.indexOf("{", i);
                if (ruleStart < 0) break;

                int ruleDepth = 1;
                int ruleEnd = ruleStart + 1;
                while (ruleEnd < permArray.length() && ruleDepth > 0) {
                    char c = permArray.charAt(ruleEnd);
                    if (c == '{') ruleDepth++;
                    else if (c == '}') ruleDepth--;
                    ruleEnd++;
                }

                String rule = permArray.substring(ruleStart, ruleEnd);

                // Parse rule
                String ruleTool = extractJsonValueString(rule, "tool");
                String ruleBehavior = extractJsonValueString(rule, "behavior");

                // Check if rule applies to this tool
                if (ruleTool != null && (ruleTool.equals("*") || ruleTool.equals(tool.name()))) {
                    if ("allow".equals(ruleBehavior)) {
                        return new PermissionResult(PermissionResult.Behavior.ALLOW, input, null);
                    } else if ("deny".equals(ruleBehavior)) {
                        String message = extractJsonValueString(rule, "message");
                        return new PermissionResult(PermissionResult.Behavior.DENY, input, message != null ? message : "Permission denied by rule");
                    }
                }

                i = ruleEnd;
            }
        } catch (Exception e) {
            // Error reading settings, continue without rules
        }
        return null;
    }

    private static String extractJsonValueString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 3) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    @SuppressWarnings("unchecked")
    private static Tool findToolByNameTyped(List<Tool<?, ?, ?>> tools, String name) {
        return tools.stream()
            .filter(t -> t.name().equals(name) || t.aliases().contains(name))
            .findFirst()
            .orElse(null);
    }

    private static List<Tool<?, ?, ?>> getAllBaseTools() {
        // Load all base tools - instantiate via reflection from tools module
        List<Tool<?, ?, ?>> tools = new ArrayList<>();

        // Known base tools - would normally be loaded via ServiceLoader or DI
        String[] toolClasses = {
            "com.anthropic.claudecode.tools.BashTool",
            "com.anthropic.claudecode.tools.FileReadTool",
            "com.anthropic.claudecode.tools.FileWriteTool",
            "com.anthropic.claudecode.tools.FileEditTool",
            "com.anthropic.claudecode.tools.GlobTool",
            "com.anthropic.claudecode.tools.GrepTool",
            "com.anthropic.claudecode.tools.WebFetchTool",
            "com.anthropic.claudecode.tools.WebSearchTool",
            "com.anthropic.claudecode.tools.AgentTool",
            "com.anthropic.claudecode.tools.TaskTool",
            "com.anthropic.claudecode.tools.AskUserQuestionTool"
        };

        for (String className : toolClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                // Try to get singleton instance or create new instance
                try {
                    java.lang.reflect.Field instanceField = clazz.getField("INSTANCE");
                    Object instance = instanceField.get(null);
                    if (instance instanceof Tool) {
                        tools.add((Tool<?, ?, ?>) instance);
                    }
                } catch (NoSuchFieldException e) {
                    // Try to instantiate
                    try {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        if (instance instanceof Tool) {
                            tools.add((Tool<?, ?, ?>) instance);
                        }
                    } catch (Exception ex) {
                        // Could not instantiate
                    }
                }
            } catch (ClassNotFoundException e) {
                // Tool class not found, skip
            } catch (Exception e) {
                // Other error, skip
            }
        }

        return tools;
    }

    /**
     * Tool use block.
     */
    public record ToolUseBlock(
        String id,
        String name,
        Map<String, Object> input
    ) {}

    /**
     * Tool progress record.
     */
    public record ToolProgress(
        String toolUseID,
        Object data
    ) {}

    /**
     * Hook result record.
     */
    public record HookResult(
        MessageTypes.Message message,
        PermissionResult permissionResult,
        boolean preventContinuation,
        String stopReason
    ) {}

    /**
     * Permission result record.
     */
    public record PermissionResult(
        Behavior behavior,
        Object updatedInput,
        String message
    ) {
        public enum Behavior {
            ALLOW, DENY, ASK
        }
    }

    /**
     * Permission decision record.
     */
    public record PermissionDecision(
        Behavior behavior,
        Object updatedInput,
        String message,
        Boolean userModified,
        String acceptFeedback
    ) {
        public enum Behavior {
            ALLOW, DENY, ASK
        }
    }

    /**
     * Telemetry safe exception.
     */
    public static class TelemetrySafeException extends Exception {
        private final String telemetryMessage;
        public TelemetrySafeException(String telemetryMessage) {
            super(telemetryMessage);
            this.telemetryMessage = telemetryMessage;
        }
        public String getTelemetryMessage() { return telemetryMessage; }
    }

    /**
     * Abort exception.
     */
    public static class AbortException extends Exception {
        public AbortException(String message) { super(message); }
    }
}