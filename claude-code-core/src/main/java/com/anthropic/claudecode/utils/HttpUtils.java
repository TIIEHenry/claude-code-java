/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code HTTP utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * HTTP utility constants and helpers.
 */
public final class HttpUtils {
    private HttpUtils() {}

    private static final String VERSION = "1.0.0";

    /**
     * Get user agent string.
     */
    public static String getUserAgent() {
        String userType = System.getenv("USER_TYPE");
        String entrypoint = System.getenv("CLAUDE_CODE_ENTRYPOINT");
        String sdkVersion = System.getenv("CLAUDE_AGENT_SDK_VERSION");
        String clientApp = System.getenv("CLAUDE_AGENT_SDK_CLIENT_APP");

        StringBuilder sb = new StringBuilder();
        sb.append("claude-cli/").append(VERSION);
        sb.append(" (").append(userType != null ? userType : "external");
        sb.append(", ").append(entrypoint != null ? entrypoint : "cli");

        if (sdkVersion != null) {
            sb.append(", agent-sdk/").append(sdkVersion);
        }
        if (clientApp != null) {
            sb.append(", client-app/").append(clientApp);
        }

        sb.append(")");
        return sb.toString();
    }

    /**
     * Get MCP user agent.
     */
    public static String getMCPUserAgent() {
        List<String> parts = new ArrayList<>();

        String entrypoint = System.getenv("CLAUDE_CODE_ENTRYPOINT");
        if (entrypoint != null) {
            parts.add(entrypoint);
        }

        String sdkVersion = System.getenv("CLAUDE_AGENT_SDK_VERSION");
        if (sdkVersion != null) {
            parts.add("agent-sdk/" + sdkVersion);
        }

        String clientApp = System.getenv("CLAUDE_AGENT_SDK_CLIENT_APP");
        if (clientApp != null) {
            parts.add("client-app/" + clientApp);
        }

        String suffix = parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
        return "claude-code/" + VERSION + suffix;
    }

    /**
     * Get web fetch user agent.
     */
    public static String getWebFetchUserAgent() {
        return "Claude-User (" + getUserAgent() + "; +https://support.anthropic.com/)";
    }

    /**
     * Auth headers result.
     */
    public record AuthHeaders(Map<String, String> headers, String error) {
        public static AuthHeaders of(Map<String, String> headers) {
            return new AuthHeaders(headers, null);
        }

        public static AuthHeaders error(String error) {
            return new AuthHeaders(Map.of(), error);
        }
    }

    /**
     * Get authentication headers.
     */
    public static AuthHeaders getAuthHeaders() {
        // Check for OAuth
        String oauthToken = System.getenv("CLAUDE_OAUTH_TOKEN");
        if (oauthToken != null && !oauthToken.isEmpty()) {
            return AuthHeaders.of(Map.of(
                    "Authorization", "Bearer " + oauthToken,
                    "anthropic-beta", "oauth-2024-01-01"
            ));
        }

        // Check for API key
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return AuthHeaders.of(Map.of("x-api-key", apiKey));
        }

        return AuthHeaders.error("No API key available");
    }

    /**
     * Build headers map.
     */
    public static Map<String, String> buildHeaders(Map<String, String>... headerMaps) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("User-Agent", getUserAgent());
        result.put("Content-Type", "application/json");

        for (Map<String, String> headers : headerMaps) {
            if (headers != null) {
                result.putAll(headers);
            }
        }

        return result;
    }
}