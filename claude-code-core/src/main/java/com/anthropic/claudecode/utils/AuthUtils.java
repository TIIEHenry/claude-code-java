/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/auth.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication utilities.
 */
public final class AuthUtils {
    private AuthUtils() {}

    // Environment variable names
    public static final String ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY";
    public static final String ANTHROPIC_AUTH_TOKEN = "ANTHROPIC_AUTH_TOKEN";
    public static final String CLAUDE_CODE_USE_BEDROCK = "CLAUDE_CODE_USE_BEDROCK";
    public static final String CLAUDE_CODE_USE_VERTEX = "CLAUDE_CODE_USE_VERTEX";
    public static final String CLAUDE_CODE_USE_FOUNDRY = "CLAUDE_CODE_USE_FOUNDRY";

    // Auth state
    private static String cachedApiKey;
    private static AuthState authState;

    /**
     * Auth state record.
     */
    public record AuthState(
            String apiKey,
            String authType,
            boolean isClaudeAISubscriber,
            String subscriptionType,
            String organizationId
    ) {
        public static AuthState unauthenticated() {
            return new AuthState(null, null, false, null, null);
        }
    }

    /**
     * Subscription types.
     */
    public enum SubscriptionType {
        FREE,
        PRO,
        MAX,
        TEAM,
        ENTERPRISE
    }

    /**
     * Auth provider types.
     */
    public enum AuthProvider {
        ANTHROPIC,
        BEDROCK,
        VERTEX,
        FOUNDRY,
        CUSTOM
    }

    /**
     * Check if Anthropic auth is enabled.
     */
    public static boolean isAnthropicAuthEnabled() {
        return !isEnvTruthy(CLAUDE_CODE_USE_BEDROCK) &&
               !isEnvTruthy(CLAUDE_CODE_USE_VERTEX) &&
               !isEnvTruthy(CLAUDE_CODE_USE_FOUNDRY);
    }

    /**
     * Get the current auth provider.
     */
    public static AuthProvider getAuthProvider() {
        if (isEnvTruthy(CLAUDE_CODE_USE_BEDROCK)) {
            return AuthProvider.BEDROCK;
        }
        if (isEnvTruthy(CLAUDE_CODE_USE_VERTEX)) {
            return AuthProvider.VERTEX;
        }
        if (isEnvTruthy(CLAUDE_CODE_USE_FOUNDRY)) {
            return AuthProvider.FOUNDRY;
        }
        return AuthProvider.ANTHROPIC;
    }

    /**
     * Get the API key from environment.
     */
    public static String getApiKey() {
        // Check cache
        if (cachedApiKey != null) {
            return cachedApiKey;
        }

        // Check environment variables
        String apiKey = System.getenv(ANTHROPIC_API_KEY);
        if (apiKey != null && !apiKey.isEmpty()) {
            cachedApiKey = apiKey;
            return apiKey;
        }

        String authToken = System.getenv(ANTHROPIC_AUTH_TOKEN);
        if (authToken != null && !authToken.isEmpty()) {
            cachedApiKey = authToken;
            return authToken;
        }

        return null;
    }

    /**
     * Set the API key.
     */
    public static void setApiKey(String apiKey) {
        cachedApiKey = apiKey;
    }

    /**
     * Clear the cached API key.
     */
    public static void clearApiKey() {
        cachedApiKey = null;
    }

    /**
     * Check if authenticated.
     */
    public static boolean isAuthenticated() {
        return getApiKey() != null;
    }

    /**
     * Check if has API key.
     */
    public static boolean hasApiKey() {
        return getApiKey() != null;
    }

    /**
     * Check if has token.
     */
    public static boolean hasToken() {
        return getApiKey() != null;
    }

    /**
     * Get subscription type.
     */
    public static String getSubscriptionType() {
        AuthState state = getAuthState();
        return state != null ? state.subscriptionType() : null;
    }

    /**
     * Get organization role.
     */
    public static String getOrganizationRole() {
        // Try to fetch from stored auth data
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path rolesPath = java.nio.file.Paths.get(home, ".claude", "user-roles.json");

            if (java.nio.file.Files.exists(rolesPath)) {
                String content = java.nio.file.Files.readString(rolesPath);
                return extractJsonValueString(content, "organization_role");
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Get workspace role.
     */
    public static String getWorkspaceRole() {
        // Try to fetch from stored auth data
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path rolesPath = java.nio.file.Paths.get(home, ".claude", "user-roles.json");

            if (java.nio.file.Files.exists(rolesPath)) {
                String content = java.nio.file.Files.readString(rolesPath);
                return extractJsonValueString(content, "workspace_role");
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private static String extractJsonValueString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 3) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    /**
     * Check if user is a Claude AI subscriber.
     */
    public static boolean isClaudeAISubscriber() {
        AuthState state = getAuthState();
        return state != null && state.isClaudeAISubscriber();
    }

    /**
     * Get auth state.
     */
    public static AuthState getAuthState() {
        if (authState != null) {
            return authState;
        }

        String apiKey = getApiKey();
        if (apiKey == null) {
            return AuthState.unauthenticated();
        }

        return new AuthState(
            apiKey,
            "api_key",
            false,
            null,
            null
        );
    }

    /**
     * Set auth state.
     */
    public static void setAuthState(AuthState state) {
        authState = state;
    }

    /**
     * Check if using third-party provider.
     */
    public static boolean isThirdPartyProvider() {
        AuthProvider provider = getAuthProvider();
        return provider == AuthProvider.BEDROCK ||
               provider == AuthProvider.VERTEX ||
               provider == AuthProvider.FOUNDRY;
    }

    /**
     * Validate API key format.
     */
    public static boolean isValidApiKeyFormat(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }

        // Anthropic API keys start with sk-ant-
        if (apiKey.startsWith("sk-ant-")) {
            return apiKey.length() >= 40;
        }

        // Bedrock/Vertex keys have different formats
        return apiKey.length() >= 20;
    }

    private static boolean isEnvTruthy(String envVar) {
        String value = System.getenv(envVar);
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }
}