/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShellUtils.
 */
class ShellUtilsTest {

    @Test
    @DisplayName("ShellUtils getShell returns shell name")
    void getShell() {
        String shell = ShellUtils.getShell();
        assertNotNull(shell);
        // Should be one of the known shells
        assertTrue(shell.equals("bash") || shell.equals("zsh") ||
                   shell.equals("fish") || shell.equals("sh") ||
                   shell.equals("cmd") || shell.equals("powershell"));
    }

    @Test
    @DisplayName("ShellUtils getShellPath returns path")
    void getShellPath() {
        String path = ShellUtils.getShellPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    @Test
    @DisplayName("ShellUtils isWindows detection")
    void isWindows() {
        boolean isWindows = ShellUtils.isWindows();
        // On macOS/Linux this should be false
        String os = System.getProperty("os.name").toLowerCase();
        assertEquals(os.contains("win"), isWindows);
    }

    @Test
    @DisplayName("ShellUtils isMacOS detection")
    void isMacOS() {
        boolean isMac = ShellUtils.isMacOS();
        String os = System.getProperty("os.name").toLowerCase();
        assertEquals(os.contains("mac"), isMac);
    }

    @Test
    @DisplayName("ShellUtils isLinux detection")
    void isLinux() {
        boolean isLinux = ShellUtils.isLinux();
        String os = System.getProperty("os.name").toLowerCase();
        boolean expected = os.contains("nix") || os.contains("nux") || os.contains("aix");
        assertEquals(expected, isLinux);
    }

    @Test
    @DisplayName("ShellUtils getShellPrefix returns array")
    void getShellPrefix() {
        String[] prefix = ShellUtils.getShellPrefix();
        assertNotNull(prefix);
        assertTrue(prefix.length >= 2);
    }

    @Test
    @DisplayName("ShellUtils getEnvFormat")
    void getEnvFormat() {
        String format = ShellUtils.getEnvFormat("KEY", "value");
        assertNotNull(format);
        assertTrue(format.contains("KEY"));
        assertTrue(format.contains("value"));
    }

    @Test
    @DisplayName("ShellUtils getPathSeparator")
    void getPathSeparator() {
        String sep = ShellUtils.getPathSeparator();
        assertNotNull(sep);
        assertEquals(1, sep.length());
    }

    @Test
    @DisplayName("ShellUtils getLineSeparator")
    void getLineSeparator() {
        String sep = ShellUtils.getLineSeparator();
        assertNotNull(sep);
        assertTrue(sep.equals("\n") || sep.equals("\r\n"));
    }

    @Test
    @DisplayName("ShellUtils getHomeDirectory returns path")
    void getHomeDirectory() {
        String home = ShellUtils.getHomeDirectory();
        assertNotNull(home);
        assertFalse(home.isEmpty());
    }

    @Test
    @DisplayName("ShellUtils getConfigDirectory returns path")
    void getConfigDirectory() {
        String config = ShellUtils.getConfigDirectory();
        assertNotNull(config);
        assertFalse(config.isEmpty());
    }

    @Test
    @DisplayName("ShellUtils quote null returns empty quotes")
    void quoteNull() {
        String quoted = ShellUtils.quote(null);
        assertEquals("''", quoted);
    }

    @Test
    @DisplayName("ShellUtils quote empty returns empty quotes")
    void quoteEmpty() {
        String quoted = ShellUtils.quote("");
        assertEquals("''", quoted);
    }

    @Test
    @DisplayName("ShellUtils quote simple string")
    void quoteSimple() {
        String quoted = ShellUtils.quote("hello");
        assertNotNull(quoted);
        assertTrue(quoted.contains("hello"));
    }

    @Test
    @DisplayName("ShellUtils quote string with single quote")
    void quoteWithSingleQuote() {
        String quoted = ShellUtils.quote("it's");
        assertNotNull(quoted);
        assertTrue(quoted.contains("it"));
        assertTrue(quoted.contains("s"));
    }

    @Test
    @DisplayName("ShellUtils escape null returns empty")
    void escapeNull() {
        String escaped = ShellUtils.escape(null);
        assertEquals("", escaped);
    }

    @Test
    @DisplayName("ShellUtils escape empty returns empty")
    void escapeEmpty() {
        String escaped = ShellUtils.escape("");
        assertEquals("", escaped);
    }

    @Test
    @DisplayName("ShellUtils escape simple string")
    void escapeSimple() {
        String escaped = ShellUtils.escape("hello");
        assertEquals("hello", escaped);
    }

    @Test
    @DisplayName("ShellUtils buildCommand adds prefix")
    void buildCommand() {
        String[] cmd = ShellUtils.buildCommand("ls", "-la");
        assertNotNull(cmd);
        assertTrue(cmd.length >= 4); // prefix (2) + args (2)
    }
}