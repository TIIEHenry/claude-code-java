/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/permissions/hooks
 */
package com.anthropic.claudecode.components.permissions;

import java.util.*;
import java.util.concurrent.*;

/**
 * Permission hooks - Hooks for permission checking.
 */
public final class PermissionHooks {
    private final List<PermissionHook> hooks = new CopyOnWriteArrayList<>();

    /**
     * Register a permission hook.
     */
    public void register(PermissionHook hook) {
        hooks.add(hook);
    }

    /**
     * Unregister a permission hook.
     */
    public void unregister(PermissionHook hook) {
        hooks.remove(hook);
    }

    /**
     * Check permission with hooks.
     */
    public PermissionResult checkPermission(
        String tool,
        String action,
        String resource,
        PermissionComponentUtils.PermissionType type
    ) {
        // Run all hooks
        for (PermissionHook hook : hooks) {
            PermissionResult result = hook.check(tool, action, resource, type);
            if (result != null && result.decision() != PermissionComponentUtils.PermissionAction.ASK) {
                return result;
            }
        }

        // Default to ask
        return new PermissionResult(
            PermissionComponentUtils.PermissionAction.ASK,
            "No hook determined permission",
            null
        );
    }

    /**
     * Get all hooks.
     */
    public List<PermissionHook> getHooks() {
        return Collections.unmodifiableList(hooks);
    }

    /**
     * Clear all hooks.
     */
    public void clear() {
        hooks.clear();
    }

    /**
     * Permission hook interface.
     */
    @FunctionalInterface
    public interface PermissionHook {
        PermissionResult check(String tool, String action, String resource, PermissionComponentUtils.PermissionType type);
    }

    /**
     * Permission result record.
     */
    public record PermissionResult(
        PermissionComponentUtils.PermissionAction decision,
        String reason,
        String source
    ) {
        public static PermissionResult allow(String reason) {
            return new PermissionResult(PermissionComponentUtils.PermissionAction.ALLOW, reason, "hook");
        }

        public static PermissionResult deny(String reason) {
            return new PermissionResult(PermissionComponentUtils.PermissionAction.DENY, reason, "hook");
        }

        public static PermissionResult ask(String reason) {
            return new PermissionResult(PermissionComponentUtils.PermissionAction.ASK, reason, "hook");
        }
    }
}