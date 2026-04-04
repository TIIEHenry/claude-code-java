/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CronUtils.
 */
class CronUtilsTest {

    @Test
    @DisplayName("CronUtils isValidCron validates standard cron")
    void isValidCronStandard() {
        assertTrue(CronUtils.isValidCron("*/5 * * * *"));
        assertTrue(CronUtils.isValidCron("0 9 * * *"));
        assertTrue(CronUtils.isValidCron("30 14 28 2 *"));
        assertTrue(CronUtils.isValidCron("0 0 * * 1-5"));
    }

    @Test
    @DisplayName("CronUtils isValidCron rejects invalid cron")
    void isValidCronInvalid() {
        assertFalse(CronUtils.isValidCron(null));
        assertFalse(CronUtils.isValidCron(""));
        assertFalse(CronUtils.isValidCron("* * *"));  // Too few fields
        assertFalse(CronUtils.isValidCron("60 * * * *"));  // Invalid minute
        assertFalse(CronUtils.isValidCron("* 24 * * *"));  // Invalid hour
        assertFalse(CronUtils.isValidCron("* * 32 * *"));  // Invalid day of month
        assertFalse(CronUtils.isValidCron("* * * 13 *"));  // Invalid month
        assertFalse(CronUtils.isValidCron("* * * * 7"));   // Invalid day of week
    }

    @Test
    @DisplayName("CronUtils isValidCron handles ranges")
    void isValidCronRanges() {
        assertTrue(CronUtils.isValidCron("0-30 * * * *"));
        assertTrue(CronUtils.isValidCron("* 9-17 * * *"));
        assertFalse(CronUtils.isValidCron("30-10 * * * *"));  // Invalid range (start > end)
    }

    @Test
    @DisplayName("CronUtils isValidCron handles lists")
    void isValidCronLists() {
        assertTrue(CronUtils.isValidCron("0,15,30,45 * * * *"));
        assertTrue(CronUtils.isValidCron("* 9,12,17 * * *"));
        assertFalse(CronUtils.isValidCron("0,60 * * * *"));  // Invalid value in list
    }

    @Test
    @DisplayName("CronUtils isValidCron handles step values")
    void isValidCronStep() {
        assertTrue(CronUtils.isValidCron("*/5 * * * *"));
        assertTrue(CronUtils.isValidCron("0 */2 * * *"));
        assertFalse(CronUtils.isValidCron("*/0 * * * *"));  // Invalid step (0)
    }

    @Test
    @DisplayName("CronUtils nextRun returns next execution time")
    void nextRunWorks() {
        long now = System.currentTimeMillis();
        Long next = CronUtils.nextRun("*/5 * * * *", now);

        assertNotNull(next);
        assertTrue(next > now);
    }

    @Test
    @DisplayName("CronUtils nextRun returns null for invalid cron")
    void nextRunInvalid() {
        Long next = CronUtils.nextRun("invalid", System.currentTimeMillis());

        assertNull(next);
    }

    @Test
    @DisplayName("CronUtils nextRun handles specific time")
    void nextRunSpecificTime() {
        // Create a time just before 9:00 AM
        LocalDateTime beforeTime = LocalDateTime.of(2026, 1, 1, 8, 59, 0);
        long fromTime = beforeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Long next = CronUtils.nextRun("0 9 * * *", fromTime);

        assertNotNull(next);
        LocalDateTime nextTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(next), ZoneId.systemDefault()
        );
        assertEquals(9, nextTime.getHour());
        assertEquals(0, nextTime.getMinute());
    }

    @Test
    @DisplayName("CronUtils cronToHuman converts every minute")
    void cronToHumanEveryMinute() {
        assertEquals("every minute", CronUtils.cronToHuman("* * * * *"));
    }

    @Test
    @DisplayName("CronUtils cronToHuman converts every N minutes")
    void cronToHumanEveryNMinutes() {
        assertEquals("every 5 minutes", CronUtils.cronToHuman("*/5 * * * *"));
        assertEquals("every 1 minute", CronUtils.cronToHuman("*/1 * * * *"));
    }

    @Test
    @DisplayName("CronUtils cronToHuman converts hourly")
    void cronToHumanHourly() {
        assertEquals("hourly", CronUtils.cronToHuman("0 * * * *"));
    }

    @Test
    @DisplayName("CronUtils cronToHuman converts every N hours")
    void cronToHumanEveryNHours() {
        assertEquals("every 2 hours", CronUtils.cronToHuman("0 */2 * * *"));
    }

    @Test
    @DisplayName("CronUtils cronToHuman converts daily")
    void cronToHumanDaily() {
        String result = CronUtils.cronToHuman("30 9 * * *");
        assertTrue(result.contains("daily"));
        assertTrue(result.contains("9:30"));
    }

    @Test
    @DisplayName("CronUtils cronToHuman converts weekly")
    void cronToHumanWeekly() {
        String result = CronUtils.cronToHuman("0 9 * * 1");
        assertTrue(result.contains("Monday"));
        assertTrue(result.contains("9:00"));
    }

    @Test
    @DisplayName("CronUtils cronToHuman returns invalid for invalid cron")
    void cronToHumanInvalid() {
        assertEquals("invalid", CronUtils.cronToHuman("invalid"));
    }

    @Test
    @DisplayName("CronUtils CronTask record works")
    void cronTaskRecord() {
        CronUtils.CronTask task = new CronUtils.CronTask(
            "test-id", "*/5 * * * *", "prompt", true, false, System.currentTimeMillis()
        );

        assertEquals("test-id", task.id());
        assertEquals("*/5 * * * *", task.cron());
        assertEquals("prompt", task.prompt());
        assertTrue(task.recurring());
        assertFalse(task.durable());
    }

    @Test
    @DisplayName("CronUtils addCronTask creates task")
    void addCronTaskWorks() {
        String id = CronUtils.addCronTask("*/5 * * * *", "test prompt", true, false);

        assertNotNull(id);
        assertTrue(id.length() == 8);

        // Clean up
        CronUtils.removeCronTask(id);
    }

    @Test
    @DisplayName("CronUtils removeCronTask removes task")
    void removeCronTaskWorks() {
        String id = CronUtils.addCronTask("*/5 * * * *", "test prompt", true, false);

        assertTrue(CronUtils.removeCronTask(id));
        assertFalse(CronUtils.removeCronTask(id));  // Already removed
    }

    @Test
    @DisplayName("CronUtils listAllCronTasks returns all tasks")
    void listAllCronTasksWorks() {
        String id1 = CronUtils.addCronTask("*/5 * * * *", "prompt1", true, false);
        String id2 = CronUtils.addCronTask("0 9 * * *", "prompt2", true, false);

        List<CronUtils.CronTask> tasks = CronUtils.listAllCronTasks();

        assertTrue(tasks.size() >= 2);

        // Clean up
        CronUtils.removeCronTask(id1);
        CronUtils.removeCronTask(id2);
    }

    @Test
    @DisplayName("CronUtils getJobCount returns count")
    void getJobCountWorks() {
        int initialCount = CronUtils.getJobCount();

        String id = CronUtils.addCronTask("*/5 * * * *", "test prompt", true, false);

        assertEquals(initialCount + 1, CronUtils.getJobCount());

        CronUtils.removeCronTask(id);

        assertEquals(initialCount, CronUtils.getJobCount());
    }

    @Test
    @DisplayName("CronUtils scheduledTasksEnabled can be set")
    void scheduledTasksEnabledWorks() {
        boolean original = CronUtils.isScheduledTasksEnabled();

        CronUtils.setScheduledTasksEnabled(true);
        assertTrue(CronUtils.isScheduledTasksEnabled());

        CronUtils.setScheduledTasksEnabled(false);
        assertFalse(CronUtils.isScheduledTasksEnabled());

        CronUtils.setScheduledTasksEnabled(original);
    }

    @Test
    @DisplayName("CronUtils getCronFilePath returns valid path")
    void getCronFilePathWorks() {
        assertNotNull(CronUtils.getCronFilePath());
        assertTrue(CronUtils.getCronFilePath().toString().contains(".claude"));
    }
}