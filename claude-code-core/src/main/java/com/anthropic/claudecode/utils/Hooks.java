/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hooks types
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Hooks are user-defined commands that can be executed at various points
 * in Claude Code's lifecycle.
 */
public final class Hooks {
    private Hooks() {}

    /**
     * Hook event types.
     */
    public enum HookEvent {
        SESSION_START,
        SESSION_END,
        PRE_TOOL_USE,
        POST_TOOL_USE,
        POST_TOOL_USE_FAILURE,
        NOTIFICATION,
        STOP,
        STOP_FAILURE,
        PRE_COMPACT,
        POST_COMPACT,
        PERMISSION_DENIED,
        USER_PROMPT_SUBMIT,
        PERMISSION_REQUEST,
        SUBAGENT_START,
        SUBAGENT_STOP,
        TEAMMATE_IDLE,
        TASK_CREATED,
        TASK_COMPLETED,
        CONFIG_CHANGE,
        CWD_CHANGED,
        FILE_CHANGED,
        INSTRUCTIONS_LOADED,
        ELICITATION,
        ELICITATION_RESULT,
        SETUP
    }

    /**
     * Hook matcher configuration.
     */
    public record HookMatcher(
            HookEvent event,
            List<String> tools,
            List<String> matcher,
            String command,
            int timeout,
            boolean background
    ) {}

    /**
     * Hook input base (using class instead of record for inheritance).
     */
    public abstract static class HookInput {
        private final String sessionId;
        private final String transcriptPath;
        private final String cwd;
        private final HookEvent event;

        protected HookInput(String sessionId, String transcriptPath, String cwd, HookEvent event) {
            this.sessionId = sessionId;
            this.transcriptPath = transcriptPath;
            this.cwd = cwd;
            this.event = event;
        }

        public String sessionId() { return sessionId; }
        public String transcriptPath() { return transcriptPath; }
        public String cwd() { return cwd; }
        public HookEvent event() { return event; }
    }

    /**
     * Pre tool use hook input.
     */
    public static final class PreToolUseHookInput extends HookInput {
        private final String toolName;
        private final Map<String, Object> toolInput;

        public PreToolUseHookInput(String sessionId, String transcriptPath, String cwd,
                                    String toolName, Map<String, Object> toolInput) {
            super(sessionId, transcriptPath, cwd, HookEvent.PRE_TOOL_USE);
            this.toolName = toolName;
            this.toolInput = toolInput;
        }

        public String toolName() { return toolName; }
        public Map<String, Object> toolInput() { return toolInput; }
    }

    /**
     * Post tool use hook input.
     */
    public static final class PostToolUseHookInput extends HookInput {
        private final String toolName;
        private final Map<String, Object> toolInput;
        private final Object toolOutput;
        private final boolean success;

        public PostToolUseHookInput(String sessionId, String transcriptPath, String cwd,
                                     String toolName, Map<String, Object> toolInput,
                                     Object toolOutput, boolean success) {
            super(sessionId, transcriptPath, cwd, HookEvent.POST_TOOL_USE);
            this.toolName = toolName;
            this.toolInput = toolInput;
            this.toolOutput = toolOutput;
            this.success = success;
        }

        public String toolName() { return toolName; }
        public Map<String, Object> toolInput() { return toolInput; }
        public Object toolOutput() { return toolOutput; }
        public boolean success() { return success; }
    }

    /**
     * Session start hook input.
     */
    public static final class SessionStartHookInput extends HookInput {
        public SessionStartHookInput(String sessionId, String transcriptPath, String cwd) {
            super(sessionId, transcriptPath, cwd, HookEvent.SESSION_START);
        }
    }

    /**
     * Session end hook input.
     */
    public static final class SessionEndHookInput extends HookInput {
        private final String exitReason;

        public SessionEndHookInput(String sessionId, String transcriptPath, String cwd, String exitReason) {
            super(sessionId, transcriptPath, cwd, HookEvent.SESSION_END);
            this.exitReason = exitReason;
        }

        public String exitReason() { return exitReason; }
    }

    /**
     * Hook output.
     */
    public record HookOutput(
            boolean succeeded,
            String message,
            String error,
            int exitCode
    ) {
        public static HookOutput success() {
            return new HookOutput(true, null, null, 0);
        }

        public static HookOutput success(String message) {
            return new HookOutput(true, message, null, 0);
        }

        public static HookOutput failure(String error, int exitCode) {
            return new HookOutput(false, null, error, exitCode);
        }
    }

    /**
     * Hook result for blocking hooks.
     */
    public sealed interface HookResult permits HookResult.Proceed, HookResult.Block {
        record Proceed() implements HookResult {}
        record Block(String reason, String message) implements HookResult {}
    }
}