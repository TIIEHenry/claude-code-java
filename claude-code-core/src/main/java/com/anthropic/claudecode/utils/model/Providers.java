/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code model provider utilities
 */
package com.anthropic.claudecode.utils.model;

/**
 * API provider types and detection.
 */
public final class Providers {
    private Providers() {}

    /**
     * API provider types.
     */
    public enum APIProvider {
        FIRST_PARTY("firstParty"),
        BEDROCK("bedrock"),
        VERTEX("vertex"),
        FOUNDRY("foundry");

        private final String value;

        APIProvider(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static APIProvider fromString(String value) {
            for (APIProvider provider : values()) {
                if (provider.value.equalsIgnoreCase(value)) {
                    return provider;
                }
            }
            return FIRST_PARTY;
        }
    }

    /**
     * Get the current API provider.
     */
    public static APIProvider getAPIProvider() {
        if (isEnvTruthy("CLAUDE_CODE_USE_BEDROCK")) {
            return APIProvider.BEDROCK;
        }
        if (isEnvTruthy("CLAUDE_CODE_USE_VERTEX")) {
            return APIProvider.VERTEX;
        }
        if (isEnvTruthy("CLAUDE_CODE_USE_FOUNDRY")) {
            return APIProvider.FOUNDRY;
        }
        return APIProvider.FIRST_PARTY;
    }

    /**
     * Check if using first-party API.
     */
    public static boolean isFirstParty() {
        return getAPIProvider() == APIProvider.FIRST_PARTY;
    }

    /**
     * Check if ANTHROPIC_BASE_URL is a first-party Anthropic API URL.
     */
    public static boolean isFirstPartyAnthropicBaseUrl() {
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            return true;
        }

        try {
            java.net.URL url = new java.net.URL(baseUrl);
            String host = url.getHost();

            if ("api.anthropic.com".equals(host)) {
                return true;
            }

            // Ant users also allow staging
            if ("ant".equals(System.getenv("USER_TYPE")) &&
                "api-staging.anthropic.com".equals(host)) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if using Bedrock.
     */
    public static boolean isBedrock() {
        return getAPIProvider() == APIProvider.BEDROCK;
    }

    /**
     * Check if using Vertex.
     */
    public static boolean isVertex() {
        return getAPIProvider() == APIProvider.VERTEX;
    }

    /**
     * Check if using Foundry.
     */
    public static boolean isFoundry() {
        return getAPIProvider() == APIProvider.FOUNDRY;
    }

    /**
     * Get the provider name for analytics.
     */
    public static String getAPIProviderForAnalytics() {
        return getAPIProvider().getValue();
    }

    private static boolean isEnvTruthy(String name) {
        String value = System.getenv(name);
        if (value == null) return false;
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "yes".equalsIgnoreCase(value);
    }
}