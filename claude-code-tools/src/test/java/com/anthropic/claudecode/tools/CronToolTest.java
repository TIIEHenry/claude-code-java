/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.time.LocalDateTime;

/**
 * Tests for CronTool.
 */
@DisplayName("CronTool Tests")
class CronToolTest {

    @Test
    @DisplayName("CronTool has correct name")
    void hasCorrectName() {
        CronTool tool = new CronTool();
        assertEquals("Cron", tool.name());
    }

    @Test
    @DisplayName("CronTool input schema is valid")
    void inputSchemaIsValid() {
        CronTool tool = new CronTool();
        Map<String, Object> schema = tool.inputSchema();

        assertNotNull(schema);
        assertTrue(schema.containsKey("type"));
    }

    @Test
    @DisplayName("CronJob record works correctly")
    void cronJobRecordWorksCorrectly() {
        CronTool.CronJob job = new CronTool.CronJob(
            "job-123",
            "*/5 * * * *",
            "test prompt",
            true,
            false,
            LocalDateTime.now()
        );

        assertEquals("job-123", job.id());
        assertEquals("*/5 * * * *", job.cronExpression());
        assertEquals("test prompt", job.prompt());
        assertTrue(job.recurring());
        assertNotNull(job.createdAt());
    }

    @Test
    @DisplayName("CronTool is not read-only")
    void isNotReadOnly() {
        CronTool tool = new CronTool();
        CronTool.Input input = CronTool.Input.create("*/5 * * * *", "prompt", true);

        assertFalse(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("CronTool Input record works correctly")
    void inputRecordWorksCorrectly() {
        CronTool.Input input = new CronTool.Input(
            "create",
            "*/5 * * * *",
            "test prompt",
            true,
            false,
            null
        );

        assertEquals("create", input.action());
        assertEquals("*/5 * * * *", input.cron());
        assertEquals("test prompt", input.prompt());
        assertTrue(input.recurring());
        assertNull(input.id());
    }

    @Test
    @DisplayName("CronTool Input.create factory method")
    void inputCreateFactoryMethod() {
        CronTool.Input input = CronTool.Input.create("*/5 * * * *", "test prompt", true);

        assertEquals("create", input.action());
        assertEquals("*/5 * * * *", input.cron());
        assertEquals("test prompt", input.prompt());
        assertTrue(input.recurring());
        assertFalse(input.durable());
        assertNull(input.id());
    }

    @Test
    @DisplayName("CronTool Input with job ID for delete")
    void inputWithJobId() {
        CronTool.Input input = new CronTool.Input(
            "delete",
            null,
            null,
            false,
            false,
            "job-to-delete"
        );

        assertEquals("delete", input.action());
        assertNull(input.cron());
        assertNull(input.prompt());
        assertEquals("job-to-delete", input.id());
    }
}
