/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HashUtils.
 */
class HashUtilsTest {

    @Test
    @DisplayName("HashUtils djb2Hash returns consistent hash")
    void djb2Hash() {
        int hash1 = HashUtils.djb2Hash("hello");
        int hash2 = HashUtils.djb2Hash("hello");

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("HashUtils djb2Hash returns different for different strings")
    void djb2HashDifferent() {
        int hash1 = HashUtils.djb2Hash("hello");
        int hash2 = HashUtils.djb2Hash("world");

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("HashUtils hashContent returns SHA-256 hash")
    void hashContent() {
        String hash = HashUtils.hashContent("test content");

        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex chars
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("HashUtils hashContent is consistent")
    void hashContentConsistent() {
        String hash1 = HashUtils.hashContent("test");
        String hash2 = HashUtils.hashContent("test");

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("HashUtils hashContent differs for different input")
    void hashContentDifferent() {
        String hash1 = HashUtils.hashContent("test1");
        String hash2 = HashUtils.hashContent("test2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("HashUtils hashBytes works")
    void hashBytes() {
        byte[] bytes = "test bytes".getBytes();
        String hash = HashUtils.hashBytes(bytes);

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("HashUtils hashPair produces hash")
    void hashPair() {
        String hash = HashUtils.hashPair("hello", "world");

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("HashUtils hashPair differs from individual hashes")
    void hashPairDifferent() {
        String hashPair = HashUtils.hashPair("a", "b");
        String hashConcat = HashUtils.hashContent("ab");

        assertNotEquals(hashPair, hashConcat);
    }

    @Test
    @DisplayName("HashUtils hashStrings produces hash")
    void hashStrings() {
        String hash = HashUtils.hashStrings("a", "b", "c");

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("HashUtils hashStrings with nulls")
    void hashStringsWithNulls() {
        String hash = HashUtils.hashStrings("a", null, "b");

        assertNotNull(hash);
    }

    @Test
    @DisplayName("HashUtils md5Hash returns MD5 hash")
    void md5Hash() {
        String hash = HashUtils.md5Hash("test");

        assertNotNull(hash);
        assertEquals(32, hash.length()); // MD5 produces 32 hex chars
    }

    @Test
    @DisplayName("HashUtils md5Hash is consistent")
    void md5HashConsistent() {
        String hash1 = HashUtils.md5Hash("test");
        String hash2 = HashUtils.md5Hash("test");

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("HashUtils quickHash returns hash code")
    void quickHash() {
        int hash = HashUtils.quickHash("test");

        assertEquals("test".hashCode(), hash);
    }

    @Test
    @DisplayName("HashUtils quickHash returns 0 for null")
    void quickHashNull() {
        assertEquals(0, HashUtils.quickHash(null));
    }

    @Test
    @DisplayName("HashUtils combineHashes combines hashes")
    void combineHashes() {
        int combined = HashUtils.combineHashes(123, 456);

        assertEquals(123 * 31 + 456, combined);
    }

    @Test
    @DisplayName("HashUtils hashPath normalizes and hashes")
    void hashPath() {
        String hash1 = HashUtils.hashPath("/home/user/file.txt");
        String hash2 = HashUtils.hashPath("\\home\\user\\file.txt");

        assertEquals(hash1, hash2); // Should be same after normalization
    }

    @Test
    @DisplayName("HashUtils hashPath handles null")
    void hashPathNull() {
        assertEquals("", HashUtils.hashPath(null));
    }
}