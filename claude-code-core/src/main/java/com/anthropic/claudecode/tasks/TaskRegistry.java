/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tasks.ts
 */
package com.anthropic.claudecode.tasks;

import java.util.*;

/**
 * Task registry - manages all available task types.
 */
public final class TaskRegistry {
    private TaskRegistry() {}

    private static final Map<TaskType, Task> tasks = new EnumMap<>(TaskType.class);

    static {
        // Register built-in tasks
        register(LocalShellTask.getInstance());
        register(LocalAgentTask.getInstance());
        register(RemoteAgentTask.getInstance());
        register(DreamTask.getInstance());
    }

    /**
     * Register a task.
     */
    public static void register(Task task) {
        tasks.put(task.getType(), task);
    }

    /**
     * Get all tasks.
     */
    public static List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * Get task by type.
     */
    public static Task getTaskByType(TaskType type) {
        return tasks.get(type);
    }

    /**
     * Check if task type is registered.
     */
    public static boolean hasTask(TaskType type) {
        return tasks.containsKey(type);
    }

    /**
     * Unregister a task.
     */
    public static void unregister(TaskType type) {
        tasks.remove(type);
    }

    /**
     * Clear all tasks.
     */
    public static void clear() {
        tasks.clear();
    }
}