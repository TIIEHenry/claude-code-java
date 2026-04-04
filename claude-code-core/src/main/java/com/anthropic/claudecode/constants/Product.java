/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/product.ts
 */
package com.anthropic.claudecode.constants;

/**
 * Product URL constants.
 */
public final class Product {
    private Product() {}

    public static final String PRODUCT_URL = "https://claude.com/claude-code";

    // Claude Code Remote session URLs
    public static final String CLAUDE_AI_BASE_URL = "https://claude.ai";
    public static final String CLAUDE_AI_STAGING_BASE_URL = "https://claude-ai.staging.ant.dev";
    public static final String CLAUDE_AI_LOCAL_BASE_URL = "http://localhost:4000";

    /**
     * Determine if we're in a staging environment for remote sessions.
     */
    public static boolean isRemoteSessionStaging(String sessionId, String ingressUrl) {
        return (sessionId != null && sessionId.contains("_staging_")) ||
               (ingressUrl != null && ingressUrl.contains("staging"));
    }

    /**
     * Determine if we're in a local-dev environment for remote sessions.
     */
    public static boolean isRemoteSessionLocal(String sessionId, String ingressUrl) {
        return (sessionId != null && sessionId.contains("_local_")) ||
               (ingressUrl != null && ingressUrl.contains("localhost"));
    }

    /**
     * Get the base URL for Claude AI based on environment.
     */
    public static String getClaudeAiBaseUrl(String sessionId, String ingressUrl) {
        if (isRemoteSessionLocal(sessionId, ingressUrl)) {
            return CLAUDE_AI_LOCAL_BASE_URL;
        }
        if (isRemoteSessionStaging(sessionId, ingressUrl)) {
            return CLAUDE_AI_STAGING_BASE_URL;
        }
        return CLAUDE_AI_BASE_URL;
    }

    /**
     * Get the full session URL for a remote session.
     */
    public static String getRemoteSessionUrl(String sessionId, String ingressUrl) {
        String compatId = toCompatSessionId(sessionId);
        String baseUrl = getClaudeAiBaseUrl(compatId, ingressUrl);
        return baseUrl + "/code/" + compatId;
    }

    /**
     * Convert session ID to compat format (cse_ -> session_).
     */
    private static String toCompatSessionId(String sessionId) {
        if (sessionId != null && sessionId.startsWith("cse_")) {
            return "session_" + sessionId.substring(4);
        }
        return sessionId;
    }
}