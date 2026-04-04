/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigManager.
 */
class ConfigManagerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("ConfigManager constants")
    void constants() {
        assertEquals(".claude", ConfigManager.CLAUDE_DIR);
        assertEquals("config.json", ConfigManager.CONFIG_JSON);
        assertEquals("settings.json", ConfigManager.SETTINGS_JSON);
        assertEquals("CLAUDE.md", ConfigManager.CLAUDE_MD);
    }

    @Test
    @DisplayName("ConfigManager ProjectConfig record")
    void projectConfigRecord() {
        ConfigManager.ProjectConfig config = new ConfigManager.ProjectConfig(
            java.util.List.of("tool1", "tool2"),
            Map.of("server", "config"),
            true,
            "session-123"
        );

        assertEquals(2, config.allowedTools().size());
        assertEquals(1, config.mcpServers().size());
        assertTrue(config.hasTrustDialogAccepted());
        assertEquals("session-123", config.lastSessionId());
    }

    @Test
    @DisplayName("ConfigManager ProjectConfig defaults")
    void projectConfigDefaults() {
        ConfigManager.ProjectConfig config = ConfigManager.ProjectConfig.defaults();

        assertTrue(config.allowedTools().isEmpty());
        assertTrue(config.mcpServers().isEmpty());
        assertFalse(config.hasTrustDialogAccepted());
        assertNull(config.lastSessionId());
    }

    @Test
    @DisplayName("ConfigManager GlobalConfig record")
    void globalConfigRecord() {
        ConfigManager.GlobalConfig config = new ConfigManager.GlobalConfig(
            "api-key-123",
            "org-456",
            "claude-sonnet-4-6",
            Map.of("key", "value")
        );

        assertEquals("api-key-123", config.apiKey());
        assertEquals("org-456", config.organizationId());
        assertEquals("claude-sonnet-4-6", config.defaultModel());
        assertEquals(1, config.settings().size());
    }

    @Test
    @DisplayName("ConfigManager GlobalConfig defaults")
    void globalConfigDefaults() {
        ConfigManager.GlobalConfig config = ConfigManager.GlobalConfig.defaults();

        assertNull(config.apiKey());
        assertNull(config.organizationId());
        assertNull(config.defaultModel());
        assertTrue(config.settings().isEmpty());
    }

    @Test
    @DisplayName("ConfigManager getClaudeConfigHome")
    void getClaudeConfigHome() {
        Path home = ConfigManager.getClaudeConfigHome();
        assertNotNull(home);
        assertTrue(home.toString().contains(".claude"));
    }

    @Test
    @DisplayName("ConfigManager getGlobalConfigPath")
    void getGlobalConfigPath() {
        Path path = ConfigManager.getGlobalConfigPath();
        assertNotNull(path);
        assertTrue(path.toString().contains("config.json"));
    }

    @Test
    @DisplayName("ConfigManager getProjectConfigDir")
    void getProjectConfigDir() {
        Path dir = ConfigManager.getProjectConfigDir("/tmp/test");
        assertEquals("/tmp/test/.claude", dir.toString().replace("\\", "/"));
    }

    @Test
    @DisplayName("ConfigManager getProjectConfigDir null uses cwd")
    void getProjectConfigDirNull() {
        Path dir = ConfigManager.getProjectConfigDir(null);
        assertNotNull(dir);
        assertTrue(dir.toString().contains(".claude"));
    }

    @Test
    @DisplayName("ConfigManager getProjectConfigPath")
    void getProjectConfigPath() {
        Path path = ConfigManager.getProjectConfigPath("/tmp/test");
        assertTrue(path.toString().contains("config.json"));
    }

    @Test
    @DisplayName("ConfigManager getClaudeMdPath")
    void getClaudeMdPath() {
        Path path = ConfigManager.getClaudeMdPath("/tmp/test");
        assertTrue(path.toString().contains("CLAUDE.md"));
    }

    @Test
    @DisplayName("ConfigManager loadConfig nonexistent file")
    void loadConfigNonexistent() {
        Map<String, Object> config = ConfigManager.loadConfig(tempDir.resolve("nonexistent.json"));
        assertTrue(config.isEmpty());
    }

    @Test
    @DisplayName("ConfigManager loadConfig null path")
    void loadConfigNull() {
        Map<String, Object> config = ConfigManager.loadConfig(null);
        assertTrue(config.isEmpty());
    }

    @Test
    @DisplayName("ConfigManager saveConfig and loadConfig")
    void saveAndLoadConfig() throws Exception {
        Path configFile = tempDir.resolve("config.json");
        Map<String, Object> config = Map.of(
            "key1", "value1",
            "key2", 42,
            "key3", true
        );

        ConfigManager.saveConfig(configFile, config);
        assertTrue(Files.exists(configFile));

        Map<String, Object> loaded = ConfigManager.loadConfig(configFile);
        assertEquals("value1", loaded.get("key1"));
        assertEquals(42, loaded.get("key2"));
        assertEquals(true, loaded.get("key3"));
    }

    @Test
    @DisplayName("ConfigManager saveConfig null path throws")
    void saveConfigNull() {
        assertThrows(IllegalArgumentException.class, () ->
            ConfigManager.saveConfig(null, Map.of())
        );
    }

    @Test
    @DisplayName("ConfigManager getGlobalConfig")
    void getGlobalConfig() {
        ConfigManager.GlobalConfig config = ConfigManager.getGlobalConfig();
        assertNotNull(config);
        // May be defaults if no config file exists
    }

    @Test
    @DisplayName("ConfigManager loadGlobalConfig")
    void loadGlobalConfig() {
        Map<String, Object> config = ConfigManager.loadGlobalConfig();
        assertNotNull(config);
    }

    @Test
    @DisplayName("ConfigManager loadUserSettings")
    void loadUserSettings() {
        Map<String, Object> settings = ConfigManager.loadUserSettings();
        assertNotNull(settings);
    }

    @Test
    @DisplayName("ConfigManager saveGlobalConfig")
    void saveGlobalConfigMap() {
        assertDoesNotThrow(() -> ConfigManager.saveGlobalConfig(Map.of("test", "value")));
    }

    @Test
    @DisplayName("ConfigManager saveGlobalConfig single key")
    void saveGlobalConfigKey() {
        assertDoesNotThrow(() -> ConfigManager.saveGlobalConfig("testKey", "testValue"));
    }

    @Test
    @DisplayName("ConfigManager saveUserSetting")
    void saveUserSetting() {
        assertDoesNotThrow(() -> ConfigManager.saveUserSetting("testKey", "testValue"));
    }

    @Test
    @DisplayName("ConfigManager getProjectConfig")
    void getProjectConfig() {
        ConfigManager.ProjectConfig config = ConfigManager.getProjectConfig(tempDir.toString());
        assertNotNull(config);
        // Should be defaults for nonexistent project
        assertTrue(config.allowedTools().isEmpty());
    }

    @Test
    @DisplayName("ConfigManager clearCache")
    void clearCache() {
        assertDoesNotThrow(() -> ConfigManager.clearCache());
    }
}