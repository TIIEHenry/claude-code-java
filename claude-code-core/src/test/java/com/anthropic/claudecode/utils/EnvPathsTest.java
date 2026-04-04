/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EnvPaths.
 */
class EnvPathsTest {

    @Test
    @DisplayName("EnvPaths getClaudeConfigHome returns path")
    void getClaudeConfigHome() {
        Path path = EnvPaths.getClaudeConfigHome();
        assertNotNull(path);
        assertTrue(path.toString().contains(".claude"));
    }

    @Test
    @DisplayName("EnvPaths getClaudeProjectsDir returns path")
    void getClaudeProjectsDir() {
        Path path = EnvPaths.getClaudeProjectsDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("projects"));
    }

    @Test
    @DisplayName("EnvPaths getClaudeMemoryDir returns path")
    void getClaudeMemoryDir() {
        Path path = EnvPaths.getClaudeMemoryDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("memory"));
    }

    @Test
    @DisplayName("EnvPaths getSessionMemoryDir returns path")
    void getSessionMemoryDir() {
        Path path = EnvPaths.getSessionMemoryDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("session-memory"));
    }

    @Test
    @DisplayName("EnvPaths getAgentMemoryDir returns path")
    void getAgentMemoryDir() {
        Path path = EnvPaths.getAgentMemoryDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("agent-memory"));
    }

    @Test
    @DisplayName("EnvPaths getLocalInstallDir returns path")
    void getLocalInstallDir() {
        Path path = EnvPaths.getLocalInstallDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("local"));
    }

    @Test
    @DisplayName("EnvPaths getClaudeCommandsDir returns path")
    void getClaudeCommandsDir() {
        Path path = EnvPaths.getClaudeCommandsDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("commands"));
    }

    @Test
    @DisplayName("EnvPaths getClaudeHooksDir returns path")
    void getClaudeHooksDir() {
        Path path = EnvPaths.getClaudeHooksDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("hooks"));
    }

    @Test
    @DisplayName("EnvPaths getClaudeMcpDir returns path")
    void getClaudeMcpDir() {
        Path path = EnvPaths.getClaudeMcpDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("mcp"));
    }

    @Test
    @DisplayName("EnvPaths getClaudeLogsDir returns path")
    void getClaudeLogsDir() {
        Path path = EnvPaths.getClaudeLogsDir();
        assertNotNull(path);
        assertTrue(path.toString().contains("logs"));
    }

    @Test
    @DisplayName("EnvPaths getSettingsFile returns path")
    void getSettingsFile() {
        Path path = EnvPaths.getSettingsFile();
        assertNotNull(path);
        assertTrue(path.toString().contains("settings.json"));
    }

    @Test
    @DisplayName("EnvPaths getLegacySettingsFile returns path")
    void getLegacySettingsFile() {
        Path path = EnvPaths.getLegacySettingsFile();
        assertNotNull(path);
        assertTrue(path.toString().contains("settings.local.json"));
    }

    @Test
    @DisplayName("EnvPaths getProjectSettingsFile returns path")
    void getProjectSettingsFile() {
        Path projectRoot = Paths.get("/tmp/test-project");
        Path path = EnvPaths.getProjectSettingsFile(projectRoot);
        assertTrue(path.toString().contains(".claude"));
        assertTrue(path.toString().contains("settings.local.json"));
    }

    @Test
    @DisplayName("EnvPaths getProjectClaudeMd returns path")
    void getProjectClaudeMd() {
        Path projectRoot = Paths.get("/tmp/test-project");
        Path path = EnvPaths.getProjectClaudeMd(projectRoot);
        assertTrue(path.toString().contains("CLAUDE.md"));
    }

    @Test
    @DisplayName("EnvPaths getProjectMemoryDir returns path")
    void getProjectMemoryDir() {
        Path projectRoot = Paths.get("/tmp/test-project");
        Path path = EnvPaths.getProjectMemoryDir(projectRoot);
        assertTrue(path.toString().contains(".claude"));
        assertTrue(path.toString().contains("memory"));
    }

    @Test
    @DisplayName("EnvPaths getProjectHooksDir returns path")
    void getProjectHooksDir() {
        Path projectRoot = Paths.get("/tmp/test-project");
        Path path = EnvPaths.getProjectHooksDir(projectRoot);
        assertTrue(path.toString().contains("hooks"));
    }

    @Test
    @DisplayName("EnvPaths getProjectCommandsDir returns path")
    void getProjectCommandsDir() {
        Path projectRoot = Paths.get("/tmp/test-project");
        Path path = EnvPaths.getProjectCommandsDir(projectRoot);
        assertTrue(path.toString().contains("commands"));
    }

    @Test
    @DisplayName("EnvPaths getScheduledTasksFile returns path")
    void getScheduledTasksFile() {
        Path path = EnvPaths.getScheduledTasksFile();
        assertTrue(path.toString().contains("scheduled_tasks.json"));
    }

    @Test
    @DisplayName("EnvPaths getMcpServersFile returns path")
    void getMcpServersFile() {
        Path path = EnvPaths.getMcpServersFile();
        assertTrue(path.toString().contains("mcp_servers.json"));
    }

    @Test
    @DisplayName("EnvPaths getCurrentProjectDir returns Optional")
    void getCurrentProjectDir() {
        Optional<Path> dir = EnvPaths.getCurrentProjectDir();
        // May be empty if env var not set
        assertNotNull(dir);
    }

    @Test
    @DisplayName("EnvPaths getProjectSessionsDir returns path")
    void getProjectSessionsDir() {
        Path path = EnvPaths.getProjectSessionsDir("test-project");
        assertTrue(path.toString().contains("test-project"));
    }

    @Test
    @DisplayName("EnvPaths getAllClaudeDirs returns list")
    void getAllClaudeDirs() {
        List<Path> dirs = EnvPaths.getAllClaudeDirs();
        assertEquals(10, dirs.size());
    }

    @Test
    @DisplayName("EnvPaths ensureClaudeDirsExist does not throw")
    void ensureClaudeDirsExist() {
        assertDoesNotThrow(() -> EnvPaths.ensureClaudeDirsExist());
    }

    @Test
    @DisplayName("EnvPaths getXdgConfigHome returns path")
    void getXdgConfigHome() {
        Path path = EnvPaths.getXdgConfigHome();
        assertNotNull(path);
    }

    @Test
    @DisplayName("EnvPaths getXdgDataHome returns path")
    void getXdgDataHome() {
        Path path = EnvPaths.getXdgDataHome();
        assertNotNull(path);
    }

    @Test
    @DisplayName("EnvPaths getXdgCacheHome returns path")
    void getXdgCacheHome() {
        Path path = EnvPaths.getXdgCacheHome();
        assertNotNull(path);
    }

    @Test
    @DisplayName("EnvPaths normalizeProjectName normalizes path")
    void normalizeProjectName() {
        Path projectPath = Paths.get("/Users/test/projects/my-app");
        String normalized = EnvPaths.normalizeProjectName(projectPath);
        assertFalse(normalized.contains("/"));
        assertFalse(normalized.startsWith("-"));
    }
}