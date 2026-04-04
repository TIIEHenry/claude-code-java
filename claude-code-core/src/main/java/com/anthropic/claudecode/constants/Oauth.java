/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/oauth.ts
 */
package com.anthropic.claudecode.constants;

import java.util.List;
import java.util.Set;

/**
 * OAuth configuration constants.
 */
public final class Oauth {
    private Oauth() {}

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
    public static final Set<String> ALL_OAUTH_SCOPES = Set.of(
        CONSOLE_SCOPE,
        CLAUDE_AI_PROFILE_SCOPE,
        CLAUDE_AI_INFERENCE_SCOPE,
        "user:sessions:claude_code",
        "user:mcp_servers",
        "user:file_upload"
    );

    // MCP client metadata URL
    public static final String MCP_CLIENT_METADATA_URL =
        "https://claude.ai/oauth/claude-code-client-metadata";

    // Production OAuth config
    public static final String PROD_BASE_API_URL = "https://api.anthropic.com";
    public static final String PROD_CONSOLE_AUTHORIZE_URL = "https://platform.claude.com/oauth/authorize";
    public static final String PROD_CLAUDE_AI_AUTHORIZE_URL = "https://claude.com/cai/oauth/authorize";
    public static final String PROD_CLAUDE_AI_ORIGIN = "https://claude.ai";
    public static final String PROD_TOKEN_URL = "https://platform.claude.com/v1/oauth/token";
    public static final String PROD_API_KEY_URL = "https://api.anthropic.com/api/oauth/claude_cli/create_api_key";
    public static final String PROD_ROLES_URL = "https://api.anthropic.com/api/oauth/claude_cli/roles";
    public static final String PROD_CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e";
    public static final String PROD_MCP_PROXY_URL = "https://mcp-proxy.anthropic.com";
    public static final String PROD_MCP_PROXY_PATH = "/v1/mcp/{server_id}";

    // Allowed custom OAuth base URLs (FedStart/PubSec only)
    public static final Set<String> ALLOWED_OAUTH_BASE_URLS = Set.of(
        "https://beacon.claude-ai.staging.ant.dev",
        "https://claude.fedstart.com",
        "https://claude-staging.fedstart.com"
    );
}