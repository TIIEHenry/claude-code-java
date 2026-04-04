/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/hooksConfigSnapshot.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hooks configuration snapshot management.
 */
public final class HooksConfigSnapshot {
    private HooksConfigSnapshot() {}

    private static volatile Map<String, List<HookMatcherConfig>> initialHooksConfig = null;

    /**
     * Hook matcher configuration.
     */
    public record HookMatcherConfig(
        String matcher,
        List<HookTypes.HookCommand> hooks
    ) {}

    /**
     * Hooks settings structure.
     */
    public record HooksSettings(
        Map<String, List<HookMatcherConfig>> hooks,
        Boolean disableAllHooks,
        Boolean allowManagedHooksOnly
    ) {}

    // Simulated settings sources
    private static volatile HooksSettings policySettings = null;
    private static volatile HooksSettings mergedSettings = null;
    private static volatile boolean strictPluginOnlyCustomization = false;

    /**
     * Set policy settings (for testing or initialization).
     */
    public static void setPolicySettings(HooksSettings settings) {
        policySettings = settings;
    }

    /**
     * Set merged settings (for testing or initialization).
     */
    public static void setMergedSettings(HooksSettings settings) {
        mergedSettings = settings;
    }

    /**
     * Set strict plugin only customization flag.
     */
    public static void setStrictPluginOnlyCustomization(boolean value) {
        strictPluginOnlyCustomization = value;
    }

    /**
     * Get hooks from allowed sources.
     */
    private static Map<String, List<HookMatcherConfig>> getHooksFromAllowedSources() {
        // If managed settings disables all hooks, return empty
        if (policySettings != null && Boolean.TRUE.equals(policySettings.disableAllHooks())) {
            return new HashMap<>();
        }

        // If allowManagedHooksOnly is set in managed settings, only use managed hooks
        if (policySettings != null && Boolean.TRUE.equals(policySettings.allowManagedHooksOnly())) {
            return policySettings.hooks() != null ? policySettings.hooks() : new HashMap<>();
        }

        // strictPluginOnlyCustomization: block user/project/local settings hooks
        if (strictPluginOnlyCustomization) {
            return policySettings != null && policySettings.hooks() != null
                ? policySettings.hooks()
                : new HashMap<>();
        }

        // If disableAllHooks is set in non-managed settings, only managed hooks still run
        if (mergedSettings != null && Boolean.TRUE.equals(mergedSettings.disableAllHooks())) {
            return policySettings != null && policySettings.hooks() != null
                ? policySettings.hooks()
                : new HashMap<>();
        }

        // Otherwise, use all hooks (merged from all sources) - backwards compatible
        return mergedSettings != null && mergedSettings.hooks() != null
            ? mergedSettings.hooks()
            : new HashMap<>();
    }

    /**
     * Check if only managed hooks should run.
     */
    public static boolean shouldAllowManagedHooksOnly() {
        if (policySettings != null && Boolean.TRUE.equals(policySettings.allowManagedHooksOnly())) {
            return true;
        }
        // If disableAllHooks is set but NOT from managed settings,
        // treat as managed-only
        if (mergedSettings != null && Boolean.TRUE.equals(mergedSettings.disableAllHooks()) &&
            (policySettings == null || !Boolean.TRUE.equals(policySettings.disableAllHooks()))) {
            return true;
        }
        return false;
    }

    /**
     * Check if all hooks (including managed) should be disabled.
     */
    public static boolean shouldDisableAllHooksIncludingManaged() {
        return policySettings != null && Boolean.TRUE.equals(policySettings.disableAllHooks());
    }

    /**
     * Capture a snapshot of the current hooks configuration.
     * This should be called once during application startup.
     */
    public static void captureHooksConfigSnapshot() {
        initialHooksConfig = getHooksFromAllowedSources();
    }

    /**
     * Update the hooks configuration snapshot.
     * This should be called when hooks are modified through the settings.
     */
    public static void updateHooksConfigSnapshot() {
        // Reset cache to ensure fresh settings
        initialHooksConfig = null;
        initialHooksConfig = getHooksFromAllowedSources();
    }

    /**
     * Get the current hooks configuration from snapshot.
     * Falls back to settings if no snapshot exists.
     */
    public static Map<String, List<HookMatcherConfig>> getHooksConfigFromSnapshot() {
        if (initialHooksConfig == null) {
            captureHooksConfigSnapshot();
        }
        return initialHooksConfig != null ? new HashMap<>(initialHooksConfig) : new HashMap<>();
    }

    /**
     * Reset the hooks configuration snapshot (for testing).
     */
    public static void resetHooksConfigSnapshot() {
        initialHooksConfig = null;
        policySettings = null;
        mergedSettings = null;
        strictPluginOnlyCustomization = false;
    }

    /**
     * Check if there are hooks for a specific event.
     */
    public static boolean hasHooksForEvent(String event) {
        Map<String, List<HookMatcherConfig>> config = getHooksConfigFromSnapshot();
        List<HookMatcherConfig> matchers = config.get(event);
        return matchers != null && !matchers.isEmpty();
    }

    /**
     * Get hooks for a specific event.
     */
    public static List<HookMatcherConfig> getHooksForEventFromSnapshot(String event) {
        Map<String, List<HookMatcherConfig>> config = getHooksConfigFromSnapshot();
        return config.getOrDefault(event, new ArrayList<>());
    }
}