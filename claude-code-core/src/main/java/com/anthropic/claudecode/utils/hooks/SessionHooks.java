/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/sessionHooks.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Session hooks management - temporary, in-memory hooks cleared when session ends.
 */
public final class SessionHooks {
    private SessionHooks() {}

    /**
     * Function hook callback interface.
     */
    @FunctionalInterface
    public interface FunctionHookCallback {
        boolean apply(List<Map<String, Object>> messages);
    }

    /**
     * Function hook type with callback embedded.
     */
    public record FunctionHook(
        String type,
        String id,
        Integer timeout,
        FunctionHookCallback callback,
        String errorMessage,
        String statusMessage
    ) {
        public FunctionHook(String id, Integer timeout, FunctionHookCallback callback, String errorMessage) {
            this("function", id, timeout, callback, errorMessage, null);
        }
    }

    /**
     * Hook success callback.
     */
    @FunctionalInterface
    public interface OnHookSuccess {
        void apply(Object hook, Map<String, Object> result);
    }

    /**
     * Hook entry with optional success callback.
     */
    public record HookEntry(
        Object hook,
        OnHookSuccess onHookSuccess
    ) {}

    /**
     * Session hook matcher.
     */
    public record SessionHookMatcher(
        String matcher,
        String skillRoot,
        List<HookEntry> hooks
    ) {}

    /**
     * Session store for hooks.
     */
    public record SessionStore(
        Map<String, List<SessionHookMatcher>> hooks
    ) {}

    /**
     * Function hook matcher.
     */
    public record FunctionHookMatcher(
        String matcher,
        List<FunctionHook> hooks
    ) {}

    /**
     * Session derived hook matcher.
     */
    public record SessionDerivedHookMatcher(
        String matcher,
        List<HookTypes.HookCommand> hooks,
        String skillRoot
    ) {}

    /**
     * Session hooks state - Map for O(1) operations without triggering listeners.
     */
    private static final ConcurrentHashMap<String, SessionStore> sessionHooksState =
        new ConcurrentHashMap<>();

    /**
     * Add a command or prompt hook to the session.
     */
    public static void addSessionHook(
            Consumer<SessionStore> setAppState,
            String sessionId,
            String event,
            String matcher,
            HookTypes.HookCommand hook,
            OnHookSuccess onHookSuccess,
            String skillRoot) {

        addHookToSession(setAppState, sessionId, event, matcher, hook, onHookSuccess, skillRoot);
    }

    /**
     * Add a function hook to the session.
     * @return The hook ID for removal
     */
    public static String addFunctionHook(
            Consumer<SessionStore> setAppState,
            String sessionId,
            String event,
            String matcher,
            FunctionHookCallback callback,
            String errorMessage,
            Integer timeout,
            String existingId) {

        String id = existingId != null ? existingId
            : "function-hook-" + System.currentTimeMillis() + "-" + Math.random();

        FunctionHook hook = new FunctionHook(id, timeout != null ? timeout : 5000, callback, errorMessage);
        addHookToSession(setAppState, sessionId, event, matcher, hook, null, null);
        return id;
    }

    /**
     * Remove a function hook by ID from the session.
     */
    public static void removeFunctionHook(
            String sessionId,
            String event,
            String hookId) {

        SessionStore store = sessionHooksState.get(sessionId);
        if (store == null) return;

        List<SessionHookMatcher> eventMatchers = store.hooks().getOrDefault(event, new ArrayList<>());

        List<SessionHookMatcher> updatedMatchers = new ArrayList<>();
        for (SessionHookMatcher m : eventMatchers) {
            List<HookEntry> updatedHooks = new ArrayList<>();
            for (HookEntry h : m.hooks()) {
                if (h.hook() instanceof FunctionHook fh) {
                    if (!fh.id().equals(hookId)) {
                        updatedHooks.add(h);
                    }
                } else {
                    updatedHooks.add(h);
                }
            }
            if (!updatedHooks.isEmpty()) {
                updatedMatchers.add(new SessionHookMatcher(m.matcher(), m.skillRoot(), updatedHooks));
            }
        }

        Map<String, List<SessionHookMatcher>> newHooks = new HashMap<>(store.hooks());
        if (!updatedMatchers.isEmpty()) {
            newHooks.put(event, updatedMatchers);
        } else {
            newHooks.remove(event);
        }

        sessionHooksState.put(sessionId, new SessionStore(newHooks));
        logForDebugging("Removed function hook " + hookId + " for event " + event + " in session " + sessionId);
    }

    /**
     * Internal helper to add a hook to session state.
     */
    private static void addHookToSession(
            Consumer<SessionStore> setAppState,
            String sessionId,
            String event,
            String matcher,
            Object hook,
            OnHookSuccess onHookSuccess,
            String skillRoot) {

        SessionStore store = sessionHooksState.getOrDefault(sessionId, new SessionStore(new HashMap<>()));
        List<SessionHookMatcher> eventMatchers = store.hooks().getOrDefault(event, new ArrayList<>());

        // Find existing matcher or create new one
        int existingMatcherIndex = -1;
        for (int i = 0; i < eventMatchers.size(); i++) {
            SessionHookMatcher m = eventMatchers.get(i);
            if (m.matcher().equals(matcher) &&
                (m.skillRoot() == null ? skillRoot == null : m.skillRoot().equals(skillRoot))) {
                existingMatcherIndex = i;
                break;
            }
        }

        List<SessionHookMatcher> updatedMatchers;
        if (existingMatcherIndex >= 0) {
            updatedMatchers = new ArrayList<>(eventMatchers);
            SessionHookMatcher existingMatcher = updatedMatchers.get(existingMatcherIndex);
            List<HookEntry> newHookEntries = new ArrayList<>(existingMatcher.hooks());
            newHookEntries.add(new HookEntry(hook, onHookSuccess));
            updatedMatchers.set(existingMatcherIndex,
                new SessionHookMatcher(existingMatcher.matcher(), existingMatcher.skillRoot(), newHookEntries));
        } else {
            updatedMatchers = new ArrayList<>(eventMatchers);
            updatedMatchers.add(new SessionHookMatcher(matcher, skillRoot,
                List.of(new HookEntry(hook, onHookSuccess))));
        }

        Map<String, List<SessionHookMatcher>> newHooks = new HashMap<>(store.hooks());
        newHooks.put(event, updatedMatchers);

        sessionHooksState.put(sessionId, new SessionStore(newHooks));
        logForDebugging("Added session hook for event " + event + " in session " + sessionId);
    }

    /**
     * Remove a specific hook from the session.
     */
    public static void removeSessionHook(
            String sessionId,
            String event,
            HookTypes.HookCommand hook) {

        SessionStore store = sessionHooksState.get(sessionId);
        if (store == null) return;

        List<SessionHookMatcher> eventMatchers = store.hooks().getOrDefault(event, new ArrayList<>());

        List<SessionHookMatcher> updatedMatchers = new ArrayList<>();
        for (SessionHookMatcher m : eventMatchers) {
            List<HookEntry> updatedHooks = new ArrayList<>();
            for (HookEntry h : m.hooks()) {
                if (h.hook() instanceof HookTypes.HookCommand hc) {
                    if (!isHookEqual(hc, hook)) {
                        updatedHooks.add(h);
                    }
                } else {
                    updatedHooks.add(h);
                }
            }
            if (!updatedHooks.isEmpty()) {
                updatedMatchers.add(new SessionHookMatcher(m.matcher(), m.skillRoot(), updatedHooks));
            }
        }

        Map<String, List<SessionHookMatcher>> newHooks = new HashMap<>(store.hooks());
        if (!updatedMatchers.isEmpty()) {
            newHooks.put(event, updatedMatchers);
        } else {
            newHooks.remove(event);
        }

        sessionHooksState.put(sessionId, new SessionStore(newHooks));
        logForDebugging("Removed session hook for event " + event + " in session " + sessionId);
    }

    /**
     * Check if two hooks are equal.
     */
    private static boolean isHookEqual(HookTypes.HookCommand a, HookTypes.HookCommand b) {
        return Objects.equals(a.type(), b.type()) &&
               Objects.equals(a.command(), b.command()) &&
               Objects.equals(a.path(), b.path()) &&
               Objects.equals(a.url(), b.url());
    }

    /**
     * Get all session hooks for a specific event (excluding function hooks).
     */
    public static Map<String, List<SessionDerivedHookMatcher>> getSessionHooks(
            String sessionId,
            String event) {

        SessionStore store = sessionHooksState.get(sessionId);
        if (store == null) return new HashMap<>();

        Map<String, List<SessionDerivedHookMatcher>> result = new HashMap<>();

        if (event != null) {
            List<SessionHookMatcher> sessionMatchers = store.hooks().get(event);
            if (sessionMatchers != null) {
                result.put(event, convertToHookMatchers(sessionMatchers));
            }
            return result;
        }

        for (String evt : store.hooks().keySet()) {
            List<SessionHookMatcher> sessionMatchers = store.hooks().get(evt);
            if (sessionMatchers != null) {
                result.put(evt, convertToHookMatchers(sessionMatchers));
            }
        }

        return result;
    }

    /**
     * Convert session hook matchers to regular hook matchers.
     */
    private static List<SessionDerivedHookMatcher> convertToHookMatchers(
            List<SessionHookMatcher> sessionMatchers) {

        return sessionMatchers.stream()
            .map(sm -> new SessionDerivedHookMatcher(
                sm.matcher(),
                sm.hooks().stream()
                    .map(h -> h.hook())
                    .filter(h -> h instanceof HookTypes.HookCommand)
                    .map(h -> (HookTypes.HookCommand) h)
                    .toList(),
                sm.skillRoot()
            ))
            .filter(m -> !m.hooks().isEmpty())
            .toList();
    }

    /**
     * Get all session function hooks for a specific event.
     */
    public static Map<String, List<FunctionHookMatcher>> getSessionFunctionHooks(
            String sessionId,
            String event) {

        SessionStore store = sessionHooksState.get(sessionId);
        if (store == null) return new HashMap<>();

        Map<String, List<FunctionHookMatcher>> result = new HashMap<>();

        if (event != null) {
            List<SessionHookMatcher> sessionMatchers = store.hooks().get(event);
            if (sessionMatchers != null) {
                List<FunctionHookMatcher> functionMatchers = extractFunctionHooks(sessionMatchers);
                if (!functionMatchers.isEmpty()) {
                    result.put(event, functionMatchers);
                }
            }
            return result;
        }

        for (String evt : store.hooks().keySet()) {
            List<SessionHookMatcher> sessionMatchers = store.hooks().get(evt);
            if (sessionMatchers != null) {
                List<FunctionHookMatcher> functionMatchers = extractFunctionHooks(sessionMatchers);
                if (!functionMatchers.isEmpty()) {
                    result.put(evt, functionMatchers);
                }
            }
        }

        return result;
    }

    private static List<FunctionHookMatcher> extractFunctionHooks(
            List<SessionHookMatcher> sessionMatchers) {

        return sessionMatchers.stream()
            .map(sm -> new FunctionHookMatcher(
                sm.matcher(),
                sm.hooks().stream()
                    .map(h -> h.hook())
                    .filter(h -> h instanceof FunctionHook)
                    .map(h -> (FunctionHook) h)
                    .toList()
            ))
            .filter(m -> !m.hooks().isEmpty())
            .toList();
    }

    /**
     * Get the full hook entry for a specific session hook.
     */
    public static HookEntry getSessionHookCallback(
            String sessionId,
            String event,
            String matcher,
            Object hook) {

        SessionStore store = sessionHooksState.get(sessionId);
        if (store == null) return null;

        List<SessionHookMatcher> eventMatchers = store.hooks().get(event);
        if (eventMatchers == null) return null;

        for (SessionHookMatcher matcherEntry : eventMatchers) {
            if (matcherEntry.matcher().equals(matcher) || matcher.isEmpty()) {
                for (HookEntry hookEntry : matcherEntry.hooks()) {
                    if (isHookObjectEqual(hookEntry.hook(), hook)) {
                        return hookEntry;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isHookObjectEqual(Object a, Object b) {
        if (a instanceof HookTypes.HookCommand hca && b instanceof HookTypes.HookCommand hcb) {
            return isHookEqual(hca, hcb);
        }
        if (a instanceof FunctionHook fha && b instanceof FunctionHook fhb) {
            return Objects.equals(fha.id(), fhb.id());
        }
        return Objects.equals(a, b);
    }

    /**
     * Clear all session hooks for a specific session.
     */
    public static void clearSessionHooks(String sessionId) {
        sessionHooksState.remove(sessionId);
        logForDebugging("Cleared all session hooks for session " + sessionId);
    }

    /**
     * Get session hooks state for app state integration.
     */
    public static ConcurrentHashMap<String, SessionStore> getSessionHooksState() {
        return sessionHooksState;
    }

    private static void logForDebugging(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[session-hooks] " + message);
        }
    }
}