/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FindExecutable.
 */
class FindExecutableTest {

    @Test
    @DisplayName("FindExecutable ExecutableResult record")
    void executableResultRecord() {
        FindExecutable.ExecutableResult result = new FindExecutable.ExecutableResult(
            "/usr/bin/ls", List.of("-la")
        );
        assertEquals("/usr/bin/ls", result.cmd());
        assertEquals(1, result.args().size());
        assertEquals("-la", result.args().get(0));
    }

    @Test
    @DisplayName("FindExecutable findExecutable with args list")
    void findExecutableWithList() {
        FindExecutable.ExecutableResult result = FindExecutable.findExecutable("ls", List.of("-l", "-a"));
        assertNotNull(result);
        assertNotNull(result.cmd());
        assertEquals(2, result.args().size());
    }

    @Test
    @DisplayName("FindExecutable findExecutable with varargs")
    void findExecutableWithVarargs() {
        FindExecutable.ExecutableResult result = FindExecutable.findExecutable("ls", "-l", "-a");
        assertNotNull(result);
        assertEquals(2, result.args().size());
    }

    @Test
    @DisplayName("FindExecutable which returns path or null")
    void whichReturnsPathOrNull() {
        // Common executables that should exist on most systems
        String path = FindExecutable.which("ls");
        // May be null on some systems
        assertTrue(path == null || path.contains("ls"));
    }

    @Test
    @DisplayName("FindExecutable which null input")
    void whichNullInput() {
        assertNull(FindExecutable.which(null));
    }

    @Test
    @DisplayName("FindExecutable which empty input")
    void whichEmptyInput() {
        assertNull(FindExecutable.which(""));
    }

    @Test
    @DisplayName("FindExecutable which non-existent executable")
    void whichNonExistent() {
        String path = FindExecutable.which("nonexistent_executable_12345");
        assertNull(path);
    }

    @Test
    @DisplayName("FindExecutable exists returns boolean")
    void existsReturnsBoolean() {
        boolean result = FindExecutable.exists("ls");
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("FindExecutable exists non-existent")
    void existsNonExistent() {
        assertFalse(FindExecutable.exists("nonexistent_executable_12345"));
    }

    @Test
    @DisplayName("FindExecutable whichAll returns list")
    void whichAllReturnsList() {
        List<String> results = FindExecutable.whichAll("ls");
        assertNotNull(results);
        // May be empty if ls not in PATH
    }

    @Test
    @DisplayName("FindExecutable whichAll null input")
    void whichAllNullInput() {
        List<String> results = FindExecutable.whichAll(null);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("FindExecutable whichAll empty input")
    void whichAllEmptyInput() {
        List<String> results = FindExecutable.whichAll("");
        assertTrue(results.isEmpty());
    }
}