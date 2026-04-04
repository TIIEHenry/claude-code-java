/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hooks types
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;

/**
 * Types for hooks system.
 */
public final class HookTypes {
    private HookTypes() {}

    /**
     * Hook event types.
     */
    public enum HookEvent {
        SESSION_START("SessionStart"),
        SESSION_END("SessionEnd"),
        SETUP("Setup"),
        PRE_TOOL_USE("PreToolUse"),
        POST_TOOL_USE("PostToolUse"),
        POST_TOOL_USE_FAILURE("PostToolUseFailure"),
        PRE_COMPACT("PreCompact"),
        POST_COMPACT("PostCompact"),
        STOP("Stop"),
        STOP_FAILURE("StopFailure"),
        PERMISSION_DENIED("PermissionDenied"),
        SUBAGENT_START("SubagentStart"),
        SUBAGENT_STOP("SubagentStop"),
        TASK_CREATED("TaskCreated"),
        TASK_COMPLETED("TaskCompleted"),
        CONFIG_CHANGE("ConfigChange"),
        CWD_CHANGED("CwdChanged"),
        FILE_CHANGED("FileChanged"),
        USER_PROMPT_SUBMIT("UserPromptSubmit"),
        PERMISSION_REQUEST("PermissionRequest");

        private final String eventName;

        HookEvent(String eventName) {
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }

        public static HookEvent fromString(String name) {
            for (HookEvent event : values()) {
                if (event.eventName.equals(name)) {
                    return event;
                }
            }
            return null;
        }
    }

    /**
     * Hook input base.
     */
    public sealed interface HookInput permits
        SessionStartHookInput,
        PreToolUseHookInput,
        PostToolUseHookInput,
        StopHookInput {}

    /**
     * Session start hook input.
     */
    public record SessionStartHookInput(
        String sessionId,
        String cwd,
        String hookEventName
    ) implements HookInput {
        public SessionStartHookInput(String sessionId, String cwd) {
            this(sessionId, cwd, "SessionStart");
        }
    }

    /**
     * Pre tool use hook input.
     */
    public record PreToolUseHookInput(
        String toolName,
        Map<String, Object> toolInput,
        String hookEventName
    ) implements HookInput {
        public PreToolUseHookInput(String toolName, Map<String, Object> toolInput) {
            this(toolName, toolInput, "PreToolUse");
        }
    }

    /**
     * Post tool use hook input.
     */
    public record PostToolUseHookInput(
        String toolName,
        Map<String, Object> toolInput,
        Object toolOutput,
        boolean isError,
        String hookEventName
    ) implements HookInput {
        public PostToolUseHookInput(String toolName, Map<String, Object> toolInput,
                Object toolOutput, boolean isError) {
            this(toolName, toolInput, toolOutput, isError, "PostToolUse");
        }
    }

    /**
     * Stop hook input.
     */
    public record StopHookInput(
        String reason,
        String hookEventName
    ) implements HookInput {
        public StopHookInput(String reason) {
            this(reason, "Stop");
        }
    }

    /**
     * Hook output.
     */
    public record HookOutput(
        boolean success,
        String output,
        String error,
        Integer exitCode
    ) {
        public static HookOutput success(String output) {
            return new HookOutput(true, output, null, 0);
        }

        public static HookOutput error(String error) {
            return new HookOutput(false, null, error, 1);
        }
    }

    /**
     * Hook matcher for matching hooks to events.
     */
    public record HookMatcher(
        String hookName,
        String hookEvent,
        List<String> tools,
        Map<String, Object> matchers
    ) {}

    /**
     * Hook command definition.
     */
    public record HookCommand(
        String type,
        String command,
        String path,
        String url,
        Map<String, String> env,
        Integer timeout
    ) {}
}