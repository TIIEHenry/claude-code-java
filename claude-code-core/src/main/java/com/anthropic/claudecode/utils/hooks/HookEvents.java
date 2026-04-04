/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/hookEvents.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hook event system for broadcasting hook execution events.
 */
public final class HookEvents {
    private HookEvents() {}

    private static final int MAX_PENDING_EVENTS = 100;
    private static final Set<String> ALWAYS_EMITTED_HOOK_EVENTS =
        Set.of("SessionStart", "Setup");

    private static final List<HookExecutionEvent> pendingEvents = new CopyOnWriteArrayList<>();
    private static volatile HookEventHandler eventHandler = null;
    private static volatile boolean allHookEventsEnabled = false;

    /**
     * Hook started event.
     */
    public record HookStartedEvent(
        String type,
        String hookId,
        String hookName,
        String hookEvent
    ) implements HookExecutionEvent {
        public HookStartedEvent(String hookId, String hookName, String hookEvent) {
            this("started", hookId, hookName, hookEvent);
        }
    }

    /**
     * Hook progress event.
     */
    public record HookProgressEvent(
        String type,
        String hookId,
        String hookName,
        String hookEvent,
        String stdout,
        String stderr,
        String output
    ) implements HookExecutionEvent {
        public HookProgressEvent(String hookId, String hookName, String hookEvent,
                String stdout, String stderr, String output) {
            this("progress", hookId, hookName, hookEvent, stdout, stderr, output);
        }
    }

    /**
     * Hook response event.
     */
    public record HookResponseEvent(
        String type,
        String hookId,
        String hookName,
        String hookEvent,
        String output,
        String stdout,
        String stderr,
        Integer exitCode,
        String outcome
    ) implements HookExecutionEvent {
        public HookResponseEvent(String hookId, String hookName, String hookEvent,
                String output, String stdout, String stderr,
                Integer exitCode, String outcome) {
            this("response", hookId, hookName, hookEvent, output, stdout, stderr, exitCode, outcome);
        }
    }

    /**
     * Base interface for hook execution events.
     */
    public sealed interface HookExecutionEvent
        permits HookStartedEvent, HookProgressEvent, HookResponseEvent {}

    /**
     * Handler for hook events.
     */
    @FunctionalInterface
    public interface HookEventHandler {
        void handle(HookExecutionEvent event);
    }

    /**
     * Register a hook event handler.
     */
    public static void registerHookEventHandler(HookEventHandler handler) {
        eventHandler = handler;
        if (handler != null && !pendingEvents.isEmpty()) {
            for (HookExecutionEvent event : new ArrayList<>(pendingEvents)) {
                handler.handle(event);
            }
            pendingEvents.clear();
        }
    }

    /**
     * Emit hook started event.
     */
    public static void emitHookStarted(String hookId, String hookName, String hookEvent) {
        if (!shouldEmit(hookEvent)) return;
        emit(new HookStartedEvent(hookId, hookName, hookEvent));
    }

    /**
     * Emit hook progress event.
     */
    public static void emitHookProgress(String hookId, String hookName, String hookEvent,
            String stdout, String stderr, String output) {
        if (!shouldEmit(hookEvent)) return;
        emit(new HookProgressEvent(hookId, hookName, hookEvent, stdout, stderr, output));
    }

    /**
     * Emit hook response event.
     */
    public static void emitHookResponse(String hookId, String hookName, String hookEvent,
            String output, String stdout, String stderr,
            Integer exitCode, String outcome) {
        if (!shouldEmit(hookEvent)) return;
        emit(new HookResponseEvent(hookId, hookName, hookEvent, output, stdout, stderr, exitCode, outcome));
    }

    /**
     * Start a progress interval for a hook.
     * @return A runnable to stop the interval
     */
    public static Runnable startHookProgressInterval(String hookId, String hookName, String hookEvent,
            java.util.function.Supplier<AsyncHookRegistry.OutputResult> getOutput) {
        // Simple implementation - just return a no-op runnable
        // Full implementation would start a periodic timer to emit progress events
        return () -> {};
    }

    /**
     * Enable emission of all hook event types.
     */
    public static void setAllHookEventsEnabled(boolean enabled) {
        allHookEventsEnabled = enabled;
    }

    /**
     * Clear hook event state.
     */
    public static void clearHookEventState() {
        eventHandler = null;
        pendingEvents.clear();
        allHookEventsEnabled = false;
    }

    private static void emit(HookExecutionEvent event) {
        if (eventHandler != null) {
            eventHandler.handle(event);
        } else {
            pendingEvents.add(event);
            if (pendingEvents.size() > MAX_PENDING_EVENTS) {
                pendingEvents.remove(0);
            }
        }
    }

    private static boolean shouldEmit(String hookEvent) {
        if (ALWAYS_EMITTED_HOOK_EVENTS.contains(hookEvent)) {
            return true;
        }
        return allHookEventsEnabled;
    }
}