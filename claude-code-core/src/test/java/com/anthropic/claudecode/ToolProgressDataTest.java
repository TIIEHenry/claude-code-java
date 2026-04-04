/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolProgressData interface and its implementations.
 */
class ToolProgressDataTest {

    @Test
    @DisplayName("ToolProgressData default type returns progress")
    void defaultType() {
        ToolProgressData data = new ToolProgressData.Default("message");
        assertEquals("progress", data.type());
    }

    @Test
    @DisplayName("ToolProgressData.AgentToolProgress record")
    void agentToolProgressRecord() {
        ToolProgressData.AgentToolProgress progress =
            new ToolProgressData.AgentToolProgress("agent-id", "running", "description");

        assertEquals("agent-id", progress.agentId());
        assertEquals("running", progress.status());
        assertEquals("description", progress.description());
        assertEquals("agent_tool", progress.type());
    }

    @Test
    @DisplayName("ToolProgressData.BashProgress record")
    void bashProgressRecord() {
        ToolProgressData.BashProgress progress =
            new ToolProgressData.BashProgress("ls -la", "output", 0);

        assertEquals("ls -la", progress.command());
        assertEquals("output", progress.output());
        assertEquals(0, progress.exitCode());
        assertEquals("bash", progress.type());
    }

    @Test
    @DisplayName("ToolProgressData.BashProgress with non-zero exit code")
    void bashProgressNonZeroExit() {
        ToolProgressData.BashProgress progress =
            new ToolProgressData.BashProgress("fail-command", "error", 1);

        assertEquals(1, progress.exitCode());
    }

    @Test
    @DisplayName("ToolProgressData.MCPProgress record")
    void mcpProgressRecord() {
        ToolProgressData.MCPProgress progress =
            new ToolProgressData.MCPProgress("server", "tool", "pending");

        assertEquals("server", progress.serverName());
        assertEquals("tool", progress.toolName());
        assertEquals("pending", progress.status());
        assertEquals("mcp", progress.type());
    }

    @Test
    @DisplayName("ToolProgressData.REPLToolProgress record")
    void replToolProgressRecord() {
        ToolProgressData.REPLToolProgress progress =
            new ToolProgressData.REPLToolProgress("waiting", "repl output");

        assertEquals("waiting", progress.status());
        assertEquals("repl output", progress.output());
        assertEquals("repl", progress.type());
    }

    @Test
    @DisplayName("ToolProgressData.SkillToolProgress record")
    void skillToolProgressRecord() {
        ToolProgressData.SkillToolProgress progress =
            new ToolProgressData.SkillToolProgress("skill-name", "active");

        assertEquals("skill-name", progress.skillName());
        assertEquals("active", progress.status());
        assertEquals("skill", progress.type());
    }

    @Test
    @DisplayName("ToolProgressData.TaskOutputProgress record")
    void taskOutputProgressRecord() {
        ToolProgressData.TaskOutputProgress progress =
            new ToolProgressData.TaskOutputProgress("task-123", "completed", "task result");

        assertEquals("task-123", progress.taskId());
        assertEquals("completed", progress.status());
        assertEquals("task result", progress.output());
        assertEquals("task_output", progress.type());
    }

    @Test
    @DisplayName("ToolProgressData.WebSearchProgress record")
    void webSearchProgressRecord() {
        ToolProgressData.WebSearchProgress progress =
            new ToolProgressData.WebSearchProgress("query text", 10);

        assertEquals("query text", progress.query());
        assertEquals(10, progress.results());
        assertEquals("web_search", progress.type());
    }

    @Test
    @DisplayName("ToolProgressData.WebSearchProgress zero results")
    void webSearchProgressZeroResults() {
        ToolProgressData.WebSearchProgress progress =
            new ToolProgressData.WebSearchProgress("no results query", 0);

        assertEquals(0, progress.results());
    }

    @Test
    @DisplayName("ToolProgressData.Default record")
    void defaultRecord() {
        ToolProgressData.Default progress = new ToolProgressData.Default("default message");

        assertEquals("default message", progress.message());
        assertEquals("default", progress.type());
    }

    @Test
    @DisplayName("ToolProgressData.Default empty message")
    void defaultEmptyMessage() {
        ToolProgressData.Default progress = new ToolProgressData.Default("");

        assertEquals("", progress.message());
    }

    @Test
    @DisplayName("ToolProgressData all types are different")
    void allTypesDifferent() {
        ToolProgressData agent = new ToolProgressData.AgentToolProgress("id", "s", "d");
        ToolProgressData bash = new ToolProgressData.BashProgress("cmd", "out", 0);
        ToolProgressData mcp = new ToolProgressData.MCPProgress("srv", "tool", "st");
        ToolProgressData repl = new ToolProgressData.REPLToolProgress("st", "out");
        ToolProgressData skill = new ToolProgressData.SkillToolProgress("name", "st");
        ToolProgressData task = new ToolProgressData.TaskOutputProgress("id", "st", "out");
        ToolProgressData web = new ToolProgressData.WebSearchProgress("q", 0);
        ToolProgressData defaultP = new ToolProgressData.Default("msg");

        assertEquals("agent_tool", agent.type());
        assertEquals("bash", bash.type());
        assertEquals("mcp", mcp.type());
        assertEquals("repl", repl.type());
        assertEquals("skill", skill.type());
        assertEquals("task_output", task.type());
        assertEquals("web_search", web.type());
        assertEquals("default", defaultP.type());
    }
}