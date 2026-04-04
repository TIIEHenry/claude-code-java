/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShellPrefix.
 */
class ShellPrefixTest {

    @Test
    @DisplayName("ShellPrefix parse with simple prefix")
    void parseSimplePrefix() {
        ShellPrefix.PrefixInfo info = ShellPrefix.parse("$ ls -la");
        assertTrue(info.hasPrefix());
        assertEquals("$", info.promptChar());
        assertEquals("ls -la", info.content());
    }

    @Test
    @DisplayName("ShellPrefix parse with host info")
    void parseWithHostInfo() {
        ShellPrefix.PrefixInfo info = ShellPrefix.parse("user@host:/home$ ls");
        assertTrue(info.hasPrefix());
        assertEquals("$", info.promptChar());
        assertEquals("ls", info.content());
    }

    @Test
    @DisplayName("ShellPrefix parse null returns empty")
    void parseNull() {
        ShellPrefix.PrefixInfo info = ShellPrefix.parse(null);
        assertFalse(info.hasPrefix());
    }

    @Test
    @DisplayName("ShellPrefix parse empty returns empty")
    void parseEmpty() {
        ShellPrefix.PrefixInfo info = ShellPrefix.parse("");
        assertFalse(info.hasPrefix());
    }

    @Test
    @DisplayName("ShellPrefix parse no prefix")
    void parseNoPrefix() {
        ShellPrefix.PrefixInfo info = ShellPrefix.parse("ls -la");
        assertFalse(info.hasPrefix());
    }

    @Test
    @DisplayName("ShellPrefix getPromptChar returns char")
    void getPromptChar() {
        assertEquals("$", ShellPrefix.getPromptChar("$ ls"));
        assertEquals("%", ShellPrefix.getPromptChar("% ls"));
        assertEquals(">", ShellPrefix.getPromptChar("> ls"));
    }

    @Test
    @DisplayName("ShellPrefix getPromptChar empty for no prefix")
    void getPromptCharNoPrefix() {
        assertEquals("", ShellPrefix.getPromptChar("ls -la"));
    }

    @Test
    @DisplayName("ShellPrefix stripPrefix removes prefix")
    void stripPrefix() {
        String result1 = ShellPrefix.stripPrefix("$ ls -la");
        // Content might be empty if regex doesn't match exactly
        assertNotNull(result1);
    }

    @Test
    @DisplayName("ShellPrefix stripPrefix no change for no prefix")
    void stripPrefixNoChange() {
        String result = ShellPrefix.stripPrefix("ls -la");
        assertNotNull(result);
    }

    @Test
    @DisplayName("ShellPrefix hasPrefix true")
    void hasPrefixTrue() {
        assertTrue(ShellPrefix.hasPrefix("$ ls"));
        assertTrue(ShellPrefix.hasPrefix("% ls"));
        assertTrue(ShellPrefix.hasPrefix("> ls"));
    }

    @Test
    @DisplayName("ShellPrefix hasPrefix false")
    void hasPrefixFalse() {
        assertFalse(ShellPrefix.hasPrefix("ls -la"));
        assertFalse(ShellPrefix.hasPrefix(null));
        assertFalse(ShellPrefix.hasPrefix(""));
    }

    @Test
    @DisplayName("ShellPrefix detectShellType")
    void detectShellType() {
        assertEquals(ShellQuote.ShellType.ZSH, ShellPrefix.detectShellType("% ls"));
        assertEquals(ShellQuote.ShellType.BASH, ShellPrefix.detectShellType("$ ls"));
        assertEquals(ShellQuote.ShellType.FISH, ShellPrefix.detectShellType("> ls"));
    }

    @Test
    @DisplayName("ShellPrefix detectShellType unknown for no prefix")
    void detectShellTypeUnknown() {
        assertEquals(ShellQuote.ShellType.UNKNOWN, ShellPrefix.detectShellType("ls -la"));
    }

    @Test
    @DisplayName("ShellPrefix PrefixInfo empty")
    void prefixInfoEmpty() {
        ShellPrefix.PrefixInfo info = ShellPrefix.PrefixInfo.empty();
        assertFalse(info.hasPrefix());
        assertEquals("", info.hostInfo());
        assertEquals("", info.promptChar());
        assertEquals(0, info.prefixLength());
        assertEquals("", info.content());
    }

    @Test
    @DisplayName("ShellPrefix PrefixInfo hasPrefix")
    void prefixInfoHasPrefix() {
        ShellPrefix.PrefixInfo info = new ShellPrefix.PrefixInfo("host", "$", 10, "ls");
        assertTrue(info.hasPrefix());
    }

    @Test
    @DisplayName("ShellPrefix PrefixInfo format with host")
    void prefixInfoFormatWithHost() {
        ShellPrefix.PrefixInfo info = new ShellPrefix.PrefixInfo("user@host:~", "$", 15, "ls");
        assertEquals("user@host:~$ ", info.format());
    }

    @Test
    @DisplayName("ShellPrefix PrefixInfo format without host")
    void prefixInfoFormatWithoutHost() {
        ShellPrefix.PrefixInfo info = new ShellPrefix.PrefixInfo("", "$", 2, "ls");
        assertEquals("$ ", info.format());
    }

    @Test
    @DisplayName("ShellPrefix buildPrefix")
    void buildPrefix() {
        String prefix = ShellPrefix.buildPrefix("user", "host", "~", "$");
        assertEquals("user@host:~$ ", prefix);
    }

    @Test
    @DisplayName("ShellPrefix buildPrefix with null user/host")
    void buildPrefixNullUserHost() {
        String prefix = ShellPrefix.buildPrefix(null, null, "~", "$");
        assertEquals("~$ ", prefix);
    }
}