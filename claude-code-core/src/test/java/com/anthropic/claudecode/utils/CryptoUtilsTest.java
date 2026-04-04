/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CryptoUtils.
 */
class CryptoUtilsTest {

    @Test
    @DisplayName("CryptoUtils randomUUID generates valid UUID")
    void randomUUID() {
        String uuid = CryptoUtils.randomUUID();

        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("CryptoUtils randomUUIDNoDashes generates 32 chars")
    void randomUUIDNoDashes() {
        String uuid = CryptoUtils.randomUUIDNoDashes();

        assertEquals(32, uuid.length());
        assertFalse(uuid.contains("-"));
    }

    @Test
    @DisplayName("CryptoUtils randomShortId generates 8 chars")
    void randomShortId() {
        String id = CryptoUtils.randomShortId();

        assertEquals(8, id.length());
    }

    @Test
    @DisplayName("CryptoUtils randomAlphanumeric generates correct length")
    void randomAlphanumeric() {
        String s = CryptoUtils.randomAlphanumeric(16);

        assertEquals(16, s.length());
        assertTrue(s.matches("[A-Za-z0-9]+"));
    }

    @Test
    @DisplayName("CryptoUtils randomAlphanumeric generates different values")
    void randomAlphanumericDifferent() {
        String s1 = CryptoUtils.randomAlphanumeric(16);
        String s2 = CryptoUtils.randomAlphanumeric(16);

        assertNotEquals(s1, s2);
    }

    @Test
    @DisplayName("CryptoUtils randomHex generates correct length")
    void randomHex() {
        String hex = CryptoUtils.randomHex(16);

        assertEquals(32, hex.length()); // 16 bytes = 32 hex chars
        assertTrue(hex.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("CryptoUtils sha256 generates hash")
    void sha256() {
        String hash = CryptoUtils.sha256("test");

        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 = 64 hex chars
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("CryptoUtils sha256 is deterministic")
    void sha256Deterministic() {
        String hash1 = CryptoUtils.sha256("test");
        String hash2 = CryptoUtils.sha256("test");

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("CryptoUtils sha256 differs for different input")
    void sha256Different() {
        String hash1 = CryptoUtils.sha256("test1");
        String hash2 = CryptoUtils.sha256("test2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("CryptoUtils md5 generates hash")
    void md5() {
        String hash = CryptoUtils.md5("test");

        assertNotNull(hash);
        assertEquals(32, hash.length()); // MD5 = 32 hex chars
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("CryptoUtils md5 is deterministic")
    void md5Deterministic() {
        String hash1 = CryptoUtils.md5("test");
        String hash2 = CryptoUtils.md5("test");

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("CryptoUtils hashId generates 16 chars")
    void hashId() {
        String id = CryptoUtils.hashId("test");

        assertEquals(16, id.length());
    }

    @Test
    @DisplayName("CryptoUtils hashId is deterministic")
    void hashIdDeterministic() {
        String id1 = CryptoUtils.hashId("test");
        String id2 = CryptoUtils.hashId("test");

        assertEquals(id1, id2);
    }
}