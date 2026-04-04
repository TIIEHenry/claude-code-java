/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OutputRedirection.
 */
class OutputRedirectionTest {

    @Test
    @DisplayName("OutputRedirection record creation")
    void recordCreation() {
        OutputRedirection redirect = new OutputRedirection("file.txt", ">");
        assertEquals("file.txt", redirect.target());
        assertEquals(">", redirect.operator());
    }

    @Test
    @DisplayName("OutputRedirection isAppend true for >>")
    void isAppendTrue() {
        OutputRedirection redirect = new OutputRedirection("file.txt", ">>");
        assertTrue(redirect.isAppend());
    }

    @Test
    @DisplayName("OutputRedirection isAppend false for >")
    void isAppendFalse() {
        OutputRedirection redirect = new OutputRedirection("file.txt", ">");
        assertFalse(redirect.isAppend());
    }

    @Test
    @DisplayName("OutputRedirection isOverwrite true for >")
    void isOverwriteTrue() {
        OutputRedirection redirect = new OutputRedirection("file.txt", ">");
        assertTrue(redirect.isOverwrite());
    }

    @Test
    @DisplayName("OutputRedirection isOverwrite false for >>")
    void isOverwriteFalse() {
        OutputRedirection redirect = new OutputRedirection("file.txt", ">>");
        assertFalse(redirect.isOverwrite());
    }

    @Test
    @DisplayName("OutputRedirection with different targets")
    void differentTargets() {
        OutputRedirection toFile = new OutputRedirection("output.log", ">");
        OutputRedirection toNull = new OutputRedirection("/dev/null", ">");
        OutputRedirection toPath = new OutputRedirection("/var/log/app.log", ">>");

        assertEquals("output.log", toFile.target());
        assertEquals("/dev/null", toNull.target());
        assertEquals("/var/log/app.log", toPath.target());
    }

    @Test
    @DisplayName("OutputRedirection isAppend false for unknown operator")
    void isAppendUnknownOperator() {
        OutputRedirection redirect = new OutputRedirection("file.txt", "|");
        assertFalse(redirect.isAppend());
    }

    @Test
    @DisplayName("OutputRedirection isOverwrite false for unknown operator")
    void isOverwriteUnknownOperator() {
        OutputRedirection redirect = new OutputRedirection("file.txt", "|");
        assertFalse(redirect.isOverwrite());
    }

    @Test
    @DisplayName("OutputRedirection empty target")
    void emptyTarget() {
        OutputRedirection redirect = new OutputRedirection("", ">");
        assertEquals("", redirect.target());
    }

    @Test
    @DisplayName("OutputRedirection null target")
    void nullTarget() {
        OutputRedirection redirect = new OutputRedirection(null, ">");
        assertNull(redirect.target());
    }
}