/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/tasks.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import com.anthropic.claudecode.ToolUseContext;

/**
 * Task manager for creating and managing tasks.
 */
public final class TaskManager {
    private TaskManager() {}

    private static final Map<String, TaskList> taskLists = new ConcurrentHashMap<>();
    private static volatile String currentTaskListId = null;

    /**
     * Task record.
     */
    public record Task(
        String id,
        String subject,
        String description,
        String activeForm,
        String status,
        String owner,
        List<String> blocks,
        List<String> blockedBy,
        Map<String, Object> metadata
    ) {}

    /**
     * Task list record.
     */
    public record TaskList(
        String id,
        String name,
        List<Task> tasks
    ) {}

    /**
     * Hook result record.
     */
    public record HookResult(
        String blockingError,
        String message
    ) {}

    /**
     * Check if todo v2 is enabled.
     */
    public static boolean isTodoV2Enabled() {
        String env = System.getenv("CLAUDE_CODE_TODO_V2");
        return "true".equalsIgnoreCase(env) || "1".equals(env);
    }

    /**
     * Get or create task list id.
     */
    public static String getTaskListId() {
        if (currentTaskListId == null) {
            currentTaskListId = UUID.randomUUID().toString().substring(0, 8);
            taskLists.put(currentTaskListId, new TaskList(
                currentTaskListId,
                "Default",
                new CopyOnWriteArrayList<>()
            ));
        }
        return currentTaskListId;
    }

    /**
     * Create a task.
     */
    public static String createTask(String listId, Task task) {
        String taskId = task.id() != null ? task.id() : UUID.randomUUID().toString().substring(0, 8);
        Task newTask = new Task(
            taskId,
            task.subject(),
            task.description(),
            task.activeForm(),
            task.status() != null ? task.status() : "pending",
            task.owner(),
            task.blocks() != null ? task.blocks() : Collections.emptyList(),
            task.blockedBy() != null ? task.blockedBy() : Collections.emptyList(),
            task.metadata() != null ? task.metadata() : Collections.emptyMap()
        );

        TaskList list = taskLists.get(listId);
        if (list != null) {
            list.tasks().add(newTask);
        }

        return taskId;
    }

    /**
     * Delete a task.
     */
    public static boolean deleteTask(String listId, String taskId) {
        TaskList list = taskLists.get(listId);
        if (list != null) {
            return list.tasks().removeIf(t -> t.id().equals(taskId));
        }
        return false;
    }

    /**
     * Get task by id.
     */
    public static Task getTask(String listId, String taskId) {
        TaskList list = taskLists.get(listId);
        if (list != null) {
            for (Task task : list.tasks()) {
                if (task.id().equals(taskId)) {
                    return task;
                }
            }
        }
        return null;
    }

    /**
     * Get all tasks.
     */
    public static List<Task> getTasks(String listId) {
        TaskList list = taskLists.get(listId);
        return list != null ? new ArrayList<>(list.tasks()) : Collections.emptyList();
    }

    /**
     * Update task status.
     */
    public static boolean updateTaskStatus(String listId, String taskId, String status) {
        TaskList list = taskLists.get(listId);
        if (list != null) {
            for (int i = 0; i < list.tasks().size(); i++) {
                Task task = list.tasks().get(i);
                if (task.id().equals(taskId)) {
                    list.tasks().set(i, new Task(
                        task.id(),
                        task.subject(),
                        task.description(),
                        task.activeForm(),
                        status,
                        task.owner(),
                        task.blocks(),
                        task.blockedBy(),
                        task.metadata()
                    ));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Execute task created hooks.
     */
    public static List<HookResult> executeTaskCreatedHooks(
        String taskId,
        String subject,
        String description
    ) {
        List<HookResult> results = new ArrayList<>();

        try {
            // Load hooks from settings file
            String home = System.getProperty("user.home");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(home, ".claude", "settings.json");

            if (!java.nio.file.Files.exists(settingsPath)) {
                return results;
            }

            String content = java.nio.file.Files.readString(settingsPath);

            // Find taskCreated hooks section
            int hooksIdx = content.indexOf("\"hooks\"");
            if (hooksIdx < 0) return results;

            int taskCreatedIdx = content.indexOf("\"taskCreated\"", hooksIdx);
            if (taskCreatedIdx < 0) return results;

            // Find the hooks array
            int arrStart = content.indexOf("[", taskCreatedIdx);
            if (arrStart < 0) return results;

            int depth = 1;
            int arrEnd = arrStart + 1;
            while (arrEnd < content.length() && depth > 0) {
                char c = content.charAt(arrEnd);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                arrEnd++;
            }

            String hooksArray = content.substring(arrStart, arrEnd);

            // Execute each hook command
            int i = 0;
            while (i < hooksArray.length()) {
                int strStart = hooksArray.indexOf("\"", i);
                if (strStart < 0) break;
                int strEnd = hooksArray.indexOf("\"", strStart + 1);
                if (strEnd < 0) break;

                String hookCmd = hooksArray.substring(strStart + 1, strEnd);
                if (!hookCmd.isEmpty()) {
                    // Replace variables in hook command
                    hookCmd = hookCmd.replace("$TASK_ID", taskId != null ? taskId : "");
                    hookCmd = hookCmd.replace("$SUBJECT", subject != null ? subject : "");
                    hookCmd = hookCmd.replace("$DESCRIPTION", description != null ? description : "");

                    // Execute the hook
                    HookResult result = executeHookCommand(hookCmd);
                    results.add(result);
                }

                i = strEnd + 1;
            }
        } catch (Exception e) {
            results.add(new HookResult(e.getMessage(), null));
        }

        return results;
    }

    private static HookResult executeHookCommand(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(30, TimeUnit.SECONDS);

            String output = new String(process.getInputStream().readAllBytes());

            if (process.exitValue() == 0) {
                return new HookResult(null, output);
            } else {
                return new HookResult(output, null);
            }
        } catch (Exception e) {
            return new HookResult(e.getMessage(), null);
        }
    }

    /**
     * Claim a task.
     */
    public static void claimTask(String taskId) {
        String listId = getTaskListId();
        TaskList list = taskLists.get(listId);
        if (list != null) {
            for (int i = 0; i < list.tasks().size(); i++) {
                Task task = list.tasks().get(i);
                if (task.id().equals(taskId)) {
                    list.tasks().set(i, new Task(
                        task.id(),
                        task.subject(),
                        task.description(),
                        task.activeForm(),
                        "in_progress",
                        "current",
                        task.blocks(),
                        task.blockedBy(),
                        task.metadata()
                    ));
                    break;
                }
            }
        }
    }

    /**
     * Complete a task.
     */
    public static void completeTask(String taskId) {
        updateTaskStatus(getTaskListId(), taskId, "completed");
    }

    /**
     * Clear all tasks.
     */
    public static void clearTasks() {
        String listId = getTaskListId();
        TaskList list = taskLists.get(listId);
        if (list != null) {
            list.tasks().clear();
        }
    }

    /**
     * Add a block relationship between tasks.
     * The blocker task blocks the blocked task.
     */
    public static boolean addBlock(String listId, String blockerId, String blockedId) {
        TaskList list = taskLists.get(listId);
        if (list == null) return false;

        Task blockerTask = null;
        Task blockedTask = null;
        int blockerIndex = -1;
        int blockedIndex = -1;

        for (int i = 0; i < list.tasks().size(); i++) {
            Task t = list.tasks().get(i);
            if (t.id().equals(blockerId)) {
                blockerTask = t;
                blockerIndex = i;
            }
            if (t.id().equals(blockedId)) {
                blockedTask = t;
                blockedIndex = i;
            }
        }

        if (blockerTask == null || blockedTask == null) return false;

        // Add blockedId to blocker's blocks list
        List<String> blockerBlocks = new ArrayList<>(blockerTask.blocks());
        if (!blockerBlocks.contains(blockedId)) {
            blockerBlocks.add(blockedId);
            list.tasks().set(blockerIndex, new Task(
                blockerTask.id(),
                blockerTask.subject(),
                blockerTask.description(),
                blockerTask.activeForm(),
                blockerTask.status(),
                blockerTask.owner(),
                blockerBlocks,
                blockerTask.blockedBy(),
                blockerTask.metadata()
            ));
        }

        // Add blockerId to blocked's blockedBy list
        List<String> blockedBy = new ArrayList<>(blockedTask.blockedBy());
        if (!blockedBy.contains(blockerId)) {
            blockedBy.add(blockerId);
            list.tasks().set(blockedIndex, new Task(
                blockedTask.id(),
                blockedTask.subject(),
                blockedTask.description(),
                blockedTask.activeForm(),
                blockedTask.status(),
                blockedTask.owner(),
                blockedTask.blocks(),
                blockedBy,
                blockedTask.metadata()
            ));
        }

        return true;
    }

    /**
     * Update task with partial updates.
     */
    public static boolean updateTask(String listId, String taskId, TaskUpdate updates) {
        TaskList list = taskLists.get(listId);
        if (list == null) return false;

        for (int i = 0; i < list.tasks().size(); i++) {
            Task task = list.tasks().get(i);
            if (task.id().equals(taskId)) {
                Task updated = new Task(
                    task.id(),
                    updates.subject() != null ? updates.subject() : task.subject(),
                    updates.description() != null ? updates.description() : task.description(),
                    updates.activeForm() != null ? updates.activeForm() : task.activeForm(),
                    updates.status() != null ? updates.status() : task.status(),
                    updates.owner() != null ? updates.owner() : task.owner(),
                    task.blocks(),
                    task.blockedBy(),
                    updates.metadata() != null ? updates.metadata() : task.metadata()
                );
                list.tasks().set(i, updated);
                return true;
            }
        }
        return false;
    }

    /**
     * Task update record for partial updates.
     */
    public record TaskUpdate(
        String subject,
        String description,
        String activeForm,
        String status,
        String owner,
        Map<String, Object> metadata
    ) {}
}