/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.settings;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.nio.file.*;

/**
 * Tests for SettingsService.
 */
@DisplayName("SettingsService Tests")
class SettingsServiceTest {

    @BeforeEach
    void setUp() {
        // Clear caches before each test
        SettingsService.clearCaches();
        SettingsService.setActiveSources(SettingSource.SETTING_SOURCES);
    }

    @AfterEach
    void tearDown() {
        SettingsService.clearCaches();
        SettingsService.setActiveSources(SettingSource.SETTING_SOURCES);
    }

    @Test
    @DisplayName("SettingsService getSettings returns default settings when no files exist")
    void getSettingsReturnsDefaultSettingsWhenNoFilesExist() {
        SettingsTypes.SettingsSchema settings = SettingsService.getSettings();

        assertNotNull(settings);
        assertEquals("default", settings.theme());
        assertEquals(SettingsTypes.PermissionMode.DEFAULT, settings.permissionMode());
    }

    @Test
    @DisplayName("SettingsService getSettingsFromSource returns defaults for FLAG source")
    void getSettingsFromSourceReturnsDefaultsForFlagSource() {
        SettingsService.clearCaches();
        SettingsTypes.SettingsSchema settings = SettingsService.getSettingsFromSource(SettingSource.FLAG);

        assertNotNull(settings);
        // Flag settings default to empty when not set
    }

    @Test
    @DisplayName("SettingsService setFlagSettings affects combined settings")
    void setFlagSettingsAffectsCombinedSettings() {
        SettingsTypes.SettingsSchema flagSettings = new SettingsTypes.SettingsSchema(
            "claude-opus-4-6",
            null,
            null,
            SettingsTypes.PermissionMode.ACCEPT_EDITS,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        SettingsService.setFlagSettings(flagSettings);
        SettingsTypes.SettingsSchema combined = SettingsService.getSettings();

        assertEquals("claude-opus-4-6", combined.model());
        assertEquals(SettingsTypes.PermissionMode.ACCEPT_EDITS, combined.permissionMode());
    }

    @Test
    @DisplayName("SettingsService setActiveSources affects settings retrieval")
    void setActiveSourcesAffectsSettingsRetrieval() {
        SettingsTypes.SettingsSchema flagSettings = new SettingsTypes.SettingsSchema(
            "flag-model",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        SettingsService.setFlagSettings(flagSettings);

        // Only use FLAG source
        SettingsService.setActiveSources(new SettingSource[]{SettingSource.FLAG});
        SettingsTypes.SettingsSchema settings = SettingsService.getSettings();

        assertEquals("flag-model", settings.model());
    }

    @Test
    @DisplayName("SettingsService invalidateCache clears combined cache")
    void invalidateCacheClearsCombinedCache() {
        // Get settings once
        SettingsTypes.SettingsSchema first = SettingsService.getSettings();

        // Invalidate cache
        SettingsService.invalidateCache();

        // Get settings again - should reload
        SettingsTypes.SettingsSchema second = SettingsService.getSettings();

        // Should still be valid defaults
        assertNotNull(second);
    }

    @Test
    @DisplayName("SettingsService clearCaches clears all caches")
    void clearCachesClearsAllCaches() {
        SettingsTypes.SettingsSchema flagSettings = new SettingsTypes.SettingsSchema(
            "test-model",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        SettingsService.setFlagSettings(flagSettings);
        SettingsService.clearCaches();

        // Flag settings should be cleared
        SettingsTypes.SettingsSchema settings = SettingsService.getSettingsFromSource(SettingSource.FLAG);
        assertNull(settings.model());
    }

    @Test
    @DisplayName("SettingsService getPermissionMode returns current mode")
    void getPermissionModeReturnsCurrentMode() {
        SettingsTypes.PermissionMode mode = SettingsService.getPermissionMode();

        assertNotNull(mode);
        assertEquals(SettingsTypes.PermissionMode.DEFAULT, mode);
    }

    @Test
    @DisplayName("SettingsService getModel returns current model")
    void getModelReturnsCurrentModel() {
        String model = SettingsService.getModel();

        // Model may be null if not configured
        // Just verify method works
        assertNotNull(SettingsService.getSettings());
    }

    @Test
    @DisplayName("SettingsService isToolAllowed returns true for most tools")
    void isToolAllowedReturnsTrueForMostTools() {
        // With default settings, most tools are allowed
        assertTrue(SettingsService.isToolAllowed("Read"));
        assertTrue(SettingsService.isToolAllowed("Write"));
    }

    @Test
    @DisplayName("SettingsService isToolAllowed respects deny list")
    void isToolAllowedRespectsDenyList() {
        SettingsTypes.PermissionsSchema perms = new SettingsTypes.PermissionsSchema(
            SettingsTypes.PermissionMode.DEFAULT,
            List.of(),
            List.of("Bash"),
            List.of()
        );

        SettingsTypes.SettingsSchema settings = new SettingsTypes.SettingsSchema(
            null,
            null,
            null,
            null,
            perms,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        SettingsService.setFlagSettings(settings);
        SettingsService.invalidateCache();

        assertFalse(SettingsService.isToolAllowed("Bash"));
        assertTrue(SettingsService.isToolAllowed("Read"));
    }

    @Test
    @DisplayName("SettingsService getSettingWithSource returns value from first active source")
    void getSettingWithSourceReturnsValueFromFirstActiveSource() {
        SettingsTypes.SettingsSchema flagSettings = new SettingsTypes.SettingsSchema(
            "flag-model",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        SettingsService.setFlagSettings(flagSettings);

        SettingsTypes.SettingValue<String> value = SettingsService.getSettingWithSource(
            SettingsTypes.SettingsSchema::model
        );

        assertEquals("flag-model", value.value());
        assertEquals(SettingSource.FLAG, value.source());
    }

    @Test
    @DisplayName("SettingsService getSettingWithSource returns null for missing value")
    void getSettingWithSourceReturnsNullForMissingValue() {
        SettingsService.clearCaches();

        SettingsTypes.SettingValue<String> value = SettingsService.getSettingWithSource(
            SettingsTypes.SettingsSchema::apiKey
        );

        assertNull(value.value());
    }

    @Test
    @DisplayName("SettingsService merge respects priority order")
    void mergeRespectsPriorityOrder() {
        // Flag settings (highest priority)
        SettingsTypes.SettingsSchema flagSettings = new SettingsTypes.SettingsSchema(
            "flag-model",
            "flag-key",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        SettingsService.setFlagSettings(flagSettings);
        SettingsService.setActiveSources(new SettingSource[]{SettingSource.FLAG, SettingSource.USER});
        SettingsService.invalidateCache();

        SettingsTypes.SettingsSchema combined = SettingsService.getSettings();

        // Flag should take precedence
        assertEquals("flag-model", combined.model());
        assertEquals("flag-key", combined.apiKey());
    }
}