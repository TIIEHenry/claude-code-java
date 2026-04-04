/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Common constants.
 */
class CommonTest {

    @Test
    @DisplayName("App info constants are correct")
    void appInfoConstants() {
        assertEquals("Claude Code", Common.APP_NAME);
        assertEquals("1.0.0", Common.APP_VERSION);
        assertEquals("Anthropic", Common.COMPANY_NAME);
    }

    @Test
    @DisplayName("Default constants are correct")
    void defaultConstants() {
        assertEquals(4096, Common.DEFAULT_MAX_TOKENS);
        assertEquals(120000, Common.DEFAULT_TIMEOUT_MS);
        assertEquals(600000, Common.MAX_TIMEOUT_MS);
        assertEquals(100, Common.DEFAULT_MAX_TURNS);
        assertEquals(4, Common.DEFAULT_THREAD_POOL_SIZE);
    }

    @Test
    @DisplayName("Limit constants are correct")
    void limitConstants() {
        assertEquals(10 * 1024 * 1024, Common.MAX_FILE_SIZE_BYTES);
        assertEquals(2000, Common.MAX_LINE_LENGTH);
        assertEquals(1000, Common.MAX_OUTPUT_LINES);
        assertEquals(1000, Common.MAX_GLOB_RESULTS);
        assertEquals(100, Common.MAX_GREP_RESULTS);
    }

    @Test
    @DisplayName("Timeout constants are correct")
    void timeoutConstants() {
        assertEquals(30000, Common.CONNECT_TIMEOUT_MS);
        assertEquals(120000, Common.READ_TIMEOUT_MS);
        assertEquals(120000, Common.WRITE_TIMEOUT_MS);
    }

    @Test
    @DisplayName("Path constants are correct")
    void pathConstants() {
        assertEquals(".claude", Common.CLAUDE_DIR);
        assertEquals("config.json", Common.CONFIG_FILE);
        assertEquals("CLAUDE.md", Common.MEMORY_FILE);
        assertEquals("skills", Common.SKILLS_DIR);
    }

    @Test
    @DisplayName("Encoding constants are correct")
    void encodingConstants() {
        assertEquals("UTF-8", Common.DEFAULT_CHARSET);
        assertNotNull(Common.LINE_SEPARATOR);
    }
}