/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegexParsedCommand.
 */
class RegexParsedCommandTest {

    @Test
    @DisplayName("originalCommand returns the command string")
    void originalCommandWorks() {
        RegexParsedCommand cmd = new RegexParsedCommand("echo hello");
        assertEquals("echo hello", cmd.originalCommand());

        RegexParsedCommand empty = new RegexParsedCommand("");
        assertEquals("", empty.originalCommand());

        RegexParsedCommand nullCmd = new RegexParsedCommand(null);
        assertEquals("", nullCmd.originalCommand());
    }

    @Test
    @DisplayName("toString returns the command string")
    void toStringWorks() {
        RegexParsedCommand cmd = new RegexParsedCommand("ls -la");
        assertEquals("ls -la", cmd.toString());
    }

    @Test
    @DisplayName("getPipeSegments splits on unquoted pipe")
    void getPipeSegmentsWorks() {
        RegexParsedCommand simple = new RegexParsedCommand("echo hello");
        List<String> segments = simple.getPipeSegments();
        assertEquals(1, segments.size());
        assertEquals("echo hello", segments.get(0));

        RegexParsedCommand piped = new RegexParsedCommand("cat file | grep test");
        segments = piped.getPipeSegments();
        assertEquals(2, segments.size());
        assertEquals("cat file", segments.get(0));
        assertEquals("grep test", segments.get(1));
    }

    @Test
    @DisplayName("getPipeSegments handles quoted pipe")
    void getPipeSegmentsHandlesQuoted() {
        RegexParsedCommand cmd = new RegexParsedCommand("echo 'a|b'");
        List<String> segments = cmd.getPipeSegments();
        assertEquals(1, segments.size());
        assertTrue(segments.get(0).contains("'a|b'"));
    }

    @Test
    @DisplayName("getTreeSitterAnalysis returns null")
    void getTreeSitterAnalysisReturnsNull() {
        RegexParsedCommand cmd = new RegexParsedCommand("echo test");
        assertNull(cmd.getTreeSitterAnalysis());
    }

    @Test
    @DisplayName("withoutOutputRedirections works")
    void withoutOutputRedirectionsWorks() {
        RegexParsedCommand simple = new RegexParsedCommand("echo hello");
        assertEquals("echo hello", simple.withoutOutputRedirections());
    }

    @Test
    @DisplayName("getOutputRedirections returns list")
    void getOutputRedirectionsWorks() {
        RegexParsedCommand simple = new RegexParsedCommand("echo hello");
        List<OutputRedirection> redirections = simple.getOutputRedirections();
        assertNotNull(redirections);
    }
}