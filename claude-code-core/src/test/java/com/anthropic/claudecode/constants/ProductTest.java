/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Product constants.
 */
class ProductTest {

    @Test
    @DisplayName("Product PRODUCT_URL")
    void productUrl() {
        assertEquals("https://claude.com/claude-code", Product.PRODUCT_URL);
    }

    @Test
    @DisplayName("Product CLAUDE_AI_BASE_URL")
    void claudeAiBaseUrl() {
        assertEquals("https://claude.ai", Product.CLAUDE_AI_BASE_URL);
    }

    @Test
    @DisplayName("Product CLAUDE_AI_STAGING_BASE_URL")
    void claudeAiStagingBaseUrl() {
        assertEquals("https://claude-ai.staging.ant.dev", Product.CLAUDE_AI_STAGING_BASE_URL);
    }

    @Test
    @DisplayName("Product CLAUDE_AI_LOCAL_BASE_URL")
    void claudeAiLocalBaseUrl() {
        assertEquals("http://localhost:4000", Product.CLAUDE_AI_LOCAL_BASE_URL);
    }

    @Test
    @DisplayName("Product isRemoteSessionStaging returns true for staging session")
    void isRemoteSessionStagingSession() {
        assertTrue(Product.isRemoteSessionStaging("abc_staging_123", null));
    }

    @Test
    @DisplayName("Product isRemoteSessionStaging returns true for staging ingress")
    void isRemoteSessionStagingIngress() {
        assertTrue(Product.isRemoteSessionStaging(null, "https://staging.example.com"));
    }

    @Test
    @DisplayName("Product isRemoteSessionStaging returns false for prod")
    void isRemoteSessionStagingProd() {
        assertFalse(Product.isRemoteSessionStaging("session-123", "https://claude.ai"));
    }

    @Test
    @DisplayName("Product isRemoteSessionStaging returns false for null")
    void isRemoteSessionStagingNull() {
        assertFalse(Product.isRemoteSessionStaging(null, null));
    }

    @Test
    @DisplayName("Product isRemoteSessionLocal returns true for local session")
    void isRemoteSessionLocalSession() {
        assertTrue(Product.isRemoteSessionLocal("abc_local_123", null));
    }

    @Test
    @DisplayName("Product isRemoteSessionLocal returns true for localhost ingress")
    void isRemoteSessionLocalIngress() {
        assertTrue(Product.isRemoteSessionLocal(null, "http://localhost:3000"));
    }

    @Test
    @DisplayName("Product isRemoteSessionLocal returns false for prod")
    void isRemoteSessionLocalProd() {
        assertFalse(Product.isRemoteSessionLocal("session-123", "https://claude.ai"));
    }

    @Test
    @DisplayName("Product isRemoteSessionLocal returns false for null")
    void isRemoteSessionLocalNull() {
        assertFalse(Product.isRemoteSessionLocal(null, null));
    }

    @Test
    @DisplayName("Product getClaudeAiBaseUrl returns local for local session")
    void getClaudeAiBaseUrlLocal() {
        String url = Product.getClaudeAiBaseUrl("abc_local_123", null);
        assertEquals(Product.CLAUDE_AI_LOCAL_BASE_URL, url);
    }

    @Test
    @DisplayName("Product getClaudeAiBaseUrl returns staging for staging session")
    void getClaudeAiBaseUrlStaging() {
        String url = Product.getClaudeAiBaseUrl("abc_staging_123", null);
        assertEquals(Product.CLAUDE_AI_STAGING_BASE_URL, url);
    }

    @Test
    @DisplayName("Product getClaudeAiBaseUrl returns prod for normal session")
    void getClaudeAiBaseUrlProd() {
        String url = Product.getClaudeAiBaseUrl("session-123", "https://claude.ai");
        assertEquals(Product.CLAUDE_AI_BASE_URL, url);
    }

    @Test
    @DisplayName("Product getClaudeAiBaseUrl returns prod for null")
    void getClaudeAiBaseUrlNull() {
        String url = Product.getClaudeAiBaseUrl(null, null);
        assertEquals(Product.CLAUDE_AI_BASE_URL, url);
    }

    @Test
    @DisplayName("Product getRemoteSessionUrl formats correctly")
    void getRemoteSessionUrl() {
        String url = Product.getRemoteSessionUrl("session-123", null);
        assertEquals("https://claude.ai/code/session-123", url);
    }

    @Test
    @DisplayName("Product getRemoteSessionUrl with staging")
    void getRemoteSessionUrlStaging() {
        String url = Product.getRemoteSessionUrl("abc_staging_456", null);
        assertTrue(url.contains("staging"));
        assertTrue(url.contains("abc_staging_456"));
    }

    @Test
    @DisplayName("Product getRemoteSessionUrl converts cse_ to session_")
    void getRemoteSessionUrlCseConversion() {
        String url = Product.getRemoteSessionUrl("cse_abc123", null);
        assertEquals("https://claude.ai/code/session_abc123", url);
    }

    @Test
    @DisplayName("Product getRemoteSessionUrl with local")
    void getRemoteSessionUrlLocal() {
        String url = Product.getRemoteSessionUrl("abc_local_123", null);
        assertTrue(url.contains("localhost"));
        assertTrue(url.contains("abc_local_123"));
    }
}