/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Auth.
 */
class AuthTest {

    @BeforeEach
    void setUp() {
        Auth.clearAuth();
    }

    @Test
    @DisplayName("Auth initial status unauthenticated")
    void initialStatus() {
        assertEquals(Auth.AuthStatus.UNAUTHENTICATED, Auth.getStatus());
    }

    @Test
    @DisplayName("Auth isAuthenticated false initially")
    void isAuthenticatedInitiallyFalse() {
        assertFalse(Auth.isAuthenticated());
    }

    @Test
    @DisplayName("Auth setAuthToken changes status")
    void setAuthToken() {
        long futureExpiry = System.currentTimeMillis() + 3600000;
        Auth.setAuthToken("test-token", futureExpiry);
        
        assertEquals(Auth.AuthStatus.AUTHENTICATED, Auth.getStatus());
        assertEquals("test-token", Auth.getAuthToken());
    }

    @Test
    @DisplayName("Auth isAuthenticated true after setAuthToken")
    void isAuthenticatedAfterSetToken() {
        long futureExpiry = System.currentTimeMillis() + 3600000;
        Auth.setAuthToken("test-token", futureExpiry);
        assertTrue(Auth.isAuthenticated());
    }

    @Test
    @DisplayName("Auth clearAuth resets state")
    void clearAuth() {
        long futureExpiry = System.currentTimeMillis() + 3600000;
        Auth.setAuthToken("test-token", futureExpiry);
        Auth.clearAuth();
        
        assertEquals(Auth.AuthStatus.UNAUTHENTICATED, Auth.getStatus());
        assertNull(Auth.getAuthToken());
    }

    @Test
    @DisplayName("Auth expired token changes status")
    void expiredToken() {
        long pastExpiry = System.currentTimeMillis() - 1000;
        Auth.setAuthToken("test-token", pastExpiry);
        
        assertFalse(Auth.isAuthenticated());
        assertEquals(Auth.AuthStatus.EXPIRED, Auth.getStatus());
    }

    @Test
    @DisplayName("Auth AuthStatus enum values")
    void authStatusEnum() {
        Auth.AuthStatus[] values = Auth.AuthStatus.values();
        assertEquals(5, values.length);
    }

    @Test
    @DisplayName("Auth AuthInfo isExpired")
    void authInfoIsExpired() {
        Auth.AuthInfo info = new Auth.AuthInfo(
            "user1", "user@example.com", "org1",
            System.currentTimeMillis() - 10000,
            System.currentTimeMillis() - 1000
        );
        assertTrue(info.isExpired());
    }

    @Test
    @DisplayName("Auth AuthInfo getRemainingTime")
    void authInfoGetRemainingTime() {
        long futureExpiry = System.currentTimeMillis() + 10000;
        Auth.AuthInfo info = new Auth.AuthInfo(
            "user1", "user@example.com", "org1",
            System.currentTimeMillis(),
            futureExpiry
        );
        assertTrue(info.getRemainingTime() > 0);
    }

    @Test
    @DisplayName("Auth OAuthConfig record")
    void oAuthConfig() {
        Auth.OAuthConfig config = new Auth.OAuthConfig(
            "clientId", "redirectUri", "scope", "authorizeUrl", "tokenUrl"
        );
        assertEquals("clientId", config.clientId());
        assertEquals("redirectUri", config.redirectUri());
        assertEquals("scope", config.scope());
    }

    @Test
    @DisplayName("Auth AuthInfo record fields")
    void authInfoFields() {
        Auth.AuthInfo info = new Auth.AuthInfo(
            "user1", "user@example.com", "org1", 1000L, 2000L
        );
        assertEquals("user1", info.userId());
        assertEquals("user@example.com", info.email());
        assertEquals("org1", info.organizationId());
        assertEquals(1000L, info.authenticatedAt());
        assertEquals(2000L, info.expiresAt());
    }
}
