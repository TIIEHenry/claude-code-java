/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for ProjectConfig.
 */
@DisplayName("ProjectConfig Tests")
class ProjectConfigTest {

    @Test
    @DisplayName("ProjectConfig createDefault returns valid config")
    void createDefaultReturnsValidConfig() {
        ProjectConfig config = ProjectConfig.createDefault();

        assertNotNull(config);
        assertTrue(config.allowedTools().isEmpty());
        assertTrue(config.mcpContextUris().isEmpty());
        assertTrue(config.mcpServers().isEmpty());
        assertNull(config.lastAPIDuration());
        assertNull(config.lastCost());
        assertEquals(0, config.projectOnboardingSeenCount());
        assertNull(config.hasTrustDialogAccepted());
        assertNull(config.hasCompletedProjectOnboarding());
    }

    @Test
    @DisplayName("ProjectConfig record works correctly")
    void projectConfigRecordWorksCorrectly() {
        ProjectConfig config = new ProjectConfig(
            List.of("Read", "Write"),
            List.of("context1"),
            Map.of(),
            1000L,
            null,
            500L,
            0.05,
            60000L,
            10,
            5,
            1000L,
            500L,
            100L,
            50L,
            0,
            "session-123",
            Map.of(),
            Map.of(),
            List.of(),
            null,
            true,
            false,
            1,
            false,
            true,
            List.of(),
            List.of(),
            true,
            List.of(),
            List.of(),
            null,
            "spawn"
        );

        assertEquals(2, config.allowedTools().size());
        assertEquals(1, config.mcpContextUris().size());
        assertEquals(1000L, config.lastAPIDuration());
        assertEquals(500L, config.lastToolDuration());
        assertEquals(0.05, config.lastCost());
        assertEquals(60000L, config.lastDuration());
        assertEquals(10, config.lastLinesAdded());
        assertEquals(5, config.lastLinesRemoved());
        assertEquals("session-123", config.lastSessionId());
        assertTrue(config.hasTrustDialogAccepted());
        assertEquals(1, config.projectOnboardingSeenCount());
        assertEquals("spawn", config.remoteControlSpawnMode());
    }

    @Test
    @DisplayName("ProjectConfig McpServerConfig record works correctly")
    void mcpServerConfigRecordWorksCorrectly() {
        ProjectConfig.McpServerConfig server = new ProjectConfig.McpServerConfig(
            "node",
            List.of("server.js"),
            Map.of("NODE_ENV", "test"),
            "stdio",
            null,
            null,
            Map.of()
        );

        assertEquals("node", server.command());
        assertEquals(1, server.args().size());
        assertEquals(1, server.env().size());
        assertEquals("stdio", server.type());
    }

    @Test
    @DisplayName("ProjectConfig ModelUsageStats record works correctly")
    void modelUsageStatsRecordWorksCorrectly() {
        ProjectConfig.ModelUsageStats stats = new ProjectConfig.ModelUsageStats(
            1000L,
            500L,
            200L,
            100L,
            2,
            0.03
        );

        assertEquals(1000L, stats.inputTokens());
        assertEquals(500L, stats.outputTokens());
        assertEquals(200L, stats.cacheReadInputTokens());
        assertEquals(100L, stats.cacheCreationInputTokens());
        assertEquals(2, stats.webSearchRequests());
        assertEquals(0.03, stats.costUSD());
    }

    @Test
    @DisplayName("ProjectConfig ActiveWorktreeSession record works correctly")
    void activeWorktreeSessionRecordWorksCorrectly() {
        ProjectConfig.ActiveWorktreeSession session = new ProjectConfig.ActiveWorktreeSession(
            "/original/path",
            "/worktree/path",
            "feature-branch",
            "main",
            "session-456",
            false
        );

        assertEquals("/original/path", session.originalCwd());
        assertEquals("/worktree/path", session.worktreePath());
        assertEquals("feature-branch", session.worktreeName());
        assertEquals("main", session.originalBranch());
        assertEquals("session-456", session.sessionId());
        assertFalse(session.hookBased());
    }

    @Test
    @DisplayName("ReleaseChannel enum has correct values")
    void releaseChannelEnumHasCorrectValues() {
        ReleaseChannel[] channels = ReleaseChannel.values();

        assertEquals(2, channels.length);
        assertTrue(Arrays.asList(channels).contains(ReleaseChannel.STABLE));
        assertTrue(Arrays.asList(channels).contains(ReleaseChannel.LATEST));
    }

    @Test
    @DisplayName("InstallMethod enum has correct values")
    void installMethodEnumHasCorrectValues() {
        InstallMethod[] methods = InstallMethod.values();

        assertEquals(4, methods.length);
        assertTrue(Arrays.asList(methods).contains(InstallMethod.LOCAL));
        assertTrue(Arrays.asList(methods).contains(InstallMethod.NATIVE));
        assertTrue(Arrays.asList(methods).contains(InstallMethod.GLOBAL));
        assertTrue(Arrays.asList(methods).contains(InstallMethod.UNKNOWN));
    }

    @Test
    @DisplayName("ProjectConfig with empty allowed tools")
    void projectConfigWithEmptyAllowedTools() {
        ProjectConfig config = ProjectConfig.createDefault();

        assertTrue(config.allowedTools().isEmpty());
        // Should not be modifiable
        assertThrows(UnsupportedOperationException.class, () ->
            config.allowedTools().add("NewTool")
        );
    }

    @Test
    @DisplayName("ProjectConfig with MCP servers")
    void projectConfigWithMcpServers() {
        ProjectConfig.McpServerConfig server1 = new ProjectConfig.McpServerConfig(
            "node", List.of("s1.js"), Map.of(), "stdio", null, null, Map.of()
        );
        ProjectConfig.McpServerConfig server2 = new ProjectConfig.McpServerConfig(
            "python", List.of("s2.py"), Map.of(), "stdio", null, null, Map.of()
        );

        Map<String, ProjectConfig.McpServerConfig> servers = new HashMap<>();
        servers.put("server1", server1);
        servers.put("server2", server2);

        ProjectConfig config = new ProjectConfig(
            List.of(), List.of(), servers,
            null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, false, false, 0, false, false,
            List.of(), List.of(), false, List.of(), List.of(), null, null
        );

        assertEquals(2, config.mcpServers().size());
        assertNotNull(config.mcpServers().get("server1"));
        assertNotNull(config.mcpServers().get("server2"));
    }
}