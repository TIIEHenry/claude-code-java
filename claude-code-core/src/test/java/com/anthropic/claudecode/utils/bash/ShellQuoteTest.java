/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShellQuote.
 */
class ShellQuoteTest {

    @Test
    @DisplayName("ShellQuote quote null returns empty quotes")
    void quoteNull() {
        assertEquals("''", ShellQuote.quote(null));
    }

    @Test
    @DisplayName("ShellQuote quote empty returns empty quotes")
    void quoteEmpty() {
        assertEquals("''", ShellQuote.quote(""));
    }

    @Test
    @DisplayName("ShellQuote quote simple string without special chars")
    void quoteSimpleString() {
        assertEquals("hello", ShellQuote.quote("hello"));
        assertEquals("test123", ShellQuote.quote("test123"));
    }

    @Test
    @DisplayName("ShellQuote quote string with spaces")
    void quoteStringWithSpaces() {
        String quoted = ShellQuote.quote("hello world");
        assertTrue(quoted.contains("hello world"));
        assertTrue(quoted.startsWith("'") || quoted.startsWith("\""));
    }

    @Test
    @DisplayName("ShellQuote quote string with single quotes uses double quotes")
    void quoteStringWithSingleQuotes() {
        String quoted = ShellQuote.quote("it's");
        assertTrue(quoted.startsWith("\""));
        assertTrue(quoted.endsWith("\""));
    }

    @Test
    @DisplayName("ShellQuote quoteDouble escapes special chars")
    void quoteDouble() {
        String quoted = ShellQuote.quoteDouble("hello $HOME");
        assertTrue(quoted.startsWith("\""));
        assertTrue(quoted.endsWith("\""));
        assertTrue(quoted.contains("\\$"));
    }

    @Test
    @DisplayName("ShellQuote quoteDouble null returns empty")
    void quoteDoubleNull() {
        assertEquals("\"\"", ShellQuote.quoteDouble(null));
    }

    @Test
    @DisplayName("ShellQuote quoteSingle wraps in single quotes")
    void quoteSingle() {
        assertEquals("'hello'", ShellQuote.quoteSingle("hello"));
    }

    @Test
    @DisplayName("ShellQuote quoteSingle null returns empty")
    void quoteSingleNull() {
        assertEquals("''", ShellQuote.quoteSingle(null));
    }

    @Test
    @DisplayName("ShellQuote quoteSingle escapes single quotes")
    void quoteSingleWithSingleQuote() {
        String quoted = ShellQuote.quoteSingle("it's");
        assertTrue(quoted.contains("'\"'\"'") || quoted.contains("\\'"));
    }

    @Test
    @DisplayName("ShellQuote needsQuoting true for strings with special chars")
    void needsQuotingTrue() {
        assertTrue(ShellQuote.needsQuoting("hello world"));
        assertTrue(ShellQuote.needsQuoting("test$var"));
        assertTrue(ShellQuote.needsQuoting("a && b"));
        assertTrue(ShellQuote.needsQuoting("a | b"));
        assertTrue(ShellQuote.needsQuoting("a;b"));
        assertTrue(ShellQuote.needsQuoting("test*"));
        assertTrue(ShellQuote.needsQuoting("test?"));
    }

    @Test
    @DisplayName("ShellQuote needsQuoting false for simple strings")
    void needsQuotingFalse() {
        assertFalse(ShellQuote.needsQuoting("hello"));
        assertFalse(ShellQuote.needsQuoting("test123"));
        assertFalse(ShellQuote.needsQuoting("path/to/file"));
    }

    @Test
    @DisplayName("ShellQuote needsQuoting false for null and empty")
    void needsQuotingNullEmpty() {
        assertFalse(ShellQuote.needsQuoting(null));
        assertFalse(ShellQuote.needsQuoting(""));
    }

    @Test
    @DisplayName("ShellQuote unquote single quoted string")
    void unquoteSingleQuoted() {
        assertEquals("hello", ShellQuote.unquote("'hello'"));
        assertEquals("hello world", ShellQuote.unquote("'hello world'"));
    }

    @Test
    @DisplayName("ShellQuote unquote double quoted string")
    void unquoteDoubleQuoted() {
        assertEquals("hello", ShellQuote.unquote("\"hello\""));
        assertEquals("hello\"world", ShellQuote.unquote("\"hello\\\"world\""));
    }

    @Test
    @DisplayName("ShellQuote unquote null returns null")
    void unquoteNull() {
        assertNull(ShellQuote.unquote(null));
    }

    @Test
    @DisplayName("ShellQuote unquote unquoted string returns same")
    void unquoteUnquoted() {
        assertEquals("hello", ShellQuote.unquote("hello"));
    }

    @Test
    @DisplayName("ShellQuote quoteForShell with different shell types")
    void quoteForShell() {
        String result = ShellQuote.quoteForShell("hello world", ShellQuote.ShellType.BASH);
        assertNotNull(result);
        assertTrue(result.contains("hello world"));

        result = ShellQuote.quoteForShell("hello world", ShellQuote.ShellType.ZSH);
        assertNotNull(result);

        result = ShellQuote.quoteForShell("hello world", ShellQuote.ShellType.FISH);
        assertNotNull(result);

        result = ShellQuote.quoteForShell("hello world", ShellQuote.ShellType.SH);
        assertNotNull(result);
    }

    @Test
    @DisplayName("ShellQuote ShellType enum values")
    void shellTypeEnum() {
        ShellQuote.ShellType[] types = ShellQuote.ShellType.values();
        assertEquals(5, types.length);
        assertEquals(ShellQuote.ShellType.ZSH, ShellQuote.ShellType.valueOf("ZSH"));
        assertEquals(ShellQuote.ShellType.BASH, ShellQuote.ShellType.valueOf("BASH"));
        assertEquals(ShellQuote.ShellType.FISH, ShellQuote.ShellType.valueOf("FISH"));
        assertEquals(ShellQuote.ShellType.SH, ShellQuote.ShellType.valueOf("SH"));
        assertEquals(ShellQuote.ShellType.UNKNOWN, ShellQuote.ShellType.valueOf("UNKNOWN"));
    }

    @Test
    @DisplayName("ShellQuote quoteAll quotes all strings")
    void quoteAll() {
        String[] input = {"hello", "world", "test"};
        String[] quoted = ShellQuote.quoteAll(input);
        assertEquals(3, quoted.length);
    }

    @Test
    @DisplayName("ShellQuote quoteAll null returns empty array")
    void quoteAllNull() {
        String[] quoted = ShellQuote.quoteAll(null);
        assertEquals(0, quoted.length);
    }

    @Test
    @DisplayName("ShellQuote buildCommand joins quoted args")
    void buildCommand() {
        String cmd = ShellQuote.buildCommand("echo", "hello world", "test");
        assertNotNull(cmd);
        assertTrue(cmd.contains("echo"));
    }

    @Test
    @DisplayName("ShellQuote buildCommand null returns empty")
    void buildCommandNull() {
        assertEquals("", ShellQuote.buildCommand((String[]) null));
    }

    @Test
    @DisplayName("ShellQuote buildCommand empty array returns empty")
    void buildCommandEmpty() {
        assertEquals("", ShellQuote.buildCommand());
    }
}