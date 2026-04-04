/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code Task.ts
 */
package com.anthropic.claudecode;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Task types and state management.
 *
 * <p>Corresponds to Task.ts in the original Claude Code.
 */
public final class Task {

    private Task() {} // Utility class

    // ==================== Task Type Enum ====================

    public enum TaskType {
        LOCAL_BASH("b"),
        LOCAL_AGENT("a"),
        REMOTE_AGENT("r"),
        IN_PROCESS_TEAMMATE("t"),
        LOCAL_WORKFLOW("w"),
        MONITOR_MCP("m"),
        DREAM("d");

        private final String prefix;

        TaskType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    // ==================== Task Status Enum ====================

    public enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        KILLED
    }

    /**
     * Check if a status is terminal (no further transitions).
     */
    public static boolean isTerminalTaskStatus(TaskStatus status) {
        return status == TaskStatus.COMPLETED
            || status == TaskStatus.FAILED
            || status == TaskStatus.KILLED;
    }

    // ==================== Task State Base ====================

    /**
     * Base fields shared by all task states.
     */
    public record TaskStateBase(
            String id,
            TaskType type,
            TaskStatus status,
            String description,
            String toolUseId,
            long startTime,
            Long endTime,
            Long totalPausedMs,
            String outputFile,
            int outputOffset,
            boolean notified
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String id;
            private TaskType type;
            private TaskStatus status = TaskStatus.PENDING;
            private String description;
            private String toolUseId;
            private long startTime = System.currentTimeMillis();
            private Long endTime;
            private Long totalPausedMs;
            private String outputFile;
            private int outputOffset;
            private boolean notified;

            public Builder id(String id) { this.id = id; return this; }
            public Builder type(TaskType type) { this.type = type; return this; }
            public Builder status(TaskStatus status) { this.status = status; return this; }
            public Builder description(String description) { this.description = description; return this; }
            public Builder toolUseId(String toolUseId) { this.toolUseId = toolUseId; return this; }
            public Builder startTime(long startTime) { this.startTime = startTime; return this; }
            public Builder endTime(Long endTime) { this.endTime = endTime; return this; }
            public Builder totalPausedMs(Long totalPausedMs) { this.totalPausedMs = totalPausedMs; return this; }
            public Builder outputFile(String outputFile) { this.outputFile = outputFile; return this; }
            public Builder outputOffset(int outputOffset) { this.outputOffset = outputOffset; return this; }
            public Builder notified(boolean notified) { this.notified = notified; return this; }

            public TaskStateBase build() {
                return new TaskStateBase(id, type, status, description, toolUseId,
                        startTime, endTime, totalPausedMs, outputFile, outputOffset, notified);
            }
        }
    }

    // ==================== Task Handle ====================

    /**
     * Handle for a running task.
     */
    public record TaskHandle(String taskId, Runnable cleanup) {}

    // ==================== Task Context ====================

    /**
     * Context passed to task implementations.
     */
    public record TaskContext(
            Thread thread,
            Map<String, Object> appState,
            java.util.function.Consumer<Map<String, Object>> setAppState
    ) {
        public static TaskContext create() {
            return new TaskContext(
                    Thread.currentThread(),
                    new HashMap<>(),
                    state -> {}
            );
        }
    }

    // ==================== Local Shell Spawn Input ====================

    /**
     * Input for local shell spawn.
     */
    public record LocalShellSpawnInput(
            String command,
            String description,
            Integer timeout,
            String toolUseId,
            String agentId,
            String kind // "bash" or "monitor"
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String command;
            private String description;
            private Integer timeout;
            private String toolUseId;
            private String agentId;
            private String kind = "bash";

            public Builder command(String command) { this.command = command; return this; }
            public Builder description(String description) { this.description = description; return this; }
            public Builder timeout(Integer timeout) { this.timeout = timeout; return this; }
            public Builder toolUseId(String toolUseId) { this.toolUseId = toolUseId; return this; }
            public Builder agentId(String agentId) { this.agentId = agentId; return this; }
            public Builder kind(String kind) { this.kind = kind; return this; }

            public LocalShellSpawnInput build() {
                return new LocalShellSpawnInput(command, description, timeout, toolUseId, agentId, kind);
            }
        }
    }

    // ==================== Task ID Generation ====================

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TASK_ID_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";

    /**
     * Generate a unique task ID with type prefix.
     */
    public static String generateTaskId(TaskType type) {
        String prefix = type.getPrefix();
        StringBuilder id = new StringBuilder(prefix);
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        for (int i = 0; i < 8; i++) {
            id.append(TASK_ID_ALPHABET.charAt(bytes[i] & 0xff % TASK_ID_ALPHABET.length()));
        }
        return id.toString();
    }

    /**
     * Create a base task state with auto-generated ID.
     */
    public static TaskStateBase createTaskStateBase(TaskType type, String description, String toolUseId) {
        return TaskStateBase.builder()
                .id(generateTaskId(type))
                .type(type)
                .description(description)
                .toolUseId(toolUseId)
                .build();
    }
}