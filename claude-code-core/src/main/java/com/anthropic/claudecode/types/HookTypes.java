/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/hooks.ts
 */
package com.anthropic.claudecode.types;

import java.util.*;

/**
 * Hook types for event handling.
 */
public final class HookTypes {
    private HookTypes() {}

    /**
     * Hook event types.
     */
    public enum HookEvent {
        PRE_TOOL_USE("PreToolUse"),
        POST_TOOL_USE("PostToolUse"),
        POST_TOOL_USE_FAILURE("PostToolUseFailure"),
        USER_PROMPT_SUBMIT("UserPromptSubmit"),
        SESSION_START("SessionStart"),
        SETUP("Setup"),
        SUBAGENT_START("SubagentStart"),
        PERMISSION_DENIED("PermissionDenied"),
        NOTIFICATION("Notification"),
        PERMISSION_REQUEST("PermissionRequest"),
        ELICITATION("Elicitation"),
        ELICITATION_RESULT("ElicitationResult"),
        CWD_CHANGED("CwdChanged"),
        FILE_CHANGED("FileChanged"),
        WORKTREE_CREATE("WorktreeCreate");

        private final String value;

        HookEvent(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static HookEvent fromString(String value) {
            for (HookEvent event : values()) {
                if (event.value.equals(value)) {
                    return event;
                }
            }
            return null;
        }
    }

    /**
     * Hook outcome types.
     */
    public enum HookOutcome {
        SUCCESS,
        BLOCKING,
        NON_BLOCKING_ERROR,
        CANCELLED
    }

    /**
     * Permission behavior types.
     */
    public enum PermissionBehavior {
        ASK,
        DENY,
        ALLOW,
        PASSTHROUGH
    }

    /**
     * Hook blocking error.
     */
    public record HookBlockingError(String blockingError, String command) {}

    /**
     * Permission request result.
     */
    public sealed interface PermissionRequestResult permits
            PermissionRequestResult.Allow,
            PermissionRequestResult.Deny {

        record Allow(
                Map<String, Object> updatedInput,
                List<PermissionUpdate> updatedPermissions
        ) implements PermissionRequestResult {}

        record Deny(
                String message,
                boolean interrupt
        ) implements PermissionRequestResult {}
    }

    /**
     * Permission update.
     */
    public record PermissionUpdate(
            String toolName,
            String behavior,
            String reason
    ) {}

    /**
     * Hook result.
     */
    public record HookResult(
            Object message,
            Object systemMessage,
            HookBlockingError blockingError,
            HookOutcome outcome,
            boolean preventContinuation,
            String stopReason,
            PermissionBehavior permissionBehavior,
            String hookPermissionDecisionReason,
            String additionalContext,
            String initialUserMessage,
            Map<String, Object> updatedInput,
            Object updatedMCPToolOutput,
            PermissionRequestResult permissionRequestResult,
            boolean retry
    ) {
        public static HookResult success() {
            return new HookResult(null, null, null, HookOutcome.SUCCESS, false, null, null, null, null, null, null, null, null, false);
        }

        public static HookResult blocking(String error, String command) {
            return new HookResult(null, null, new HookBlockingError(error, command), HookOutcome.BLOCKING, true, error, null, null, null, null, null, null, null, false);
        }
    }

    /**
     * Aggregated hook result.
     */
    public record AggregatedHookResult(
            Object message,
            List<HookBlockingError> blockingErrors,
            boolean preventContinuation,
            String stopReason,
            String hookPermissionDecisionReason,
            PermissionBehavior permissionBehavior,
            List<String> additionalContexts,
            String initialUserMessage,
            Map<String, Object> updatedInput,
            Object updatedMCPToolOutput,
            PermissionRequestResult permissionRequestResult,
            boolean retry
    ) {
        public static AggregatedHookResult empty() {
            return new AggregatedHookResult(null, List.of(), false, null, null, null, List.of(), null, null, null, null, false);
        }
    }

    /**
     * Hook callback interface.
     */
    @FunctionalInterface
    public interface HookCallback {
        HookJSONOutput call(HookInput input, String toolUseId);
    }

    /**
     * Hook input.
     */
    public record HookInput(
            HookEvent event,
            Map<String, Object> data
    ) {}

    /**
     * Hook JSON output.
     */
    public sealed interface HookJSONOutput permits
            HookJSONOutput.Sync,
            HookJSONOutput.Async {

        record Sync(
                boolean continueAfter,
                boolean suppressOutput,
                String stopReason,
                String decision,
                String reason,
                String systemMessage
        ) implements HookJSONOutput {}

        record Async(
                int asyncTimeout
        ) implements HookJSONOutput {}
    }

    /**
     * Check if output is sync.
     */
    public static boolean isSyncHookJSONOutput(HookJSONOutput output) {
        return output instanceof HookJSONOutput.Sync;
    }

    /**
     * Check if output is async.
     */
    public static boolean isAsyncHookJSONOutput(HookJSONOutput output) {
        return output instanceof HookJSONOutput.Async;
    }
}