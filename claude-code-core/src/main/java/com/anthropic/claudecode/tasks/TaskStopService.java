/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tasks/stopTask.ts
 */
package com.anthropic.claudecode.tasks;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Task stop service - stops running tasks.
 */
public final class TaskStopService {
    private TaskStopService() {}

    /**
     * Stop task error.
     */
    public static class StopTaskError extends Exception {
        private final String code;

        public StopTaskError(String message, String code) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * Stop task result.
     */
    public record StopTaskResult(
        String taskId,
        String taskType,
        String command
    ) {}

    /**
     * Stop task context.
     */
    public interface StopTaskContext {
        Map<String, TaskState> getTasks();
        void updateTask(String taskId, UnaryOperator<TaskState> updater);
    }

    /**
     * Stop a running task.
     *
     * @param taskId Task ID to stop
     * @param context Task context
     * @return Stop result
     * @throws StopTaskError if task cannot be stopped
     */
    public static CompletableFuture<StopTaskResult> stopTask(
        String taskId,
        StopTaskContext context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, TaskState> tasks = context.getTasks();
            TaskState task = tasks.get(taskId);

            if (task == null) {
                throw new RuntimeException(new StopTaskError(
                    "No task found with ID: " + taskId,
                    "not_found"
                ));
            }

            if (task.status() != TaskState.TaskStatus.RUNNING) {
                throw new RuntimeException(new StopTaskError(
                    "Task " + taskId + " is not running (status: " + task.status() + ")",
                    "not_running"
                ));
            }

            // Get task implementation
            Task taskImpl = TaskRegistry.getTaskByType(task.type());
            if (taskImpl == null) {
                throw new RuntimeException(new StopTaskError(
                    "Unsupported task type: " + task.type(),
                    "unsupported_type"
                ));
            }

            // Kill the task
            taskImpl.cancel();

            // Mark as notified for shell tasks (suppress exit code 137)
            if (task instanceof TaskState.LocalShellTaskState shellTask) {
                if (!shellTask.notified()) {
                    context.updateTask(taskId, t -> {
                        if (t instanceof TaskState.LocalShellTaskState st) {
                            return new TaskState.LocalShellTaskState(
                                st.id(), st.type(), TaskState.TaskStatus.KILLED,
                                st.description(), st.toolUseId(), st.startTime(),
                                System.currentTimeMillis(), st.outputFile(),
                                st.outputOffset(), true, st.isBackgrounded(),
                                st.command(), st.pid()
                            );
                        }
                        return t;
                    });
                }
            }

            // Build result
            String command = task instanceof TaskState.LocalShellTaskState shellTask
                ? shellTask.command()
                : task.description();

            return new StopTaskResult(taskId, task.type().name(), command);
        });
    }

    /**
     * Stop all running tasks.
     */
    public static CompletableFuture<List<StopTaskResult>> stopAllTasks(
        StopTaskContext context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<StopTaskResult> results = new ArrayList<>();
            Map<String, TaskState> tasks = context.getTasks();

            for (String taskId : new ArrayList<>(tasks.keySet())) {
                TaskState task = tasks.get(taskId);
                if (task != null && task.status() == TaskState.TaskStatus.RUNNING) {
                    try {
                        results.add(stopTask(taskId, context).join());
                    } catch (Exception e) {
                        // Continue stopping other tasks
                    }
                }
            }

            return results;
        });
    }

    /**
     * Check if a task can be stopped.
     */
    public static boolean canStop(TaskState task) {
        if (task == null) return false;
        if (task.status() != TaskState.TaskStatus.RUNNING) return false;

        Task taskImpl = TaskRegistry.getTaskByType(task.type());
        return taskImpl != null;
    }

    /**
     * Get all stoppable tasks.
     */
    public static List<TaskState> getStoppableTasks(Map<String, TaskState> tasks) {
        List<TaskState> stoppable = new ArrayList<>();
        for (TaskState task : tasks.values()) {
            if (canStop(task)) {
                stoppable.add(task);
            }
        }
        return stoppable;
    }
}