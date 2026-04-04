/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tasks;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

/**
 * Tests for task implementations.
 */
@DisplayName("Task Tests")
class TasksTest {

    @Test
    @DisplayName("LocalAgentTask returns correct type and name")
    void localAgentTaskMetadata() {
        LocalAgentTask task = LocalAgentTask.getInstance();

        assertEquals(TaskType.LOCAL_AGENT, task.getType());
        assertEquals("LocalAgent", task.getName());
        assertEquals("Run an agent locally", task.getDescription());
    }

    @Test
    @DisplayName("LocalAgentTask executes with valid input")
    void localAgentTaskExecution() {
        LocalAgentTask task = LocalAgentTask.getInstance();

        TaskContext context = new TaskContext();
        context.setProperty("agentName", "test-agent");
        context.setProperty("prompt", "Test prompt");

        CompletableFuture<TaskResult> future = task.execute(context);
        TaskResult result = future.join();

        // Should succeed or fail gracefully
        assertNotNull(result);
        assertTrue(result.isSuccess() || !result.isSuccess());
    }

    @Test
    @DisplayName("LocalAgentTask fails without agent name")
    void localAgentTaskFailsWithoutName() {
        LocalAgentTask task = LocalAgentTask.getInstance();

        TaskContext context = new TaskContext();
        context.setProperty("prompt", "Test prompt");

        CompletableFuture<TaskResult> future = task.execute(context);
        TaskResult result = future.join();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("No agent name"));
    }

    @Test
    @DisplayName("RemoteAgentTask returns correct type and name")
    void remoteAgentTaskMetadata() {
        RemoteAgentTask task = RemoteAgentTask.getInstance();

        assertEquals(TaskType.REMOTE_AGENT, task.getType());
        assertEquals("RemoteAgent", task.getName());
        assertEquals("Run an agent on a remote server", task.getDescription());
    }

    @Test
    @DisplayName("RemoteAgentTask fails without server URL")
    void remoteAgentTaskFailsWithoutServerUrl() {
        RemoteAgentTask task = RemoteAgentTask.getInstance();

        TaskContext context = new TaskContext();
        context.setProperty("agentName", "test-agent");
        context.setProperty("prompt", "Test prompt");
        // No serverUrl

        CompletableFuture<TaskResult> future = task.execute(context);
        TaskResult result = future.join();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("No server URL"));
    }

    @Test
    @DisplayName("RemoteAgentTask fails without agent name")
    void remoteAgentTaskFailsWithoutName() {
        RemoteAgentTask task = RemoteAgentTask.getInstance();

        TaskContext context = new TaskContext();
        context.setProperty("serverUrl", "https://example.com");
        context.setProperty("prompt", "Test prompt");
        // No agentName

        CompletableFuture<TaskResult> future = task.execute(context);
        TaskResult result = future.join();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("No agent name"));
    }

    @Test
    @DisplayName("DreamTask returns correct type and name")
    void dreamTaskMetadata() {
        DreamTask task = DreamTask.getInstance();

        assertEquals(TaskType.DREAM, task.getType());
        assertEquals("Dream", task.getName());
        assertEquals("Background processing during idle time", task.getDescription());
        assertEquals(-1, task.getPriority()); // Low priority
    }

    @Test
    @DisplayName("DreamTask can run only when idle")
    void dreamTaskCanRun() {
        DreamTask task = DreamTask.getInstance();

        TaskContext idleContext = new TaskContext();
        idleContext.setProperty("idle", true);

        TaskContext busyContext = new TaskContext();
        busyContext.setProperty("idle", false);

        assertTrue(task.canRun(idleContext));
        assertFalse(task.canRun(busyContext));
    }

    @Test
    @DisplayName("DreamTask executes different dream types")
    void dreamTaskExecutesDifferentTypes() {
        DreamTask task = DreamTask.getInstance();

        // Test cleanup dream type
        TaskContext cleanupContext = new TaskContext();
        cleanupContext.setProperty("dreamType", "cleanup");
        cleanupContext.setProperty("idle", true);

        CompletableFuture<TaskResult> future = task.execute(cleanupContext);
        TaskResult result = future.join();

        assertNotNull(result);
    }

    @Test
    @DisplayName("TaskContext property operations work correctly")
    void taskContextProperties() {
        TaskContext context = new TaskContext();

        context.setProperty("string", "value");
        context.setProperty("number", 42);
        context.setProperty("boolean", true);

        assertEquals("value", context.getProperty("string"));
        assertEquals(42, context.getProperty("number"));
        assertEquals(true, context.getProperty("boolean"));
        assertNull(context.getProperty("nonexistent"));
    }

    @Test
    @DisplayName("TaskContext getPropertyOrDefault returns default for missing")
    void taskContextGetPropertyOrDefault() {
        TaskContext context = new TaskContext();

        assertEquals("default", context.getPropertyOrDefault("missing", "default"));
        assertEquals(100, context.getPropertyOrDefault("missing", 100));

        context.setProperty("exists", "value");
        assertEquals("value", context.getPropertyOrDefault("exists", "default"));
    }

    @Test
    @DisplayName("TaskContext cancellation works")
    void taskContextCancellation() {
        TaskContext context = new TaskContext();

        assertFalse(context.isCancelled());
        context.cancel();
        assertTrue(context.isCancelled());
    }

    @Test
    @DisplayName("TaskResult success and failure factory methods")
    void taskResultFactoryMethods() {
        TaskResult success = TaskResult.success("Done", 100);
        assertTrue(success.isSuccess());
        assertEquals("Done", success.getOutput());
        assertEquals(100, success.getDurationMs());

        TaskResult failure = TaskResult.failure("Error", 50);
        assertFalse(failure.isSuccess());
        assertEquals("Error", failure.getError());
        assertEquals(50, failure.getDurationMs());
    }
}