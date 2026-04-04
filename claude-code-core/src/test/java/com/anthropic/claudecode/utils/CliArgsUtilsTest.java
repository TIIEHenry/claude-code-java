/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CliArgsUtils.
 */
class CliArgsUtilsTest {

    @Test
    @DisplayName("CliArgsUtils parse empty args")
    void parseEmptyArgs() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{});
        assertTrue(args.isEmpty());
    }

    @Test
    @DisplayName("CliArgsUtils parse null args throws NPE")
    void parseNullArgs() {
        assertThrows(NullPointerException.class, () -> CliArgsUtils.parse(null));
    }

    @Test
    @DisplayName("CliArgsUtils parse positional args")
    void parsePositionalArgs() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"file1", "file2", "file3"});
        assertFalse(args.isEmpty());
        assertEquals(3, args.getArgCount());
        assertEquals("file1", args.getPositionalArg(0));
        assertEquals("file2", args.getPositionalArg(1));
        assertEquals("file3", args.getPositionalArg(2));
    }

    @Test
    @DisplayName("CliArgsUtils parse long option with equals")
    void parseLongOptionEquals() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--config=file.json"});
        assertTrue(args.hasOption("config"));
        assertEquals("file.json", args.getOption("config"));
    }

    @Test
    @DisplayName("CliArgsUtils parse long option with value")
    void parseLongOptionValue() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--config", "file.json"});
        assertTrue(args.hasOption("config"));
        assertEquals("file.json", args.getOption("config"));
    }

    @Test
    @DisplayName("CliArgsUtils parse long flag")
    void parseLongFlag() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--verbose"});
        assertTrue(args.hasFlag("verbose"));
    }

    @Test
    @DisplayName("CliArgsUtils parse short option with value")
    void parseShortOptionValue() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"-c", "file.json"});
        assertTrue(args.hasOption("c"));
        assertEquals("file.json", args.getOption("c"));
    }

    @Test
    @DisplayName("CliArgsUtils parse short flag")
    void parseShortFlag() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"-v"});
        assertTrue(args.hasFlag("v"));
    }

    @Test
    @DisplayName("CliArgsUtils parse multiple short flags")
    void parseMultipleShortFlags() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"-abc"});
        assertTrue(args.hasFlag("a"));
        assertTrue(args.hasFlag("b"));
        assertTrue(args.hasFlag("c"));
    }

    @Test
    @DisplayName("CliArgsUtils parse mixed args")
    void parseMixedArgs() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{
            "file1", "--verbose", "-c", "config.json", "file2", "-x"
        });

        assertEquals(2, args.getArgCount());
        assertEquals("file1", args.getPositionalArg(0));
        assertEquals("file2", args.getPositionalArg(1));

        assertTrue(args.hasFlag("verbose"));
        assertTrue(args.hasFlag("x"));

        assertTrue(args.hasOption("c"));
        assertEquals("config.json", args.getOption("c"));
    }

    @Test
    @DisplayName("CliArgsUtils getOption missing")
    void getOptionMissing() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"file"});
        assertNull(args.getOption("missing"));
    }

    @Test
    @DisplayName("CliArgsUtils getOption with default")
    void getOptionWithDefault() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"file"});
        assertEquals("default", args.getOption("missing", "default"));
    }

    @Test
    @DisplayName("CliArgsUtils getOptionAsInt")
    void getOptionAsInt() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--count=42"});
        assertEquals(42, args.getOptionAsInt("count", 0));
    }

    @Test
    @DisplayName("CliArgsUtils getOptionAsInt missing")
    void getOptionAsIntMissing() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"file"});
        assertEquals(10, args.getOptionAsInt("count", 10));
    }

    @Test
    @DisplayName("CliArgsUtils getOptionAsInt invalid")
    void getOptionAsIntInvalid() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--count=abc"});
        assertEquals(5, args.getOptionAsInt("count", 5));
    }

    @Test
    @DisplayName("CliArgsUtils getOptionAsBool true")
    void getOptionAsBoolTrue() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--flag=true"});
        assertTrue(args.getOptionAsBool("flag", false));
    }

    @Test
    @DisplayName("CliArgsUtils getOptionAsBool 1")
    void getOptionAsBoolOne() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--flag=1"});
        assertTrue(args.getOptionAsBool("flag", false));
    }

    @Test
    @DisplayName("CliArgsUtils getOptionAsBool yes")
    void getOptionAsBoolYes() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--flag=yes"});
        assertTrue(args.getOptionAsBool("flag", false));
    }

    @Test
    @DisplayName("CliArgsUtils getOptionAsBool false")
    void getOptionAsBoolFalse() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--flag=false"});
        assertFalse(args.getOptionAsBool("flag", true));
    }

    @Test
    @DisplayName("CliArgsUtils getOptionAsBool missing")
    void getOptionAsBoolMissing() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"file"});
        assertTrue(args.getOptionAsBool("flag", true));
    }

    @Test
    @DisplayName("CliArgsUtils hasFlag true")
    void hasFlagTrue() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--verbose"});
        assertTrue(args.hasFlag("verbose"));
    }

    @Test
    @DisplayName("CliArgsUtils hasFlag false")
    void hasFlagFalse() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--verbose"});
        assertFalse(args.hasFlag("quiet"));
    }

    @Test
    @DisplayName("CliArgsUtils hasOption true")
    void hasOptionTrue() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--config=file.json"});
        assertTrue(args.hasOption("config"));
    }

    @Test
    @DisplayName("CliArgsUtils hasOption false")
    void hasOptionFalse() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--config=file.json"});
        assertFalse(args.hasOption("output"));
    }

    @Test
    @DisplayName("CliArgsUtils getPositionalArgs")
    void getPositionalArgs() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"a", "b", "c"});
        List<String> positional = args.getPositionalArgs();
        assertEquals(3, positional.size());
        assertEquals(List.of("a", "b", "c"), positional);
    }

    @Test
    @DisplayName("CliArgsUtils getPositionalArg index out of bounds")
    void getPositionalArgOutOfBounds() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"a"});
        assertNull(args.getPositionalArg(5));
        assertNull(args.getPositionalArg(-1));
    }

    @Test
    @DisplayName("CliArgsUtils getPositionalArg with default")
    void getPositionalArgWithDefault() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"a"});
        assertEquals("default", args.getPositionalArg(5, "default"));
        assertEquals("a", args.getPositionalArg(0, "default"));
    }

    @Test
    @DisplayName("CliArgsUtils getFlags")
    void getFlags() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"-abc", "--verbose"});
        Set<String> flags = args.getFlags();
        assertTrue(flags.contains("a"));
        assertTrue(flags.contains("b"));
        assertTrue(flags.contains("c"));
        assertTrue(flags.contains("verbose"));
    }

    @Test
    @DisplayName("CliArgsUtils getOptions")
    void getOptions() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--config=file.json", "-o", "output.txt"});
        Map<String, String> options = args.getOptions();
        assertEquals(2, options.size());
        assertEquals("file.json", options.get("config"));
        assertEquals("output.txt", options.get("o"));
    }

    @Test
    @DisplayName("CliArgsUtils getArgCount")
    void getArgCount() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"a", "b", "c"});
        assertEquals(3, args.getArgCount());
    }

    @Test
    @DisplayName("CliArgsUtils isEmpty true")
    void isEmptyTrue() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{});
        assertTrue(args.isEmpty());
    }

    @Test
    @DisplayName("CliArgsUtils isEmpty false with positional")
    void isEmptyFalseWithPositional() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"file"});
        assertFalse(args.isEmpty());
    }

    @Test
    @DisplayName("CliArgsUtils isEmpty false with option")
    void isEmptyFalseWithOption() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--flag"});
        assertFalse(args.isEmpty());
    }

    @Test
    @DisplayName("CliArgsUtils isEmpty false with flag")
    void isEmptyFalseWithFlag() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"-v"});
        assertFalse(args.isEmpty());
    }

    @Test
    @DisplayName("CliArgsUtils toString")
    void toStringTest() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"file", "--config=test.json", "-v"});
        String str = args.toString();
        assertTrue(str.contains("CliArgs"));
        assertTrue(str.contains("options"));
        assertTrue(str.contains("flags"));
        assertTrue(str.contains("args"));
    }

    @Test
    @DisplayName("CliArgsUtils short option not followed by value becomes flag")
    void shortOptionNoValue() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"-c", "--other"});
        assertTrue(args.hasFlag("c"));
        assertFalse(args.hasOption("c"));
    }

    @Test
    @DisplayName("CliArgsUtils long option not followed by value becomes flag")
    void longOptionNoValue() {
        CliArgsUtils args = CliArgsUtils.parse(new String[]{"--config", "--other"});
        assertTrue(args.hasFlag("config"));
        assertFalse(args.hasOption("config"));
    }
}