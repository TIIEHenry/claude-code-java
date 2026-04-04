/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Config.
 */
class ConfigTest {

    @Test
    @DisplayName("Config creates with default path")
    void configCreates() {
        Config config = new Config();

        assertNotNull(config.getConfigPath());
        assertTrue(config.getConfigPath().toString().contains(".claude"));
    }

    @Test
    @DisplayName("Config set and getString works")
    void configSetGetString() {
        Config config = new Config();

        config.set("testKey", "testValue");
        assertTrue(config.getString("testKey").isPresent());
        assertEquals("testValue", config.getString("testKey").get());
    }

    @Test
    @DisplayName("Config getString with default returns default for missing key")
    void configGetStringDefault() {
        Config config = new Config();

        assertEquals("default", config.getString("nonexistent", "default"));
    }

    @Test
    @DisplayName("Config getInteger works")
    void configGetInteger() {
        Config config = new Config();
        config.set("intKey", 42);

        assertTrue(config.getInteger("intKey").isPresent());
        assertEquals(42, config.getInteger("intKey").get());
    }

    @Test
    @DisplayName("Config getBoolean works")
    void configGetBoolean() {
        Config config = new Config();
        config.set("boolKey", true);

        assertTrue(config.getBoolean("boolKey").isPresent());
        assertTrue(config.getBoolean("boolKey").get());
    }

    @Test
    @DisplayName("Config has works")
    void configHasWorks() {
        Config config = new Config();
        config.set("existing", "value");

        assertTrue(config.has("existing"));
        assertFalse(config.has("nonexistent"));
    }

    @Test
    @DisplayName("Config remove works")
    void configRemove() {
        Config config = new Config();
        config.set("toRemove", "value");

        config.remove("toRemove");
        assertFalse(config.has("toRemove"));
    }

    @Test
    @DisplayName("Config clear removes all settings")
    void configClear() {
        Config config = new Config();
        config.set("key1", "value1");
        config.set("key2", "value2");

        config.clear();
        assertTrue(config.keys().isEmpty());
    }

    @Test
    @DisplayName("Config keys returns all keys")
    void configKeys() {
        Config config = new Config();
        config.set("key1", "value1");
        config.set("key2", "value2");

        Set<String> keys = config.keys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    @DisplayName("Config getGlobalConfig returns new instance")
    void configGetGlobalConfig() {
        Config config1 = Config.getGlobalConfig();
        Config config2 = Config.getGlobalConfig();

        assertNotNull(config1);
        assertNotSame(config1, config2);
    }
}