/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/registerFrontmatterHooks.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.function.*;

/**
 * Register hooks from frontmatter (agent or skill) into session-scoped hooks.
 */
public final class RegisterFrontmatterHooks {
    private RegisterFrontmatterHooks() {}

    private static final List<String> HOOK_EVENTS = List.of(
        "SessionStart", "SessionEnd", "Setup", "PreToolUse", "PostToolUse",
        "PostToolUseFailure", "PreCompact", "PostCompact", "Stop", "StopFailure",
        "PermissionDenied", "SubagentStart", "SubagentStop", "TaskCreated",
        "TaskCompleted", "ConfigChange", "CwdChanged", "FileChanged",
        "UserPromptSubmit", "PermissionRequest"
    );

    /**
     * Register hooks from frontmatter into session-scoped hooks.
     *
     * @param sessionId Session ID to scope the hooks
     * @param hooks The hooks settings from frontmatter
     * @param sourceName Human-readable source name for logging
     * @param isAgent If true, converts Stop hooks to SubagentStop
     */
    public static void registerFrontmatterHooks(
            String sessionId,
            Map<String, List<HooksConfigSnapshot.HookMatcherConfig>> hooks,
            String sourceName,
            boolean isAgent) {

        if (hooks == null || hooks.isEmpty()) {
            return;
        }

        int hookCount = 0;

        for (String event : HOOK_EVENTS) {
            List<HooksConfigSnapshot.HookMatcherConfig> matchers = hooks.get(event);
            if (matchers == null || matchers.isEmpty()) {
                continue;
            }

            // For agents, convert Stop hooks to SubagentStop
            String targetEvent = event;
            if (isAgent && event.equals("Stop")) {
                targetEvent = "SubagentStop";
                logForDebugging("Converting Stop hook to SubagentStop for " +
                    sourceName + " (subagents trigger SubagentStop)");
            }

            for (HooksConfigSnapshot.HookMatcherConfig matcherConfig : matchers) {
                String matcher = matcherConfig.matcher() != null ? matcherConfig.matcher() : "";
                List<HookTypes.HookCommand> hooksArray = matcherConfig.hooks();

                if (hooksArray == null || hooksArray.isEmpty()) {
                    continue;
                }

                for (HookTypes.HookCommand hook : hooksArray) {
                    SessionHooks.addSessionHook(
                        null, // setAppState placeholder
                        sessionId,
                        targetEvent,
                        matcher,
                        hook,
                        null,
                        null
                    );
                    hookCount++;
                }
            }
        }

        if (hookCount > 0) {
            logForDebugging("Registered " + hookCount + " frontmatter hook(s) from " +
                sourceName + " for session " + sessionId);
        }
    }

    /**
     * Register hooks with consumer for state updates.
     */
    public static void registerFrontmatterHooks(
            Consumer<SessionHooks.SessionStore> setAppState,
            String sessionId,
            Map<String, List<HooksConfigSnapshot.HookMatcherConfig>> hooks,
            String sourceName,
            boolean isAgent) {

        if (hooks == null || hooks.isEmpty()) {
            return;
        }

        int hookCount = 0;

        for (String event : HOOK_EVENTS) {
            List<HooksConfigSnapshot.HookMatcherConfig> matchers = hooks.get(event);
            if (matchers == null || matchers.isEmpty()) {
                continue;
            }

            String targetEvent = event;
            if (isAgent && event.equals("Stop")) {
                targetEvent = "SubagentStop";
                logForDebugging("Converting Stop hook to SubagentStop for " + sourceName);
            }

            for (HooksConfigSnapshot.HookMatcherConfig matcherConfig : matchers) {
                String matcher = matcherConfig.matcher() != null ? matcherConfig.matcher() : "";
                List<HookTypes.HookCommand> hooksArray = matcherConfig.hooks();

                if (hooksArray == null || hooksArray.isEmpty()) {
                    continue;
                }

                for (HookTypes.HookCommand hook : hooksArray) {
                    SessionHooks.addSessionHook(
                        setAppState,
                        sessionId,
                        targetEvent,
                        matcher,
                        hook,
                        null,
                        null
                    );
                    hookCount++;
                }
            }
        }

        if (hookCount > 0) {
            logForDebugging("Registered " + hookCount + " frontmatter hook(s) from " +
                sourceName + " for session " + sessionId);
        }
    }

    private static void logForDebugging(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[frontmatter-hooks] " + message);
        }
    }
}