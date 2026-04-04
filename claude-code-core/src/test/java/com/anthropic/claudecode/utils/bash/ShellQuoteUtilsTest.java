/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShellQuoteUtils.
 */
class ShellQuoteUtilsTest {

    @Test
    @DisplayName("quote handles safe strings without quoting")
    void quoteSafeStrings() {
        assertEquals("simple", ShellQuoteUtils.quote("simple"));
        assertEquals("test.txt", ShellQuoteUtils.quote("test.txt"));
        assertEquals("path/to/file", ShellQuoteUtils.quote("path/to/file"));
    }

    @Test
    @DisplayName("quote handles strings needing quoting")
    void quoteStringsNeedingQuoting() {
        assertEquals("'hello world'", ShellQuoteUtils.quote("hello world"));
        assertEquals("'test$var'", ShellQuoteUtils.quote("test$var"));
        // Single quote escaping: ' becomes '"'"' (end quote, escaped quote, new quote)
        String expected = "'it'" + '"' + "'" + '"' + "'s'";
        assertEquals(expected, ShellQuoteUtils.quote("it's"));
    }

    @Test
    @DisplayName("quote handles null and empty")
    void quoteNullOrEmpty() {
        assertEquals("''", ShellQuoteUtils.quote((String) null));
        assertEquals("''", ShellQuoteUtils.quote(""));
    }

    @Test
    @DisplayName("quote handles list of arguments")
    void quoteList() {
        List<String> args = List.of("echo", "hello world");
        assertEquals("echo 'hello world'", ShellQuoteUtils.quote(args));
        List<String> catArgs = List.of("cat", "file.txt");
        assertEquals("cat file.txt", ShellQuoteUtils.quote(catArgs));
    }

    @Test
    @DisplayName("quote handles array of arguments")
    void quoteArray() {
        assertEquals("echo test", ShellQuoteUtils.quote("echo", "test"));
        assertEquals("ls -la", ShellQuoteUtils.quote("ls", "-la"));
    }

    @Test
    @DisplayName("isSafe detects safe strings")
    void isSafeWorks() {
        assertTrue(ShellQuoteUtils.isSafe("simple"));
        assertTrue(ShellQuoteUtils.isSafe("test-file.txt"));
        assertTrue(ShellQuoteUtils.isSafe("path/to/file"));
        assertTrue(ShellQuoteUtils.isSafe("user@email.com"));
        assertFalse(ShellQuoteUtils.isSafe("hello world"));
        assertFalse(ShellQuoteUtils.isSafe("test$var"));
        assertFalse(ShellQuoteUtils.isSafe(null));
        assertFalse(ShellQuoteUtils.isSafe(""));
    }

    @Test
    @DisplayName("escapeForDoubleQuotes escapes special chars")
    void escapeForDoubleQuotesWorks() {
        assertEquals("test\\\\file", ShellQuoteUtils.escapeForDoubleQuotes("test\\file"));
        assertEquals("test\\\"quoted\\\"", ShellQuoteUtils.escapeForDoubleQuotes("test\"quoted\""));
        assertEquals("test\\$var", ShellQuoteUtils.escapeForDoubleQuotes("test$var"));
    }

    @Test
    @DisplayName("escapeForSingleQuotes escapes single quotes")
    void escapeForSingleQuotesWorks() {
        assertEquals("it'\"'\"'s", ShellQuoteUtils.escapeForSingleQuotes("it's"));
        assertEquals("normal", ShellQuoteUtils.escapeForSingleQuotes("normal"));
    }

    @Test
    @DisplayName("unquote removes quotes")
    void unquoteWorks() {
        assertEquals("hello", ShellQuoteUtils.unquote("'hello'"));
        assertEquals("hello world", ShellQuoteUtils.unquote("'hello world'"));
        assertEquals("test", ShellQuoteUtils.unquote("\"test\""));
        assertEquals("unquoted", ShellQuoteUtils.unquote("unquoted"));
    }

    @Test
    @DisplayName("parseCommand splits on whitespace")
    void parseCommandWorks() {
        List<String> tokens = ShellQuoteUtils.parseCommand("echo hello world");
        assertEquals(List.of("echo", "hello", "world"), tokens);

        tokens = ShellQuoteUtils.parseCommand("ls -la /tmp");
        assertEquals(List.of("ls", "-la", "/tmp"), tokens);
    }

    @Test
    @DisplayName("parseCommand handles quoted strings")
    void parseCommandHandlesQuotes() {
        List<String> tokens = ShellQuoteUtils.parseCommand("echo 'hello world'");
        // parseCommand returns the whole quoted content as one token
        assertEquals(List.of("echo", "hello world"), tokens);

        tokens = ShellQuoteUtils.parseCommand("echo \"hello world\"");
        assertEquals(List.of("echo", "hello world"), tokens);
    }

    @Test
    @DisplayName("parseCommand handles escapes")
    void parseCommandHandlesEscapes() {
        List<String> tokens = ShellQuoteUtils.parseCommand("echo hello\\ world");
        assertEquals(List.of("echo", "hello world"), tokens);
    }

    @Test
    @DisplayName("parseCommand handles empty and null")
    void parseCommandHandlesEmpty() {
        assertEquals(List.of(), ShellQuoteUtils.parseCommand(""));
        assertEquals(List.of(), ShellQuoteUtils.parseCommand(null));
        assertEquals(List.of(), ShellQuoteUtils.parseCommand("   "));
    }

    @Test
    @DisplayName("tryParseShellCommand returns success for valid commands")
    void tryParseShellCommandSuccess() {
        ShellQuoteUtils.ShellParseResult result = ShellQuoteUtils.tryParseShellCommand("echo test");
        assertTrue(result.success);
        assertNull(result.error);
        assertEquals(List.of("echo", "test"), result.tokens);
    }

    @Test
    @DisplayName("tryQuoteShellArgs quotes various types")
    void tryQuoteShellArgsWorks() {
        ShellQuoteUtils.ShellQuoteResult result = ShellQuoteUtils.tryQuoteShellArgs(
            List.of("echo", "hello world")
        );
        assertTrue(result.success);
        assertEquals("echo 'hello world'", result.quoted);
    }

    @Test
    @DisplayName("hasMalformedTokens detects unterminated quotes")
    void hasMalformedTokensWorks() {
        // Unterminated single quote
        assertTrue(ShellQuoteUtils.hasMalformedTokens("echo 'test", List.of("echo", "'test")));

        // Balanced command
        assertFalse(ShellQuoteUtils.hasMalformedTokens("echo test", List.of("echo", "test")));

        // Unterminated double quote
        assertTrue(ShellQuoteUtils.hasMalformedTokens("echo \"test", List.of("echo", "\"test")));
    }

    @Test
    @DisplayName("ShellParseResult factory methods work")
    void shellParseResultFactory() {
        ShellQuoteUtils.ShellParseResult success = ShellQuoteUtils.ShellParseResult.success(List.of("a", "b"));
        assertTrue(success.success);
        assertEquals(List.of("a", "b"), success.tokens);
        assertNull(success.error);

        ShellQuoteUtils.ShellParseResult failure = ShellQuoteUtils.ShellParseResult.failure("error");
        assertFalse(failure.success);
        assertEquals(List.of(), failure.tokens);
        assertEquals("error", failure.error);
    }

    @Test
    @DisplayName("ShellQuoteResult factory methods work")
    void shellQuoteResultFactory() {
        ShellQuoteUtils.ShellQuoteResult success = ShellQuoteUtils.ShellQuoteResult.success("quoted");
        assertTrue(success.success);
        assertEquals("quoted", success.quoted);
        assertNull(success.error);

        ShellQuoteUtils.ShellQuoteResult failure = ShellQuoteUtils.ShellQuoteResult.failure("error");
        assertFalse(failure.success);
        assertNull(failure.quoted);
        assertEquals("error", failure.error);
    }
}