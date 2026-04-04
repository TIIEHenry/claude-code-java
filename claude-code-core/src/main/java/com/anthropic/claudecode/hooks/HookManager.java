/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hooks
 */
package com.anthropic.claudecode.hooks;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Hook manager for managing lifecycle hooks.
 */
public class HookManager {
    private final List<Hook> hooks = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<HookContext>>> hookHandlers = new ConcurrentHashMap<>();

    /**
     * Register a hook.
     */
    public void register(String event, Consumer<HookContext> handler) {
        hookHandlers.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * Unregister a hook.
     */
    public void unregister(String event, Consumer<HookContext> handler) {
        List<Consumer<HookContext>> handlers = hookHandlers.get(event);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * Trigger a hook event.
     */
    public void trigger(String event, HookContext context) {
        List<Consumer<HookContext>> handlers = hookHandlers.get(event);
        if (handlers != null) {
            for (Consumer<HookContext> handler : handlers) {
                try {
                    handler.accept(context);
                } catch (Exception e) {
                    // Log and continue
                }
            }
        }
    }

    /**
     * Clear all hooks.
     */
    public void clear() {
        hooks.clear();
        hookHandlers.clear();
    }

    /**
     * Check if there are hooks for an event.
     */
    public boolean hasHooks(String event) {
        List<Consumer<HookContext>> handlers = hookHandlers.get(event);
        return handlers != null && !handlers.isEmpty();
    }

    /**
     * Create a new hook manager.
     */
    public static HookManager create() {
        return new HookManager();
    }
}