/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/hooksSettings.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.function.ToIntFunction;

/**
 * Hooks settings utilities.
 */
public final class HooksSettingsUtil {
    private HooksSettingsUtil() {}

    private static final String DEFAULT_HOOK_SHELL = "bash";

    /**
     * Hook source types.
     */
    public enum HookSource {
        USER_SETTINGS,
        PROJECT_SETTINGS,
        LOCAL_SETTINGS,
        POLICY_SETTINGS,
        PLUGIN_HOOK,
        SESSION_HOOK,
        BUILTIN_HOOK
    }

    /**
     * Individual hook configuration.
     */
    public record IndividualHookConfig(
        String event,
        HookTypes.HookCommand config,
        String matcher,
        HookSource source,
        String pluginName
    ) {}

    /**
     * Check if two hooks are equal.
     */
    public static boolean isHookEqual(
            HookTypes.HookCommand a,
            HookTypes.HookCommand b) {

        if (!Objects.equals(a.type(), b.type())) return false;

        String aIf = a.env() != null ? a.env().get("if") : null;
        String bIf = b.env() != null ? b.env().get("if") : null;
        boolean sameIf = Objects.equals(aIf != null ? aIf : "", bIf != null ? bIf : "");

        switch (a.type()) {
            case "command":
                if (!"command".equals(b.type())) return false;
                String aShell = a.env() != null ? a.env().get("shell") : null;
                String bShell = b.env() != null ? b.env().get("shell") : null;
                return Objects.equals(a.command(), b.command()) &&
                       Objects.equals(aShell != null ? aShell : DEFAULT_HOOK_SHELL,
                                     bShell != null ? bShell : DEFAULT_HOOK_SHELL) &&
                       sameIf;
            case "prompt":
                return "prompt".equals(b.type()) &&
                       Objects.equals(a.command(), b.command()) && sameIf;
            case "agent":
                return "agent".equals(b.type()) &&
                       Objects.equals(a.command(), b.command()) && sameIf;
            case "http":
                return "http".equals(b.type()) &&
                       Objects.equals(a.url(), b.url()) && sameIf;
            default:
                return false;
        }
    }

    /**
     * Get the display text for a hook.
     */
    public static String getHookDisplayText(HookTypes.HookCommand hook) {
        switch (hook.type()) {
            case "command":
                return hook.command();
            case "prompt":
                return hook.command();
            case "agent":
                return hook.command();
            case "http":
                return hook.url();
            default:
                return hook.type();
        }
    }

    /**
     * Get all hooks from app state.
     */
    public static List<IndividualHookConfig> getAllHooks() {
        List<IndividualHookConfig> hooks = new ArrayList<>();

        // Check if restricted to managed hooks only
        boolean restrictedToManagedOnly = HooksConfigSnapshot.shouldAllowManagedHooksOnly();

        // If allowManagedHooksOnly is set, don't show any hooks in the UI
        if (!restrictedToManagedOnly) {
            // Get hooks from settings sources
            Map<String, List<HooksConfigSnapshot.HookMatcherConfig>> config =
                HooksConfigSnapshot.getHooksConfigFromSnapshot();

            for (Map.Entry<String, List<HooksConfigSnapshot.HookMatcherConfig>> entry : config.entrySet()) {
                String event = entry.getKey();
                for (HooksConfigSnapshot.HookMatcherConfig matcher : entry.getValue()) {
                    for (HookTypes.HookCommand hookCommand : matcher.hooks()) {
                        hooks.add(new IndividualHookConfig(
                            event,
                            hookCommand,
                            matcher.matcher(),
                            HookSource.USER_SETTINGS,
                            null
                        ));
                    }
                }
            }
        }

        // Get session hooks
        Map<String, List<SessionHooks.SessionDerivedHookMatcher>> sessionHooks =
            SessionHooks.getSessionHooks("default-session", null);

        for (Map.Entry<String, List<SessionHooks.SessionDerivedHookMatcher>> entry : sessionHooks.entrySet()) {
            String event = entry.getKey();
            for (SessionHooks.SessionDerivedHookMatcher matcher : entry.getValue()) {
                for (HookTypes.HookCommand hookCommand : matcher.hooks()) {
                    hooks.add(new IndividualHookConfig(
                        event,
                        hookCommand,
                        matcher.matcher(),
                        HookSource.SESSION_HOOK,
                        null
                    ));
                }
            }
        }

        return hooks;
    }

    /**
     * Get hooks for a specific event.
     */
    public static List<IndividualHookConfig> getHooksForEvent(String event) {
        return getAllHooks().stream()
            .filter(hook -> hook.event().equals(event))
            .toList();
    }

    /**
     * Get source description display string.
     */
    public static String hookSourceDescriptionDisplayString(HookSource source) {
        switch (source) {
            case USER_SETTINGS:
                return "User settings (~/.claude/settings.json)";
            case PROJECT_SETTINGS:
                return "Project settings (.claude/settings.json)";
            case LOCAL_SETTINGS:
                return "Local settings (.claude/settings.local.json)";
            case PLUGIN_HOOK:
                return "Plugin hooks (~/.claude/plugins/*/hooks/hooks.json)";
            case SESSION_HOOK:
                return "Session hooks (in-memory, temporary)";
            case BUILTIN_HOOK:
                return "Built-in hooks (registered internally by Claude Code)";
            default:
                return source.name();
        }
    }

    /**
     * Get source header display string.
     */
    public static String hookSourceHeaderDisplayString(HookSource source) {
        switch (source) {
            case USER_SETTINGS:
                return "User Settings";
            case PROJECT_SETTINGS:
                return "Project Settings";
            case LOCAL_SETTINGS:
                return "Local Settings";
            case PLUGIN_HOOK:
                return "Plugin Hooks";
            case SESSION_HOOK:
                return "Session Hooks";
            case BUILTIN_HOOK:
                return "Built-in Hooks";
            default:
                return source.name();
        }
    }

    /**
     * Get source inline display string.
     */
    public static String hookSourceInlineDisplayString(HookSource source) {
        switch (source) {
            case USER_SETTINGS:
                return "User";
            case PROJECT_SETTINGS:
                return "Project";
            case LOCAL_SETTINGS:
                return "Local";
            case PLUGIN_HOOK:
                return "Plugin";
            case SESSION_HOOK:
                return "Session";
            case BUILTIN_HOOK:
                return "Built-in";
            default:
                return source.name();
        }
    }

    /**
     * Sort matchers by priority.
     */
    public static List<String> sortMatchersByPriority(
            List<String> matchers,
            Map<String, Map<String, List<IndividualHookConfig>>> hooksByEventAndMatcher,
            String selectedEvent) {

        // Priority order
        List<HookSource> sourcePriority = List.of(
            HookSource.USER_SETTINGS,
            HookSource.PROJECT_SETTINGS,
            HookSource.LOCAL_SETTINGS,
            HookSource.POLICY_SETTINGS
        );

        return matchers.stream()
            .sorted((a, b) -> {
                List<IndividualHookConfig> aHooks = hooksByEventAndMatcher
                    .getOrDefault(selectedEvent, new HashMap<>())
                    .getOrDefault(a, new ArrayList<>());
                List<IndividualHookConfig> bHooks = hooksByEventAndMatcher
                    .getOrDefault(selectedEvent, new HashMap<>())
                    .getOrDefault(b, new ArrayList<>());

                Set<HookSource> aSources = new HashSet<>();
                Set<HookSource> bSources = new HashSet<>();
                for (IndividualHookConfig h : aHooks) aSources.add(h.source());
                for (IndividualHookConfig h : bHooks) bSources.add(h.source());

                ToIntFunction<HookSource> getSourcePriority = s ->
                    s == HookSource.PLUGIN_HOOK || s == HookSource.BUILTIN_HOOK
                        ? 999
                        : sourcePriority.indexOf(s);

                int aHighestPriority = aSources.stream()
                    .mapToInt(getSourcePriority)
                    .min()
                    .orElse(999);
                int bHighestPriority = bSources.stream()
                    .mapToInt(getSourcePriority)
                    .min()
                    .orElse(999);

                if (aHighestPriority != bHighestPriority) {
                    return aHighestPriority - bHighestPriority;
                }

                return a.compareTo(b);
            })
            .toList();
    }
}