/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.agentsummary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentSummaryService.
 */
class AgentSummaryServiceTest {

    private final AgentSummaryService service = new AgentSummaryService();

    @Test
    @DisplayName("AgentSummaryService AgentStatus enum")
    void agentStatusEnum() {
        AgentSummaryService.AgentStatus[] statuses = AgentSummaryService.AgentStatus.values();
        assertEquals(5, statuses.length);
        assertEquals(AgentSummaryService.AgentStatus.IDLE, AgentSummaryService.AgentStatus.valueOf("IDLE"));
        assertEquals(AgentSummaryService.AgentStatus.RUNNING, AgentSummaryService.AgentStatus.valueOf("RUNNING"));
    }

    @Test
    @DisplayName("AgentSummaryService TaskStatus enum")
    void taskStatusEnum() {
        AgentSummaryService.TaskStatus[] statuses = AgentSummaryService.TaskStatus.values();
        assertEquals(5, statuses.length);
        assertEquals(AgentSummaryService.TaskStatus.PENDING, AgentSummaryService.TaskStatus.valueOf("PENDING"));
        assertEquals(AgentSummaryService.TaskStatus.COMPLETED, AgentSummaryService.TaskStatus.valueOf("COMPLETED"));
    }

    @Test
    @DisplayName("AgentSummaryService AgentSummary getSuccessRate")
    void agentSummarySuccessRate() {
        AgentSummaryService.AgentSummary summary = new AgentSummaryService.AgentSummary(
            "agent-1", "TestAgent", "Test",
            AgentSummaryService.AgentStatus.RUNNING,
            10, 8, 2, 1000L, Duration.ofMinutes(5),
            List.of(), Map.of()
        );

        assertEquals(0.8, summary.getSuccessRate(), 0.01);
    }

    @Test
    @DisplayName("AgentSummaryService AgentSummary getSuccessRate zero tasks")
    void agentSummarySuccessRateZero() {
        AgentSummaryService.AgentSummary summary = new AgentSummaryService.AgentSummary(
            "agent-1", "TestAgent", "Test",
            AgentSummaryService.AgentStatus.IDLE,
            0, 0, 0, 0L, Duration.ZERO,
            List.of(), Map.of()
        );

        assertEquals(0.0, summary.getSuccessRate(), 0.01);
    }

    @Test
    @DisplayName("AgentSummaryService AgentSummary formatSummary")
    void agentSummaryFormat() {
        AgentSummaryService.AgentSummary summary = new AgentSummaryService.AgentSummary(
            "agent-1", "TestAgent", "Test",
            AgentSummaryService.AgentStatus.RUNNING,
            10, 8, 2, 1000L, Duration.ofMinutes(5),
            List.of(), Map.of()
        );

        String formatted = summary.formatSummary();
        assertTrue(formatted.contains("TestAgent"));
        assertTrue(formatted.contains("10 tasks"));
        assertTrue(formatted.contains("80.0%"));
    }

    @Test
    @DisplayName("AgentSummaryService TaskSummary getDuration")
    void taskSummaryDuration() {
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now();

        AgentSummaryService.TaskSummary task = new AgentSummaryService.TaskSummary(
            "task-1", "Test task", AgentSummaryService.TaskStatus.COMPLETED,
            start, end, "success"
        );

        Duration duration = task.getDuration();
        assertTrue(duration.toSeconds() >= 60);
    }

    @Test
    @DisplayName("AgentSummaryService SessionSummary getDuration")
    void sessionSummaryDuration() {
        Instant start = Instant.now().minusSeconds(120);

        AgentSummaryService.SessionSummary session = new AgentSummaryService.SessionSummary(
            "session-1", start, null, 10, 5, 1000L,
            List.of("bash", "read"), Map.of("bash", 3, "read", 2)
        );

        Duration duration = session.getDuration();
        assertTrue(duration.toSeconds() >= 120);
    }

    @Test
    @DisplayName("AgentSummaryService SessionSummary format")
    void sessionSummaryFormat() {
        AgentSummaryService.SessionSummary session = new AgentSummaryService.SessionSummary(
            "session-1", Instant.now(), Instant.now(), 10, 5, 1000L,
            List.of(), Map.of()
        );

        String formatted = session.format();
        assertTrue(formatted.contains("10 messages"));
        assertTrue(formatted.contains("5 tool calls"));
        assertTrue(formatted.contains("1000 tokens"));
    }

    @Test
    @DisplayName("AgentSummaryService ToolCall record")
    void toolCallRecord() {
        AgentSummaryService.ToolCall call = new AgentSummaryService.ToolCall(
            "bash", Map.of("command", "ls"), Instant.now(), true, 100L
        );

        assertEquals("bash", call.toolName());
        assertTrue(call.success());
        assertEquals(100L, call.durationMs());
    }

    @Test
    @DisplayName("AgentSummaryService generateSummary")
    void generateSummary() {
        AgentSummaryService.TaskSummary task = new AgentSummaryService.TaskSummary(
            "task-1", "Test", AgentSummaryService.TaskStatus.COMPLETED,
            Instant.now().minusSeconds(60), Instant.now(), "done"
        );

        AgentSummaryService.AgentSummary summary = service.generateSummary(
            "agent-1", "TestAgent", "Test agent",
            List.of(task), Map.of("key", "value")
        );

        assertEquals("agent-1", summary.agentId());
        assertEquals("TestAgent", summary.name());
        assertEquals(1, summary.taskCount());
        assertEquals(1, summary.completedTasks());
    }

    @Test
    @DisplayName("AgentSummaryService generateSessionSummary")
    void generateSessionSummary() {
        AgentSummaryService.ToolCall call = new AgentSummaryService.ToolCall(
            "bash", Map.of(), Instant.now(), true, 50L
        );

        AgentSummaryService.SessionSummary session = service.generateSessionSummary(
            "session-1", Instant.now().minusSeconds(60),
            List.of(call, call), 10, 1000L
        );

        assertEquals("session-1", session.sessionId());
        assertEquals(2, session.toolCalls());
        assertEquals(1, session.toolsUsed().size());
    }

    @Test
    @DisplayName("AgentSummaryService OverallStats format")
    void overallStatsFormat() {
        AgentSummaryService.OverallStats stats = new AgentSummaryService.OverallStats(
            5, 100, 80, 20, 50000L, Duration.ofMinutes(30)
        );

        String formatted = stats.format();
        assertTrue(formatted.contains("Agents: 5"));
        assertTrue(formatted.contains("Tasks: 100"));
        assertTrue(formatted.contains("Tokens: 50000"));
    }

    @Test
    @DisplayName("AgentSummaryService OverallStats empty")
    void overallStatsEmpty() {
        AgentSummaryService.OverallStats stats = AgentSummaryService.OverallStats.empty();

        assertEquals(0, stats.totalAgents());
        assertEquals(0, stats.totalTasks());
        assertEquals(Duration.ZERO, stats.totalDuration());
    }

    @Test
    @DisplayName("AgentSummaryService SummaryReport formatReport")
    void summaryReportFormat() {
        AgentSummaryService.AgentSummary agent = new AgentSummaryService.AgentSummary(
            "agent-1", "Test", "Test",
            AgentSummaryService.AgentStatus.IDLE, 0, 0, 0, 0L,
            Duration.ZERO, List.of(), Map.of()
        );

        AgentSummaryService.OverallStats stats = AgentSummaryService.OverallStats.empty();
        AgentSummaryService.SessionSummary session = new AgentSummaryService.SessionSummary(
            "session-1", Instant.now(), Instant.now(), 0, 0, 0L, List.of(), Map.of()
        );

        AgentSummaryService.SummaryReport report = new AgentSummaryService.SummaryReport(
            "report-1", Instant.now(), List.of(agent), session, stats
        );

        String formatted = report.formatReport();
        assertTrue(formatted.contains("# Summary Report"));
        assertTrue(formatted.contains("## Agents"));
    }
}