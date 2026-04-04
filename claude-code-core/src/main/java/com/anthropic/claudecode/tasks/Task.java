/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Task interface
 */
package com.anthropic.claudecode.tasks;

import java.util.concurrent.*;

/**
 * Task interface for background operations.
 */
public interface Task {

    /**
     * Get the task type.
     */
    TaskType getType();

    /**
     * Get the task name.
     */
    String getName();

    /**
     * Get the task description.
     */
    String getDescription();

    /**
     * Execute the task.
     */
    CompletableFuture<TaskResult> execute(TaskContext context);

    /**
     * Check if the task can run.
     */
    default boolean canRun(TaskContext context) {
        return true;
    }

    /**
     * Cancel the task.
     */
    default void cancel() {}

    /**
     * Get task priority (higher = more important).
     */
    default int getPriority() {
        return 0;
    }
}