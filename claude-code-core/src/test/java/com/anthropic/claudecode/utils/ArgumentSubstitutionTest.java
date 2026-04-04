/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArgumentSubstitution.
 */
class ArgumentSubstitutionTest {

    @Test
    @DisplayName("ArgumentSubstitution parseArguments parses simple args")
    void parseArgumentsSimple() {
        List<String> args = ArgumentSubstitution.parseArguments("arg1 arg2 arg3");

        assertEquals(List.of("arg1", "arg2", "arg3"), args);
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArguments handles null")
    void parseArgumentsNull() {
        List<String> args = ArgumentSubstitution.parseArguments(null);

        assertTrue(args.isEmpty());
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArguments handles empty string")
    void parseArgumentsEmpty() {
        List<String> args = ArgumentSubstitution.parseArguments("");

        assertTrue(args.isEmpty());
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArguments handles whitespace only")
    void parseArgumentsWhitespace() {
        List<String> args = ArgumentSubstitution.parseArguments("   ");

        assertTrue(args.isEmpty());
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArguments handles double quotes")
    void parseArgumentsDoubleQuotes() {
        List<String> args = ArgumentSubstitution.parseArguments("\"arg with space\" arg2");

        assertEquals(List.of("arg with space", "arg2"), args);
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArguments handles single quotes")
    void parseArgumentsSingleQuotes() {
        List<String> args = ArgumentSubstitution.parseArguments("'arg with space' arg2");

        assertEquals(List.of("arg with space", "arg2"), args);
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArguments handles escape characters")
    void parseArgumentsEscape() {
        List<String> args = ArgumentSubstitution.parseArguments("arg\\ with\\ space arg2");

        assertEquals(List.of("arg with space", "arg2"), args);
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArgumentNames from list")
    void parseArgumentNamesList() {
        List<String> names = ArgumentSubstitution.parseArgumentNames(List.of("foo", "bar", "baz"));

        assertEquals(List.of("foo", "bar", "baz"), names);
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArgumentNames from string")
    void parseArgumentNamesString() {
        List<String> names = ArgumentSubstitution.parseArgumentNames("foo bar baz");

        assertEquals(List.of("foo", "bar", "baz"), names);
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArgumentNames handles null")
    void parseArgumentNamesNull() {
        List<String> names = ArgumentSubstitution.parseArgumentNames(null);

        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("ArgumentSubstitution parseArgumentNames filters numeric")
    void parseArgumentNamesFiltersNumeric() {
        List<String> names = ArgumentSubstitution.parseArgumentNames("foo 123 bar");

        assertEquals(List.of("foo", "bar"), names);
    }

    @Test
    @DisplayName("ArgumentSubstitution generateProgressiveArgumentHint works")
    void generateProgressiveArgumentHintWorks() {
        List<String> argNames = List.of("foo", "bar", "baz");
        List<String> typedArgs = List.of("value1");

        String hint = ArgumentSubstitution.generateProgressiveArgumentHint(argNames, typedArgs);

        assertEquals("[bar] [baz]", hint);
    }

    @Test
    @DisplayName("ArgumentSubstitution generateProgressiveArgumentHint returns null when complete")
    void generateProgressiveArgumentHintComplete() {
        List<String> argNames = List.of("foo", "bar");
        List<String> typedArgs = List.of("value1", "value2");

        String hint = ArgumentSubstitution.generateProgressiveArgumentHint(argNames, typedArgs);

        assertNull(hint);
    }

    @Test
    @DisplayName("ArgumentSubstitution substituteArguments replaces $ARGUMENTS")
    void substituteArgumentsFull() {
        String content = "Command: $ARGUMENTS";

        String result = ArgumentSubstitution.substituteArguments(content, "arg1 arg2");

        assertEquals("Command: arg1 arg2", result);
    }

    @Test
    @DisplayName("ArgumentSubstitution substituteArguments replaces indexed $ARGUMENTS[0]")
    void substituteArgumentsIndexed() {
        String content = "First: $ARGUMENTS[0], Second: $ARGUMENTS[1]";

        String result = ArgumentSubstitution.substituteArguments(content, "arg1 arg2");

        assertEquals("First: arg1, Second: arg2", result);
    }

    @Test
    @DisplayName("ArgumentSubstitution substituteArguments replaces shorthand $0")
    void substituteArgumentsShorthand() {
        String content = "First: $0, Second: $1";

        String result = ArgumentSubstitution.substituteArguments(content, "arg1 arg2");

        assertEquals("First: arg1, Second: arg2", result);
    }

    @Test
    @DisplayName("ArgumentSubstitution substituteArguments replaces named arguments")
    void substituteArgumentsNamed() {
        String content = "Name: $name, Value: $value";

        String result = ArgumentSubstitution.substituteArguments(
            content, "john 42", false, List.of("name", "value")
        );

        assertEquals("Name: john, Value: 42", result);
    }

    @Test
    @DisplayName("ArgumentSubstitution substituteArguments appends if no placeholder")
    void substituteArgumentsAppend() {
        String content = "Command:";

        String result = ArgumentSubstitution.substituteArguments(content, "arg1 arg2");

        assertTrue(result.contains("ARGUMENTS: arg1 arg2"));
    }

    @Test
    @DisplayName("ArgumentSubstitution substituteArguments does not append if placeholder found")
    void substituteArgumentsNoAppend() {
        String content = "Command: $ARGUMENTS";

        String result = ArgumentSubstitution.substituteArguments(content, "arg1 arg2");

        assertEquals("Command: arg1 arg2", result);
    }

    @Test
    @DisplayName("ArgumentSubstitution substituteArguments handles null args")
    void substituteArgumentsNullArgs() {
        String content = "Command: $ARGUMENTS";

        String result = ArgumentSubstitution.substituteArguments(content, null);

        assertEquals("Command: $ARGUMENTS", result);
    }

    @Test
    @DisplayName("ArgumentSubstitution substituteArguments handles out of range index")
    void substituteArgumentsOutOfRange() {
        String content = "Arg: $ARGUMENTS[5]";

        String result = ArgumentSubstitution.substituteArguments(content, "arg1");

        assertEquals("Arg: ", result);
    }
}