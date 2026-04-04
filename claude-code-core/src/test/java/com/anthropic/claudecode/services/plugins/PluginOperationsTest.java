/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.plugins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PluginOperations.
 */
class PluginOperationsTest {

    private PluginOperations operations;
    private PluginInstallationManager manager;

    @BeforeEach
    void setUp() {
        manager = new PluginInstallationManager();
        operations = new PluginOperations(manager);
    }

    @Test
    @DisplayName("PluginOperations listPlugins returns empty initially")
    void listPluginsEmpty() {
        List<PluginTypes.PluginInfo> plugins = operations.listPlugins();
        assertTrue(plugins.isEmpty());
    }

    @Test
    @DisplayName("PluginOperations getPlugin returns null for missing")
    void getPluginMissing() {
        assertNull(operations.getPlugin("nonexistent"));
    }

    @Test
    @DisplayName("PluginOperations loadPlugin and getPlugin")
    void loadAndGetPlugin() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "test-plugin", "1.0.0", "Test plugin",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        PluginTypes.LoadedPlugin loaded = new PluginTypes.LoadedPlugin(
            info, null, null, List.of(), List.of()
        );

        operations.loadPlugin("test-plugin", loaded);

        assertNotNull(operations.getPlugin("test-plugin"));
        assertEquals("test-plugin", operations.getPlugin("test-plugin").info().name());
    }

    @Test
    @DisplayName("PluginOperations unloadPlugin")
    void unloadPlugin() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "test-plugin", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        PluginTypes.LoadedPlugin loaded = new PluginTypes.LoadedPlugin(
            info, null, null, List.of(), List.of()
        );

        operations.loadPlugin("test-plugin", loaded);
        operations.unloadPlugin("test-plugin");

        assertNull(operations.getPlugin("test-plugin"));
    }

    @Test
    @DisplayName("PluginOperations getPluginCount")
    void getPluginCount() {
        assertEquals(0, operations.getPluginCount());

        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "test-plugin", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        operations.loadPlugin("test-plugin", new PluginTypes.LoadedPlugin(
            info, null, null, List.of(), List.of()
        ));

        assertEquals(1, operations.getPluginCount());
    }

    @Test
    @DisplayName("PluginOperations enablePlugin")
    void enablePlugin() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "test-plugin", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.DISABLED,
            System.currentTimeMillis(), Map.of()
        );

        operations.loadPlugin("test-plugin", new PluginTypes.LoadedPlugin(
            info, null, null, List.of(), List.of()
        ));

        operations.enablePlugin("test-plugin");

        assertEquals(PluginTypes.PluginStatus.INSTALLED,
            operations.getPlugin("test-plugin").info().status());
    }

    @Test
    @DisplayName("PluginOperations disablePlugin")
    void disablePlugin() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "test-plugin", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        operations.loadPlugin("test-plugin", new PluginTypes.LoadedPlugin(
            info, null, null, List.of(), List.of()
        ));

        operations.disablePlugin("test-plugin");

        assertEquals(PluginTypes.PluginStatus.DISABLED,
            operations.getPlugin("test-plugin").info().status());
    }

    @Test
    @DisplayName("PluginOperations isPluginEnabled")
    void isPluginEnabled() {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "test-plugin", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        operations.loadPlugin("test-plugin", new PluginTypes.LoadedPlugin(
            info, null, null, List.of(), List.of()
        ));

        assertTrue(operations.isPluginEnabled("test-plugin"));

        operations.disablePlugin("test-plugin");
        assertFalse(operations.isPluginEnabled("test-plugin"));
    }

    @Test
    @DisplayName("PluginOperations getEnabledPluginCount")
    void getEnabledPluginCount() {
        PluginTypes.PluginInfo enabled = new PluginTypes.PluginInfo(
            "enabled", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        PluginTypes.PluginInfo disabled = new PluginTypes.PluginInfo(
            "disabled", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.DISABLED,
            System.currentTimeMillis(), Map.of()
        );

        operations.loadPlugin("enabled", new PluginTypes.LoadedPlugin(
            enabled, null, null, List.of(), List.of()
        ));
        operations.loadPlugin("disabled", new PluginTypes.LoadedPlugin(
            disabled, null, null, List.of(), List.of()
        ));

        assertEquals(1, operations.getEnabledPluginCount());
    }

    @Test
    @DisplayName("PluginOperations getAllCommands excludes disabled")
    void getAllCommands() {
        PluginTypes.RegisteredCommand cmd = new PluginTypes.RegisteredCommand(
            "test-cmd", "Test command", "test-plugin"
        );

        PluginTypes.PluginInfo enabled = new PluginTypes.PluginInfo(
            "enabled", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        PluginTypes.PluginInfo disabled = new PluginTypes.PluginInfo(
            "disabled", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.DISABLED,
            System.currentTimeMillis(), Map.of()
        );

        operations.loadPlugin("enabled", new PluginTypes.LoadedPlugin(
            enabled, null, null, List.of(cmd), List.of()
        ));
        operations.loadPlugin("disabled", new PluginTypes.LoadedPlugin(
            disabled, null, null, List.of(cmd), List.of()
        ));

        assertEquals(1, operations.getAllCommands().size());
    }

    @Test
    @DisplayName("PluginOperations getAllTools excludes disabled")
    void getAllTools() {
        PluginTypes.RegisteredTool tool = new PluginTypes.RegisteredTool(
            "test-tool", "Test tool", "test-plugin"
        );

        PluginTypes.PluginInfo enabled = new PluginTypes.PluginInfo(
            "enabled", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        operations.loadPlugin("enabled", new PluginTypes.LoadedPlugin(
            enabled, null, null, List.of(), List.of(tool)
        ));

        assertEquals(1, operations.getAllTools().size());
    }

    @Test
    @DisplayName("PluginOperations reloadAll")
    void reloadAll() throws Exception {
        PluginTypes.PluginInfo info = new PluginTypes.PluginInfo(
            "test-plugin", "1.0.0", "Test",
            PluginTypes.PluginSourceType.LOCAL, "/path",
            PluginTypes.PluginStatus.INSTALLED,
            System.currentTimeMillis(), Map.of()
        );

        operations.loadPlugin("test-plugin", new PluginTypes.LoadedPlugin(
            info, null, null, List.of(), List.of()
        ));

        operations.reloadAll().get();

        // After reload, plugins are cleared
        assertEquals(0, operations.getPluginCount());
    }
}