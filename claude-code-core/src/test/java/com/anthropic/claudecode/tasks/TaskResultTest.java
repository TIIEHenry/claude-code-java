/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TaskResult - 测试任务执行结果的各种场景.
 */
class TaskResultTest {

    @Test
    @DisplayName("TaskResult.success 创建成功结果")
    void successCreatesSuccessfulResult() {
        TaskResult result = TaskResult.success("任务输出");

        assertTrue(result.isSuccess());
        assertEquals("任务输出", result.getOutput());
        assertNull(result.getError());
        assertEquals(0, result.getDurationMs());
    }

    @Test
    @DisplayName("TaskResult.success 带执行时间")
    void successWithDuration() {
        TaskResult result = TaskResult.success("输出", 1500L);

        assertTrue(result.isSuccess());
        assertEquals("输出", result.getOutput());
        assertEquals(1500L, result.getDurationMs());
    }

    @Test
    @DisplayName("TaskResult.failure 创建失败结果")
    void failureCreatesFailedResult() {
        TaskResult result = TaskResult.failure("执行失败");

        assertFalse(result.isSuccess());
        assertNull(result.getOutput());
        assertEquals("执行失败", result.getError());
    }

    @Test
    @DisplayName("TaskResult.failure 带执行时间")
    void failureWithDuration() {
        TaskResult result = TaskResult.failure("超时", 30000L);

        assertFalse(result.isSuccess());
        assertEquals("超时", result.getError());
        assertEquals(30000L, result.getDurationMs());
    }

    @Test
    @DisplayName("TaskResult 成功结果可以有空输出")
    void successCanHaveNullOutput() {
        TaskResult result = TaskResult.success(null);

        assertTrue(result.isSuccess());
        assertNull(result.getOutput());
    }

    @Test
    @DisplayName("TaskResult 失败结果可以有空错误")
    void failureCanHaveNullError() {
        TaskResult result = TaskResult.failure(null);

        assertFalse(result.isSuccess());
        assertNull(result.getError());
    }

    @Test
    @DisplayName("TaskResult toString 显示成功状态")
    void toStringShowsSuccessStatus() {
        TaskResult result = TaskResult.success("Hello World");

        String str = result.toString();

        assertTrue(str.contains("success"));
        assertTrue(str.contains("11 chars")); // "Hello World" length
    }

    @Test
    @DisplayName("TaskResult toString 显示失败状态")
    void toStringShowsFailureStatus() {
        TaskResult result = TaskResult.failure("错误信息");

        String str = result.toString();

        assertTrue(str.contains("failure"));
        assertTrue(str.contains("错误信息"));
    }

    @Test
    @DisplayName("TaskResult 链式调用场景: 重试后成功")
    void retryScenario() {
        // 场景: 第一次失败，重试后成功
        TaskResult first = TaskResult.failure("连接超时", 5000L);
        TaskResult retry = TaskResult.success("连接成功", 1200L);

        assertFalse(first.isSuccess());
        assertTrue(retry.isSuccess());
        assertTrue(first.getDurationMs() > retry.getDurationMs());
    }

    @Test
    @DisplayName("TaskResult 长时间运行任务")
    void longRunningTask() {
        // 场景: 长时间运行的任务成功完成
        long longDuration = 60 * 60 * 1000L; // 1小时
        TaskResult result = TaskResult.success("构建完成", longDuration);

        assertTrue(result.isSuccess());
        assertEquals(longDuration, result.getDurationMs());
    }
}

/**
 * Tests for TaskType - 测试任务类型枚举.
 */
class TaskTypeTest {

    @Test
    @DisplayName("TaskType 包含所有预期类型")
    void containsAllExpectedTypes() {
        TaskType[] types = TaskType.values();

        assertEquals(7, types.length);
        assertTrue(containsType(types, TaskType.LOCAL_BASH));
        assertTrue(containsType(types, TaskType.LOCAL_AGENT));
        assertTrue(containsType(types, TaskType.REMOTE_AGENT));
        assertTrue(containsType(types, TaskType.DREAM));
    }

    @Test
    @DisplayName("TaskType valueOf 正确解析")
    void valueOfParsesCorrectly() {
        assertEquals(TaskType.LOCAL_BASH, TaskType.valueOf("LOCAL_BASH"));
        assertEquals(TaskType.REMOTE_AGENT, TaskType.valueOf("REMOTE_AGENT"));
    }

    private boolean containsType(TaskType[] types, TaskType target) {
        for (TaskType t : types) {
            if (t == target) return true;
        }
        return false;
    }
}

/**
 * Tests for TaskState - 测试任务状态场景.
 */
class TaskStateTest {

    @Test
    @DisplayName("TaskState.TaskStatus 包含所有状态")
    void taskStatusContainsAllStates() {
        TaskState.TaskStatus[] statuses = TaskState.TaskStatus.values();

        assertEquals(5, statuses.length);
        assertEquals(TaskState.TaskStatus.PENDING, TaskState.TaskStatus.valueOf("PENDING"));
        assertEquals(TaskState.TaskStatus.RUNNING, TaskState.TaskStatus.valueOf("RUNNING"));
        assertEquals(TaskState.TaskStatus.COMPLETED, TaskState.TaskStatus.valueOf("COMPLETED"));
        assertEquals(TaskState.TaskStatus.FAILED, TaskState.TaskStatus.valueOf("FAILED"));
        assertEquals(TaskState.TaskStatus.KILLED, TaskState.TaskStatus.valueOf("KILLED"));
    }

    @Test
    @DisplayName("LocalShellTaskState 完整生命周期场景")
    void localShellTaskLifecycle() {
        // 场景: Shell任务从创建到完成的完整生命周期

        // 1. 创建时状态
        TaskState.LocalShellTaskState pending = new TaskState.LocalShellTaskState(
            "task-123",
            TaskType.LOCAL_BASH,
            TaskState.TaskStatus.PENDING,
            "执行ls命令",
            "tool-456",
            System.currentTimeMillis(),
            null,
            "/tmp/output.log",
            0,
            false,
            true,
            "ls -la",
            0
        );

        assertEquals("task-123", pending.id());
        assertEquals(TaskState.TaskStatus.PENDING, pending.status());
        assertFalse(pending.notified());
        assertTrue(pending.isBackgrounded());

        // 2. 运行中状态
        TaskState.LocalShellTaskState running = new TaskState.LocalShellTaskState(
            pending.id(),
            pending.type(),
            TaskState.TaskStatus.RUNNING,
            pending.description(),
            pending.toolUseId(),
            pending.startTime(),
            null,
            pending.outputFile(),
            100, // 已有100字节输出
            false,
            true,
            pending.command(),
            12345 // 进程PID
        );

        assertEquals(TaskState.TaskStatus.RUNNING, running.status());
        assertEquals(12345, running.pid());
        assertEquals(100, running.outputOffset());

        // 3. 完成状态
        long completedTime = running.startTime() + 1000; // 确保endTime > startTime
        TaskState.LocalShellTaskState completed = new TaskState.LocalShellTaskState(
            running.id(),
            running.type(),
            TaskState.TaskStatus.COMPLETED,
            running.description(),
            running.toolUseId(),
            running.startTime(),
            completedTime,
            running.outputFile(),
            5000, // 最终输出大小
            true, // 已通知
            true,
            running.command(),
            0 // 进程已结束
        );

        assertEquals(TaskState.TaskStatus.COMPLETED, completed.status());
        assertTrue(completed.notified());
        assertNotNull(completed.endTime());
        assertTrue(completed.endTime() >= completed.startTime());
    }

    @Test
    @DisplayName("LocalAgentTaskState 代理任务场景")
    void localAgentTaskScenario() {
        // 场景: 本地代理任务执行
        TaskState.LocalAgentTaskState state = new TaskState.LocalAgentTaskState(
            "agent-task-1",
            TaskType.LOCAL_AGENT,
            TaskState.TaskStatus.RUNNING,
            "代码审查代理",
            "tool-789",
            System.currentTimeMillis(),
            null,
            "/tmp/agent.log",
            0,
            false,
            false, // 前台运行
            "code-review-agent"
        );

        assertEquals("code-review-agent", state.agentName());
        assertFalse(state.isBackgrounded());
        assertEquals(TaskType.LOCAL_AGENT, state.type());
    }

    @Test
    @DisplayName("RemoteAgentTaskState 远程代理场景")
    void remoteAgentTaskScenario() {
        // 场景: 远程代理任务连接到服务器
        TaskState.RemoteAgentTaskState state = new TaskState.RemoteAgentTaskState(
            "remote-1",
            TaskType.REMOTE_AGENT,
            TaskState.TaskStatus.RUNNING,
            "远程构建任务",
            "tool-111",
            System.currentTimeMillis(),
            null,
            "/tmp/remote.log",
            0,
            false,
            true,
            "https://build-server.example.com/agent"
        );

        assertEquals("https://build-server.example.com/agent", state.serverUrl());
        assertEquals(TaskType.REMOTE_AGENT, state.type());
    }

    @Test
    @DisplayName("DreamTaskState Dream任务场景")
    void dreamTaskScenario() {
        // 场景: Dream后台任务
        TaskState.DreamTaskState state = new TaskState.DreamTaskState(
            "dream-1",
            TaskType.DREAM,
            TaskState.TaskStatus.PENDING,
            "后台索引构建",
            "tool-222",
            System.currentTimeMillis(),
            null,
            "/tmp/dream.log",
            0,
            false,
            "index-build"
        );

        assertEquals("index-build", state.dreamType());
        assertTrue(state.isBackgrounded()); // Dream任务总是后台运行
        assertEquals(TaskType.DREAM, state.type());
    }

    @Test
    @DisplayName("TaskState 任务失败场景")
    void taskFailureScenario() {
        // 场景: 任务执行失败
        TaskState.LocalShellTaskState failed = new TaskState.LocalShellTaskState(
            "failed-task",
            TaskType.LOCAL_BASH,
            TaskState.TaskStatus.FAILED,
            "失败的任务",
            "tool-333",
            System.currentTimeMillis(),
            System.currentTimeMillis() + 1000,
            "/tmp/error.log",
            50,
            false,
            true,
            "exit 1",
            0
        );

        assertEquals(TaskState.TaskStatus.FAILED, failed.status());
        assertNotNull(failed.endTime());
    }

    @Test
    @DisplayName("TaskState 任务被终止场景")
    void taskKilledScenario() {
        // 场景: 用户手动终止任务
        TaskState.LocalShellTaskState killed = new TaskState.LocalShellTaskState(
            "killed-task",
            TaskType.LOCAL_BASH,
            TaskState.TaskStatus.KILLED,
            "被终止的任务",
            "tool-444",
            System.currentTimeMillis(),
            System.currentTimeMillis() + 500,
            "/tmp/killed.log",
            25,
            false,
            true,
            "sleep 1000",
            0
        );

        assertEquals(TaskState.TaskStatus.KILLED, killed.status());
    }

    @Test
    @DisplayName("TaskState sealed interface 实现检查")
    void sealedInterfaceImplementations() {
        TaskState localShell = new TaskState.LocalShellTaskState(
            "id", TaskType.LOCAL_BASH, TaskState.TaskStatus.PENDING,
            "desc", "tool", 0L, null, null, 0, false, true, "cmd", 0
        );
        TaskState localAgent = new TaskState.LocalAgentTaskState(
            "id", TaskType.LOCAL_AGENT, TaskState.TaskStatus.PENDING,
            "desc", "tool", 0L, null, null, 0, false, true, "agent"
        );
        TaskState remoteAgent = new TaskState.RemoteAgentTaskState(
            "id", TaskType.REMOTE_AGENT, TaskState.TaskStatus.PENDING,
            "desc", "tool", 0L, null, null, 0, false, true, "url"
        );
        TaskState dream = new TaskState.DreamTaskState(
            "id", TaskType.DREAM, TaskState.TaskStatus.PENDING,
            "desc", "tool", 0L, null, null, 0, false, "type"
        );

        assertTrue(localShell instanceof TaskState);
        assertTrue(localAgent instanceof TaskState);
        assertTrue(remoteAgent instanceof TaskState);
        assertTrue(dream instanceof TaskState);
    }
}