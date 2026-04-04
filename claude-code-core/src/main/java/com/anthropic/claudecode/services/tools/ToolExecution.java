/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tools/toolExecution
 */
package com.anthropic.claudecode.services.tools;

import com.anthropic.claudecode.message.Message;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Tool execution - Execute tool calls with permission checks.
 *
 * Handles permission checking, input validation, and tool execution.
 */
public final class ToolExecution {
    private final ToolHooks toolHooks;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Minimum total hook duration (ms) to show inline timing summary */
    public static final int HOOK_TIMING_DISPLAY_THRESHOLD_MS = 500;
    /** Log threshold for slow phases */
    public static final int SLOW_PHASE_LOG_THRESHOLD_MS = 2000;

    /**
     * Create tool execution.
     */
    public ToolExecution(ToolHooks toolHooks) {
        this.toolHooks = toolHooks;
    }

    /**
     * MCP server type enum.
     */
    public enum McpServerType {
        STDIO,
        SSE,
        HTTP,
        WS,
        SDK,
        SSE_IDE,
        WS_IDE,
        CLAUDEAI_PROXY
    }

    /**
     * Message update lazy record.
     */
    public record MessageUpdateLazy(
        Object message,
        ContextModifier contextModifier
    ) {
        public static MessageUpdateLazy of(Object message) {
            return new MessageUpdateLazy(message, null);
        }

        public boolean hasContextModifier() {
            return contextModifier != null;
        }
    }

    /**
     * Context modifier record.
     */
    public record ContextModifier(
        String toolUseId,
        Function<Object, Object> modifyContext
    ) {}

    /**
     * Tool execution context.
     */
    public record ToolExecutionContext(
        Object tool,
        String toolUseId,
        Object input,
        Object toolUseContext,
        Object canUseTool,
        Object assistantMessage,
        String messageId,
        String requestId,
        McpServerType mcpServerType,
        String mcpServerBaseUrl
    ) {}

    /**
     * Permission decision reason sealed interface.
     */
    public sealed interface PermissionDecisionReason permits
        PermissionPromptTool,
        RuleDecision,
        HookDecision,
        ModeDecision,
        ClassifierDecision,
        SandboxOverride {}

    public static final class PermissionPromptTool implements PermissionDecisionReason {
        private final String type = "permissionPromptTool";
        private final Object toolResult;
        public PermissionPromptTool(Object toolResult) { this.toolResult = toolResult; }
    }

    public static final class RuleDecision implements PermissionDecisionReason {
        private final String type = "rule";
        private final RuleInfo rule;
        public RuleDecision(RuleInfo rule) { this.rule = rule; }
    }

    public static final class HookDecision implements PermissionDecisionReason {
        private final String type = "hook";
        private final String hookName;
        private final String hookSource;
        private final String reason;
        public HookDecision(String hookName, String hookSource, String reason) {
            this.hookName = hookName;
            this.hookSource = hookSource;
            this.reason = reason;
        }
    }

    public static final class ModeDecision implements PermissionDecisionReason {
        private final String type = "mode";
        private final String mode;
        public ModeDecision(String mode) { this.mode = mode; }
    }

    public static final class ClassifierDecision implements PermissionDecisionReason {
        private final String type = "classifier";
        private final String classifier;
        private final String reason;
        public ClassifierDecision(String classifier, String reason) {
            this.classifier = classifier;
            this.reason = reason;
        }
    }

    public static final class SandboxOverride implements PermissionDecisionReason {
        private final String type = "sandboxOverride";
    }

    /**
     * Rule info record.
     */
    public record RuleInfo(String source, String behavior, String message) {}

    /**
     * Permission result sealed interface.
     */
    public sealed interface PermissionResult permits
        Allow,
        Deny,
        Ask {}

    public static final class Allow implements PermissionResult {
        private final String behavior = "allow";
        private final Object updatedInput;
        private final Object decisionReason;
        private final boolean userModified;
        private final List<Object> contentBlocks;
        private final String acceptFeedback;

        public Allow(Object input, Object reason, boolean userModified, List<Object> blocks, String feedback) {
            this.updatedInput = input;
            this.decisionReason = reason;
            this.userModified = userModified;
            this.contentBlocks = blocks;
            this.acceptFeedback = feedback;
        }

        public String getBehavior() { return behavior; }
        public Object getUpdatedInput() { return updatedInput; }
        public Object getDecisionReason() { return decisionReason; }
        public boolean isUserModified() { return userModified; }
        public List<Object> getContentBlocks() { return contentBlocks; }
        public String getAcceptFeedback() { return acceptFeedback; }
    }

    public static final class Deny implements PermissionResult {
        private final String behavior = "deny";
        private final String message;
        private final Object decisionReason;
        private final List<Object> contentBlocks;

        public Deny(String message, Object reason, List<Object> blocks) {
            this.message = message;
            this.decisionReason = reason;
            this.contentBlocks = blocks;
        }

        public String getBehavior() { return behavior; }
        public String getMessage() { return message; }
        public Object getDecisionReason() { return decisionReason; }
        public List<Object> getContentBlocks() { return contentBlocks; }
    }

    public static final class Ask implements PermissionResult {
        private final String behavior = "ask";
        private final Object updatedInput;
        private final String message;
        private final Object decisionReason;
        private final List<Object> contentBlocks;

        public Ask(Object input, String message, Object reason, List<Object> blocks) {
            this.updatedInput = input;
            this.message = message;
            this.decisionReason = reason;
            this.contentBlocks = blocks;
        }

        public String getBehavior() { return behavior; }
        public Object getUpdatedInput() { return updatedInput; }
        public String getMessage() { return message; }
        public Object getDecisionReason() { return decisionReason; }
        public List<Object> getContentBlocks() { return contentBlocks; }
    }

    /**
     * Tool result record.
     */
    public record ToolResult(
        Object data,
        Object contextModifier,
        Object mcpMeta,
        Object structuredOutput,
        List<Object> newMessages
    ) {
        public static ToolResult empty() {
            return new ToolResult(null, null, null, null, Collections.emptyList());
        }

        public boolean hasData() {
            return data != null;
        }
    }

    /**
     * Tool progress record.
     */
    public record ToolProgress(
        String toolUseId,
        Object data
    ) {}

    /**
     * Classify tool error.
     */
    public String classifyToolError(Throwable error) {
        if (error == null) {
            return "UnknownError";
        }

        // Check for known error types
        String name = error.getClass().getSimpleName();
        if (name != null && name.length() > 3) {
            return name.substring(0, Math.min(60, name.length()));
        }

        return "Error";
    }

    /**
     * Run tool use.
     */
    public CompletableFuture<List<MessageUpdateLazy>> runToolUse(
        ToolExecutionContext context,
        Consumer<ToolProgress> onProgress
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<MessageUpdateLazy> results = new ArrayList<>();

            // Check for abort
            if (isAborted(context)) {
                results.add(MessageUpdateLazy.of(createStopMessage(context.toolUseId())));
                return results;
            }

            // Validate input
            ValidationResult validation = validateInput(context);
            if (!validation.isValid()) {
                results.add(MessageUpdateLazy.of(createErrorResult(
                    context.toolUseId(),
                    "InputValidationError: " + validation.message()
                )));
                return results;
            }

            // Check permissions
            PermissionResult permission = checkPermissions(context);
            if (!(permission instanceof Allow)) {
                results.add(MessageUpdateLazy.of(createPermissionDeniedResult(
                    context.toolUseId(),
                    permission
                )));
                return results;
            }

            // Execute tool
            try {
                ToolResult result = executeTool(context, onProgress);
                results.add(MessageUpdateLazy.of(createToolResultMessage(
                    context.toolUseId(),
                    result
                )));
            } catch (Exception e) {
                results.add(MessageUpdateLazy.of(createErrorResult(
                    context.toolUseId(),
                    classifyToolError(e)
                )));
            }

            return results;
        }, scheduler);
    }

    /**
     * Check if aborted.
     */
    private boolean isAborted(ToolExecutionContext context) {
        // Implementation would check abort signal
        return false;
    }

    /**
     * Validate input.
     */
    private ValidationResult validateInput(ToolExecutionContext context) {
        // Implementation would validate input schema
        return new ValidationResult(true, null, null);
    }

    /**
     * Check permissions.
     */
    private PermissionResult checkPermissions(ToolExecutionContext context) {
        // Implementation would check permissions
        return new Allow(context.input(), null, false, Collections.emptyList(), null);
    }

    /**
     * Execute tool.
     */
    private ToolResult executeTool(ToolExecutionContext context, Consumer<ToolProgress> onProgress) {
        // Implementation would execute actual tool
        return ToolResult.empty();
    }

    /**
     * Create stop message.
     */
    private Object createStopMessage(String toolUseId) {
        return new ToolResultMessage(toolUseId, "Execution cancelled", true);
    }

    /**
     * Create error result.
     */
    private Object createErrorResult(String toolUseId, String error) {
        return new ToolResultMessage(toolUseId, error, true);
    }

    /**
     * Create permission denied result.
     */
    private Object createPermissionDeniedResult(String toolUseId, PermissionResult permission) {
        String message = permission instanceof Deny d ? d.getMessage() : "Permission denied";
        return new ToolResultMessage(toolUseId, message, true);
    }

    /**
     * Create tool result message.
     */
    private Object createToolResultMessage(String toolUseId, ToolResult result) {
        return new ToolResultMessage(toolUseId, result.hasData() ? result.data() : "", false);
    }

    /**
     * Validation result record.
     */
    public record ValidationResult(boolean isValid, String message, String errorCode) {}

    /**
     * Tool result message record.
     */
    public record ToolResultMessage(String toolUseId, Object content, boolean isError) {}

    /**
     * Execution result record.
     */
    public record ExecutionResult(
        boolean success,
        List<MessageUpdateLazy> messages,
        Duration duration,
        String error
    ) {
        public static ExecutionResult success(List<MessageUpdateLazy> messages, Duration duration) {
            return new ExecutionResult(true, messages, duration, null);
        }

        public static ExecutionResult failure(String error) {
            return new ExecutionResult(false, Collections.emptyList(), Duration.ZERO, error);
        }
    }

    /**
     * Shutdown executor.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}