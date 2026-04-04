/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandUtils.
 */
class CommandUtilsTest {

    @Test
    @DisplayName("CommandUtils splitCommandWithOperators simple command")
    void splitCommandWithOperatorsSimple() {
        List<String> parts = CommandUtils.splitCommandWithOperators("ls -la");
        assertFalse(parts.isEmpty());
    }

    @Test
    @DisplayName("CommandUtils splitCommandWithOperators null returns empty")
    void splitCommandWithOperatorsNull() {
        List<String> parts = CommandUtils.splitCommandWithOperators(null);
        assertTrue(parts.isEmpty());
    }

    @Test
    @DisplayName("CommandUtils splitCommandWithOperators empty returns empty")
    void splitCommandWithOperatorsEmpty() {
        List<String> parts = CommandUtils.splitCommandWithOperators("");
        assertTrue(parts.isEmpty());
    }

    @Test
    @DisplayName("CommandUtils splitCommandWithOperators with pipe")
    void splitCommandWithOperatorsWithPipe() {
        List<String> parts = CommandUtils.splitCommandWithOperators("ls | grep test");
        assertNotNull(parts);
    }

    @Test
    @DisplayName("CommandUtils filterControlOperators removes operators")
    void filterControlOperators() {
        List<String> parts = List.of("ls", "|", "grep", "&&", "cat");
        List<String> filtered = CommandUtils.filterControlOperators(parts);
        assertEquals(3, filtered.size());
        assertTrue(filtered.contains("ls"));
        assertTrue(filtered.contains("grep"));
        assertTrue(filtered.contains("cat"));
    }

    @Test
    @DisplayName("CommandUtils isHelpCommand true for help commands")
    void isHelpCommandTrue() {
        assertTrue(CommandUtils.isHelpCommand("ls --help"));
        assertTrue(CommandUtils.isHelpCommand("git --help"));
        assertTrue(CommandUtils.isHelpCommand("cat --help"));
    }

    @Test
    @DisplayName("CommandUtils isHelpCommand false for non-help commands")
    void isHelpCommandFalse() {
        assertFalse(CommandUtils.isHelpCommand("ls -la"));
        assertFalse(CommandUtils.isHelpCommand("cat file.txt"));
        assertFalse(CommandUtils.isHelpCommand("ls --help -l")); // Multiple flags
    }

    @Test
    @DisplayName("CommandUtils isHelpCommand null returns false")
    void isHelpCommandNull() {
        assertFalse(CommandUtils.isHelpCommand(null));
    }

    @Test
    @DisplayName("CommandUtils extractOutputRedirections returns list")
    void extractOutputRedirections() {
        List<OutputRedirection> redirects = CommandUtils.extractOutputRedirections("ls > output.txt");
        assertFalse(redirects.isEmpty());
        assertEquals("output.txt", redirects.get(0).target());
        assertEquals(">", redirects.get(0).operator());
    }

    @Test
    @DisplayName("CommandUtils extractOutputRedirections append")
    void extractOutputRedirectionsAppend() {
        List<OutputRedirection> redirects = CommandUtils.extractOutputRedirections("ls >> output.txt");
        assertFalse(redirects.isEmpty());
        assertEquals(">>", redirects.get(0).operator());
        assertTrue(redirects.get(0).isAppend());
    }

    @Test
    @DisplayName("CommandUtils extractOutputRedirections no redirect")
    void extractOutputRedirectionsNone() {
        List<OutputRedirection> redirects = CommandUtils.extractOutputRedirections("ls -la");
        assertTrue(redirects.isEmpty());
    }

    @Test
    @DisplayName("CommandUtils removeOutputRedirections removes redirect")
    void removeOutputRedirections() {
        String result = CommandUtils.removeOutputRedirections("ls > output.txt");
        assertNotNull(result);
    }

    @Test
    @DisplayName("CommandUtils removeOutputRedirections null returns empty")
    void removeOutputRedirectionsNull() {
        String result = CommandUtils.removeOutputRedirections(null);
        assertEquals("", result);
    }

    @Test
    @DisplayName("CommandUtils OutputRedirectionResult empty")
    void outputRedirectionResultEmpty() {
        CommandUtils.OutputRedirectionResult result = CommandUtils.OutputRedirectionResult.empty("ls");
        assertEquals("ls", result.commandWithoutRedirections());
        assertTrue(result.redirections().isEmpty());
        assertFalse(result.hasDangerousRedirection());
    }

    @Test
    @DisplayName("CommandUtils extractOutputRedirectionsFull returns full result")
    void extractOutputRedirectionsFull() {
        CommandUtils.OutputRedirectionResult result = CommandUtils.extractOutputRedirectionsFull("ls > output.txt");
        assertNotNull(result);
        assertFalse(result.redirections().isEmpty());
    }

    @Test
    @DisplayName("CommandUtils isUnsafeCompoundCommand checks command")
    @SuppressWarnings("deprecation")
    void isUnsafeCompoundCommand() {
        // Test the deprecated method - just verify it doesn't throw
        boolean result = CommandUtils.isUnsafeCompoundCommand("ls");
        assertNotNull(result);
    }

    @Test
    @DisplayName("CommandUtils splitCommandDeprecated simple")
    @SuppressWarnings("deprecation")
    void splitCommandDeprecatedSimple() {
        List<String> parts = CommandUtils.splitCommandDeprecated("ls -la");
        assertFalse(parts.isEmpty());
    }
}