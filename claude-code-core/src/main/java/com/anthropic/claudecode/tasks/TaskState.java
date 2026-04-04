/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tasks/types.ts
 */
package com.anthropic.claudecode.tasks;

/**
 * Task state interface - base for all task states.
 */
public sealed interface TaskState permits
    TaskState.LocalShellTaskState,
    TaskState.LocalAgentTaskState,
    TaskState.RemoteAgentTaskState,
    TaskState.DreamTaskState {

    /**
     * Get task ID.
     */
    String id();

    /**
     * Get task type.
     */
    TaskType type();

    /**
     * Get task status.
     */
    TaskStatus status();

    /**
     * Get task description.
     */
    String description();

    /**
     * Get tool use ID.
     */
    String toolUseId();

    /**
     * Check if task is backgrounded.
     */
    default boolean isBackgrounded() {
        return true;
    }

    /**
     * Check if task has been notified.
     */
    boolean notified();

    /**
     * Task status enum.
     */
    enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        KILLED
    }

    /**
     * Local shell task state.
     */
    record LocalShellTaskState(
        String id,
        TaskType type,
        TaskStatus status,
        String description,
        String toolUseId,
        long startTime,
        Long endTime,
        String outputFile,
        int outputOffset,
        boolean notified,
        boolean isBackgrounded,
        String command,
        int pid
    ) implements TaskState {}

    /**
     * Local agent task state.
     */
    record LocalAgentTaskState(
        String id,
        TaskType type,
        TaskStatus status,
        String description,
        String toolUseId,
        long startTime,
        Long endTime,
        String outputFile,
        int outputOffset,
        boolean notified,
        boolean isBackgrounded,
        String agentName
    ) implements TaskState {}

    /**
     * Remote agent task state.
     */
    record RemoteAgentTaskState(
        String id,
        TaskType type,
        TaskStatus status,
        String description,
        String toolUseId,
        long startTime,
        Long endTime,
        String outputFile,
        int outputOffset,
        boolean notified,
        boolean isBackgrounded,
        String serverUrl
    ) implements TaskState {}

    /**
     * Dream task state.
     */
    record DreamTaskState(
        String id,
        TaskType type,
        TaskStatus status,
        String description,
        String toolUseId,
        long startTime,
        Long endTime,
        String outputFile,
        int outputOffset,
        boolean notified,
        String dreamType
    ) implements TaskState {
        @Override
        public boolean isBackgrounded() {
            return true;
        }
    }
}