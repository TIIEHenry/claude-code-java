/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/referral.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;

/**
 * Referral service for managing referral codes and tracking.
 */
public final class ReferralService {
    private ReferralService() {}

    private static volatile String referralCode = null;
    private static volatile String referrerId = null;
    private static volatile Long referralTimestamp = null;

    /**
     * Get referral code.
     */
    public static String getReferralCode() {
        if (referralCode == null) {
            referralCode = System.getenv("CLAUDE_CODE_REFERRAL_CODE");
        }
        return referralCode;
    }

    /**
     * Set referral code.
     */
    public static void setReferralCode(String code) {
        referralCode = code;
    }

    /**
     * Get referrer ID.
     */
    public static String getReferrerId() {
        return referrerId;
    }

    /**
     * Set referrer ID.
     */
    public static void setReferrerId(String id) {
        referrerId = id;
    }

    /**
     * Get referral timestamp.
     */
    public static Long getReferralTimestamp() {
        return referralTimestamp;
    }

    /**
     * Record referral event.
     */
    public static void recordReferral(String code, String referrer) {
        referralCode = code;
        referrerId = referrer;
        referralTimestamp = System.currentTimeMillis();
    }

    /**
     * Check if referral is active.
     */
    public static boolean hasReferral() {
        return referralCode != null || referrerId != null;
    }

    /**
     * Clear referral data.
     */
    public static void clear() {
        referralCode = null;
        referrerId = null;
        referralTimestamp = null;
    }

    /**
     * Get referral info as map.
     */
    public static Map<String, Object> getReferralInfo() {
        Map<String, Object> info = new HashMap<>();
        if (referralCode != null) {
            info.put("referralCode", referralCode);
        }
        if (referrerId != null) {
            info.put("referrerId", referrerId);
        }
        if (referralTimestamp != null) {
            info.put("timestamp", referralTimestamp);
        }
        return info;
    }
}