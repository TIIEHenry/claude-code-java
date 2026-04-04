/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BashParser.
 */
class BashParserTest {

    @Test
    @DisplayName("BashParser parse null returns empty result")
    void parseNull() {
        BashParser.BashParseResult result = BashParser.parse(null);
        assertEquals("", result.original());
        assertEquals("", result.processed());
        assertTrue(result.elements().isEmpty());
        assertFalse(result.isPipeline());
        assertEquals(BashParser.CommandType.UNKNOWN, result.commandType());
    }

    @Test
    @DisplayName("BashParser parse empty returns empty result")
    void parseEmpty() {
        BashParser.BashParseResult result = BashParser.parse("");
        assertEquals("", result.original());
    }

    @Test
    @DisplayName("BashParser parse simple command")
    void parseSimpleCommand() {
        BashParser.BashParseResult result = BashParser.parse("ls -la");
        assertEquals("ls -la", result.original());
        assertEquals(BashParser.CommandType.STANDARD, result.commandType());
        assertFalse(result.isPipeline());
    }

    @Test
    @DisplayName("BashParser parse with variable")
    void parseWithVariable() {
        BashParser.BashParseResult result = BashParser.parse("echo $HOME");
        assertTrue(result.hasVariables());
    }

    @Test
    @DisplayName("BashParser parse with command substitution")
    void parseWithCommandSubstitution() {
        BashParser.BashParseResult result = BashParser.parse("echo $(pwd)");
        assertTrue(result.hasCommandSubstitution());
    }

    @Test
    @DisplayName("BashParser parse with glob")
    void parseWithGlob() {
        BashParser.BashParseResult result = BashParser.parse("ls *.java");
        assertTrue(result.hasGlob());
    }

    @Test
    @DisplayName("BashParser parse pipeline")
    void parsePipeline() {
        BashParser.BashParseResult result = BashParser.parse("ls | grep test");
        assertTrue(result.isPipeline());
    }

    @Test
    @DisplayName("BashParser needsConfirmation true for destructive commands")
    void needsConfirmationDestructive() {
        assertTrue(BashParser.needsConfirmation("rm -rf /tmp"));
        assertTrue(BashParser.needsConfirmation("rm -rf dir"));
    }

    @Test
    @DisplayName("BashParser needsConfirmation true for force flag")
    void needsConfirmationForce() {
        assertTrue(BashParser.needsConfirmation("git push -f"));
        assertTrue(BashParser.needsConfirmation("npm install --force"));
    }

    @Test
    @DisplayName("BashParser needsConfirmation false for safe commands")
    void needsConfirmationSafe() {
        assertFalse(BashParser.needsConfirmation("ls -la"));
        assertFalse(BashParser.needsConfirmation("cat file.txt"));
    }

    @Test
    @DisplayName("BashParser BashElement record")
    void bashElementRecord() {
        BashParser.BashElement element = new BashParser.BashElement(
            BashParser.BashElementType.VARIABLE, "$HOME", "HOME", 5
        );
        assertEquals(BashParser.BashElementType.VARIABLE, element.type());
        assertEquals("$HOME", element.raw());
        assertEquals("HOME", element.value());
        assertEquals(5, element.position());
    }

    @Test
    @DisplayName("BashParser BashElementType enum values")
    void bashElementTypeEnum() {
        BashParser.BashElementType[] types = BashParser.BashElementType.values();
        assertEquals(8, types.length);
        assertEquals(BashParser.BashElementType.VARIABLE, BashParser.BashElementType.valueOf("VARIABLE"));
        assertEquals(BashParser.BashElementType.COMMAND_SUBSTITUTION, BashParser.BashElementType.valueOf("COMMAND_SUBSTITUTION"));
        assertEquals(BashParser.BashElementType.GLOB, BashParser.BashElementType.valueOf("GLOB"));
        assertEquals(BashParser.BashElementType.REDIRECT, BashParser.BashElementType.valueOf("REDIRECT"));
        assertEquals(BashParser.BashElementType.PIPE, BashParser.BashElementType.valueOf("PIPE"));
        assertEquals(BashParser.BashElementType.HEREDOC, BashParser.BashElementType.valueOf("HEREDOC"));
        assertEquals(BashParser.BashElementType.QUOTE, BashParser.BashElementType.valueOf("QUOTE"));
        assertEquals(BashParser.BashElementType.ESCAPE, BashParser.BashElementType.valueOf("ESCAPE"));
    }

    @Test
    @DisplayName("BashParser CommandType enum values")
    void commandTypeEnum() {
        BashParser.CommandType[] types = BashParser.CommandType.values();
        assertEquals(7, types.length);
        assertEquals(BashParser.CommandType.STANDARD, BashParser.CommandType.valueOf("STANDARD"));
        assertEquals(BashParser.CommandType.LOCAL_SCRIPT, BashParser.CommandType.valueOf("LOCAL_SCRIPT"));
        assertEquals(BashParser.CommandType.ASSIGNMENT, BashParser.CommandType.valueOf("ASSIGNMENT"));
        assertEquals(BashParser.CommandType.BUILTIN, BashParser.CommandType.valueOf("BUILTIN"));
        assertEquals(BashParser.CommandType.FUNCTION, BashParser.CommandType.valueOf("FUNCTION"));
        assertEquals(BashParser.CommandType.ALIAS, BashParser.CommandType.valueOf("ALIAS"));
        assertEquals(BashParser.CommandType.UNKNOWN, BashParser.CommandType.valueOf("UNKNOWN"));
    }

    @Test
    @DisplayName("BashParser BashParseResult empty")
    void bashParseResultEmpty() {
        BashParser.BashParseResult empty = BashParser.BashParseResult.empty();
        assertEquals("", empty.original());
        assertEquals("", empty.processed());
        assertTrue(empty.elements().isEmpty());
        assertFalse(empty.isPipeline());
        assertEquals(BashParser.CommandType.UNKNOWN, empty.commandType());
    }

    @Test
    @DisplayName("BashParser BashParseResult hasVariables")
    void bashParseResultHasVariables() {
        BashParser.BashParseResult result = new BashParser.BashParseResult(
            "echo $HOME", "echo ",
            List.of(new BashParser.BashElement(BashParser.BashElementType.VARIABLE, "$HOME", "HOME", 5)),
            false, BashParser.CommandType.STANDARD
        );
        assertTrue(result.hasVariables());
        assertFalse(result.hasCommandSubstitution());
        assertFalse(result.hasGlob());
    }

    @Test
    @DisplayName("BashParser local script detection")
    void parseLocalScript() {
        BashParser.BashParseResult result = BashParser.parse("./script.sh");
        assertEquals(BashParser.CommandType.LOCAL_SCRIPT, result.commandType());
    }

    @Test
    @DisplayName("BashParser assignment detection")
    void parseAssignment() {
        BashParser.BashParseResult result = BashParser.parse("VAR=value");
        assertEquals(BashParser.CommandType.ASSIGNMENT, result.commandType());
    }
}