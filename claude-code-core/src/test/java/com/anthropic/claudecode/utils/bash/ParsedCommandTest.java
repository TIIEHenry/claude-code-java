/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParsedCommand.
 */
class ParsedCommandTest {

    @Test
    @DisplayName("hasPipe detects unquoted pipe")
    void hasPipeWorks() {
        assertTrue(ParsedCommand.hasPipe("cat file | grep test"));
        assertTrue(ParsedCommand.hasPipe("echo a | echo b | echo c"));
        assertFalse(ParsedCommand.hasPipe("echo '|' is quoted"));
        assertFalse(ParsedCommand.hasPipe("echo \"|\" is quoted"));
        assertFalse(ParsedCommand.hasPipe("simple command"));
        assertFalse(ParsedCommand.hasPipe(null));
    }

    @Test
    @DisplayName("hasOutputRedirection detects unquoted redirection")
    void hasOutputRedirectionWorks() {
        assertTrue(ParsedCommand.hasOutputRedirection("cat file > output"));
        assertTrue(ParsedCommand.hasOutputRedirection("cat file >> output"));
        assertTrue(ParsedCommand.hasOutputRedirection("cat file < input"));
        assertFalse(ParsedCommand.hasOutputRedirection("echo '>' is quoted"));
        assertFalse(ParsedCommand.hasOutputRedirection("simple command"));
        assertFalse(ParsedCommand.hasOutputRedirection(null));
        // Note: << (heredoc) check depends on implementation
    }

    @Test
    @DisplayName("getCommandName extracts first word")
    void getCommandNameWorks() {
        assertEquals("ls", ParsedCommand.getCommandName("ls -la"));
        assertEquals("cat", ParsedCommand.getCommandName("cat file.txt"));
        assertEquals("git", ParsedCommand.getCommandName("git commit -m \"message\""));
        assertEquals("echo", ParsedCommand.getCommandName("echo 'hello world'"));
        assertNull(ParsedCommand.getCommandName(null));
        assertNull(ParsedCommand.getCommandName(""));
        assertNull(ParsedCommand.getCommandName("   "));
    }

    @Test
    @DisplayName("getArguments extracts remaining arguments")
    void getArgumentsWorks() {
        List<String> args = ParsedCommand.getArguments("ls -la /tmp");
        assertTrue(args.contains("-la"));
        assertTrue(args.contains("/tmp"));

        args = ParsedCommand.getArguments("git commit -m message");
        assertTrue(args.contains("commit"));
        assertTrue(args.contains("-m"));

        assertEquals(List.of(), ParsedCommand.getArguments("ls"));
        assertEquals(List.of(), ParsedCommand.getArguments(null));
        assertEquals(List.of(), ParsedCommand.getArguments(""));
    }

    @Test
    @DisplayName("parse returns IParsedCommand")
    void parseWorks() throws Exception {
        IParsedCommand cmd = ParsedCommand.parse("echo test").get();
        assertNotNull(cmd);
        assertEquals("echo test", cmd.originalCommand());
    }

    @Test
    @DisplayName("parseSync returns IParsedCommand synchronously")
    void parseSyncWorks() {
        IParsedCommand cmd = ParsedCommand.parseSync("echo test");
        assertNotNull(cmd);
        assertEquals("echo test", cmd.originalCommand());
    }

    @Test
    @DisplayName("parse handles null and empty")
    void parseHandlesEmpty() throws Exception {
        assertNull(ParsedCommand.parse(null).get());
        assertNull(ParsedCommand.parse("").get());
    }

    @Test
    @DisplayName("clearCache resets cached command")
    void clearCacheWorks() {
        ParsedCommand.parse("test1");
        ParsedCommand.clearCache();
        // After clear, cache should be empty
        // This is hard to test directly, but clearCache should not throw
        ParsedCommand.clearCache();
    }

    @Test
    @DisplayName("buildFromRoot returns RegexParsedCommand")
    void buildFromRootWorks() {
        IParsedCommand cmd = ParsedCommand.buildFromRoot("echo test", null);
        assertNotNull(cmd);
        assertTrue(cmd instanceof RegexParsedCommand);
    }
}