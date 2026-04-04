/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShellCommand.
 */
class ShellCommandTest {

    @Test
    @DisplayName("ShellCommand constructor parses command")
    void constructor() {
        ShellCommand cmd = new ShellCommand("ls -la");
        assertEquals("ls -la", cmd.getCommand());
    }

    @Test
    @DisplayName("ShellCommand getCommand returns original")
    void getCommand() {
        ShellCommand cmd = new ShellCommand("echo hello");
        assertEquals("echo hello", cmd.getCommand());
    }

    @Test
    @DisplayName("ShellCommand getParts parses simple command")
    void getPartsSimple() {
        ShellCommand cmd = new ShellCommand("ls -la /home");
        List<String> parts = cmd.getParts();
        assertEquals(3, parts.size());
        assertEquals("ls", parts.get(0));
        assertEquals("-la", parts.get(1));
        assertEquals("/home", parts.get(2));
    }

    @Test
    @DisplayName("ShellCommand getParts handles quoted strings")
    void getPartsQuoted() {
        ShellCommand cmd = new ShellCommand("echo 'hello world'");
        List<String> parts = cmd.getParts();
        assertTrue(parts.size() >= 1);
        assertEquals("echo", parts.get(0));
        // Quoted string should be unquoted
        assertTrue(parts.contains("hello world"));
    }

    @Test
    @DisplayName("ShellCommand getParts handles double quoted strings")
    void getPartsDoubleQuoted() {
        ShellCommand cmd = new ShellCommand("echo \"hello world\"");
        List<String> parts = cmd.getParts();
        assertTrue(parts.size() >= 1);
        assertEquals("echo", parts.get(0));
    }

    @Test
    @DisplayName("ShellCommand getExecutable returns first part")
    void getExecutable() {
        ShellCommand cmd = new ShellCommand("ls -la");
        assertEquals("ls", cmd.getExecutable());
    }

    @Test
    @DisplayName("ShellCommand getExecutable empty command returns empty")
    void getExecutableEmpty() {
        ShellCommand cmd = new ShellCommand("");
        assertEquals("", cmd.getExecutable());
    }

    @Test
    @DisplayName("ShellCommand getArguments returns remaining parts")
    void getArguments() {
        ShellCommand cmd = new ShellCommand("ls -la /home");
        List<String> args = cmd.getArguments();
        assertEquals(2, args.size());
        assertEquals("-la", args.get(0));
        assertEquals("/home", args.get(1));
    }

    @Test
    @DisplayName("ShellCommand getArguments empty for no args")
    void getArgumentsEmpty() {
        ShellCommand cmd = new ShellCommand("ls");
        assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    @DisplayName("ShellCommand hasPipe returns true for piped command")
    void hasPipeTrue() {
        ShellCommand cmd = new ShellCommand("ls | grep test");
        assertTrue(cmd.hasPipe());
    }

    @Test
    @DisplayName("ShellCommand hasPipe returns false for simple command")
    void hasPipeFalse() {
        ShellCommand cmd = new ShellCommand("ls -la");
        assertFalse(cmd.hasPipe());
    }

    @Test
    @DisplayName("ShellCommand splitByPipe splits commands")
    void splitByPipe() {
        ShellCommand cmd = new ShellCommand("ls | grep test | wc");
        List<ShellCommand> split = cmd.splitByPipe();
        assertEquals(3, split.size());
        assertEquals("ls", split.get(0).getExecutable());
        assertEquals("grep", split.get(1).getExecutable());
        assertEquals("wc", split.get(2).getExecutable());
    }

    @Test
    @DisplayName("ShellCommand splitByPipe single command")
    void splitByPipeSingle() {
        ShellCommand cmd = new ShellCommand("ls -la");
        List<ShellCommand> split = cmd.splitByPipe();
        assertEquals(1, split.size());
    }

    @Test
    @DisplayName("ShellCommand quote simple string")
    void quoteSimple() {
        String quoted = ShellCommand.quote("hello");
        assertEquals("hello", quoted);
    }

    @Test
    @DisplayName("ShellCommand quote string with spaces")
    void quoteWithSpaces() {
        String quoted = ShellCommand.quote("hello world");
        assertTrue(quoted.contains("'"));
        assertTrue(quoted.contains("hello world"));
    }

    @Test
    @DisplayName("ShellCommand quote null returns empty quotes")
    void quoteNull() {
        String quoted = ShellCommand.quote(null);
        assertEquals("''", quoted);
    }

    @Test
    @DisplayName("ShellCommand quote empty returns empty quotes")
    void quoteEmpty() {
        String quoted = ShellCommand.quote("");
        assertEquals("''", quoted);
    }

    @Test
    @DisplayName("ShellCommand quote string with single quote")
    void quoteWithSingleQuote() {
        String quoted = ShellCommand.quote("it's");
        // Should use double quotes since string contains single quote
        assertTrue(quoted.startsWith("\""));
        assertTrue(quoted.endsWith("\""));
    }

    @Test
    @DisplayName("ShellCommand quote string with special chars")
    void quoteWithSpecialChars() {
        String quoted = ShellCommand.quote("hello$world");
        assertTrue(quoted.contains("'") || quoted.contains("\""));
    }

    @Test
    @DisplayName("ShellCommand build creates command string")
    void build() {
        List<String> parts = List.of("ls", "-la", "/home");
        String built = ShellCommand.build(parts);
        assertTrue(built.contains("ls"));
        assertTrue(built.contains("-la"));
        assertTrue(built.contains("/home"));
    }

    @Test
    @DisplayName("ShellCommand build handles special chars")
    void buildSpecialChars() {
        List<String> parts = List.of("echo", "hello world");
        String built = ShellCommand.build(parts);
        assertTrue(built.contains("'hello world'") || built.contains("\"hello world\""));
    }

    @Test
    @DisplayName("ShellCommand build empty list returns empty string")
    void buildEmpty() {
        String built = ShellCommand.build(List.of());
        assertEquals("", built);
    }
}