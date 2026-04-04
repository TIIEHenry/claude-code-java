/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Keys constants.
 */
class KeysTest {

    @Test
    @DisplayName("Keys getGrowthBookClientKey returns ant prod for ant user")
    void getGrowthBookClientKeyAntProd() {
        String key = Keys.getGrowthBookClientKey("ant", false);
        assertNotNull(key);
        assertTrue(key.startsWith("sdk-"));
    }

    @Test
    @DisplayName("Keys getGrowthBookClientKey returns ant dev for ant user with dev enabled")
    void getGrowthBookClientKeyAntDev() {
        String key = Keys.getGrowthBookClientKey("ant", true);
        assertNotNull(key);
        assertTrue(key.startsWith("sdk-"));
    }

    @Test
    @DisplayName("Keys getGrowthBookClientKey returns external for non-ant user")
    void getGrowthBookClientKeyExternal() {
        String key = Keys.getGrowthBookClientKey("external", false);
        assertNotNull(key);
        assertTrue(key.startsWith("sdk-"));
    }

    @Test
    @DisplayName("Keys getGrowthBookClientKey returns external for null user")
    void getGrowthBookClientKeyNullUser() {
        String key = Keys.getGrowthBookClientKey(null, false);
        assertNotNull(key);
        assertTrue(key.startsWith("sdk-"));
    }

    @Test
    @DisplayName("Keys getGrowthBookClientKey returns external for empty user")
    void getGrowthBookClientKeyEmptyUser() {
        String key = Keys.getGrowthBookClientKey("", false);
        assertNotNull(key);
        assertTrue(key.startsWith("sdk-"));
    }

    @Test
    @DisplayName("Keys getGrowthBookClientKey default returns non-null")
    void getGrowthBookClientKeyDefault() {
        String key = Keys.getGrowthBookClientKey();
        assertNotNull(key);
        assertTrue(key.startsWith("sdk-"));
    }

    @Test
    @DisplayName("Keys all keys are non-empty")
    void allKeysNonEmpty() {
        String antProd = Keys.getGrowthBookClientKey("ant", false);
        String antDev = Keys.getGrowthBookClientKey("ant", true);
        String external = Keys.getGrowthBookClientKey("external", false);

        assertFalse(antProd.isEmpty());
        assertFalse(antDev.isEmpty());
        assertFalse(external.isEmpty());
    }

    @Test
    @DisplayName("Keys ant prod and dev keys are different")
    void antKeysDifferent() {
        String prod = Keys.getGrowthBookClientKey("ant", false);
        String dev = Keys.getGrowthBookClientKey("ant", true);

        assertNotEquals(prod, dev);
    }
}