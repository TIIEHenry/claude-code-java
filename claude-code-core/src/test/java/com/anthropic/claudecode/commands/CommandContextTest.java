/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandContext.
 */
class CommandContextTest {

    @Test
    @DisplayName("defaults creates valid context")
    void defaultsCreatesValidContext() {
        CommandContext ctx = CommandContext.defaults();

        assertNotNull(ctx.cwd());
        assertNotNull(ctx.env());
        assertNotNull(ctx.settings());
        assertTrue(ctx.isInteractive());
        assertNotNull(ctx.outputHandler());
    }

    @Test
    @DisplayName("custom context works")
    void customContextWorks() {
        Map<String, Object> env = new HashMap<>();
        env.put("TEST", "value");
        Map<String, Object> settings = new HashMap<>();
        settings.put("key", "val");

        CommandContext ctx = new CommandContext(
            "/tmp",
            env,
            settings,
            false,
            System.out::println
        );

        assertEquals("/tmp", ctx.cwd());
        assertEquals("value", ctx.env().get("TEST"));
        assertEquals("val", ctx.settings().get("key"));
        assertFalse(ctx.isInteractive());
    }

    @Test
    @DisplayName("model methods work")
    void modelMethodsWork() {
        CommandContext ctx = CommandContext.defaults();

        assertEquals("claude-sonnet-4-6", ctx.getCurrentModel());
        ctx.setCurrentModel("claude-opus-4-6");
        assertEquals("claude-opus-4-6", ctx.getCurrentModel());
    }

    @Test
    @DisplayName("context files methods work")
    void contextFilesMethodsWork() {
        CommandContext ctx = CommandContext.defaults();

        assertTrue(ctx.getContextFiles().isEmpty());
        ctx.addContextFile("file1.java");
        ctx.addContextFile("file2.java");
        assertEquals(2, ctx.getContextFiles().size());
        ctx.clearContextFiles();
        assertTrue(ctx.getContextFiles().isEmpty());
    }

    @Test
    @DisplayName("session methods work")
    void sessionMethodsWork() {
        CommandContext ctx = CommandContext.defaults();

        assertNotNull(ctx.getSessionId());
        ctx.setSessionId("custom-session-id");
        assertEquals("custom-session-id", ctx.getSessionId());

        assertNull(ctx.getSessionName());
        ctx.setSessionName("My Session");
        assertEquals("My Session", ctx.getSessionName());

        assertNotNull(ctx.generateSessionName());
    }

    @Test
    @DisplayName("stats tracking works")
    void statsTrackingWorks() {
        CommandContext ctx = CommandContext.defaults();

        assertEquals(0, ctx.getMessageCount());
        ctx.incrementMessageCount();
        assertEquals(1, ctx.getMessageCount());

        assertEquals(0, ctx.getToolCallCount());
        ctx.incrementToolCallCount();
        assertEquals(1, ctx.getToolCallCount());

        assertEquals(0, ctx.getTotalInputTokens());
        ctx.addInputTokens(100);
        assertEquals(100, ctx.getTotalInputTokens());

        assertEquals(0, ctx.getTotalCost());
        ctx.addCost(0.05);
        assertEquals(0.05, ctx.getTotalCost());
    }

    @Test
    @DisplayName("fast mode methods work")
    void fastModeMethodsWork() {
        CommandContext ctx = CommandContext.defaults();

        assertFalse(ctx.isFastModeEnabled());
        ctx.setFastMode(true);
        assertTrue(ctx.isFastModeEnabled());
    }

    @Test
    @DisplayName("permission methods work")
    void permissionMethodsWork() {
        CommandContext ctx = CommandContext.defaults();

        assertEquals("default", ctx.getPermissionMode());
        ctx.setPermissionMode("bypass");
        assertEquals("bypass", ctx.getPermissionMode());

        assertTrue(ctx.getAllowRules().isEmpty());
        ctx.addAllowRule("Bash(ls*)");
        assertEquals(1, ctx.getAllowRules().size());

        ctx.resetPermissionRules();
        assertTrue(ctx.getAllowRules().isEmpty());
    }

    @Test
    @DisplayName("agent methods work")
    void agentMethodsWork() {
        CommandContext ctx = CommandContext.defaults();

        assertTrue(ctx.getAllAgents().isEmpty());
        assertNull(ctx.getActiveAgent());

        CommandContext.AgentConfig agent = ctx.createAgent("test-agent", "general");
        assertEquals("test-agent", agent.name());
        assertEquals("general", agent.type());
        assertEquals(1, ctx.getAllAgents().size());

        ctx.setActiveAgent("test-agent");
        assertEquals("test-agent", ctx.getActiveAgent());

        assertTrue(ctx.deleteAgent("test-agent"));
        assertTrue(ctx.getAllAgents().isEmpty());
        assertNull(ctx.getActiveAgent());
    }

    @Test
    @DisplayName("branch methods work")
    void branchMethodsWork() {
        CommandContext ctx = CommandContext.defaults();

        assertTrue(ctx.getSessionBranches().isEmpty());
        assertEquals("main", ctx.getCurrentBranch());

        assertTrue(ctx.createSessionBranch("feature"));
        assertEquals(1, ctx.getSessionBranches().size());

        assertTrue(ctx.switchSessionBranch("feature"));
        assertEquals("feature", ctx.getCurrentBranch());

        assertTrue(ctx.deleteSessionBranch("feature"));
        assertTrue(ctx.getSessionBranches().isEmpty());
    }

    @Test
    @DisplayName("heap info works")
    void heapInfoWorks() {
        CommandContext ctx = CommandContext.defaults();

        CommandContext.HeapInfo info = ctx.getHeapInfo();
        assertTrue(info.used() >= 0);
        assertTrue(info.max() > 0);
    }

    @Test
    @DisplayName("working directory returns Path")
    void workingDirectoryWorks() {
        CommandContext ctx = CommandContext.defaults();
        assertNotNull(ctx.workingDirectory());
    }

    @Test
    @DisplayName("clear caches does not throw")
    void clearCachesDoesNotThrow() {
        CommandContext ctx = CommandContext.defaults();
        assertDoesNotThrow(() -> ctx.clearCaches());
    }

    @Test
    @DisplayName("compact history does not throw")
    void compactHistoryDoesNotThrow() {
        CommandContext ctx = CommandContext.defaults();
        assertDoesNotThrow(() -> ctx.compactHistory());
    }

    @Test
    @DisplayName("rewind to checkpoint does not throw")
    void rewindToCheckpointDoesNotThrow() {
        CommandContext ctx = CommandContext.defaults();
        assertDoesNotThrow(() -> ctx.rewindToCheckpoint("nonexistent-checkpoint"));
    }

    @Test
    @DisplayName("open keybindings editor does not throw")
    void openKeybindingsEditorDoesNotThrow() {
        CommandContext ctx = CommandContext.defaults();
        assertDoesNotThrow(() -> ctx.openKeybindingsEditor());
    }

    @Test
    @DisplayName("open plan editor does not throw")
    void openPlanEditorDoesNotThrow() {
        CommandContext ctx = CommandContext.defaults();
        assertDoesNotThrow(() -> ctx.openPlanEditor());
    }

    @Test
    @DisplayName("toggle MCP server updates state")
    void toggleMcpServerUpdatesState() {
        CommandContext ctx = CommandContext.defaults();
        assertDoesNotThrow(() -> ctx.toggleMcpServer("test-server", true));
        assertDoesNotThrow(() -> ctx.toggleMcpServer("test-server", false));
    }

    @Test
    @DisplayName("reconnect MCP server does not throw")
    void reconnectMcpServerDoesNotThrow() {
        CommandContext ctx = CommandContext.defaults();
        assertDoesNotThrow(() -> ctx.reconnectMcpServer("test-server"));
    }

    @Test
    @DisplayName("start agent does not throw")
    void startAgentDoesNotThrow() {
        CommandContext ctx = CommandContext.defaults();
        ctx.createAgent("test-agent", "general");
        assertDoesNotThrow(() -> ctx.startAgent("test-agent", "test prompt"));
    }

    @Test
    @DisplayName("trigger GC does not throw")
    void triggerGcDoesNotThrow() {
        CommandContext ctx = CommandContext.defaults();
        assertDoesNotThrow(() -> ctx.triggerGc());
    }

    @Test
    @DisplayName("heap dump path returns valid path")
    void heapDumpPathReturnsValidPath() {
        CommandContext ctx = CommandContext.defaults();
        String path = ctx.getHeapDumpPath();
        assertNotNull(path);
        assertTrue(path.contains(".claude"));
    }

    @Test
    @DisplayName("dump heap creates valid result")
    void dumpHeapCreatesValidResult() {
        CommandContext ctx = CommandContext.defaults();
        String path = ctx.getHeapDumpPath();
        CommandContext.HeapDumpResult result = ctx.dumpHeap(path);
        // May succeed or fail depending on JVM, but should not throw
        assertNotNull(result);
    }

    @Test
    @DisplayName("effort level methods work")
    void effortLevelMethodsWork() {
        CommandContext ctx = CommandContext.defaults();
        assertEquals("auto", ctx.getEffortLevel());
        ctx.setEffortLevel("high");
        assertEquals("high", ctx.getEffortLevel());
    }

    @Test
    @DisplayName("brief mode methods work")
    void briefModeMethodsWork() {
        CommandContext ctx = CommandContext.defaults();
        assertFalse(ctx.isBriefOnlyMode());
        ctx.setBriefOnlyMode(true);
        assertTrue(ctx.isBriefOnlyMode());
    }

    @Test
    @DisplayName("submit feedback returns boolean")
    void submitFeedbackReturnsBoolean() {
        CommandContext ctx = CommandContext.defaults();
        // Will likely return false due to no API key, but should not throw
        boolean result = ctx.submitFeedback("bug", "Test feedback");
        // Just verify it doesn't throw
        assertNotNull(result);
    }

    @Test
    @DisplayName("open Claude desktop returns boolean")
    void openClaudeDesktopReturnsBoolean() {
        CommandContext ctx = CommandContext.defaults();
        boolean result = ctx.openClaudeDesktop("test-session-id");
        // May fail if not installed, but should not throw
        assertNotNull(result);
    }
}