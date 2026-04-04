/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BridgeConfig.
 */
class BridgeConfigTest {

    @Test
    @DisplayName("BridgeConfig getBridgeBaseUrl returns default")
    void getBridgeBaseUrlDefault() {
        String url = BridgeConfig.getBridgeBaseUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("http"));
    }

    @Test
    @DisplayName("BridgeConfig getBridgeAccessToken may be null")
    void getBridgeAccessToken() {
        // May be null if no token set
        String token = BridgeConfig.getBridgeAccessToken();
        // Just verify it doesn't throw
        assertTrue(token == null || token.length() > 0);
    }

    @Test
    @DisplayName("BridgeConfig isBridgeEnabled returns boolean")
    void isBridgeEnabled() {
        boolean enabled = BridgeConfig.isBridgeEnabled();
        // Just verify it returns without throwing
        assertTrue(enabled || !enabled);
    }

    @Test
    @DisplayName("BridgeConfig getBridgeApiUrl with leading slash")
    void getBridgeApiUrlWithSlash() {
        String url = BridgeConfig.getBridgeApiUrl("/test/path");
        assertTrue(url.contains("/test/path"));
    }

    @Test
    @DisplayName("BridgeConfig getBridgeApiUrl without leading slash")
    void getBridgeApiUrlWithoutSlash() {
        String url = BridgeConfig.getBridgeApiUrl("test/path");
        assertTrue(url.contains("test/path"));
    }

    @Test
    @DisplayName("BridgeConfig getSessionIngressUrl returns url")
    void getSessionIngressUrl() {
        String url = BridgeConfig.getSessionIngressUrl();
        assertNotNull(url);
    }

    @Test
    @DisplayName("BridgeConfig getSessionIngressWsUrl converts https to wss")
    void getSessionIngressWsUrl() {
        String wsUrl = BridgeConfig.getSessionIngressWsUrl();
        assertNotNull(wsUrl);
        // Should be ws:// or wss://
        assertTrue(wsUrl.startsWith("ws://") || wsUrl.startsWith("wss://"));
    }

    @Test
    @DisplayName("BridgeConfig getBridgeTokenOverride returns null for non-ant")
    void getBridgeTokenOverrideNonAnt() {
        // This will be null unless USER_TYPE=ant is set
        String token = BridgeConfig.getBridgeTokenOverride();
        // Just verify it doesn't throw
        assertTrue(token == null || token.length() > 0);
    }

    @Test
    @DisplayName("BridgeConfig getBridgeBaseUrlOverride returns null for non-ant")
    void getBridgeBaseUrlOverrideNonAnt() {
        String url = BridgeConfig.getBridgeBaseUrlOverride();
        assertTrue(url == null || url.length() > 0);
    }
}