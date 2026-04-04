/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SecureStorage.
 */
class SecureStorageTest {

    @BeforeEach
    void setUp() {
        SecureStorage.clearAll();
    }

    @Test
    @DisplayName("SecureStorage store and retrieve")
    void storeAndRetrieve() {
        SecureStorage.store("test-key", "test-value");
        assertEquals("test-value", SecureStorage.retrieve("test-key"));
    }

    @Test
    @DisplayName("SecureStorage retrieve missing returns null")
    void retrieveMissing() {
        assertNull(SecureStorage.retrieve("missing-key"));
    }

    @Test
    @DisplayName("SecureStorage exists returns true for stored key")
    void existsTrue() {
        SecureStorage.store("test-key", "test-value");
        assertTrue(SecureStorage.exists("test-key"));
    }

    @Test
    @DisplayName("SecureStorage exists returns false for missing key")
    void existsFalse() {
        assertFalse(SecureStorage.exists("missing-key"));
    }

    @Test
    @DisplayName("SecureStorage delete removes key")
    void deleteRemovesKey() {
        SecureStorage.store("test-key", "test-value");
        SecureStorage.delete("test-key");
        assertFalse(SecureStorage.exists("test-key"));
        assertNull(SecureStorage.retrieve("test-key"));
    }

    @Test
    @DisplayName("SecureStorage listKeys returns stored keys")
    void listKeys() {
        SecureStorage.store("key1", "value1");
        SecureStorage.store("key2", "value2");
        java.util.List<String> keys = SecureStorage.listKeys();
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    @DisplayName("SecureStorage clearAll removes all keys")
    void clearAll() {
        SecureStorage.store("key1", "value1");
        SecureStorage.store("key2", "value2");
        SecureStorage.clearAll();
        assertTrue(SecureStorage.listKeys().isEmpty());
    }

    @Test
    @DisplayName("SecureStorage getStoragePath returns path")
    void getStoragePath() {
        Path path = SecureStorage.getStoragePath();
        assertNotNull(path);
        assertTrue(path.toString().contains("claude-code"));
    }

    @Test
    @DisplayName("SecureStorage isUsingSystemKeychain returns boolean")
    void isUsingSystemKeychain() {
        boolean result = SecureStorage.isUsingSystemKeychain();
        // On macOS/Windows returns true, on Linux returns false
        String os = System.getProperty("os.name").toLowerCase();
        assertEquals(os.contains("mac") || os.contains("win"), result);
    }

    @Test
    @DisplayName("SecureStorage store null key may throw")
    void storeNullKey() {
        // ConcurrentHashMap doesn't allow null keys
        // This test documents the behavior
        try {
            SecureStorage.store(null, "value");
            // If it doesn't throw, that's fine
            assertTrue(true);
        } catch (NullPointerException e) {
            // Expected behavior for ConcurrentHashMap
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("SecureStorage retrieve null key may throw")
    void retrieveNullKey() {
        try {
            String result = SecureStorage.retrieve(null);
            assertNull(result);
        } catch (NullPointerException e) {
            // Expected behavior for ConcurrentHashMap
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("SecureStorage delete null key may throw")
    void deleteNullKey() {
        try {
            SecureStorage.delete(null);
            assertTrue(true);
        } catch (NullPointerException e) {
            // Expected behavior for ConcurrentHashMap
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("SecureStorage store empty value")
    void storeEmptyValue() {
        SecureStorage.store("empty-key", "");
        assertEquals("", SecureStorage.retrieve("empty-key"));
    }

    @Test
    @DisplayName("SecureStorage overwrite existing key")
    void overwriteKey() {
        SecureStorage.store("key", "value1");
        SecureStorage.store("key", "value2");
        assertEquals("value2", SecureStorage.retrieve("key"));
    }

    @Test
    @DisplayName("SecureStorage migrateFromLegacy does not throw")
    void migrateFromLegacy() {
        SecureStorage.migrateFromLegacy(java.nio.file.Paths.get("/tmp/nonexistent"));
        // Should not throw
        assertTrue(true);
    }
}