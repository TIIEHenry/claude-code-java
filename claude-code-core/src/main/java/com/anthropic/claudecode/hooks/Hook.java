/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hooks
 */
package com.anthropic.claudecode.hooks;

/**
 * Hook interface for lifecycle callbacks.
 */
public interface Hook {
    /**
     * Get the hook name.
     */
    String name();

    /**
     * Called before tool execution.
     */
    default void beforeToolUse(HookContext context) {}

    /**
     * Called after tool execution.
     */
    default void afterToolUse(HookContext context) {}

    /**
     * Called on tool error.
     */
    default void onToolError(HookContext context) {}

    /**
     * Get hook priority (higher = earlier execution).
     */
    default int priority() {
        return 0;
    }
}