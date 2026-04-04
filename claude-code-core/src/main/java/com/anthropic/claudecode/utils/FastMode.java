/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code fast mode utilities
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.*;
import java.nio.file.*;
import java.nio.file.Paths;

/**
 * Fast mode utilities for model speed optimization.
 * Fast mode enables faster responses using optimized models.
 */
public final class FastMode {
    private FastMode() {}

    private static volatile FastModeRuntimeState runtimeState = new Active();
    private static volatile boolean hasLoggedCooldownExpiry = false;
    private static volatile OrgStatus orgStatus = new Pending();

    // Signals for events
    private static final Signal cooldownTriggered = new Signal();
    private static final Signal cooldownExpired = new Signal();
    private static final Signal orgFastModeChange = new Signal();
    private static final Signal overageRejection = new Signal();

    /**
     * Fast mode runtime state.
     */
    public sealed interface FastModeRuntimeState permits Active, Cooldown {
        String status();
    }

    public static final class Active implements FastModeRuntimeState {
        @Override public String status() { return "active"; }
    }

    public static final class Cooldown implements FastModeRuntimeState {
        private final long resetAt;
        private final CooldownReason reason;

        public Cooldown(long resetAt, CooldownReason reason) {
            this.resetAt = resetAt;
            this.reason = reason;
        }

        public long resetAt() { return resetAt; }
        public CooldownReason reason() { return reason; }
        @Override public String status() { return "cooldown"; }
    }

    /**
     * Cooldown reason enum.
     */
    public enum CooldownReason {
        RATE_LIMIT, OVERLOADED
    }

    /**
     * Org status for fast mode.
     */
    public sealed interface OrgStatus permits Pending, Enabled, Disabled {
        default String status() { return "unknown"; }
    }

    public static final class Pending implements OrgStatus {
        @Override public String status() { return "pending"; }
    }

    public static final class Enabled implements OrgStatus {
        @Override public String status() { return "enabled"; }
    }

    public static final class Disabled implements OrgStatus {
        private final FastModeDisabledReason reason;

        public Disabled(FastModeDisabledReason reason) {
            this.reason = reason;
        }

        public FastModeDisabledReason reason() { return reason; }
        @Override public String status() { return "disabled"; }
    }

    /**
     * Fast mode disabled reason enum.
     */
    public enum FastModeDisabledReason {
        FREE, PREFERENCE, EXTRA_USAGE_DISABLED, NETWORK_ERROR, UNKNOWN
    }

    /**
     * Check if fast mode is enabled.
     */
    public static boolean isFastModeEnabled() {
        return !EnvUtils.isTruthy(System.getenv("CLAUDE_CODE_DISABLE_FAST_MODE"));
    }

    /**
     * Check if fast mode is available.
     */
    public static boolean isFastModeAvailable() {
        if (!isFastModeEnabled()) {
            return false;
        }
        return getFastModeUnavailableReason() == null;
    }

    /**
     * Get reason why fast mode is unavailable.
     */
    public static String getFastModeUnavailableReason() {
        if (!isFastModeEnabled()) {
            return "Fast mode is not available";
        }

        // Check API provider
        String provider = System.getenv("CLAUDE_CODE_API_PROVIDER");
        if (provider != null && !provider.equals("firstParty")) {
            return "Fast mode is not available on Bedrock, Vertex, or Foundry";
        }

        if (orgStatus instanceof Disabled disabled) {
            return getDisabledReasonMessage(disabled.reason());
        }

        return null;
    }

    /**
     * Get disabled reason message.
     */
    private static String getDisabledReasonMessage(FastModeDisabledReason reason) {
        return switch (reason) {
            case FREE -> "Fast mode requires a paid subscription";
            case PREFERENCE -> "Fast mode has been disabled by your organization";
            case EXTRA_USAGE_DISABLED -> "Fast mode requires extra usage billing · /extra-usage to enable";
            case NETWORK_ERROR -> "Fast mode unavailable due to network connectivity issues";
            case UNKNOWN -> "Fast mode is currently unavailable";
        };
    }

    /**
     * Get fast mode runtime state.
     */
    public static FastModeRuntimeState getFastModeRuntimeState() {
        if (runtimeState instanceof Cooldown cooldown && System.currentTimeMillis() >= cooldown.resetAt()) {
            if (isFastModeEnabled() && !hasLoggedCooldownExpiry) {
                hasLoggedCooldownExpiry = true;
                cooldownExpired.emit();
            }
            runtimeState = new Active();
        }
        return runtimeState;
    }

    /**
     * Trigger fast mode cooldown.
     */
    public static void triggerFastModeCooldown(long resetTimestamp, CooldownReason reason) {
        if (!isFastModeEnabled()) {
            return;
        }
        runtimeState = new Cooldown(resetTimestamp, reason);
        hasLoggedCooldownExpiry = false;
        cooldownTriggered.emit();
    }

    /**
     * Clear fast mode cooldown.
     */
    public static void clearFastModeCooldown() {
        runtimeState = new Active();
    }

    /**
     * Check if in fast mode cooldown.
     */
    public static boolean isFastModeCooldown() {
        return getFastModeRuntimeState() instanceof Cooldown;
    }

    /**
     * Get fast mode state.
     */
    public static String getFastModeState(String model, boolean fastModeUserEnabled) {
        boolean enabled = isFastModeEnabled() &&
                isFastModeAvailable() &&
                fastModeUserEnabled &&
                isFastModeSupportedByModel(model);

        if (enabled && isFastModeCooldown()) {
            return "cooldown";
        }
        if (enabled) {
            return "on";
        }
        return "off";
    }

    /**
     * Check if model supports fast mode.
     */
    public static boolean isFastModeSupportedByModel(String model) {
        if (!isFastModeEnabled()) {
            return false;
        }
        if (model == null) return false;
        String lower = model.toLowerCase();
        return lower.contains("opus-4-6");
    }

    /**
     * Get fast mode model.
     */
    public static String getFastModeModel() {
        return "opus";
    }

    /**
     * Subscribe to cooldown triggered.
     */
    public static void onCooldownTriggered(Runnable callback) {
        cooldownTriggered.subscribe(callback);
    }

    /**
     * Subscribe to cooldown expired.
     */
    public static void onCooldownExpired(Runnable callback) {
        cooldownExpired.subscribe(callback);
    }

    /**
     * Subscribe to org fast mode change.
     */
    public static void onOrgFastModeChanged(Runnable callback) {
        orgFastModeChange.subscribe(callback);
    }

    /**
     * Handle fast mode rejected by API.
     */
    public static void handleFastModeRejectedByAPI() {
        if (orgStatus instanceof Disabled) {
            return;
        }
        orgStatus = new Disabled(FastModeDisabledReason.PREFERENCE);
        orgFastModeChange.emit();
    }

    /**
     * Handle fast mode overage rejection.
     */
    public static void handleFastModeOverageRejection(String reason) {
        overageRejection.emit();
    }

    /**
     * Resolve fast mode status from cache.
     */
    public static void resolveFastModeStatusFromCache() {
        if (!isFastModeEnabled()) {
            return;
        }
        if (!(orgStatus instanceof Pending)) {
            return;
        }
        String userType = System.getenv("USER_TYPE");
        boolean isAnt = "ant".equals(userType);
        // Check cached enabled status from config
        orgStatus = isAnt ? new Enabled() : new Disabled(FastModeDisabledReason.UNKNOWN);
    }

    /**
     * Prefetch fast mode status.
     */
    public static CompletableFuture<Void> prefetchFastModeStatus() {
        return CompletableFuture.runAsync(() -> {
            try {
                // First try cache
                resolveFastModeStatusFromCache();

                if (!(orgStatus instanceof Pending)) {
                    return;
                }

                // Check stored preference
                Path configFile = Paths.get(System.getProperty("user.home"))
                    .resolve(".claude")
                    .resolve("fast-mode.json");

                if (Files.exists(configFile)) {
                    String content = Files.readString(configFile);
                    boolean enabled = content.contains("\"enabled\":true");
                    orgStatus = enabled ? new Enabled() : new Disabled(FastModeDisabledReason.PREFERENCE);
                    orgFastModeChange.emit();
                    return;
                }

                // Check subscription type from environment or API
                String subscription = System.getenv("CLAUDE_SUBSCRIPTION_TYPE");
                if (subscription != null) {
                    switch (subscription.toLowerCase()) {
                        case "pro", "team", "enterprise" -> {
                            orgStatus = new Enabled();
                        }
                        case "free" -> {
                            orgStatus = new Disabled(FastModeDisabledReason.FREE);
                        }
                        default -> {
                            orgStatus = new Disabled(FastModeDisabledReason.UNKNOWN);
                        }
                    }
                    orgFastModeChange.emit();
                }
            } catch (Exception e) {
                // On error, disable fast mode
                orgStatus = new Disabled(FastModeDisabledReason.NETWORK_ERROR);
                orgFastModeChange.emit();
            }
        });
    }
}