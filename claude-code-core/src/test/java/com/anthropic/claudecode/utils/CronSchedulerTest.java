/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CronScheduler.
 */
class CronSchedulerTest {

    @BeforeEach
    void setUp() {
        CronTasks.removeSessionCronTasks(List.of());
    }

    @Test
    @DisplayName("CronScheduler SchedulerOptions record")
    void schedulerOptionsRecord() {
        CronScheduler.SchedulerOptions options = new CronScheduler.SchedulerOptions(
            prompt -> {},
            () -> false,
            false,
            null,
            null,
            null,
            "test-identity",
            () -> CronJitterConfig.getConfig(),
            () -> false,
            null
        );

        assertFalse(options.assistantMode());
        assertEquals("test-identity", options.lockIdentity());
    }

    @Test
    @DisplayName("CronScheduler Scheduler constructor")
    void schedulerConstructor() {
        CronScheduler.SchedulerOptions options = new CronScheduler.SchedulerOptions(
            null, null, false, null, null, null, null, null, null, null
        );

        CronScheduler.Scheduler scheduler = new CronScheduler.Scheduler(options);
        assertNotNull(scheduler);
    }

    @Test
    @DisplayName("CronScheduler createScheduler")
    void createScheduler() {
        CronScheduler.SchedulerOptions options = new CronScheduler.SchedulerOptions(
            null, null, false, null, null, null, null, null, null, null
        );

        CronScheduler.Scheduler scheduler = CronScheduler.createScheduler(options);
        assertNotNull(scheduler);
    }

    @Test
    @DisplayName("CronScheduler Scheduler start and stop")
    void schedulerStartStop() {
        CronScheduler.SchedulerOptions options = new CronScheduler.SchedulerOptions(
            null, null, false, null, null, null, null, null, null, null
        );

        CronScheduler.Scheduler scheduler = CronScheduler.createScheduler(options);
        assertDoesNotThrow(() -> scheduler.start());
        assertDoesNotThrow(() -> scheduler.stop());
    }

    @Test
    @DisplayName("CronScheduler Scheduler getNextFireTime null")
    void schedulerGetNextFireTimeNull() {
        CronScheduler.SchedulerOptions options = new CronScheduler.SchedulerOptions(
            null, null, false, null, null, null, null, null, null, null
        );

        CronScheduler.Scheduler scheduler = CronScheduler.createScheduler(options);
        assertNull(scheduler.getNextFireTime());
    }

    @Test
    @DisplayName("CronScheduler isRecurringTaskAged false for young task")
    void isRecurringTaskAgedYoungTask() {
        CronTasks.CronTask task = new CronTasks.CronTask(
            "test-id", "*/5 * * * *", "test prompt",
            System.currentTimeMillis(), null, true, false, null, null
        );

        assertFalse(CronScheduler.Scheduler.isRecurringTaskAged(task, System.currentTimeMillis(), 7L * 24 * 60 * 60 * 1000));
    }

    @Test
    @DisplayName("CronScheduler isRecurringTaskAged true for old task")
    void isRecurringTaskAgedOldTask() {
        long oldTime = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000); // 8 days ago
        CronTasks.CronTask task = new CronTasks.CronTask(
            "test-id", "*/5 * * * *", "test prompt",
            oldTime, null, true, false, null, null
        );

        assertTrue(CronScheduler.Scheduler.isRecurringTaskAged(task, System.currentTimeMillis(), 7L * 24 * 60 * 60 * 1000));
    }

    @Test
    @DisplayName("CronScheduler isRecurringTaskAged false for permanent task")
    void isRecurringTaskAgedPermanentTask() {
        long oldTime = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000);
        CronTasks.CronTask task = new CronTasks.CronTask(
            "test-id", "*/5 * * * *", "test prompt",
            oldTime, null, true, true, null, null // permanent = true
        );

        assertFalse(CronScheduler.Scheduler.isRecurringTaskAged(task, System.currentTimeMillis(), 7L * 24 * 60 * 60 * 1000));
    }

    @Test
    @DisplayName("CronScheduler isRecurringTaskAged false for one-shot")
    void isRecurringTaskAgedOneShot() {
        long oldTime = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000);
        CronTasks.CronTask task = new CronTasks.CronTask(
            "test-id", "*/5 * * * *", "test prompt",
            oldTime, null, false, false, null, null // recurring = false
        );

        assertFalse(CronScheduler.Scheduler.isRecurringTaskAged(task, System.currentTimeMillis(), 7L * 24 * 60 * 60 * 1000));
    }

    @Test
    @DisplayName("CronScheduler isRecurringTaskAged false for zero maxAge")
    void isRecurringTaskAgedZeroMaxAge() {
        long oldTime = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000);
        CronTasks.CronTask task = new CronTasks.CronTask(
            "test-id", "*/5 * * * *", "test prompt",
            oldTime, null, true, false, null, null
        );

        assertFalse(CronScheduler.Scheduler.isRecurringTaskAged(task, System.currentTimeMillis(), 0));
    }

    @Test
    @DisplayName("CronScheduler buildMissedTaskNotification single")
    void buildMissedTaskNotificationSingle() {
        CronTasks.CronTask task = new CronTasks.CronTask(
            "test-id", "0 9 * * *", "test prompt",
            System.currentTimeMillis() - 10000, null, false, false, null, null
        );

        String notification = CronScheduler.Scheduler.buildMissedTaskNotification(List.of(task));
        assertTrue(notification.contains("one-shot scheduled task was missed"));
        assertTrue(notification.contains("test prompt"));
    }

    @Test
    @DisplayName("CronScheduler buildMissedTaskNotification multiple")
    void buildMissedTaskNotificationMultiple() {
        CronTasks.CronTask task1 = new CronTasks.CronTask(
            "id1", "0 9 * * *", "prompt 1",
            System.currentTimeMillis(), null, false, false, null, null
        );
        CronTasks.CronTask task2 = new CronTasks.CronTask(
            "id2", "0 10 * * *", "prompt 2",
            System.currentTimeMillis(), null, false, false, null, null
        );

        String notification = CronScheduler.Scheduler.buildMissedTaskNotification(List.of(task1, task2));
        assertTrue(notification.contains("one-shot scheduled tasks were missed"));
        assertTrue(notification.contains("prompt 1"));
        assertTrue(notification.contains("prompt 2"));
    }
}