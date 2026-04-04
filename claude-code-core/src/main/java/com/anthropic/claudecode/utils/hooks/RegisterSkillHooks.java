/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/registerSkillHooks.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.function.*;

/**
 * Register hooks from a skill's frontmatter as session hooks.
 */
public final class RegisterSkillHooks {
    private RegisterSkillHooks() {}

    private static final List<String> HOOK_EVENTS = List.of(
        "SessionStart", "SessionEnd", "Setup", "PreToolUse", "PostToolUse",
        "PostToolUseFailure", "PreCompact", "PostCompact", "Stop", "StopFailure",
        "PermissionDenied", "SubagentStart", "SubagentStop", "TaskCreated",
        "TaskCompleted", "ConfigChange", "CwdChanged", "FileChanged",
        "UserPromptSubmit", "PermissionRequest"
    );

    /**
     * Skill hook with once flag.
     */
    public record SkillHookCommand(
        String type,
        String command,
        String path,
        String url,
        Map<String, String> env,
        Integer timeout,
        boolean once
    ) {}

    /**
     * Skill hook matcher config.
     */
    public record SkillHookMatcherConfig(
        String matcher,
        List<SkillHookCommand> hooks
    ) {}

    /**
     * Register hooks from a skill's frontmatter as session hooks.
     *
     * Hooks are registered as session-scoped hooks that persist for the duration
     * of the session. If a hook has once: true, it will be automatically removed
     * after its first successful execution.
     *
     * @param sessionId The current session ID
     * @param hooks The hooks settings from the skill's frontmatter
     * @param skillName The name of the skill (for logging)
     * @param skillRoot The base directory of the skill
     */
    public static void registerSkillHooks(
            String sessionId,
            Map<String, List<SkillHookMatcherConfig>> hooks,
            String skillName,
            String skillRoot) {

        int registeredCount = 0;

        for (String eventName : HOOK_EVENTS) {
            List<SkillHookMatcherConfig> matchers = hooks.get(eventName);
            if (matchers == null) continue;

            for (SkillHookMatcherConfig matcher : matchers) {
                for (SkillHookCommand hook : matcher.hooks()) {
                    // For once: true hooks, use onHookSuccess callback to remove after execution
                    SessionHooks.OnHookSuccess onHookSuccess = hook.once()
                        ? (h, r) -> {
                            logForDebugging("Removing one-shot hook for event " +
                                eventName + " in skill '" + skillName + "'");
                            SessionHooks.removeSessionHook(sessionId, eventName,
                                convertToHookCommand(hook));
                        }
                        : null;

                    SessionHooks.addSessionHook(
                        null, // setAppState placeholder
                        sessionId,
                        eventName,
                        matcher.matcher() != null ? matcher.matcher() : "",
                        convertToHookCommand(hook),
                        onHookSuccess,
                        skillRoot
                    );
                    registeredCount++;
                }
            }
        }

        if (registeredCount > 0) {
            logForDebugging("Registered " + registeredCount +
                " hooks from skill '" + skillName + "'");
        }
    }

    /**
     * Register skill hooks with state updater.
     */
    public static void registerSkillHooks(
            Consumer<SessionHooks.SessionStore> setAppState,
            String sessionId,
            Map<String, List<SkillHookMatcherConfig>> hooks,
            String skillName,
            String skillRoot) {

        int registeredCount = 0;

        for (String eventName : HOOK_EVENTS) {
            List<SkillHookMatcherConfig> matchers = hooks.get(eventName);
            if (matchers == null) continue;

            for (SkillHookMatcherConfig matcher : matchers) {
                for (SkillHookCommand hook : matcher.hooks()) {
                    SessionHooks.OnHookSuccess onHookSuccess = hook.once()
                        ? (h, r) -> {
                            logForDebugging("Removing one-shot hook for event " +
                                eventName + " in skill '" + skillName + "'");
                            SessionHooks.removeSessionHook(sessionId, eventName,
                                convertToHookCommand(hook));
                        }
                        : null;

                    SessionHooks.addSessionHook(
                        setAppState,
                        sessionId,
                        eventName,
                        matcher.matcher() != null ? matcher.matcher() : "",
                        convertToHookCommand(hook),
                        onHookSuccess,
                        skillRoot
                    );
                    registeredCount++;
                }
            }
        }

        if (registeredCount > 0) {
            logForDebugging("Registered " + registeredCount +
                " hooks from skill '" + skillName + "'");
        }
    }

    private static HookTypes.HookCommand convertToHookCommand(SkillHookCommand skillHook) {
        return new HookTypes.HookCommand(
            skillHook.type(),
            skillHook.command(),
            skillHook.path(),
            skillHook.url(),
            skillHook.env(),
            skillHook.timeout()
        );
    }

    private static void logForDebugging(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[skill-hooks] " + message);
        }
    }
}