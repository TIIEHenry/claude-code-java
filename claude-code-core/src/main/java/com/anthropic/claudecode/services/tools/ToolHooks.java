/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/tools/toolHooks
 */
package com.anthropic.claudecode.services.tools;

import com.anthropic.claudecode.message.Message;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Tool hooks - Hook execution for tool use.
 *
 * Handles PreToolUse, PostToolUse, and PostToolUseFailure hooks.
 */
public final class ToolHooks {
    private final List<ToolHookListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Hook type enum.
     */
    public enum HookType {
        PRE_TOOL_USE,
        POST_TOOL_USE,
        POST_TOOL_USE_FAILURE,
        PERMISSION_DENIED
    }

    /**
     * Hook result sealed interface.
     */
    public sealed interface HookResult permits
        MessageResult,
        BlockingResult,
        PermissionResult,
        ContextResult {}

    public static final class MessageResult implements HookResult {
        private final Object message;
        public MessageResult(Object message) { this.message = message; }
        public Object getMessage() { return message; }
    }

    public static final class BlockingResult implements HookResult {
        private final String blockingError;
        private final boolean preventContinuation;
        private final String stopReason;
        public BlockingResult(String error, boolean prevent, String reason) {
            this.blockingError = error;
            this.preventContinuation = prevent;
            this.stopReason = reason;
        }
        public String getBlockingError() { return blockingError; }
        public boolean shouldPreventContinuation() { return preventContinuation; }
        public String getStopReason() { return stopReason; }
    }

    public static final class PermissionResult implements HookResult {
        private final String behavior;
        private final Object updatedInput;
        private final String message;
        private final Object decisionReason;
        public PermissionResult(String behavior, Object input, String msg, Object reason) {
            this.behavior = behavior;
            this.updatedInput = input;
            this.message = msg;
            this.decisionReason = reason;
        }
        public String getBehavior() { return behavior; }
        public Object getUpdatedInput() { return updatedInput; }
        public String getMessage() { return message; }
        public Object getDecisionReason() { return decisionReason; }
    }

    public static final class ContextResult implements HookResult {
        private final List<String> additionalContexts;
        private final Object updatedMCPToolOutput;
        public ContextResult(List<String> contexts, Object output) {
            this.additionalContexts = contexts;
            this.updatedMCPToolOutput = output;
        }
        public List<String> getAdditionalContexts() { return additionalContexts; }
        public Object getUpdatedMCPToolOutput() { return updatedMCPToolOutput; }
    }

    /**
     * Hook execution context.
     */
    public record HookExecutionContext(
        String toolName,
        String toolUseId,
        Object input,
        Object output,
        Object toolUseContext,
        String permissionMode,
        boolean isAbortSignal
    ) {}

    /**
     * Pre tool use hook result.
     */
    public record PreToolUseHookResult(
        Object message,
        String blockingError,
        boolean preventContinuation,
        String stopReason,
        String permissionBehavior,
        Object updatedInput,
        String hookSource,
        String hookPermissionDecisionReason,
        List<String> additionalContexts
    ) {}

    /**
     * Post tool use hook result.
     */
    public record PostToolUseHookResult(
        Object message,
        String blockingError,
        boolean preventContinuation,
        String stopReason,
        List<String> additionalContexts,
        Object updatedMCPToolOutput
    ) {}

    /**
     * Hook listener interface.
     */
    public interface ToolHookListener {
        void onPreToolUse(HookExecutionContext context, Consumer<PreToolUseHookResult> resultConsumer);
        void onPostToolUse(HookExecutionContext context, Consumer<PostToolUseHookResult> resultConsumer);
        void onPostToolUseFailure(HookExecutionContext context, String error, Consumer<PostToolUseHookResult> resultConsumer);
    }

    /**
     * Add listener.
     */
    public void addListener(ToolHookListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(ToolHookListener listener) {
        listeners.remove(listener);
    }

    /**
     * Execute pre tool use hooks.
     */
    public CompletableFuture<List<PreToolUseHookResult>> executePreToolHooks(
        HookExecutionContext context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<PreToolUseHookResult> results = new ArrayList<>();
            for (ToolHookListener listener : listeners) {
                listener.onPreToolUse(context, results::add);
            }
            return results;
        });
    }

    /**
     * Execute post tool use hooks.
     */
    public CompletableFuture<List<PostToolUseHookResult>> executePostToolHooks(
        HookExecutionContext context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<PostToolUseHookResult> results = new ArrayList<>();
            for (ToolHookListener listener : listeners) {
                listener.onPostToolUse(context, results::add);
            }
            return results;
        });
    }

    /**
     * Execute post tool use failure hooks.
     */
    public CompletableFuture<List<PostToolUseHookResult>> executePostToolUseFailureHooks(
        HookExecutionContext context,
        String error
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<PostToolUseHookResult> results = new ArrayList<>();
            for (ToolHookListener listener : listeners) {
                listener.onPostToolUseFailure(context, error, results::add);
            }
            return results;
        });
    }

    /**
     * Resolve hook permission decision.
     */
    public PermissionDecision resolveHookPermissionDecision(
        HookResult hookPermissionResult,
        Object tool,
        Object input,
        Object toolUseContext,
        Object canUseTool,
        Object assistantMessage,
        String toolUseId
    ) {
        if (hookPermissionResult instanceof PermissionResult pr) {
            if ("allow".equals(pr.getBehavior())) {
                // Hook allow - check rule based permissions
                return new PermissionDecision(
                    "allow",
                    pr.getUpdatedInput() != null ? pr.getUpdatedInput() : input,
                    pr.getMessage(),
                    pr.getDecisionReason()
                );
            } else if ("deny".equals(pr.getBehavior())) {
                return new PermissionDecision(
                    "deny",
                    input,
                    pr.getMessage(),
                    pr.getDecisionReason()
                );
            }
        }

        // No hook decision - normal permission flow
        return new PermissionDecision("ask", input, null, null);
    }

    /**
     * Permission decision record.
     */
    public record PermissionDecision(
        String behavior,
        Object input,
        String message,
        Object decisionReason
    ) {
        public boolean isAllowed() { return "allow".equals(behavior); }
        public boolean isDenied() { return "deny".equals(behavior); }
        public boolean isAsk() { return "ask".equals(behavior); }
    }

    /**
     * Hook timing threshold constants.
     */
    public static final int HOOK_TIMING_DISPLAY_THRESHOLD_MS = 500;
    public static final int SLOW_PHASE_LOG_THRESHOLD_MS = 2000;
}