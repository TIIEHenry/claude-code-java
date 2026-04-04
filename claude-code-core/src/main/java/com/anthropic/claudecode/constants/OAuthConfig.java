/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/oauth
 */
package com.anthropic.claudecode.constants;

import java.util.*;

/**
 * OAuth configuration constants.
 */
public final class OAuthConfig {
    private OAuthConfig() {}

    // OAuth scopes
    public static final String CLAUDE_AI_INFERENCE_SCOPE = "user:inference";
    public static final String CLAUDE_AI_PROFILE_SCOPE = "user:profile";
    private static final String CONSOLE_SCOPE = "org:create_api_key";
    public static final String OAUTH_BETA_HEADER = "oauth-2025-04-20";

    // Console OAuth scopes
    public static final List<String> CONSOLE_OAUTH_SCOPES = List.of(
        CONSOLE_SCOPE,
        CLAUDE_AI_PROFILE_SCOPE
    );

    // Claude.ai OAuth scopes
    public static final List<String> CLAUDE_AI_OAUTH_SCOPES = List.of(
        CLAUDE_AI_PROFILE_SCOPE,
        CLAUDE_AI_INFERENCE_SCOPE,
        "user:sessions:claude_code",
        "user:mcp_servers",
        "user:file_upload"
    );

    // All OAuth scopes
    public static final List<String> ALL_OAUTH_SCOPES;
    static {
        Set<String> allScopes = new LinkedHashSet<>();
        allScopes.addAll(CONSOLE_OAUTH_SCOPES);
        allScopes.addAll(CLAUDE_AI_OAUTH_SCOPES);
        ALL_OAUTH_SCOPES = List.copyOf(allScopes);
    }

    // MCP Client Metadata URL
    public static final String MCP_CLIENT_METADATA_URL =
        "https://claude.ai/oauth/claude-code-client-metadata";

    // Production OAuth configuration
    private static final OAuthConfigInstance PROD_CONFIG = new OAuthConfigInstance(
        "https://api.anthropic.com",
        "https://platform.claude.com/oauth/authorize",
        "https://claude.com/cai/oauth/authorize",
        "https://claude.ai",
        "https://platform.claude.com/v1/oauth/token",
        "https://api.anthropic.com/api/oauth/claude_cli/create_api_key",
        "https://api.anthropic.com/api/oauth/claude_cli/roles",
        "https://platform.claude.com/buy_credits?returnUrl=/oauth/code/success%3Fapp%3Dclaude-code",
        "https://platform.claude.com/oauth/code/success?app=claude-code",
        "https://platform.claude.com/oauth/code/callback",
        "9d1c250a-e61b-44d9-88ed-5944d1962f5e",
        "",
        "https://mcp-proxy.anthropic.com",
        "/v1/mcp/{server_id}"
    );

    // Allowed custom OAuth base URLs
    private static final Set<String> ALLOWED_OAUTH_BASE_URLS = Set.of(
        "https://beacon.claude-ai.staging.ant.dev",
        "https://claude.fedstart.com",
        "https://claude-staging.fedstart.com"
    );

    /**
     * OAuth config instance record.
     */
    public record OAuthConfigInstance(
        String baseApiUrl,
        String consoleAuthorizeUrl,
        String claudeAiAuthorizeUrl,
        String claudeAiOrigin,
        String tokenUrl,
        String apiKeyUrl,
        String rolesUrl,
        String consoleSuccessUrl,
        String claudeaiSuccessUrl,
        String manualRedirectUrl,
        String clientId,
        String oauthFileSuffix,
        String mcpProxyUrl,
        String mcpProxyPath
    ) {}

    /**
     * Get OAuth config type.
     */
    public static String getOauthConfigType() {
        String userType = System.getenv("USER_TYPE");
        if ("ant".equals(userType)) {
            if (isEnvTruthy("USE_LOCAL_OAUTH")) {
                return "local";
            }
            if (isEnvTruthy("USE_STAGING_OAUTH")) {
                return "staging";
            }
        }
        return "prod";
    }

    /**
     * Get file suffix for OAuth config.
     */
    public static String fileSuffixForOauthConfig() {
        String customUrl = System.getenv("CLAUDE_CODE_CUSTOM_OAUTH_URL");
        if (customUrl != null) {
            return "-custom-oauth";
        }
        switch (getOauthConfigType()) {
            case "local":
                return "-local-oauth";
            case "staging":
                return "-staging-oauth";
            default:
                return "";
        }
    }

    /**
     * Get OAuth configuration.
     */
    public static OAuthConfigInstance getOauthConfig() {
        OAuthConfigInstance config;

        switch (getOauthConfigType()) {
            case "local":
                config = getLocalOauthConfig();
                break;
            case "staging":
                config = getStagingOauthConfig();
                break;
            default:
                config = PROD_CONFIG;
        }

        // Handle custom OAuth URL override
        String oauthBaseUrl = System.getenv("CLAUDE_CODE_CUSTOM_OAUTH_URL");
        if (oauthBaseUrl != null) {
            String base = oauthBaseUrl.replaceAll("/$", "");
            if (!ALLOWED_OAUTH_BASE_URLS.contains(base)) {
                throw new RuntimeException(
                    "CLAUDE_CODE_CUSTOM_OAUTH_URL is not an approved endpoint."
                );
            }
            config = new OAuthConfigInstance(
                base,
                base + "/oauth/authorize",
                base + "/oauth/authorize",
                base,
                base + "/v1/oauth/token",
                base + "/api/oauth/claude_cli/create_api_key",
                base + "/api/oauth/claude_cli/roles",
                base + "/oauth/code/success?app=claude-code",
                base + "/oauth/code/success?app=claude-code",
                base + "/oauth/code/callback",
                config.clientId(),
                "-custom-oauth",
                config.mcpProxyUrl(),
                config.mcpProxyPath()
            );
        }

        // Allow CLIENT_ID override
        String clientIdOverride = System.getenv("CLAUDE_CODE_OAUTH_CLIENT_ID");
        if (clientIdOverride != null) {
            config = new OAuthConfigInstance(
                config.baseApiUrl(),
                config.consoleAuthorizeUrl(),
                config.claudeAiAuthorizeUrl(),
                config.claudeAiOrigin(),
                config.tokenUrl(),
                config.apiKeyUrl(),
                config.rolesUrl(),
                config.consoleSuccessUrl(),
                config.claudeaiSuccessUrl(),
                config.manualRedirectUrl(),
                clientIdOverride,
                config.oauthFileSuffix(),
                config.mcpProxyUrl(),
                config.mcpProxyPath()
            );
        }

        return config;
    }

    private static OAuthConfigInstance getStagingOauthConfig() {
        String userType = System.getenv("USER_TYPE");
        if (!"ant".equals(userType)) {
            return PROD_CONFIG;
        }
        return new OAuthConfigInstance(
            "https://api-staging.anthropic.com",
            "https://platform.staging.ant.dev/oauth/authorize",
            "https://claude-ai.staging.ant.dev/oauth/authorize",
            "https://claude-ai.staging.ant.dev",
            "https://platform.staging.ant.dev/v1/oauth/token",
            "https://api-staging.anthropic.com/api/oauth/claude_cli/create_api_key",
            "https://api-staging.anthropic.com/api/oauth/claude_cli/roles",
            "https://platform.staging.ant.dev/buy_credits?returnUrl=/oauth/code/success%3Fapp%3Dclaude-code",
            "https://platform.staging.ant.dev/oauth/code/success?app=claude-code",
            "https://platform.staging.ant.dev/oauth/code/callback",
            "22422756-60c9-4084-8eb7-27705fd5cf9a",
            "-staging-oauth",
            "https://mcp-proxy-staging.anthropic.com",
            "/v1/mcp/{server_id}"
        );
    }

    private static OAuthConfigInstance getLocalOauthConfig() {
        String api = System.getenv("CLAUDE_LOCAL_OAUTH_API_BASE");
        if (api == null) api = "http://localhost:8000";
        api = api.replaceAll("/$", "");

        String apps = System.getenv("CLAUDE_LOCAL_OAUTH_APPS_BASE");
        if (apps == null) apps = "http://localhost:4000";
        apps = apps.replaceAll("/$", "");

        String consoleBase = System.getenv("CLAUDE_LOCAL_OAUTH_CONSOLE_BASE");
        if (consoleBase == null) consoleBase = "http://localhost:3000";
        consoleBase = consoleBase.replaceAll("/$", "");

        return new OAuthConfigInstance(
            api,
            consoleBase + "/oauth/authorize",
            apps + "/oauth/authorize",
            apps,
            api + "/v1/oauth/token",
            api + "/api/oauth/claude_cli/create_api_key",
            api + "/api/oauth/claude_cli/roles",
            consoleBase + "/buy_credits?returnUrl=/oauth/code/success%3Fapp%3Dclaude-code",
            consoleBase + "/oauth/code/success?app=claude-code",
            consoleBase + "/oauth/code/callback",
            "22422756-60c9-4084-8eb7-27705fd5cf9a",
            "-local-oauth",
            "http://localhost:8205",
            "/v1/toolbox/shttp/mcp/{server_id}"
        );
    }

    private static boolean isEnvTruthy(String name) {
        String value = System.getenv(name);
        if (value == null) return false;
        value = value.toLowerCase().trim();
        return "true".equals(value) || "1".equals(value) || "yes".equals(value);
    }
}