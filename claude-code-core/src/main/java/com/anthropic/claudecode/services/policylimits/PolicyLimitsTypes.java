/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/policyLimits/types
 */
package com.anthropic.claudecode.services.policylimits;

import java.util.*;

/**
 * Policy limits types - Types for policy limits API.
 */
public final class PolicyLimitsTypes {

    /**
     * Policy limits response record.
     */
    public record PolicyLimitsResponse(
        Map<String, PolicyRestriction> restrictions
    ) {
        public static PolicyLimitsResponse empty() {
            return new PolicyLimitsResponse(Collections.emptyMap());
        }

        public boolean isAllowed(String policyKey) {
            PolicyRestriction restriction = restrictions.get(policyKey);
            return restriction == null || restriction.isAllowed();
        }

        public boolean isRestricted(String policyKey) {
            PolicyRestriction restriction = restrictions.get(policyKey);
            return restriction != null && !restriction.isAllowed();
        }
    }

    /**
     * Policy restriction record.
     */
    public record PolicyRestriction(
        boolean isAllowed
    ) {
        public static PolicyRestriction allowed() {
            return new PolicyRestriction(true);
        }

        public static PolicyRestriction denied() {
            return new PolicyRestriction(false);
        }
    }

    /**
     * Policy limits fetch result record.
     */
    public record PolicyLimitsFetchResult(
        boolean success,
        Map<String, PolicyRestriction> restrictions,
        String etag,
        String error,
        boolean skipRetry
    ) {
        public static PolicyLimitsFetchResult success(
            Map<String, PolicyRestriction> restrictions,
            String etag
        ) {
            return new PolicyLimitsFetchResult(true, restrictions, etag, null, false);
        }

        public static PolicyLimitsFetchResult notModified(String etag) {
            return new PolicyLimitsFetchResult(true, null, etag, null, false);
        }

        public static PolicyLimitsFetchResult failure(String error, boolean skipRetry) {
            return new PolicyLimitsFetchResult(false, null, null, error, skipRetry);
        }

        public static PolicyLimitsFetchResult failure(String error) {
            return failure(error, false);
        }

        public boolean hasRestrictions() {
            return restrictions != null;
        }

        public boolean isNotModified() {
            return success && restrictions == null;
        }
    }

    /**
     * Policy key enum.
     */
    public enum PolicyKey {
        BASH_EXECUTION("bash_execution"),
        FILE_READ("file_read"),
        FILE_WRITE("file_write"),
        FILE_EDIT("file_edit"),
        WEB_FETCH("web_fetch"),
        WEB_SEARCH("web_search"),
        GLOB("glob"),
        GREP("grep"),
        AGENT_SPAWN("agent_spawn"),
        MCP_TOOLS("mcp_tools");

        private final String key;

        PolicyKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static PolicyKey fromKey(String key) {
            for (PolicyKey pk : values()) {
                if (pk.key.equals(key)) {
                    return pk;
                }
            }
            return null;
        }
    }

    /**
     * Policy limits config record.
     */
    public record PolicyLimitsConfig(
        boolean enabled,
        long refreshIntervalMs,
        int maxRetries,
        long retryDelayMs
    ) {
        public static PolicyLimitsConfig defaults() {
            return new PolicyLimitsConfig(
                true,
                300_000, // 5 minutes
                3,
                1000
            );
        }
    }

    /**
     * Policy check result sealed interface.
     */
    public sealed interface PolicyCheckResult permits
        PolicyCheckResult.Allowed,
        PolicyCheckResult.Denied {

        public static final class Allowed implements PolicyCheckResult {
            public static final Allowed INSTANCE = new Allowed();
        }

        public static final class Denied implements PolicyCheckResult {
            private final String policyKey;
            private final String reason;

            public Denied(String policyKey, String reason) {
                this.policyKey = policyKey;
                this.reason = reason;
            }

            public String getPolicyKey() { return policyKey; }
            public String getReason() { return reason; }
        }
    }
}