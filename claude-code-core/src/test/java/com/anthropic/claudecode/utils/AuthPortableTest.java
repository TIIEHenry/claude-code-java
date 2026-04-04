/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AuthPortable.
 */
class AuthPortableTest {

    @Test
    @DisplayName("AuthPortable normalizeApiKeyForConfig null")
    void normalizeApiKeyForConfigNull() {
        assertNull(AuthPortable.normalizeApiKeyForConfig(null));
    }

    @Test
    @DisplayName("AuthPortable normalizeApiKeyForConfig short key")
    void normalizeApiKeyForConfigShort() {
        assertEquals("short-key", AuthPortable.normalizeApiKeyForConfig("short-key"));
    }

    @Test
    @DisplayName("AuthPortable normalizeApiKeyForConfig exactly 20 chars")
    void normalizeApiKeyForConfigExactly20() {
        String key = "12345678901234567890";
        assertEquals(key, AuthPortable.normalizeApiKeyForConfig(key));
    }

    @Test
    @DisplayName("AuthPortable normalizeApiKeyForConfig long key")
    void normalizeApiKeyForConfigLong() {
        String key = "sk-ant-api03-1234567890123456789012345678901234567890";
        String normalized = AuthPortable.normalizeApiKeyForConfig(key);
        assertEquals(20, normalized.length());
        // Last 20 chars of the key
        assertEquals("12345678901234567890", normalized);
    }

    @Test
    @DisplayName("AuthPortable normalizeApiKeyForConfig 21 chars")
    void normalizeApiKeyForConfig21Chars() {
        String key = "123456789012345678901";
        String normalized = AuthPortable.normalizeApiKeyForConfig(key);
        assertEquals("23456789012345678901", normalized);
    }

    @Test
    @DisplayName("AuthPortable maybeRemoveApiKeyFromMacOSKeychain does not throw")
    void maybeRemoveApiKeyFromMacOSKeychain() {
        assertDoesNotThrow(() -> AuthPortable.maybeRemoveApiKeyFromMacOSKeychain());
    }
}
