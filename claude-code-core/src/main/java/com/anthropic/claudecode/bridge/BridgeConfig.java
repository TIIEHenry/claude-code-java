/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bridge/bridgeConfig
 */
package com.anthropic.claudecode.bridge;

/**
 * Bridge config - Shared bridge auth/URL resolution.
 *
 * Two layers: *Override() returns the ant-only env var (or undefined);
 * the non-Override versions fall through to the real OAuth store/config.
 */
public final class BridgeConfig {
    private BridgeConfig() {}

    /**
     * Ant-only dev override: CLAUDE_BRIDGE_OAUTH_TOKEN, else null.
     */
    public static String getBridgeTokenOverride() {
        if ("ant".equals(System.getenv("USER_TYPE"))) {
            return System.getenv("CLAUDE_BRIDGE_OAUTH_TOKEN");
        }
        return null;
    }

    /**
     * Ant-only dev override: CLAUDE_BRIDGE_BASE_URL, else null.
     */
    public static String getBridgeBaseUrlOverride() {
        if ("ant".equals(System.getenv("USER_TYPE"))) {
            return System.getenv("CLAUDE_BRIDGE_BASE_URL");
        }
        return null;
    }

    /**
     * Access token for bridge API calls: dev override first, then the OAuth
     * keychain. Null means "not logged in".
     */
    public static String getBridgeAccessToken() {
        String override = getBridgeTokenOverride();
        if (override != null) {
            return override;
        }
        // Fall through to OAuth tokens
        return System.getenv("CLAUDE_CODE_ACCESS_TOKEN");
    }

    /**
     * Base URL for bridge API calls: dev override first, then the production
     * OAuth config. Always returns a URL.
     */
    public static String getBridgeBaseUrl() {
        String override = getBridgeBaseUrlOverride();
        if (override != null) {
            return override;
        }
        // Fall through to OAuth config
        String url = System.getenv("CLAUDE_CODE_API_URL");
        return url != null ? url : "https://api.claude.ai";
    }

    /**
     * Check if bridge is enabled.
     */
    public static boolean isBridgeEnabled() {
        return getBridgeAccessToken() != null;
    }

    /**
     * Get bridge API URL for a specific path.
     */
    public static String getBridgeApiUrl(String path) {
        String baseUrl = getBridgeBaseUrl();
        if (path.startsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    /**
     * Get session ingress URL.
     */
    public static String getSessionIngressUrl() {
        String url = System.getenv("CLAUDE_CODE_SESSION_INGRESS_URL");
        if (url != null) {
            return url;
        }
        return getBridgeBaseUrl();
    }

    /**
     * Get session ingress WebSocket URL.
     */
    public static String getSessionIngressWsUrl() {
        String httpUrl = getSessionIngressUrl();
        if (httpUrl.startsWith("https://")) {
            return "wss://" + httpUrl.substring(8);
        }
        if (httpUrl.startsWith("http://")) {
            return "ws://" + httpUrl.substring(7);
        }
        return httpUrl;
    }
}