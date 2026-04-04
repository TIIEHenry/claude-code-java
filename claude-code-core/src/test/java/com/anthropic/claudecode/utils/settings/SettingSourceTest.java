/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.settings;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for SettingSource.
 */
@DisplayName("SettingSource Tests")
class SettingSourceTest {

    @Test
    @DisplayName("SettingSource enum has correct values in order")
    void settingSourceHasCorrectValuesInOrder() {
        SettingSource[] sources = SettingSource.values();

        assertEquals(5, sources.length);
        // Order matters - FLAG should be first (highest priority)
        assertEquals(SettingSource.FLAG, sources[0]);
        assertEquals(SettingSource.LOCAL, sources[1]);
        assertEquals(SettingSource.PROJECT, sources[2]);
        assertEquals(SettingSource.USER, sources[3]);
        assertEquals(SettingSource.POLICY, sources[4]);
    }

    @Test
    @DisplayName("SettingSource getId works correctly")
    void settingSourceGetIdWorksCorrectly() {
        assertEquals("flag", SettingSource.FLAG.getId());
        assertEquals("local", SettingSource.LOCAL.getId());
        assertEquals("project", SettingSource.PROJECT.getId());
        assertEquals("user", SettingSource.USER.getId());
        assertEquals("policy", SettingSource.POLICY.getId());
    }

    @Test
    @DisplayName("SettingSource getDisplayName works correctly")
    void settingSourceGetDisplayNameWorksCorrectly() {
        assertEquals("Flag", SettingSource.FLAG.getDisplayName());
        assertEquals("Local", SettingSource.LOCAL.getDisplayName());
        assertEquals("Project", SettingSource.PROJECT.getDisplayName());
        assertEquals("User", SettingSource.USER.getDisplayName());
        assertEquals("Policy", SettingSource.POLICY.getDisplayName());
    }

    @Test
    @DisplayName("SETTING_SOURCES constant contains all values")
    void settingSourcesConstantContainsAllValues() {
        SettingSource[] sources = SettingSource.SETTING_SOURCES;

        assertEquals(5, sources.length);
        assertTrue(Arrays.asList(sources).contains(SettingSource.FLAG));
        assertTrue(Arrays.asList(sources).contains(SettingSource.LOCAL));
        assertTrue(Arrays.asList(sources).contains(SettingSource.PROJECT));
        assertTrue(Arrays.asList(sources).contains(SettingSource.USER));
        assertTrue(Arrays.asList(sources).contains(SettingSource.POLICY));
    }

    @Test
    @DisplayName("parseSettingSourcesFlag with null returns all sources")
    void parseSettingSourcesFlagWithNullReturnsAllSources() {
        SettingSource[] sources = SettingSource.parseSettingSourcesFlag(null);

        assertEquals(5, sources.length);
    }

    @Test
    @DisplayName("parseSettingSourcesFlag with empty string returns all sources")
    void parseSettingSourcesFlagWithEmptyStringReturnsAllSources() {
        SettingSource[] sources = SettingSource.parseSettingSourcesFlag("");

        assertEquals(5, sources.length);
    }

    @Test
    @DisplayName("parseSettingSourcesFlag with single source")
    void parseSettingSourcesFlagWithSingleSource() {
        SettingSource[] sources = SettingSource.parseSettingSourcesFlag("user");

        assertEquals(1, sources.length);
        assertEquals(SettingSource.USER, sources[0]);
    }

    @Test
    @DisplayName("parseSettingSourcesFlag with multiple sources")
    void parseSettingSourcesFlagWithMultipleSources() {
        SettingSource[] sources = SettingSource.parseSettingSourcesFlag("user,project,local");

        assertEquals(3, sources.length);
        assertEquals(SettingSource.USER, sources[0]);
        assertEquals(SettingSource.PROJECT, sources[1]);
        assertEquals(SettingSource.LOCAL, sources[2]);
    }

    @Test
    @DisplayName("parseSettingSourcesFlag with spaces")
    void parseSettingSourcesFlagWithSpaces() {
        SettingSource[] sources = SettingSource.parseSettingSourcesFlag("user, project");

        assertEquals(2, sources.length);
        assertEquals(SettingSource.USER, sources[0]);
        assertEquals(SettingSource.PROJECT, sources[1]);
    }

    @Test
    @DisplayName("parseSettingSourcesFlag with case insensitive")
    void parseSettingSourcesFlagWithCaseInsensitive() {
        SettingSource[] sources = SettingSource.parseSettingSourcesFlag("USER,PROJECT");

        assertEquals(2, sources.length);
        assertEquals(SettingSource.USER, sources[0]);
        assertEquals(SettingSource.PROJECT, sources[1]);
    }

    @Test
    @DisplayName("parseSettingSourcesFlag throws on unknown source")
    void parseSettingSourcesFlagThrowsOnUnknownSource() {
        assertThrows(IllegalArgumentException.class, () ->
            SettingSource.parseSettingSourcesFlag("unknown")
        );
    }

    @Test
    @DisplayName("getSourceDisplayName with null value returns displayName")
    void getSourceDisplayNameWithNullValueReturnsDisplayName() {
        String name = SettingSource.getSourceDisplayName(SettingSource.USER, null);

        assertEquals("User", name);
    }

    @Test
    @DisplayName("getSourceDisplayName with empty value returns displayName")
    void getSourceDisplayNameWithEmptyValueReturnsDisplayName() {
        String name = SettingSource.getSourceDisplayName(SettingSource.USER, "");

        assertEquals("User", name);
    }

    @Test
    @DisplayName("getSourceDisplayName with value returns displayName with value")
    void getSourceDisplayNameWithValueReturnsDisplayNameWithValue() {
        String name = SettingSource.getSourceDisplayName(SettingSource.PROJECT, ".claude/settings.json");

        assertEquals("Project (.claude/settings.json)", name);
    }
}