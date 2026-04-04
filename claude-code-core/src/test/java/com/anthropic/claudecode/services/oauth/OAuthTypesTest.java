/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.oauth;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for OAuth types.
 */
@DisplayName("OAuth Types Tests")
class OAuthTypesTest {

    @Test
    @DisplayName("OAuthConfig instance can be retrieved")
    void oauthConfigCanBeRetrieved() {
        OAuthConfig config = OAuthConfig.getInstance();

        assertNotNull(config);
        assertNotNull(config.getClientId());
        assertNotNull(config.getTokenUrl());
        assertNotNull(config.getAllOauthScopes());
        assertFalse(config.getAllOauthScopes().isEmpty());
    }

    @Test
    @DisplayName("OAuthConfig shouldUseClaudeAiAuth works correctly")
    void oauthConfigShouldUseClaudeAiAuth() {
        OAuthConfig config = OAuthConfig.getInstance();

        assertTrue(config.shouldUseClaudeAiAuth(List.of("https://claude.ai/inference")));
        assertFalse(config.shouldUseClaudeAiAuth(List.of("other-scope")));
        assertFalse(config.shouldUseClaudeAiAuth(null));
    }

    @Test
    @DisplayName("OAuthConfig hasProfileScope works correctly")
    void oauthConfigHasProfileScope() {
        OAuthConfig config = OAuthConfig.getInstance();

        assertTrue(config.hasProfileScope(List.of("user:profile")));
        assertFalse(config.hasProfileScope(List.of("other-scope")));
        assertFalse(config.hasProfileScope(null));
    }

    @Test
    @DisplayName("OAuthTokens record works correctly")
    void oauthTokensRecordWorksCorrectly() {
        OAuthTypes.OAuthTokens tokens = new OAuthTypes.OAuthTokens(
            "access-token",
            "refresh-token",
            System.currentTimeMillis() + 3600000,
            List.of("read", "write"),
            OAuthTypes.SubscriptionType.PRO,
            OAuthTypes.RateLimitTier.TIER_2,
            null,
            null
        );

        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        assertTrue(tokens.expiresAt() > System.currentTimeMillis());
        assertEquals(2, tokens.scopes().size());
        assertEquals(OAuthTypes.SubscriptionType.PRO, tokens.subscriptionType());
    }

    @Test
    @DisplayName("SubscriptionType enum has correct values")
    void subscriptionTypeHasCorrectValues() {
        OAuthTypes.SubscriptionType[] types = OAuthTypes.SubscriptionType.values();

        assertEquals(5, types.length);
        assertTrue(Arrays.asList(types).contains(OAuthTypes.SubscriptionType.FREE));
        assertTrue(Arrays.asList(types).contains(OAuthTypes.SubscriptionType.PRO));
        assertTrue(Arrays.asList(types).contains(OAuthTypes.SubscriptionType.MAX));
        assertTrue(Arrays.asList(types).contains(OAuthTypes.SubscriptionType.TEAM));
        assertTrue(Arrays.asList(types).contains(OAuthTypes.SubscriptionType.ENTERPRISE));
    }

    @Test
    @DisplayName("RateLimitTier enum has correct values")
    void rateLimitTierHasCorrectValues() {
        OAuthTypes.RateLimitTier[] tiers = OAuthTypes.RateLimitTier.values();

        assertEquals(6, tiers.length);
        assertTrue(Arrays.asList(tiers).contains(OAuthTypes.RateLimitTier.TIER_1));
        assertTrue(Arrays.asList(tiers).contains(OAuthTypes.RateLimitTier.UNLIMITED));
    }

    @Test
    @DisplayName("PKCE generates valid code verifier")
    void pkceGeneratesValidCodeVerifier() {
        String verifier = PKCE.generateCodeVerifier();

        assertNotNull(verifier);
        assertTrue(verifier.length() >= 43);
        assertTrue(verifier.length() <= 128);
    }

    @Test
    @DisplayName("PKCE generates valid code challenge")
    void pkceGeneratesValidCodeChallenge() {
        String verifier = PKCE.generateCodeVerifier();
        String challenge = PKCE.generateCodeChallenge(verifier);

        assertNotNull(challenge);
        assertTrue(challenge.length() > 0);
    }

    @Test
    @DisplayName("PKCE generates different verifiers each time")
    void pkceGeneratesDifferentVerifiers() {
        String verifier1 = PKCE.generateCodeVerifier();
        String verifier2 = PKCE.generateCodeVerifier();

        assertNotEquals(verifier1, verifier2);
    }

    @Test
    @DisplayName("OAuthTypes isOAuthTokenExpired works correctly")
    void isOAuthTokenExpiredWorksCorrectly() {
        // Token that expires in 10 minutes - not expired
        assertFalse(OAuthTypes.isOAuthTokenExpired(System.currentTimeMillis() + 600000));

        // Token that expires in 3 minutes - expired (with 5 min buffer)
        assertTrue(OAuthTypes.isOAuthTokenExpired(System.currentTimeMillis() + 180000));

        // Null token - not expired
        assertFalse(OAuthTypes.isOAuthTokenExpired(null));
    }

    @Test
    @DisplayName("OAuthTypes parseScopes works correctly")
    void parseScopesWorksCorrectly() {
        List<String> scopes = OAuthTypes.parseScopes("read write profile");

        assertEquals(3, scopes.size());
        assertTrue(scopes.contains("read"));
        assertTrue(scopes.contains("write"));
        assertTrue(scopes.contains("profile"));

        // Empty string
        assertTrue(OAuthTypes.parseScopes("").isEmpty());

        // Null
        assertTrue(OAuthTypes.parseScopes(null).isEmpty());
    }
}
