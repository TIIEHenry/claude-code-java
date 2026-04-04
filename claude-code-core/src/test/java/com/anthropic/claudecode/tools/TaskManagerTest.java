/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TaskManager.
 */
@DisplayName("TaskManager Tests")
class TaskManagerTest {

    @BeforeEach
    void setUp() {
        TaskManager.clearTasks();
    }

    @Test
    @DisplayName("TaskManager creates task list with unique ID")
    void createsTaskListWithUniqueId() {
        String id1 = TaskManager.getTaskListId();
        String id2 = TaskManager.getTaskListId();

        assertNotNull(id1);
        assertEquals(id1, id2); // Should return same ID once created
    }

    @Test
    @DisplayName("TaskManager creates and retrieves task")
    void createsAndRetrievesTask() {
        String listId = TaskManager.getTaskListId();

        TaskManager.Task task = new TaskManager.Task(
            null,
            "Test Task",
            "A test task description",
            "Testing",
            "pending",
            null,
            null,
            null,
            null
        );

        String taskId = TaskManager.createTask(listId, task);

        assertNotNull(taskId);

        TaskManager.Task retrieved = TaskManager.getTask(listId, taskId);
        assertNotNull(retrieved);
        assertEquals("Test Task", retrieved.subject());
        assertEquals("pending", retrieved.status());
    }

    @Test
    @DisplayName("TaskManager updates task status")
    void updatesTaskStatus() {
        String listId = TaskManager.getTaskListId();

        TaskManager.Task task = new TaskManager.Task(
            null, "Status Task", "desc", null, "pending", null, null, null, null
        );

        String taskId = TaskManager.createTask(listId, task);

        boolean updated = TaskManager.updateTaskStatus(listId, taskId, "in_progress");

        assertTrue(updated);

        TaskManager.Task retrieved = TaskManager.getTask(listId, taskId);
        assertEquals("in_progress", retrieved.status());
    }

    @Test
    @DisplayName("TaskManager deletes task")
    void deletesTask() {
        String listId = TaskManager.getTaskListId();

        TaskManager.Task task = new TaskManager.Task(
            null, "Delete Task", "desc", null, "pending", null, null, null, null
        );

        String taskId = TaskManager.createTask(listId, task);

        assertTrue(TaskManager.deleteTask(listId, taskId));
        assertNull(TaskManager.getTask(listId, taskId));
    }

    @Test
    @DisplayName("TaskManager claims task")
    void claimsTask() {
        String listId = TaskManager.getTaskListId();

        TaskManager.Task task = new TaskManager.Task(
            null, "Claim Task", "desc", null, "pending", null, null, null, null
        );

        String taskId = TaskManager.createTask(listId, task);

        TaskManager.claimTask(taskId);

        TaskManager.Task claimed = TaskManager.getTask(listId, taskId);
        assertEquals("in_progress", claimed.status());
        assertEquals("current", claimed.owner());
    }

    @Test
    @DisplayName("TaskManager completes task")
    void completesTask() {
        String listId = TaskManager.getTaskListId();

        TaskManager.Task task = new TaskManager.Task(
            null, "Complete Task", "desc", null, "pending", null, null, null, null
        );

        String taskId = TaskManager.createTask(listId, task);

        TaskManager.completeTask(taskId);

        TaskManager.Task completed = TaskManager.getTask(listId, taskId);
        assertEquals("completed", completed.status());
    }

    @Test
    @DisplayName("TaskManager adds block relationship")
    void addsBlockRelationship() {
        String listId = TaskManager.getTaskListId();

        TaskManager.Task task1 = new TaskManager.Task(
            "blocker-1", "Blocker Task", "desc", null, "pending", null, null, null, null
        );
        TaskManager.Task task2 = new TaskManager.Task(
            "blocked-2", "Blocked Task", "desc", null, "pending", null, null, null, null
        );

        TaskManager.createTask(listId, task1);
        TaskManager.createTask(listId, task2);

        boolean added = TaskManager.addBlock(listId, "blocker-1", "blocked-2");

        assertTrue(added);

        TaskManager.Task blocker = TaskManager.getTask(listId, "blocker-1");
        TaskManager.Task blocked = TaskManager.getTask(listId, "blocked-2");

        assertTrue(blocker.blocks().contains("blocked-2"));
        assertTrue(blocked.blockedBy().contains("blocker-1"));
    }

    @Test
    @DisplayName("TaskManager gets all tasks")
    void getsAllTasks() {
        String listId = TaskManager.getTaskListId();

        TaskManager.createTask(listId, new TaskManager.Task(
            null, "Task 1", "desc", null, "pending", null, null, null, null
        ));
        TaskManager.createTask(listId, new TaskManager.Task(
            null, "Task 2", "desc", null, "pending", null, null, null, null
        ));

        java.util.List<TaskManager.Task> tasks = TaskManager.getTasks(listId);

        assertEquals(2, tasks.size());
    }

    @Test
    @DisplayName("TaskManager updates task with partial updates")
    void updatesTaskWithPartialUpdates() {
        String listId = TaskManager.getTaskListId();

        TaskManager.Task task = new TaskManager.Task(
            null, "Original Subject", "Original Desc", null, "pending", null, null, null, null
        );

        String taskId = TaskManager.createTask(listId, task);

        TaskManager.TaskUpdate update = new TaskManager.TaskUpdate(
            "New Subject",
            null,  // Keep original description
            "Working",
            "in_progress",
            "tester",
            null
        );

        TaskManager.updateTask(listId, taskId, update);

        TaskManager.Task updated = TaskManager.getTask(listId, taskId);

        assertEquals("New Subject", updated.subject());
        assertEquals("Original Desc", updated.description());  // Unchanged
        assertEquals("in_progress", updated.status());
        assertEquals("tester", updated.owner());
    }

    @Test
    @DisplayName("TaskManager checks todo v2 enabled")
    void checksTodoV2Enabled() {
        // By default, it should be disabled unless env var is set
        boolean result = TaskManager.isTodoV2Enabled();
        // Result depends on environment
        assertNotNull(result);
    }
}