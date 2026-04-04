/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/billing
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Billing utilities - Billing access and subscription management.
 */
public final class BillingUtils {
    private static Boolean mockBillingAccessOverride = null;

    /**
     * Check if user has console billing access.
     */
    public static boolean hasConsoleBillingAccess() {
        // Check if cost reporting is disabled
        if ("true".equalsIgnoreCase(System.getenv("DISABLE_COST_WARNINGS"))) {
            return false;
        }

        // Check if using subscription
        if (AuthUtils.isClaudeAISubscriber()) {
            return false;
        }

        // Check if user has authentication
        if (!AuthUtils.hasToken() && !AuthUtils.hasApiKey()) {
            return false;
        }

        // Check organization and workspace roles
        String orgRole = AuthUtils.getOrganizationRole();
        String workspaceRole = AuthUtils.getWorkspaceRole();

        if (orgRole == null || workspaceRole == null) {
            return false;
        }

        // Users have billing access if they are admins or billing roles
        return isAdminOrBilling(orgRole) || isWorkspaceAdminOrBilling(workspaceRole);
    }

    /**
     * Check if user has Claude.ai billing access.
     */
    public static boolean hasClaudeAiBillingAccess() {
        // Check for mock billing access first
        if (mockBillingAccessOverride != null) {
            return mockBillingAccessOverride;
        }

        if (!AuthUtils.isClaudeAISubscriber()) {
            return false;
        }

        String subscriptionType = AuthUtils.getSubscriptionType();

        // Consumer plans - individual users always have billing access
        if ("max".equals(subscriptionType) || "pro".equals(subscriptionType)) {
            return true;
        }

        // Team/Enterprise - check for admin or billing roles
        String orgRole = AuthUtils.getOrganizationRole();
        return orgRole != null && isOrgAdminOrBilling(orgRole);
    }

    /**
     * Set mock billing access override (for testing).
     */
    public static void setMockBillingAccessOverride(Boolean value) {
        mockBillingAccessOverride = value;
    }

    /**
     * Check if user can view costs.
     */
    public static boolean canViewCosts() {
        return hasConsoleBillingAccess() || hasClaudeAiBillingAccess();
    }

    /**
     * Check if user can manage billing.
     */
    public static boolean canManageBilling() {
        String orgRole = AuthUtils.getOrganizationRole();
        return orgRole != null && isOrgAdminOrBilling(orgRole);
    }

    /**
     * Get billing plan type.
     */
    public static String getBillingPlanType() {
        String subscriptionType = AuthUtils.getSubscriptionType();

        if (subscriptionType == null) {
            return "free";
        }

        return switch (subscriptionType) {
            case "max" -> "claude_max";
            case "pro" -> "claude_pro";
            case "team" -> "claude_team";
            case "enterprise" -> "claude_enterprise";
            default -> "free";
        };
    }

    /**
     * Check if subscription is enterprise.
     */
    public static boolean isEnterpriseSubscription() {
        String subscriptionType = AuthUtils.getSubscriptionType();
        return "enterprise".equals(subscriptionType) || "team".equals(subscriptionType);
    }

    /**
     * Check if subscription is consumer (Max/Pro).
     */
    public static boolean isConsumerSubscription() {
        String subscriptionType = AuthUtils.getSubscriptionType();
        return "max".equals(subscriptionType) || "pro".equals(subscriptionType);
    }

    private static boolean isAdminOrBilling(String role) {
        return "admin".equals(role) || "billing".equals(role);
    }

    private static boolean isWorkspaceAdminOrBilling(String role) {
        return "workspace_admin".equals(role) || "workspace_billing".equals(role);
    }

    private static boolean isOrgAdminOrBilling(String role) {
        return "admin".equals(role) || "billing".equals(role) ||
               "owner".equals(role) || "primary_owner".equals(role);
    }

    /**
     * Billing info record.
     */
    public record BillingInfo(
        String subscriptionType,
        String organizationRole,
        String workspaceRole,
        boolean hasBillingAccess,
        boolean canViewCosts,
        boolean canManageBilling
    ) {
        public static BillingInfo current() {
            return new BillingInfo(
                AuthUtils.getSubscriptionType(),
                AuthUtils.getOrganizationRole(),
                AuthUtils.getWorkspaceRole(),
                hasClaudeAiBillingAccess(),
                BillingUtils.canViewCosts(),
                BillingUtils.canManageBilling()
            );
        }
    }
}