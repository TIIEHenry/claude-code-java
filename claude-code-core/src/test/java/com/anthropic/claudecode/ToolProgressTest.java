/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolProgress.
 */
class ToolProgressTest {

    @Test
    @DisplayName("ToolProgress record creates instance")
    void recordCreatesInstance() {
        ToolProgressData.Default data = new ToolProgressData.Default("test message");
        ToolProgress<ToolProgressData.Default> progress = new ToolProgress<>("tool-123", data);

        assertEquals("tool-123", progress.toolUseId());
        assertEquals(data, progress.data());
    }

    @Test
    @DisplayName("ToolProgress of creates instance")
    void ofCreatesInstance() {
        ToolProgressData.Default data = new ToolProgressData.Default("test message");
        ToolProgress<ToolProgressData.Default> progress = ToolProgress.of("tool-123", data);

        assertEquals("tool-123", progress.toolUseId());
        assertEquals(data, progress.data());
    }

    @Test
    @DisplayName("ToolProgress with AgentToolProgress")
    void withAgentToolProgress() {
        ToolProgressData.AgentToolProgress agentProgress =
            new ToolProgressData.AgentToolProgress("agent-1", "running", "Working on task");
        ToolProgress<ToolProgressData.AgentToolProgress> progress = ToolProgress.of("tool-456", agentProgress);

        assertEquals("tool-456", progress.toolUseId());
        assertEquals("agent-1", progress.data().agentId());
        assertEquals("running", progress.data().status());
        assertEquals("Working on task", progress.data().description());
        assertEquals("agent_tool", progress.data().type());
    }

    @Test
    @DisplayName("ToolProgress with BashProgress")
    void withBashProgress() {
        ToolProgressData.BashProgress bashProgress =
            new ToolProgressData.BashProgress("ls -la", "output here", 0);
        ToolProgress<ToolProgressData.BashProgress> progress = ToolProgress.of("tool-789", bashProgress);

        assertEquals("tool-789", progress.toolUseId());
        assertEquals("ls -la", progress.data().command());
        assertEquals("output here", progress.data().output());
        assertEquals(0, progress.data().exitCode());
        assertEquals("bash", progress.data().type());
    }

    @Test
    @DisplayName("ToolProgress with MCPProgress")
    void withMCPProgress() {
        ToolProgressData.MCPProgress mcpProgress =
            new ToolProgressData.MCPProgress("server-1", "tool-name", "completed");
        ToolProgress<ToolProgressData.MCPProgress> progress = ToolProgress.of("tool-101", mcpProgress);

        assertEquals("tool-101", progress.toolUseId());
        assertEquals("server-1", progress.data().serverName());
        assertEquals("tool-name", progress.data().toolName());
        assertEquals("completed", progress.data().status());
        assertEquals("mcp", progress.data().type());
    }

    @Test
    @DisplayName("ToolProgress with REPLToolProgress")
    void withREPLToolProgress() {
        ToolProgressData.REPLToolProgress replProgress =
            new ToolProgressData.REPLToolProgress("executing", "output");
        ToolProgress<ToolProgressData.REPLToolProgress> progress = ToolProgress.of("tool-202", replProgress);

        assertEquals("tool-202", progress.toolUseId());
        assertEquals("executing", progress.data().status());
        assertEquals("output", progress.data().output());
        assertEquals("repl", progress.data().type());
    }

    @Test
    @DisplayName("ToolProgress with SkillToolProgress")
    void withSkillToolProgress() {
        ToolProgressData.SkillToolProgress skillProgress =
            new ToolProgressData.SkillToolProgress("skill-name", "running");
        ToolProgress<ToolProgressData.SkillToolProgress> progress = ToolProgress.of("tool-303", skillProgress);

        assertEquals("tool-303", progress.toolUseId());
        assertEquals("skill-name", progress.data().skillName());
        assertEquals("running", progress.data().status());
        assertEquals("skill", progress.data().type());
    }

    @Test
    @DisplayName("ToolProgress with TaskOutputProgress")
    void withTaskOutputProgress() {
        ToolProgressData.TaskOutputProgress taskProgress =
            new ToolProgressData.TaskOutputProgress("task-1", "done", "result output");
        ToolProgress<ToolProgressData.TaskOutputProgress> progress = ToolProgress.of("tool-404", taskProgress);

        assertEquals("tool-404", progress.toolUseId());
        assertEquals("task-1", progress.data().taskId());
        assertEquals("done", progress.data().status());
        assertEquals("result output", progress.data().output());
        assertEquals("task_output", progress.data().type());
    }

    @Test
    @DisplayName("ToolProgress with WebSearchProgress")
    void withWebSearchProgress() {
        ToolProgressData.WebSearchProgress webProgress =
            new ToolProgressData.WebSearchProgress("search query", 5);
        ToolProgress<ToolProgressData.WebSearchProgress> progress = ToolProgress.of("tool-505", webProgress);

        assertEquals("tool-505", progress.toolUseId());
        assertEquals("search query", progress.data().query());
        assertEquals(5, progress.data().results());
        assertEquals("web_search", progress.data().type());
    }

    @Test
    @DisplayName("ToolProgress with Default")
    void withDefault() {
        ToolProgressData.Default defaultProgress = new ToolProgressData.Default("default message");
        ToolProgress<ToolProgressData.Default> progress = ToolProgress.of("tool-606", defaultProgress);

        assertEquals("tool-606", progress.toolUseId());
        assertEquals("default message", progress.data().message());
        assertEquals("default", progress.data().type());
    }
}