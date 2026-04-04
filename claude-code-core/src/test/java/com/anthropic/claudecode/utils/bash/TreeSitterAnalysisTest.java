/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TreeSitterAnalysis.
 */
class TreeSitterAnalysisTest {

    @Test
    @DisplayName("TreeSitterAnalysis empty creates default values")
    void empty() {
        TreeSitterAnalysis empty = TreeSitterAnalysis.empty();
        assertNull(empty.commandName());
        assertTrue(empty.arguments().isEmpty());
        assertTrue(empty.operators().isEmpty());
        assertFalse(empty.hasCommandSubstitution());
        assertFalse(empty.hasProcessSubstitution());
        assertFalse(empty.hasVariableExpansion());
        assertFalse(empty.hasGlobPattern());
        assertFalse(empty.hasBraceExpansion());
        assertFalse(empty.hasHeredoc());
        assertFalse(empty.hasSubshell());
        assertEquals(0, empty.depth());
        assertTrue(empty.subcommands().isEmpty());
        assertTrue(empty.metadata().isEmpty());
    }

    @Test
    @DisplayName("TreeSitterAnalysis simpleCommand creates simple analysis")
    void simpleCommand() {
        TreeSitterAnalysis cmd = TreeSitterAnalysis.simpleCommand("ls", List.of("-la", "/home"));
        assertEquals("ls", cmd.commandName());
        assertEquals(2, cmd.arguments().size());
        assertEquals("-la", cmd.arguments().get(0));
        assertEquals("/home", cmd.arguments().get(1));
        assertTrue(cmd.isSimple());
    }

    @Test
    @DisplayName("TreeSitterAnalysis isSimple returns true for simple command")
    void isSimpleTrue() {
        TreeSitterAnalysis simple = TreeSitterAnalysis.simpleCommand("echo", List.of("hello"));
        assertTrue(simple.isSimple());
    }

    @Test
    @DisplayName("TreeSitterAnalysis isSimple false for complex command")
    void isSimpleFalse() {
        TreeSitterAnalysis complex = new TreeSitterAnalysis(
            "echo", List.of("hello"), List.of(),
            true, false, false, false, false, false, false, 0, List.of(), Map.of()
        );
        assertFalse(complex.isSimple());
    }

    @Test
    @DisplayName("TreeSitterAnalysis isPotentiallyDangerous true for command substitution")
    void isPotentiallyDangerousCommandSubstitution() {
        TreeSitterAnalysis dangerous = new TreeSitterAnalysis(
            "echo", List.of("$(cat file)"), List.of(),
            true, false, false, false, false, false, false, 0, List.of(), Map.of()
        );
        assertTrue(dangerous.isPotentiallyDangerous());
    }

    @Test
    @DisplayName("TreeSitterAnalysis isPotentiallyDangerous true for subshell")
    void isPotentiallyDangerousSubshell() {
        TreeSitterAnalysis dangerous = new TreeSitterAnalysis(
            "echo", List.of(), List.of(),
            false, false, false, false, false, false, true, 0, List.of(), Map.of()
        );
        assertTrue(dangerous.isPotentiallyDangerous());
    }

    @Test
    @DisplayName("TreeSitterAnalysis isPotentiallyDangerous false for simple command")
    void isPotentiallyDangerousFalse() {
        TreeSitterAnalysis safe = TreeSitterAnalysis.simpleCommand("ls", List.of("-la"));
        assertFalse(safe.isPotentiallyDangerous());
    }

    @Test
    @DisplayName("TreeSitterAnalysis getReferencedVariables returns list")
    void getReferencedVariables() {
        TreeSitterAnalysis cmd = TreeSitterAnalysis.simpleCommand("echo", List.of("$HOME"));
        List<String> vars = cmd.getReferencedVariables();
        assertNotNull(vars);
    }

    @Test
    @DisplayName("TreeSitterAnalysis getAllExecutableCommands includes command name")
    void getAllExecutableCommands() {
        TreeSitterAnalysis cmd = new TreeSitterAnalysis(
            "git", List.of("status"), List.of(),
            false, false, false, false, false, false, false, 0,
            List.of("git", "status"), Map.of()
        );
        List<String> all = cmd.getAllExecutableCommands();
        assertEquals(3, all.size());
        assertTrue(all.contains("git"));
    }

    @Test
    @DisplayName("TreeSitterAnalysis getAllExecutableCommands empty for null command")
    void getAllExecutableCommandsNullCommand() {
        TreeSitterAnalysis cmd = new TreeSitterAnalysis(
            null, List.of(), List.of(),
            false, false, false, false, false, false, false, 0, List.of(), Map.of()
        );
        List<String> all = cmd.getAllExecutableCommands();
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("TreeSitterAnalysis record with all fields")
    void recordWithAllFields() {
        TreeSitterAnalysis cmd = new TreeSitterAnalysis(
            "find", List.of(".", "-name", "*.java"), List.of("|"),
            false, false, true, true, false, false, false, 1,
            List.of("grep"), Map.of("key", "value")
        );
        assertEquals("find", cmd.commandName());
        assertEquals(3, cmd.arguments().size());
        assertEquals(1, cmd.operators().size());
        assertEquals("|", cmd.operators().get(0));
        assertTrue(cmd.hasVariableExpansion());
        assertTrue(cmd.hasGlobPattern());
        assertEquals(1, cmd.depth());
        assertEquals("value", cmd.metadata().get("key"));
    }
}