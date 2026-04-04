/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigService.
 */
class ConfigServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private Path configPath;

    @BeforeEach
    void setUp() {
        configPath = tempDir.resolve("test.conf");
        configService = new ConfigService(configPath);
    }

    @Test
    @DisplayName("ConfigService get returns Optional")
    void getReturnsOptional() {
        configService.set("testKey", "testValue");

        Optional<Object> value = configService.get("testKey");

        assertTrue(value.isPresent());
        assertEquals("testValue", value.get());
    }

    @Test
    @DisplayName("ConfigService get returns empty for missing key")
    void getMissingReturnsEmpty() {
        Optional<Object> value = configService.get("missingKey");

        assertFalse(value.isPresent());
    }

    @Test
    @DisplayName("ConfigService getString returns value")
    void getString() {
        configService.set("stringKey", "stringValue");

        assertEquals("stringValue", configService.getString("stringKey", "default"));
    }

    @Test
    @DisplayName("ConfigService getString returns default for missing")
    void getStringDefault() {
        assertEquals("default", configService.getString("missingKey", "default"));
    }

    @Test
    @DisplayName("ConfigService getInt returns value")
    void getInt() {
        configService.set("intKey", 42);

        assertEquals(42, configService.getInt("intKey", 0));
    }

    @Test
    @DisplayName("ConfigService getInt returns default for missing")
    void getIntDefault() {
        assertEquals(100, configService.getInt("missingKey", 100));
    }

    @Test
    @DisplayName("ConfigService getBoolean returns value")
    void getBoolean() {
        configService.set("boolKey", true);

        assertTrue(configService.getBoolean("boolKey", false));
    }

    @Test
    @DisplayName("ConfigService getBoolean returns default for missing")
    void getBooleanDefault() {
        assertTrue(configService.getBoolean("missingKey", true));
        assertFalse(configService.getBoolean("missingKey", false));
    }

    @Test
    @DisplayName("ConfigService set stores value")
    void setStoresValue() {
        configService.set("newKey", "newValue");

        assertEquals("newValue", configService.getString("newKey", null));
    }

    @Test
    @DisplayName("ConfigService set overwrites existing value")
    void setOverwrites() {
        configService.set("key", "value1");
        configService.set("key", "value2");

        assertEquals("value2", configService.getString("key", null));
    }

    @Test
    @DisplayName("ConfigService remove deletes value")
    void removeDeletesValue() {
        configService.set("key", "value");
        assertTrue(configService.has("key"));

        configService.remove("key");

        assertFalse(configService.has("key"));
    }

    @Test
    @DisplayName("ConfigService has checks existence")
    void hasChecksExistence() {
        assertFalse(configService.has("key"));

        configService.set("key", "value");

        assertTrue(configService.has("key"));
    }

    @Test
    @DisplayName("ConfigService getKeys returns all keys")
    void getKeysReturnsAll() {
        configService.set("key1", "value1");
        configService.set("key2", "value2");

        Set<String> keys = configService.getKeys();

        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertEquals(2, keys.size());
    }

    @Test
    @DisplayName("ConfigService resetDefaults clears and loads defaults")
    void resetDefaults() {
        configService.set("customKey", "customValue");
        configService.set("theme", "customTheme");

        configService.resetDefaults();

        assertFalse(configService.has("customKey"));
        assertEquals("default", configService.getString("theme", null));
        assertEquals("vim", configService.getString("editor", null));
    }

    @Test
    @DisplayName("ConfigService ConfigChangeListener receives notifications")
    void configChangeListener() {
        StringBuilder sb = new StringBuilder();
        ConfigService.ConfigChangeListener listener = (key, oldValue, newValue) -> {
            sb.append(key).append(":").append(oldValue).append("->").append(newValue);
        };

        configService.addListener(listener);
        configService.set("key", "value");

        assertTrue(sb.toString().contains("key"));
        assertTrue(sb.toString().contains("value"));
    }

    @Test
    @DisplayName("ConfigService removeListener stops notifications")
    void removeListener() {
        int[] count = {0};
        ConfigService.ConfigChangeListener listener = (key, oldValue, newValue) -> count[0]++;

        configService.addListener(listener);
        configService.set("key1", "value1");
        assertEquals(1, count[0]);

        configService.removeListener(listener);
        configService.set("key2", "value2");
        assertEquals(1, count[0]); // Still 1, not incremented
    }

    @Test
    @DisplayName("ConfigService ConfigSection record")
    void configSectionRecord() {
        Map<String, Object> values = Map.of("key1", "value1", "key2", 42);
        ConfigService.ConfigSection section = new ConfigService.ConfigSection("test", values);

        assertEquals("test", section.name());
        assertEquals(values, section.values());
        assertTrue(section.get("key1").isPresent());
        assertEquals("value1", section.get("key1").get());
    }

    @Test
    @DisplayName("ConfigService getSections groups by prefix")
    void getSections() {
        configService.set("general.key1", "value1");
        configService.set("project.key1", "value2");
        configService.set("standalone", "value3");

        Map<String, ConfigService.ConfigSection> sections = configService.getSections();

        assertTrue(sections.containsKey("general"));
        assertTrue(sections.containsKey("project"));
        // Keys without prefix go into "general" section
        assertTrue(sections.get("general").values().containsKey("standalone"));
    }

    @Test
    @DisplayName("ConfigService saves and loads config")
    void saveAndLoad() {
        configService.set("key", "value");
        configService.set("number", 42);
        configService.saveConfig();

        // Create new instance to load from file
        ConfigService loaded = new ConfigService(configPath);

        assertEquals("value", loaded.getString("key", null));
        assertEquals(42, loaded.getInt("number", 0));
    }

    @Test
    @DisplayName("ConfigService handles non-existent file")
    void nonExistentFile() {
        Path newPath = tempDir.resolve("nonexistent.conf");
        ConfigService newService = new ConfigService(newPath);

        // Should work with defaults
        assertEquals("default", newService.getString("missingKey", "default"));
    }
}