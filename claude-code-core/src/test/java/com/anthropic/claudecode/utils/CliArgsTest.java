/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CliArgs.
 */
class CliArgsTest {

    @Test
    @DisplayName("CliArgs eagerParseCliFlag parses --flag=value syntax")
    void eagerParseFlagEqualsSyntax() {
        String[] args = {"--name=value", "--other=test"};
        String result = CliArgs.eagerParseCliFlag("--name", args);

        assertEquals("value", result);
    }

    @Test
    @DisplayName("CliArgs eagerParseCliFlag parses --flag value syntax")
    void eagerParseFlagSpaceSyntax() {
        String[] args = {"--name", "value", "--other", "test"};
        String result = CliArgs.eagerParseCliFlag("--name", args);

        assertEquals("value", result);
    }

    @Test
    @DisplayName("CliArgs eagerParseCliFlag returns null for missing flag")
    void eagerParseFlagMissing() {
        String[] args = {"--other", "value"};
        String result = CliArgs.eagerParseCliFlag("--name", args);

        assertNull(result);
    }

    @Test
    @DisplayName("CliArgs eagerParseCliFlag handles empty args")
    void eagerParseFlagEmpty() {
        String[] args = {};
        String result = CliArgs.eagerParseCliFlag("--name", args);

        assertNull(result);
    }

    @Test
    @DisplayName("CliArgs eagerParseCliFlag handles flag at end without value")
    void eagerParseFlagAtEnd() {
        String[] args = {"--name"};
        String result = CliArgs.eagerParseCliFlag("--name", args);

        assertNull(result);
    }

    @Test
    @DisplayName("CliArgs hasFlag returns true for present flag")
    void hasFlagTrue() {
        String[] args = {"--verbose", "--other=value"};

        assertTrue(CliArgs.hasFlag("--verbose", args));
        assertTrue(CliArgs.hasFlag("--other", args));
    }

    @Test
    @DisplayName("CliArgs hasFlag returns false for missing flag")
    void hasFlagFalse() {
        String[] args = {"--verbose"};

        assertFalse(CliArgs.hasFlag("--quiet", args));
    }

    @Test
    @DisplayName("CliArgs getBooleanFlag returns true for true value")
    void getBooleanFlagTrue() {
        String[] args = {"--enabled=true"};

        assertTrue(CliArgs.getBooleanFlag("--enabled", args));
    }

    @Test
    @DisplayName("CliArgs getBooleanFlag returns true for 1 value")
    void getBooleanFlagOne() {
        String[] args = {"--enabled=1"};

        assertTrue(CliArgs.getBooleanFlag("--enabled", args));
    }

    @Test
    @DisplayName("CliArgs getBooleanFlag returns false for false value")
    void getBooleanFlagFalse() {
        String[] args = {"--enabled=false"};

        assertFalse(CliArgs.getBooleanFlag("--enabled", args));
    }

    @Test
    @DisplayName("CliArgs getBooleanFlag returns true when flag present without value")
    void getBooleanFlagPresent() {
        String[] args = {"--enabled"};

        assertTrue(CliArgs.getBooleanFlag("--enabled", args));
    }

    @Test
    @DisplayName("CliArgs getIntFlag returns int value")
    void getIntFlagValue() {
        String[] args = {"--count=42"};

        assertEquals(42, CliArgs.getIntFlag("--count", args, 0));
    }

    @Test
    @DisplayName("CliArgs getIntFlag returns default for missing flag")
    void getIntFlagDefault() {
        String[] args = {"--other=value"};

        assertEquals(10, CliArgs.getIntFlag("--count", args, 10));
    }

    @Test
    @DisplayName("CliArgs getIntFlag returns default for invalid value")
    void getIntFlagInvalid() {
        String[] args = {"--count=abc"};

        assertEquals(5, CliArgs.getIntFlag("--count", args, 5));
    }

    @Test
    @DisplayName("CliArgs extractArgsAfterDoubleDash extracts after --")
    void extractArgsAfterDoubleDash() {
        String[] args = {"subcommand", "arg1", "arg2"};
        CliArgs.ExtractedArgs result = CliArgs.extractArgsAfterDoubleDash("--", args);

        assertEquals("subcommand", result.command());
        assertEquals(2, result.args().length);
        assertEquals("arg1", result.args()[0]);
        assertEquals("arg2", result.args()[1]);
    }

    @Test
    @DisplayName("CliArgs extractArgsAfterDoubleDash returns original when no --")
    void extractArgsNoDoubleDash() {
        String[] args = {"arg1", "arg2"};
        CliArgs.ExtractedArgs result = CliArgs.extractArgsAfterDoubleDash("command", args);

        assertEquals("command", result.command());
        assertEquals(2, result.args().length);
    }

    @Test
    @DisplayName("CliArgs extractArgsAfterDoubleDash handles null args")
    void extractArgsNullArgs() {
        CliArgs.ExtractedArgs result = CliArgs.extractArgsAfterDoubleDash("command", null);

        assertEquals("command", result.command());
        assertEquals(0, result.args().length);
    }

    @Test
    @DisplayName("CliArgs ExtractedArgs record works")
    void extractedArgsRecord() {
        CliArgs.ExtractedArgs extracted = new CliArgs.ExtractedArgs("cmd", new String[]{"a", "b"});

        assertEquals("cmd", extracted.command());
        assertEquals(2, extracted.args().length);
    }
}