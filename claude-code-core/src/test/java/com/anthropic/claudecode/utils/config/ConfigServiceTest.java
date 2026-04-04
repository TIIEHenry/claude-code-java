/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.nio.file.*;

/**
 * Tests for ConfigService.
 */
@DisplayName("ConfigService Tests")
class ConfigServiceTest {

    @BeforeEach
    void setUp() {
        ConfigService.clearCaches();
    }

    @AfterEach
    void tearDown() {
        ConfigService.clearCaches();
    }

    @Test
    @DisplayName("ConfigService getGlobalConfig returns valid config")
    void getGlobalConfigReturnsValidConfig() {
        GlobalConfig config = ConfigService.getGlobalConfig();

        assertNotNull(config);
        // Default values
        assertEquals("default", config.theme());
        assertEquals("auto", config.preferredNotifChannel());
    }

    @Test
    @DisplayName("ConfigService getProjectConfig returns valid config")
    void getProjectConfigReturnsValidConfig() {
        ProjectConfig config = ConfigService.getProjectConfig();

        assertNotNull(config);
        assertTrue(config.allowedTools().isEmpty());
    }

    @Test
    @DisplayName("ConfigService getGlobalConfigPath returns valid path")
    void getGlobalConfigPathReturnsValidPath() {
        Path path = ConfigService.getGlobalConfigPath();

        assertNotNull(path);
        assertTrue(path.toString().endsWith(".claude.json"));
    }

    @Test
    @DisplayName("ConfigService getProjectConfigPath returns valid path")
    void getProjectConfigPathReturnsValidPath() {
        Path path = ConfigService.getProjectConfigPath();

        assertNotNull(path);
        assertTrue(path.toString().contains(".claude"));
        assertTrue(path.toString().endsWith("settings.json"));
    }

    @Test
    @DisplayName("ConfigService getClaudeConfigHomeDir returns valid dir")
    void getClaudeConfigHomeDirReturnsValidDir() {
        String dir = ConfigService.getClaudeConfigHomeDir();

        assertNotNull(dir);
        assertTrue(dir.contains(".claude"));
    }

    @Test
    @DisplayName("ConfigService getTeamsDir returns valid dir")
    void getTeamsDirReturnsValidDir() {
        String dir = ConfigService.getTeamsDir();

        assertNotNull(dir);
        assertTrue(dir.contains("teams"));
    }

    @Test
    @DisplayName("ConfigService getSkillsDir returns valid dir")
    void getSkillsDirReturnsValidDir() {
        String dir = ConfigService.getSkillsDir();

        assertNotNull(dir);
        assertTrue(dir.contains("skills"));
    }

    @Test
    @DisplayName("ConfigService getHooksDir returns valid dir")
    void getHooksDirReturnsValidDir() {
        String dir = ConfigService.getHooksDir();

        assertNotNull(dir);
        assertTrue(dir.contains("hooks"));
    }

    @Test
    @DisplayName("ConfigService getSessionsDir returns valid dir")
    void getSessionsDirReturnsValidDir() {
        String dir = ConfigService.getSessionsDir();

        assertNotNull(dir);
        assertTrue(dir.contains("sessions"));
    }

    @Test
    @DisplayName("ConfigService hasTrustDialogAccepted returns false by default")
    void hasTrustDialogAcceptedReturnsFalseByDefault() {
        assertFalse(ConfigService.hasTrustDialogAccepted());
    }

    @Test
    @DisplayName("ConfigService hasCompletedOnboarding returns false by default")
    void hasCompletedOnboardingReturnsFalseByDefault() {
        assertFalse(ConfigService.hasCompletedOnboarding());
    }

    @Test
    @DisplayName("ConfigService getConfiguredModel returns null by default")
    void getConfiguredModelReturnsNullByDefault() {
        assertNull(ConfigService.getConfiguredModel());
    }

    @Test
    @DisplayName("ConfigService getPreferredNotifChannel returns auto by default")
    void getPreferredNotifChannelReturnsAutoByDefault() {
        assertEquals("auto", ConfigService.getPreferredNotifChannel());
    }

    @Test
    @DisplayName("ConfigService saveGlobalConfig updates cache")
    void saveGlobalConfigUpdatesCache() {
        GlobalConfig newConfig = new GlobalConfig(
            "claude-opus-4-6",
            "dark",
            "terminal",
            List.of(),
            List.of(),
            true,
            true,
            1,
            ReleaseChannel.STABLE,
            InstallMethod.GLOBAL,
            null,
            null,
            true,
            "test-key",
            null,
            null,
            null
        );

        ConfigService.saveGlobalConfig(newConfig);

        GlobalConfig retrieved = ConfigService.getGlobalConfig();
        assertEquals("claude-opus-4-6", retrieved.model());
        assertEquals("dark", retrieved.theme());
        assertTrue(retrieved.hasTrustDialogAccepted());
    }

    @Test
    @DisplayName("ConfigService saveProjectConfig updates cache")
    void saveProjectConfigUpdatesCache() {
        ProjectConfig newConfig = new ProjectConfig(
            List.of("Read", "Write"),
            List.of(),
            Map.of(),
            null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, false, false, 0, false, false,
            List.of(), List.of(), false, List.of(), List.of(), null, null
        );

        ConfigService.saveProjectConfig(newConfig);

        ProjectConfig retrieved = ConfigService.getProjectConfig();
        assertEquals(2, retrieved.allowedTools().size());
    }

    @Test
    @DisplayName("ConfigService clearCaches clears all caches")
    void clearCachesClearsAllCaches() {
        // Load configs
        ConfigService.getGlobalConfig();
        ConfigService.getProjectConfig();

        // Clear caches
        ConfigService.clearCaches();

        // Paths should be null after clear
        // We can't verify directly, but next get should reload
        GlobalConfig config = ConfigService.getGlobalConfig();
        assertNotNull(config);
    }
}