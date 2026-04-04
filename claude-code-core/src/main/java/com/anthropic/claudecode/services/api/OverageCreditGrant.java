/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/overageCreditGrant.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Overage credit grant service for managing extra usage credits.
 */
public final class OverageCreditGrant {
    private OverageCreditGrant() {}

    private static final AtomicReference<Double> creditBalance = new AtomicReference<>(0.0);
    private static final AtomicBoolean overageEnabled = new AtomicBoolean(false);
    private static final AtomicReference<String> disabledReason = new AtomicReference<>(null);

    /**
     * Check if overage is enabled.
     */
    public static boolean isOverageEnabled() {
        return overageEnabled.get();
    }

    /**
     * Set overage enabled status.
     */
    public static void setOverageEnabled(boolean enabled) {
        overageEnabled.set(enabled);
        if (enabled) {
            disabledReason.set(null);
        }
    }

    /**
     * Get disabled reason.
     */
    public static String getDisabledReason() {
        return disabledReason.get();
    }

    /**
     * Set disabled reason.
     */
    public static void setDisabledReason(String reason) {
        disabledReason.set(reason);
        overageEnabled.set(false);
    }

    /**
     * Get credit balance.
     */
    public static double getCreditBalance() {
        return creditBalance.get();
    }

    /**
     * Set credit balance.
     */
    public static void setCreditBalance(double balance) {
        creditBalance.set(balance);
    }

    /**
     * Add credits.
     */
    public static void addCredits(double amount) {
        creditBalance.updateAndGet(v -> v + amount);
    }

    /**
     * Use credits.
     * @return true if credits were available and used
     */
    public static boolean useCredits(double amount) {
        AtomicBoolean success = new AtomicBoolean(false);
        creditBalance.updateAndGet(v -> {
            if (v >= amount) {
                success.set(true);
                return v - amount;
            }
            return v;
        });
        return success.get();
    }

    /**
     * Check if has sufficient credits.
     */
    public static boolean hasCredits(double amount) {
        return creditBalance.get() >= amount;
    }

    /**
     * Update overage status from headers.
     */
    public static void updateFromHeaders(Map<String, String> headers) {
        String status = headers.get("anthropic-ratelimit-unified-overage-status");
        if (status != null) {
            setOverageEnabled("allowed".equalsIgnoreCase(status));
        }

        String reason = headers.get("anthropic-ratelimit-unified-overage-disabled-reason");
        if (reason != null) {
            setDisabledReason(reason);
        }
    }

    /**
     * Get overage status info.
     */
    public static Map<String, Object> getOverageStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", overageEnabled.get());
        status.put("creditBalance", creditBalance.get());
        if (disabledReason.get() != null) {
            status.put("disabledReason", disabledReason.get());
        }
        return status;
    }

    /**
     * Reset overage state.
     */
    public static void reset() {
        creditBalance.set(0.0);
        overageEnabled.set(false);
        disabledReason.set(null);
    }
}