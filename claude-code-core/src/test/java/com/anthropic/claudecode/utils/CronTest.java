/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Cron.
 */
class CronTest {

    @Test
    @DisplayName("Cron CronFields record")
    void cronFieldsRecord() {
        Cron.CronFields fields = new Cron.CronFields(
            Set.of(0, 30),
            Set.of(9),
            Set.of(1, 15),
            Set.of(1, 2, 3),
            Set.of(1, 2, 3, 4, 5)
        );

        assertEquals(2, fields.minute().size());
        assertEquals(1, fields.hour().size());
        assertTrue(fields.minute().contains(0));
        assertTrue(fields.minute().contains(30));
    }

    @Test
    @DisplayName("Cron parseCronExpression valid")
    void parseCronExpressionValid() {
        Cron.CronFields fields = Cron.parseCronExpression("*/5 * * * *");
        assertNotNull(fields);
        assertEquals(12, fields.minute().size()); // 0, 5, 10, ..., 55
    }

    @Test
    @DisplayName("Cron parseCronExpression every minute")
    void parseCronExpressionEveryMinute() {
        Cron.CronFields fields = Cron.parseCronExpression("* * * * *");
        assertNotNull(fields);
        assertEquals(60, fields.minute().size());
        assertEquals(24, fields.hour().size());
        assertEquals(31, fields.dayOfMonth().size());
        assertEquals(12, fields.month().size());
        assertEquals(7, fields.dayOfWeek().size());
    }

    @Test
    @DisplayName("Cron parseCronExpression specific time")
    void parseCronExpressionSpecificTime() {
        Cron.CronFields fields = Cron.parseCronExpression("30 9 * * 1-5");
        assertNotNull(fields);
        assertTrue(fields.minute().contains(30));
        assertTrue(fields.hour().contains(9));
        assertEquals(5, fields.dayOfWeek().size()); // 1-5
    }

    @Test
    @DisplayName("Cron parseCronExpression null")
    void parseCronExpressionNull() {
        assertNull(Cron.parseCronExpression(null));
    }

    @Test
    @DisplayName("Cron parseCronExpression empty")
    void parseCronExpressionEmpty() {
        assertNull(Cron.parseCronExpression(""));
    }

    @Test
    @DisplayName("Cron parseCronExpression wrong fields")
    void parseCronExpressionWrongFields() {
        assertNull(Cron.parseCronExpression("* * * *")); // 4 fields
        assertNull(Cron.parseCronExpression("* * * * * *")); // 6 fields
    }

    @Test
    @DisplayName("Cron parseCronExpression range")
    void parseCronExpressionRange() {
        Cron.CronFields fields = Cron.parseCronExpression("0-30 * * * *");
        assertNotNull(fields);
        assertEquals(31, fields.minute().size());
    }

    @Test
    @DisplayName("Cron parseCronExpression list")
    void parseCronExpressionList() {
        Cron.CronFields fields = Cron.parseCronExpression("0,15,30,45 * * * *");
        assertNotNull(fields);
        assertEquals(4, fields.minute().size());
    }

    @Test
    @DisplayName("Cron parseCronExpression step")
    void parseCronExpressionStep() {
        Cron.CronFields fields = Cron.parseCronExpression("*/10 * * * *");
        assertNotNull(fields);
        assertEquals(6, fields.minute().size()); // 0, 10, 20, 30, 40, 50
    }

    @Test
    @DisplayName("Cron computeNextCronRun")
    void computeNextCronRun() {
        Cron.CronFields fields = Cron.parseCronExpression("0 * * * *");
        LocalDateTime from = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        LocalDateTime next = Cron.computeNextCronRun(fields, from);

        assertNotNull(next);
        assertEquals(11, next.getHour());
        assertEquals(0, next.getMinute());
    }

    @Test
    @DisplayName("Cron computeNextCronRun every 5 minutes")
    void computeNextCronRunEvery5Minutes() {
        Cron.CronFields fields = Cron.parseCronExpression("*/5 * * * *");
        LocalDateTime from = LocalDateTime.of(2024, 1, 15, 10, 3, 0);
        LocalDateTime next = Cron.computeNextCronRun(fields, from);

        assertNotNull(next);
        assertEquals(5, next.getMinute());
    }

    @Test
    @DisplayName("Cron computeNextCronRun daily")
    void computeNextCronRunDaily() {
        Cron.CronFields fields = Cron.parseCronExpression("0 9 * * *");
        LocalDateTime from = LocalDateTime.of(2024, 1, 15, 8, 0, 0);
        LocalDateTime next = Cron.computeNextCronRun(fields, from);

        assertNotNull(next);
        assertEquals(15, next.getDayOfMonth());
        assertEquals(9, next.getHour());
        assertEquals(0, next.getMinute());
    }

    @Test
    @DisplayName("Cron cronToHuman every minute")
    void cronToHumanEveryMinute() {
        // `* * * * *` is not matched by the pattern, returns original
        assertEquals("* * * * *", Cron.cronToHuman("* * * * *"));
        // `*/1 * * * *` is matched
        assertEquals("Every minute", Cron.cronToHuman("*/1 * * * *"));
    }

    @Test
    @DisplayName("Cron cronToHuman every N minutes")
    void cronToHumanEveryNMinutes() {
        assertEquals("Every 5 minutes", Cron.cronToHuman("*/5 * * * *"));
        assertEquals("Every 10 minutes", Cron.cronToHuman("*/10 * * * *"));
    }

    @Test
    @DisplayName("Cron cronToHuman every hour")
    void cronToHumanEveryHour() {
        assertEquals("Every hour", Cron.cronToHuman("0 * * * *"));
    }

    @Test
    @DisplayName("Cron cronToHuman every N hours")
    void cronToHumanEveryNHours() {
        assertEquals("Every 2 hours", Cron.cronToHuman("0 */2 * * *"));
    }

    @Test
    @DisplayName("Cron cronToHuman daily")
    void cronToHumanDaily() {
        String result = Cron.cronToHuman("30 9 * * *");
        assertTrue(result.contains("Every day at"));
    }

    @Test
    @DisplayName("Cron cronToHuman weekdays")
    void cronToHumanWeekdays() {
        String result = Cron.cronToHuman("0 9 * * 1-5");
        assertTrue(result.contains("Weekdays at"));
    }

    @Test
    @DisplayName("Cron cronToHuman specific day of week")
    void cronToHumanSpecificDayOfWeek() {
        String result = Cron.cronToHuman("0 9 * * 1");
        assertTrue(result.contains("Monday"));
    }

    @Test
    @DisplayName("Cron cronToHuman invalid")
    void cronToHumanInvalid() {
        assertEquals("invalid", Cron.cronToHuman("invalid"));
    }

    @Test
    @DisplayName("Cron isValidCronExpression true")
    void isValidCronExpressionTrue() {
        assertTrue(Cron.isValidCronExpression("* * * * *"));
        assertTrue(Cron.isValidCronExpression("0 0 * * *"));
        assertTrue(Cron.isValidCronExpression("*/5 * * * *"));
    }

    @Test
    @DisplayName("Cron isValidCronExpression false")
    void isValidCronExpressionFalse() {
        assertFalse(Cron.isValidCronExpression(null));
        assertFalse(Cron.isValidCronExpression(""));
        assertFalse(Cron.isValidCronExpression("* * * *"));
        assertFalse(Cron.isValidCronExpression("60 * * * *")); // Invalid minute
    }
}