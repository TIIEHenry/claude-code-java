/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/oauth/config
 */
package com.anthropic.claudecode.services.oauth;

import java.util.List;
import java.util.Map;

/**
 * OAuth configuration - OAuth endpoints and client configuration.
 */
public final class OAuthConfig {
    private final String clientId;
    private final String tokenUrl;
    private final String consoleAuthorizeUrl;
    private final String claudeAiAuthorizeUrl;
    private final String manualRedirectUrl;
    private final String rolesUrl;
    private final String apiKeyUrl;
    private final String profileUrl;
    private final String claudeAiInferenceScope;
    private final List<String> claudeAiOauthScopes;
    private final List<String> allOauthScopes;

    public OAuthConfig() {
        this.clientId = getEnvOrDefault("CLAUDE_CODE_CLIENT_ID", "claude-code");
        this.tokenUrl = getEnvOrDefault("CLAUDE_CODE_TOKEN_URL", "https://claude.ai/api/oauth/token");
        this.consoleAuthorizeUrl = getEnvOrDefault("CLAUDE_CODE_CONSOLE_AUTHORIZE_URL", "https://console.anthropic.com/oauth/authorize");
        this.claudeAiAuthorizeUrl = getEnvOrDefault("CLAUDE_CODE_CLAUDE_AI_AUTHORIZE_URL", "https://claude.ai/oauth/authorize");
        this.manualRedirectUrl = getEnvOrDefault("CLAUDE_CODE_MANUAL_REDIRECT_URL", "https://claude.ai/oauth/callback/manual");
        this.rolesUrl = getEnvOrDefault("CLAUDE_CODE_ROLES_URL", "https://claude.ai/api/oauth/roles");
        this.apiKeyUrl = getEnvOrDefault("CLAUDE_CODE_API_KEY_URL", "https://claude.ai/api/oauth/api_key");
        this.profileUrl = getEnvOrDefault("CLAUDE_CODE_PROFILE_URL", "https://claude.ai/api/oauth/profile");
        this.claudeAiInferenceScope = "https://claude.ai/inference";

        this.claudeAiOauthScopes = List.of(
            "https://claude.ai/inference",
            "https://claude.ai/api/oauth",
            "user:profile"
        );

        this.allOauthScopes = List.of(
            "https://claude.ai/inference",
            "https://claude.ai/api/oauth",
            "user:profile",
            "offline_access"
        );
    }

    private String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return value != null ? value : defaultValue;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getConsoleAuthorizeUrl() {
        return consoleAuthorizeUrl;
    }

    public String getClaudeAiAuthorizeUrl() {
        return claudeAiAuthorizeUrl;
    }

    public String getManualRedirectUrl() {
        return manualRedirectUrl;
    }

    public String getRolesUrl() {
        return rolesUrl;
    }

    public String getApiKeyUrl() {
        return apiKeyUrl;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public String getClaudeAiInferenceScope() {
        return claudeAiInferenceScope;
    }

    public List<String> getClaudeAiOauthScopes() {
        return claudeAiOauthScopes;
    }

    public List<String> getAllOauthScopes() {
        return allOauthScopes;
    }

    /**
     * Check if user has Claude AI inference scope.
     */
    public boolean shouldUseClaudeAiAuth(List<String> scopes) {
        return scopes != null && scopes.contains(claudeAiInferenceScope);
    }

    /**
     * Check if user has profile scope.
     */
    public boolean hasProfileScope(List<String> scopes) {
        return scopes != null && scopes.contains("user:profile");
    }

    // Singleton instance
    private static final OAuthConfig INSTANCE = new OAuthConfig();

    public static OAuthConfig getInstance() {
        return INSTANCE;
    }
}