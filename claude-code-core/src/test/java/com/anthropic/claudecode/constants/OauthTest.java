/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Oauth constants.
 */
class OauthTest {

    @Test
    @DisplayName("Oauth CLAUDE_AI_INFERENCE_SCOPE")
    void claudeAiInferenceScope() {
        assertEquals("user:inference", Oauth.CLAUDE_AI_INFERENCE_SCOPE);
    }

    @Test
    @DisplayName("Oauth CLAUDE_AI_PROFILE_SCOPE")
    void claudeAiProfileScope() {
        assertEquals("user:profile", Oauth.CLAUDE_AI_PROFILE_SCOPE);
    }

    @Test
    @DisplayName("Oauth OAUTH_BETA_HEADER")
    void oauthBetaHeader() {
        assertEquals("oauth-2025-04-20", Oauth.OAUTH_BETA_HEADER);
    }

    @Test
    @DisplayName("Oauth CONSOLE_OAUTH_SCOPES not empty")
    void consoleOauthScopesNotEmpty() {
        assertEquals(2, Oauth.CONSOLE_OAUTH_SCOPES.size());
    }

    @Test
    @DisplayName("Oauth CONSOLE_OAUTH_SCOPES contains profile scope")
    void consoleOauthScopesContainsProfile() {
        assertTrue(Oauth.CONSOLE_OAUTH_SCOPES.contains(Oauth.CLAUDE_AI_PROFILE_SCOPE));
    }

    @Test
    @DisplayName("Oauth CLAUDE_AI_OAUTH_SCOPES not empty")
    void claudeAiOauthScopesNotEmpty() {
        assertEquals(5, Oauth.CLAUDE_AI_OAUTH_SCOPES.size());
    }

    @Test
    @DisplayName("Oauth CLAUDE_AI_OAUTH_SCOPES contains expected scopes")
    void claudeAiOauthScopesContainsExpected() {
        assertTrue(Oauth.CLAUDE_AI_OAUTH_SCOPES.contains(Oauth.CLAUDE_AI_PROFILE_SCOPE));
        assertTrue(Oauth.CLAUDE_AI_OAUTH_SCOPES.contains(Oauth.CLAUDE_AI_INFERENCE_SCOPE));
    }

    @Test
    @DisplayName("Oauth ALL_OAUTH_SCOPES not empty")
    void allOauthScopesNotEmpty() {
        assertEquals(6, Oauth.ALL_OAUTH_SCOPES.size());
    }

    @Test
    @DisplayName("Oauth ALL_OAUTH_SCOPES contains all expected")
    void allOauthScopesContainsAll() {
        assertTrue(Oauth.ALL_OAUTH_SCOPES.contains("user:inference"));
        assertTrue(Oauth.ALL_OAUTH_SCOPES.contains("user:profile"));
        assertTrue(Oauth.ALL_OAUTH_SCOPES.contains("user:sessions:claude_code"));
        assertTrue(Oauth.ALL_OAUTH_SCOPES.contains("user:mcp_servers"));
        assertTrue(Oauth.ALL_OAUTH_SCOPES.contains("user:file_upload"));
    }

    @Test
    @DisplayName("Oauth MCP_CLIENT_METADATA_URL")
    void mcpClientMetadataUrl() {
        assertEquals("https://claude.ai/oauth/claude-code-client-metadata", Oauth.MCP_CLIENT_METADATA_URL);
    }

    @Test
    @DisplayName("Oauth PROD_BASE_API_URL")
    void prodBaseApiUrl() {
        assertEquals("https://api.anthropic.com", Oauth.PROD_BASE_API_URL);
    }

    @Test
    @DisplayName("Oauth PROD_CONSOLE_AUTHORIZE_URL")
    void prodConsoleAuthorizeUrl() {
        assertEquals("https://platform.claude.com/oauth/authorize", Oauth.PROD_CONSOLE_AUTHORIZE_URL);
    }

    @Test
    @DisplayName("Oauth PROD_CLAUDE_AI_AUTHORIZE_URL")
    void prodClaudeAiAuthorizeUrl() {
        assertEquals("https://claude.com/cai/oauth/authorize", Oauth.PROD_CLAUDE_AI_AUTHORIZE_URL);
    }

    @Test
    @DisplayName("Oauth PROD_CLAUDE_AI_ORIGIN")
    void prodClaudeAiOrigin() {
        assertEquals("https://claude.ai", Oauth.PROD_CLAUDE_AI_ORIGIN);
    }

    @Test
    @DisplayName("Oauth PROD_TOKEN_URL")
    void prodTokenUrl() {
        assertEquals("https://platform.claude.com/v1/oauth/token", Oauth.PROD_TOKEN_URL);
    }

    @Test
    @DisplayName("Oauth PROD_API_KEY_URL")
    void prodApiKeyUrl() {
        assertEquals("https://api.anthropic.com/api/oauth/claude_cli/create_api_key", Oauth.PROD_API_KEY_URL);
    }

    @Test
    @DisplayName("Oauth PROD_ROLES_URL")
    void prodRolesUrl() {
        assertEquals("https://api.anthropic.com/api/oauth/claude_cli/roles", Oauth.PROD_ROLES_URL);
    }

    @Test
    @DisplayName("Oauth PROD_CLIENT_ID")
    void prodClientId() {
        assertEquals("9d1c250a-e61b-44d9-88ed-5944d1962f5e", Oauth.PROD_CLIENT_ID);
    }

    @Test
    @DisplayName("Oauth PROD_MCP_PROXY_URL")
    void prodMcpProxyUrl() {
        assertEquals("https://mcp-proxy.anthropic.com", Oauth.PROD_MCP_PROXY_URL);
    }

    @Test
    @DisplayName("Oauth PROD_MCP_PROXY_PATH")
    void prodMcpProxyPath() {
        assertEquals("/v1/mcp/{server_id}", Oauth.PROD_MCP_PROXY_PATH);
    }

    @Test
    @DisplayName("Oauth ALLOWED_OAUTH_BASE_URLS not empty")
    void allowedOauthBaseUrlsNotEmpty() {
        assertEquals(3, Oauth.ALLOWED_OAUTH_BASE_URLS.size());
    }

    @Test
    @DisplayName("Oauth ALLOWED_OAUTH_BASE_URLS contains expected URLs")
    void allowedOauthBaseUrlsContainsExpected() {
        assertTrue(Oauth.ALLOWED_OAUTH_BASE_URLS.contains("https://claude.fedstart.com"));
        assertTrue(Oauth.ALLOWED_OAUTH_BASE_URLS.contains("https://claude-staging.fedstart.com"));
    }

    @Test
    @DisplayName("Oauth lists are immutable")
    void listsAreImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> {
            Oauth.CONSOLE_OAUTH_SCOPES.add("test");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            Oauth.CLAUDE_AI_OAUTH_SCOPES.add("test");
        });
    }

    @Test
    @DisplayName("Oauth sets are immutable")
    void setsAreImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> {
            Oauth.ALL_OAUTH_SCOPES.add("test");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            Oauth.ALLOWED_OAUTH_BASE_URLS.add("test");
        });
    }
}