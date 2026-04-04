/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/agentSummary
 */
package com.anthropic.claudecode.services.agentsummary;

import java.util.*;
import java.time.*;

/**
 * Agent summary service - Generate agent summaries.
 */
public final class AgentSummaryService {

    /**
     * Agent summary record.
     */
    public record AgentSummary(
        String agentId,
        String name,
        String description,
        AgentStatus status,
        int taskCount,
        int completedTasks,
        int failedTasks,
        long totalTokens,
        Duration totalDuration,
        List<TaskSummary> recentTasks,
        Map<String, Object> metadata
    ) {
        public double getSuccessRate() {
            if (taskCount == 0) return 0.0;
            return (double) completedTasks / taskCount;
        }

        public String formatSummary() {
            return String.format("%s [%s]: %d tasks, %.1f%% success",
                name, status, taskCount, getSuccessRate() * 100);
        }
    }

    /**
     * Agent status enum.
     */
    public enum AgentStatus {
        IDLE,
        RUNNING,
        PAUSED,
        ERROR,
        COMPLETED
    }

    /**
     * Task summary record.
     */
    public record TaskSummary(
        String taskId,
        String description,
        TaskStatus status,
        Instant startTime,
        Instant endTime,
        String result
    ) {
        public Duration getDuration() {
            if (endTime == null) return Duration.between(startTime, Instant.now());
            return Duration.between(startTime, endTime);
        }
    }

    /**
     * Task status enum.
     */
    public enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Session summary record.
     */
    public record SessionSummary(
        String sessionId,
        Instant startTime,
        Instant endTime,
        int messageCount,
        int toolCalls,
        long totalTokens,
        List<String> toolsUsed,
        Map<String, Integer> toolCallCounts
    ) {
        public Duration getDuration() {
            if (endTime == null) return Duration.between(startTime, Instant.now());
            return Duration.between(startTime, endTime);
        }

        public String format() {
            return String.format("Session: %d messages, %d tool calls, %d tokens",
                messageCount, toolCalls, totalTokens);
        }
    }

    /**
     * Generate agent summary.
     */
    public AgentSummary generateSummary(
        String agentId,
        String name,
        String description,
        List<TaskSummary> tasks,
        Map<String, Object> metadata
    ) {
        int taskCount = tasks.size();
        int completed = (int) tasks.stream().filter(t -> t.status() == TaskStatus.COMPLETED).count();
        int failed = (int) tasks.stream().filter(t -> t.status() == TaskStatus.FAILED).count();

        long totalTokens = 0;
        Duration totalDuration = Duration.ZERO;
        List<TaskSummary> recent = new ArrayList<>();

        for (TaskSummary task : tasks) {
            totalDuration = totalDuration.plus(task.getDuration());
            if (recent.size() < 10) {
                recent.add(task);
            }
        }

        return new AgentSummary(
            agentId,
            name,
            description,
            tasks.isEmpty() ? AgentStatus.IDLE : AgentStatus.RUNNING,
            taskCount,
            completed,
            failed,
            totalTokens,
            totalDuration,
            recent,
            metadata
        );
    }

    /**
     * Generate session summary.
     */
    public SessionSummary generateSessionSummary(
        String sessionId,
        Instant startTime,
        List<ToolCall> toolCalls,
        int messageCount,
        long totalTokens
    ) {
        Map<String, Integer> callCounts = new HashMap<>();
        Set<String> toolsUsed = new HashSet<>();

        for (ToolCall call : toolCalls) {
            toolsUsed.add(call.toolName());
            callCounts.merge(call.toolName(), 1, Integer::sum);
        }

        return new SessionSummary(
            sessionId,
            startTime,
            Instant.now(),
            messageCount,
            toolCalls.size(),
            totalTokens,
            new ArrayList<>(toolsUsed),
            callCounts
        );
    }

    /**
     * Tool call record.
     */
    public record ToolCall(
        String toolName,
        Map<String, Object> input,
        Instant timestamp,
        boolean success,
        long durationMs
    ) {}

    /**
     * Summary report record.
     */
    public record SummaryReport(
        String reportId,
        Instant generatedAt,
        List<AgentSummary> agents,
        SessionSummary session,
        OverallStats stats
    ) {
        public String formatReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("# Summary Report\n\n");
            sb.append("Generated: ").append(generatedAt).append("\n\n");

            sb.append("## Overall Stats\n");
            sb.append(stats.format()).append("\n\n");

            sb.append("## Agents\n");
            for (AgentSummary agent : agents) {
                sb.append("- ").append(agent.formatSummary()).append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * Overall stats record.
     */
    public record OverallStats(
        int totalAgents,
        int totalTasks,
        int completedTasks,
        int failedTasks,
        long totalTokens,
        Duration totalDuration
    ) {
        public String format() {
            return String.format(
                "Agents: %d | Tasks: %d (%d completed, %d failed) | Tokens: %d | Duration: %s",
                totalAgents, totalTasks, completedTasks, failedTasks,
                totalTokens, formatDuration(totalDuration)
            );
        }

        private String formatDuration(Duration d) {
            long hours = d.toHours();
            long minutes = d.toMinutesPart();
            if (hours > 0) {
                return String.format("%dh %dm", hours, minutes);
            }
            return String.format("%dm", minutes);
        }

        public static OverallStats empty() {
            return new OverallStats(0, 0, 0, 0, 0, Duration.ZERO);
        }
    }
}