/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for McpAuth.
 */
class McpAuthTest {

    private McpAuth mcpAuth;

    @BeforeEach
    void setUp() {
        mcpAuth = new McpAuth();
    }

    @Test
    @DisplayName("McpAuth AuthStatus enum values")
    void authStatusEnum() {
        McpAuth.AuthStatus[] statuses = McpAuth.AuthStatus.values();
        assertEquals(5, statuses.length);
        assertEquals(McpAuth.AuthStatus.UNAUTHENTICATED, McpAuth.AuthStatus.valueOf("UNAUTHENTICATED"));
        assertEquals(McpAuth.AuthStatus.PENDING, McpAuth.AuthStatus.valueOf("PENDING"));
        assertEquals(McpAuth.AuthStatus.AUTHENTICATED, McpAuth.AuthStatus.valueOf("AUTHENTICATED"));
        assertEquals(McpAuth.AuthStatus.EXPIRED, McpAuth.AuthStatus.valueOf("EXPIRED"));
        assertEquals(McpAuth.AuthStatus.ERROR, McpAuth.AuthStatus.valueOf("ERROR"));
    }

    @Test
    @DisplayName("McpAuth AuthState record")
    void authStateRecord() {
        Instant future = Instant.now().plusSeconds(3600);
        McpAuth.AuthState state = new McpAuth.AuthState(
            "my-server",
            McpAuth.AuthStatus.AUTHENTICATED,
            "token-123",
            future,
            "refresh-token",
            Map.of("key", "value")
        );

        assertEquals("my-server", state.serverName());
        assertEquals(McpAuth.AuthStatus.AUTHENTICATED, state.status());
        assertEquals("token-123", state.token());
        assertEquals(future, state.expiresAt());
        assertEquals("refresh-token", state.refreshToken());
        assertEquals("value", state.additionalData().get("key"));
    }

    @Test
    @DisplayName("McpAuth AuthState isExpired")
    void authStateIsExpired() {
        // Past expiry
        McpAuth.AuthState expired = new McpAuth.AuthState(
            "server", McpAuth.AuthStatus.AUTHENTICATED, "token",
            Instant.now().minusSeconds(60), null, null
        );
        assertTrue(expired.isExpired());

        // Future expiry
        McpAuth.AuthState valid = new McpAuth.AuthState(
            "server", McpAuth.AuthStatus.AUTHENTICATED, "token",
            Instant.now().plusSeconds(3600), null, null
        );
        assertFalse(valid.isExpired());

        // No expiry
        McpAuth.AuthState noExpiry = new McpAuth.AuthState(
            "server", McpAuth.AuthStatus.AUTHENTICATED, "token",
            null, null, null
        );
        assertFalse(noExpiry.isExpired());
    }

    @Test
    @DisplayName("McpAuth AuthState needsRefresh")
    void authStateNeedsRefresh() {
        // About to expire (within 5 minutes)
        McpAuth.AuthState needsRefresh = new McpAuth.AuthState(
            "server", McpAuth.AuthStatus.AUTHENTICATED, "token",
            Instant.now().plusSeconds(200), null, null
        );
        assertTrue(needsRefresh.needsRefresh());

        // Plenty of time
        McpAuth.AuthState valid = new McpAuth.AuthState(
            "server", McpAuth.AuthStatus.AUTHENTICATED, "token",
            Instant.now().plusSeconds(3600), null, null
        );
        assertFalse(valid.needsRefresh());

        // No expiry
        McpAuth.AuthState noExpiry = new McpAuth.AuthState(
            "server", McpAuth.AuthStatus.AUTHENTICATED, "token",
            null, null, null
        );
        assertFalse(noExpiry.needsRefresh());
    }

    @Test
    @DisplayName("McpAuth AuthConfig oauth factory")
    void authConfigOauth() {
        McpAuth.AuthConfig config = McpAuth.AuthConfig.oauth(
            "my-server", "client-123", "https://auth.example.com", "https://token.example.com"
        );

        assertEquals("my-server", config.serverName());
        assertEquals("oauth", config.authType());
        assertEquals("client-123", config.clientId());
        assertEquals("https://auth.example.com", config.authUrl());
        assertEquals("https://token.example.com", config.tokenUrl());
    }

    @Test
    @DisplayName("McpAuth AuthConfig apiKey factory")
    void authConfigApiKey() {
        McpAuth.AuthConfig config = McpAuth.AuthConfig.apiKey("my-server");

        assertEquals("my-server", config.serverName());
        assertEquals("api_key", config.authType());
    }

    @Test
    @DisplayName("McpAuth isAuthenticated returns false initially")
    void isAuthenticatedInitial() {
        assertFalse(mcpAuth.isAuthenticated("unknown-server"));
    }

    @Test
    @DisplayName("McpAuth setAuthenticated and isAuthenticated")
    void setAuthenticated() {
        mcpAuth.setAuthenticated("my-server", "token-123", Instant.now().plusSeconds(3600), "refresh");

        assertTrue(mcpAuth.isAuthenticated("my-server"));
    }

    @Test
    @DisplayName("McpAuth getAuthState returns state")
    void getAuthState() {
        mcpAuth.setAuthenticated("my-server", "token-123", Instant.now().plusSeconds(3600), "refresh");

        McpAuth.AuthState state = mcpAuth.getAuthState("my-server");
        assertNotNull(state);
        assertEquals("token-123", state.token());
    }

    @Test
    @DisplayName("McpAuth clearAuth removes state")
    void clearAuth() {
        mcpAuth.setAuthenticated("my-server", "token", Instant.now().plusSeconds(3600), null);
        assertTrue(mcpAuth.isAuthenticated("my-server"));

        mcpAuth.clearAuth("my-server");

        assertFalse(mcpAuth.isAuthenticated("my-server"));
    }

    @Test
    @DisplayName("McpAuth getToken returns token")
    void getToken() {
        mcpAuth.setAuthenticated("my-server", "token-123", Instant.now().plusSeconds(3600), null);

        Optional<String> token = mcpAuth.getToken("my-server");

        assertTrue(token.isPresent());
        assertEquals("token-123", token.get());
    }

    @Test
    @DisplayName("McpAuth getToken empty for missing server")
    void getTokenMissing() {
        Optional<String> token = mcpAuth.getToken("unknown");

        assertFalse(token.isPresent());
    }

    @Test
    @DisplayName("McpAuth AuthResult success factory")
    void authResultSuccess() {
        McpAuth.AuthResult result = McpAuth.AuthResult.success("token-123");

        assertTrue(result.success());
        assertEquals("token-123", result.token());
        assertNull(result.error());
        assertEquals(McpAuth.AuthStatus.AUTHENTICATED, result.status());
    }

    @Test
    @DisplayName("McpAuth AuthResult failure factory")
    void authResultFailure() {
        McpAuth.AuthResult result = McpAuth.AuthResult.failure("Auth failed");

        assertFalse(result.success());
        assertNull(result.token());
        assertEquals("Auth failed", result.error());
        assertEquals(McpAuth.AuthStatus.ERROR, result.status());
    }

    @Test
    @DisplayName("McpAuth AuthResult pending factory")
    void authResultPending() {
        McpAuth.AuthResult result = McpAuth.AuthResult.pending();

        assertFalse(result.success());
        assertNull(result.token());
        assertNull(result.error());
        assertEquals(McpAuth.AuthStatus.PENDING, result.status());
    }

    @Test
    @DisplayName("McpAuth AuthHeader bearer factory")
    void authHeaderBearer() {
        McpAuth.AuthHeader header = McpAuth.AuthHeader.bearer("my-token");

        assertEquals("Authorization", header.name());
        assertEquals("Bearer my-token", header.value());
    }

    @Test
    @DisplayName("McpAuth AuthHeader apiKey factory")
    void authHeaderApiKey() {
        McpAuth.AuthHeader header = McpAuth.AuthHeader.apiKey("my-key");

        assertEquals("X-API-Key", header.name());
        assertEquals("my-key", header.value());
    }

    @Test
    @DisplayName("McpAuth getAuthHeader returns header")
    void getAuthHeader() {
        mcpAuth.setAuthenticated("my-server", "token-123", Instant.now().plusSeconds(3600), null);

        Optional<McpAuth.AuthHeader> header = mcpAuth.getAuthHeader("my-server");

        assertTrue(header.isPresent());
        assertEquals("Authorization", header.get().name());
        assertEquals("Bearer token-123", header.get().value());
    }

    @Test
    @DisplayName("McpAuth startAuthFlow returns pending")
    void startAuthFlow() throws Exception {
        McpAuth.AuthConfig config = McpAuth.AuthConfig.apiKey("test-server");

        CompletableFuture<McpAuth.AuthResult> future = mcpAuth.startAuthFlow(config);
        McpAuth.AuthResult result = future.get();

        assertEquals(McpAuth.AuthStatus.PENDING, result.status());
    }

    @Test
    @DisplayName("McpAuth refreshTokenIfNeeded returns true")
    void refreshTokenIfNeeded() throws Exception {
        mcpAuth.setAuthenticated("my-server", "token", Instant.now().plusSeconds(3600), "refresh");

        CompletableFuture<Boolean> future = mcpAuth.refreshTokenIfNeeded("my-server");
        Boolean result = future.get();

        assertTrue(result);
    }
}