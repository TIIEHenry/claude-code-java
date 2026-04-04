/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CronTasks.
 */
class CronTasksTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        CronTasks.removeSessionCronTasks(List.of());
    }

    @Test
    @DisplayName("CronTasks CronTask record")
    void cronTaskRecord() {
        CronTasks.CronTask task = new CronTasks.CronTask(
            "task-123",
            "*/5 * * * *",
            "test prompt",
            System.currentTimeMillis(),
            null,
            true,
            false,
            null,
            null
        );

        assertEquals("task-123", task.id());
        assertEquals("*/5 * * * *", task.cron());
        assertEquals("test prompt", task.prompt());
        assertTrue(task.recurring());
        assertFalse(task.permanent());
    }

    @Test
    @DisplayName("CronTasks getCronFilePath")
    void getCronFilePath() {
        Path path = CronTasks.getCronFilePath();
        assertNotNull(path);
        assertTrue(path.toString().contains("scheduled_tasks.json"));
    }

    @Test
    @DisplayName("CronTasks getCronFilePath with dir")
    void getCronFilePathWithDir() {
        Path path = CronTasks.getCronFilePath(tempDir);
        assertTrue(path.toString().contains("scheduled_tasks.json"));
    }

    @Test
    @DisplayName("CronTasks readCronTasks empty")
    void readCronTasksEmpty() {
        List<CronTasks.CronTask> tasks = CronTasks.readCronTasks(tempDir);
        assertTrue(tasks.isEmpty());
    }

    @Test
    @DisplayName("CronTasks writeCronTasks and readCronTasks")
    void writeAndReadCronTasks() {
        CronTasks.CronTask task = new CronTasks.CronTask(
            "test-id",
            "*/5 * * * *",
            "test prompt",
            System.currentTimeMillis(),
            null,
            true,
            false,
            null,
            null
        );

        CronTasks.writeCronTasks(List.of(task), tempDir);
        List<CronTasks.CronTask> read = CronTasks.readCronTasks(tempDir);

        assertEquals(1, read.size());
        assertEquals("test-id", read.get(0).id());
        assertEquals("*/5 * * * *", read.get(0).cron());
    }

    @Test
    @DisplayName("CronTasks addCronTask session only")
    void addCronTaskSessionOnly() {
        String id = CronTasks.addCronTask("*/5 * * * *", "test prompt", true, false);
        assertNotNull(id);
        assertEquals(8, id.length());

        List<CronTasks.CronTask> sessionTasks = CronTasks.getSessionCronTasks();
        assertFalse(sessionTasks.isEmpty());
    }

    @Test
    @DisplayName("CronTasks removeSessionCronTasks")
    void removeSessionCronTasks() {
        String id = CronTasks.addCronTask("*/5 * * * *", "test prompt", true, false);
        assertEquals(1, CronTasks.getSessionCronTasks().size());

        int removed = CronTasks.removeSessionCronTasks(List.of(id));
        assertEquals(1, removed);
        assertTrue(CronTasks.getSessionCronTasks().isEmpty());
    }

    @Test
    @DisplayName("CronTasks removeCronTasks")
    void removeCronTasks() {
        CronTasks.CronTask task = new CronTasks.CronTask(
            "remove-test",
            "*/5 * * * *",
            "test prompt",
            System.currentTimeMillis(),
            null,
            true,
            false,
            null,
            null
        );

        CronTasks.writeCronTasks(List.of(task), tempDir);
        assertEquals(1, CronTasks.readCronTasks(tempDir).size());

        CronTasks.removeCronTasks(List.of("remove-test"), tempDir);
        assertTrue(CronTasks.readCronTasks(tempDir).isEmpty());
    }

    @Test
    @DisplayName("CronTasks nextCronRunMs")
    void nextCronRunMs() {
        long now = System.currentTimeMillis();
        Long next = CronTasks.nextCronRunMs("*/5 * * * *", now);

        assertNotNull(next);
        assertTrue(next > now);
    }

    @Test
    @DisplayName("CronTasks nextCronRunMs invalid cron")
    void nextCronRunMsInvalidCron() {
        Long next = CronTasks.nextCronRunMs("invalid", System.currentTimeMillis());
        assertNull(next);
    }

    @Test
    @DisplayName("CronTasks jitterFrac")
    void jitterFrac() {
        double frac = CronTasks.jitterFrac("abcd1234");
        assertTrue(frac >= 0 && frac <= 1);
    }

    @Test
    @DisplayName("CronTasks jitterFrac short id")
    void jitterFracShortId() {
        double frac = CronTasks.jitterFrac("abc");
        assertTrue(frac >= 0 && frac <= 1);
    }

    @Test
    @DisplayName("CronTasks jitteredNextCronRunMs")
    void jitteredNextCronRunMs() {
        long now = System.currentTimeMillis();
        Long next = CronTasks.jitteredNextCronRunMs("*/5 * * * *", now, "test1234");
        assertNotNull(next);
    }

    @Test
    @DisplayName("CronTasks oneShotJitteredNextCronRunMs")
    void oneShotJitteredNextCronRunMs() {
        long now = System.currentTimeMillis();
        Long next = CronTasks.oneShotJitteredNextCronRunMs("*/5 * * * *", now, "test1234");
        assertNotNull(next);
    }

    @Test
    @DisplayName("CronTasks findMissedTasks empty")
    void findMissedTasksEmpty() {
        List<CronTasks.CronTask> missed = CronTasks.findMissedTasks(List.of(), System.currentTimeMillis());
        assertTrue(missed.isEmpty());
    }

    @Test
    @DisplayName("CronTasks listAllCronTasks")
    void listAllCronTasks() {
        CronTasks.addCronTask("*/5 * * * *", "session task", true, false);

        List<CronTasks.CronTask> all = CronTasks.listAllCronTasks();
        assertFalse(all.isEmpty());
    }

    @Test
    @DisplayName("CronTasks addSessionCronTask")
    void addSessionCronTask() {
        CronTasks.CronTask task = new CronTasks.CronTask(
            "manual-id",
            "*/5 * * * *",
            "manual task",
            System.currentTimeMillis(),
            null,
            true,
            false,
            null,
            null
        );

        CronTasks.addSessionCronTask(task);
        List<CronTasks.CronTask> tasks = CronTasks.getSessionCronTasks();
        assertTrue(tasks.stream().anyMatch(t -> "manual-id".equals(t.id())));
    }
}