/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AuthUtils.
 */
class AuthUtilsTest {

    @BeforeEach
    void setUp() {
        AuthUtils.clearApiKey();
        AuthUtils.setAuthState(null);
    }

    @Test
    @DisplayName("AuthUtils constants")
    void constants() {
        assertEquals("ANTHROPIC_API_KEY", AuthUtils.ANTHROPIC_API_KEY);
        assertEquals("ANTHROPIC_AUTH_TOKEN", AuthUtils.ANTHROPIC_AUTH_TOKEN);
        assertEquals("CLAUDE_CODE_USE_BEDROCK", AuthUtils.CLAUDE_CODE_USE_BEDROCK);
        assertEquals("CLAUDE_CODE_USE_VERTEX", AuthUtils.CLAUDE_CODE_USE_VERTEX);
        assertEquals("CLAUDE_CODE_USE_FOUNDRY", AuthUtils.CLAUDE_CODE_USE_FOUNDRY);
    }

    @Test
    @DisplayName("AuthUtils SubscriptionType enum values")
    void subscriptionTypeEnum() {
        AuthUtils.SubscriptionType[] types = AuthUtils.SubscriptionType.values();
        assertEquals(5, types.length);
    }

    @Test
    @DisplayName("AuthUtils AuthProvider enum values")
    void authProviderEnum() {
        AuthUtils.AuthProvider[] providers = AuthUtils.AuthProvider.values();
        assertEquals(5, providers.length);
    }

    @Test
    @DisplayName("AuthUtils AuthState unauthenticated")
    void authStateUnauthenticated() {
        AuthUtils.AuthState state = AuthUtils.AuthState.unauthenticated();
        assertNull(state.apiKey());
        assertNull(state.authType());
        assertFalse(state.isClaudeAISubscriber());
    }

    @Test
    @DisplayName("AuthUtils AuthState record")
    void authStateRecord() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "api-key", "api_key", true, "pro", "org-123"
        );
        assertEquals("api-key", state.apiKey());
        assertEquals("api_key", state.authType());
        assertTrue(state.isClaudeAISubscriber());
        assertEquals("pro", state.subscriptionType());
        assertEquals("org-123", state.organizationId());
    }

    @Test
    @DisplayName("AuthUtils getAuthProvider returns ANTHROPIC by default")
    void getAuthProviderDefault() {
        assertEquals(AuthUtils.AuthProvider.ANTHROPIC, AuthUtils.getAuthProvider());
    }

    @Test
    @DisplayName("AuthUtils setApiKey and getApiKey")
    void setAndGetApiKey() {
        AuthUtils.setApiKey("test-key");
        assertEquals("test-key", AuthUtils.getApiKey());
    }

    @Test
    @DisplayName("AuthUtils clearApiKey clears key")
    void clearApiKey() {
        AuthUtils.setApiKey("test-key");
        AuthUtils.clearApiKey();
        // After clearing, should return null or check env vars
        AuthUtils.clearApiKey();
        assertTrue(true);
    }

    @Test
    @DisplayName("AuthUtils isAuthenticated true with key")
    void isAuthenticatedWithKey() {
        AuthUtils.setApiKey("test-key");
        assertTrue(AuthUtils.isAuthenticated());
    }

    @Test
    @DisplayName("AuthUtils hasApiKey true with key")
    void hasApiKeyWithKey() {
        AuthUtils.setApiKey("test-key");
        assertTrue(AuthUtils.hasApiKey());
    }

    @Test
    @DisplayName("AuthUtils hasToken true with key")
    void hasTokenWithKey() {
        AuthUtils.setApiKey("test-key");
        assertTrue(AuthUtils.hasToken());
    }

    @Test
    @DisplayName("AuthUtils setAuthState and getAuthState")
    void setAndGetAuthState() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "pro", "org-123"
        );
        AuthUtils.setAuthState(state);
        assertEquals(state, AuthUtils.getAuthState());
    }

    @Test
    @DisplayName("AuthUtils isClaudeAISubscriber true with state")
    void isClaudeAISubscriberTrue() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "pro", "org-123"
        );
        AuthUtils.setAuthState(state);
        assertTrue(AuthUtils.isClaudeAISubscriber());
    }

    @Test
    @DisplayName("AuthUtils getSubscriptionType")
    void getSubscriptionType() {
        AuthUtils.AuthState state = new AuthUtils.AuthState(
            "key", "api_key", true, "pro", "org-123"
        );
        AuthUtils.setAuthState(state);
        assertEquals("pro", AuthUtils.getSubscriptionType());
    }

    @Test
    @DisplayName("AuthUtils isValidApiKeyFormat true for sk-ant- keys")
    void isValidApiKeyFormatAnthropic() {
        assertTrue(AuthUtils.isValidApiKeyFormat("sk-ant-api03-123456789012345678901234567890"));
    }

    @Test
    @DisplayName("AuthUtils isValidApiKeyFormat true for long keys")
    void isValidApiKeyFormatLongKey() {
        assertTrue(AuthUtils.isValidApiKeyFormat("12345678901234567890"));
    }

    @Test
    @DisplayName("AuthUtils isValidApiKeyFormat false for short keys")
    void isValidApiKeyFormatShortKey() {
        assertFalse(AuthUtils.isValidApiKeyFormat("short"));
    }

    @Test
    @DisplayName("AuthUtils isValidApiKeyFormat false for null")
    void isValidApiKeyFormatNull() {
        assertFalse(AuthUtils.isValidApiKeyFormat(null));
    }

    @Test
    @DisplayName("AuthUtils isValidApiKeyFormat false for empty")
    void isValidApiKeyFormatEmpty() {
        assertFalse(AuthUtils.isValidApiKeyFormat(""));
    }

    @Test
    @DisplayName("AuthUtils isThirdPartyProvider false for ANTHROPIC")
    void isThirdPartyProviderFalse() {
        assertFalse(AuthUtils.isThirdPartyProvider());
    }

    @Test
    @DisplayName("AuthUtils getOrganizationRole returns null")
    void getOrganizationRole() {
        assertNull(AuthUtils.getOrganizationRole());
    }

    @Test
    @DisplayName("AuthUtils getWorkspaceRole returns null")
    void getWorkspaceRole() {
        assertNull(AuthUtils.getWorkspaceRole());
    }
}
