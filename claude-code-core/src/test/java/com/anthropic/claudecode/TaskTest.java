/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Task types and state management.
 */
class TaskTest {

    @Test
    @DisplayName("TaskType enum has all expected values")
    void taskTypeHasAllValues() {
        Task.TaskType[] types = Task.TaskType.values();
        assertEquals(7, types.length);

        // Verify prefixes
        assertEquals("b", Task.TaskType.LOCAL_BASH.getPrefix());
        assertEquals("a", Task.TaskType.LOCAL_AGENT.getPrefix());
        assertEquals("r", Task.TaskType.REMOTE_AGENT.getPrefix());
        assertEquals("t", Task.TaskType.IN_PROCESS_TEAMMATE.getPrefix());
        assertEquals("w", Task.TaskType.LOCAL_WORKFLOW.getPrefix());
        assertEquals("m", Task.TaskType.MONITOR_MCP.getPrefix());
        assertEquals("d", Task.TaskType.DREAM.getPrefix());
    }

    @Test
    @DisplayName("TaskStatus enum has all expected values")
    void taskStatusHasAllValues() {
        Task.TaskStatus[] statuses = Task.TaskStatus.values();
        assertEquals(5, statuses.length);

        assertEquals(Task.TaskStatus.PENDING, statuses[0]);
        assertEquals(Task.TaskStatus.RUNNING, statuses[1]);
        assertEquals(Task.TaskStatus.COMPLETED, statuses[2]);
        assertEquals(Task.TaskStatus.FAILED, statuses[3]);
        assertEquals(Task.TaskStatus.KILLED, statuses[4]);
    }

    @Test
    @DisplayName("isTerminalTaskStatus identifies terminal states")
    void isTerminalStatusWorks() {
        assertFalse(Task.isTerminalTaskStatus(Task.TaskStatus.PENDING));
        assertFalse(Task.isTerminalTaskStatus(Task.TaskStatus.RUNNING));
        assertTrue(Task.isTerminalTaskStatus(Task.TaskStatus.COMPLETED));
        assertTrue(Task.isTerminalTaskStatus(Task.TaskStatus.FAILED));
        assertTrue(Task.isTerminalTaskStatus(Task.TaskStatus.KILLED));
    }

    @Test
    @DisplayName("generateTaskId creates unique IDs with correct prefix")
    void generateTaskIdWorks() {
        String bashId = Task.generateTaskId(Task.TaskType.LOCAL_BASH);
        assertTrue(bashId.startsWith("b"));
        assertEquals(9, bashId.length()); // prefix + 8 chars

        String agentId = Task.generateTaskId(Task.TaskType.LOCAL_AGENT);
        assertTrue(agentId.startsWith("a"));
        assertEquals(9, agentId.length());

        // IDs should be unique
        String anotherBashId = Task.generateTaskId(Task.TaskType.LOCAL_BASH);
        assertNotEquals(bashId, anotherBashId);
    }

    @Test
    @DisplayName("TaskStateBase builder creates valid state")
    void taskStateBaseBuilderWorks() {
        long now = System.currentTimeMillis();

        Task.TaskStateBase state = Task.TaskStateBase.builder()
            .id("b12345678")
            .type(Task.TaskType.LOCAL_BASH)
            .status(Task.TaskStatus.RUNNING)
            .description("Test task")
            .toolUseId("tool-123")
            .startTime(now)
            .build();

        assertEquals("b12345678", state.id());
        assertEquals(Task.TaskType.LOCAL_BASH, state.type());
        assertEquals(Task.TaskStatus.RUNNING, state.status());
        assertEquals("Test task", state.description());
        assertEquals("tool-123", state.toolUseId());
        assertEquals(now, state.startTime());
        assertNull(state.endTime());
    }

    @Test
    @DisplayName("createTaskStateBase generates ID automatically")
    void createTaskStateBaseAutoId() {
        Task.TaskStateBase state = Task.createTaskStateBase(
            Task.TaskType.LOCAL_AGENT,
            "Agent task",
            "tool-456"
        );

        assertTrue(state.id().startsWith("a"));
        assertEquals(Task.TaskType.LOCAL_AGENT, state.type());
        assertEquals(Task.TaskStatus.PENDING, state.status());
        assertEquals("Agent task", state.description());
        assertEquals("tool-456", state.toolUseId());
        assertTrue(state.startTime() > 0);
    }

    @Test
    @DisplayName("LocalShellSpawnInput builder works")
    void localShellSpawnInputBuilderWorks() {
        Task.LocalShellSpawnInput input = Task.LocalShellSpawnInput.builder()
            .command("ls -la")
            .description("List files")
            .timeout(30000)
            .toolUseId("bash-789")
            .kind("bash")
            .build();

        assertEquals("ls -la", input.command());
        assertEquals("List files", input.description());
        assertEquals(30000, input.timeout());
        assertEquals("bash-789", input.toolUseId());
        assertEquals("bash", input.kind());
    }

    @Test
    @DisplayName("TaskContext create provides defaults")
    void taskContextCreateWorks() {
        Task.TaskContext ctx = Task.TaskContext.create();

        assertNotNull(ctx.thread());
        assertNotNull(ctx.appState());
        assertNotNull(ctx.setAppState());
    }
}