/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Shell.
 */
class ShellTest {

    @Test
    @DisplayName("Shell getCurrentShell returns shell path")
    void getCurrentShell() {
        String shell = Shell.getCurrentShell();
        assertNotNull(shell);
        assertFalse(shell.isEmpty());
    }

    @Test
    @DisplayName("Shell getCurrentShell returns default if not set")
    void getCurrentShellDefault() {
        // Even without SHELL env, should return a default
        String shell = Shell.getCurrentShell();
        assertTrue(shell.contains("sh") || shell.contains("bash") ||
                   shell.contains("zsh") || shell.contains("fish"));
    }

    @Test
    @DisplayName("Shell isZsh returns boolean")
    void isZsh() {
        boolean isZsh = Shell.isZsh();
        String shell = Shell.getCurrentShell();
        assertEquals(shell.contains("zsh"), isZsh);
    }

    @Test
    @DisplayName("Shell isBash returns boolean")
    void isBash() {
        boolean isBash = Shell.isBash();
        String shell = Shell.getCurrentShell();
        assertEquals(shell.contains("bash"), isBash);
    }

    @Test
    @DisplayName("Shell isFish returns boolean")
    void isFish() {
        boolean isFish = Shell.isFish();
        String shell = Shell.getCurrentShell();
        assertEquals(shell.contains("fish"), isFish);
    }

    @Test
    @DisplayName("Shell getShellConfigPath returns path")
    void getShellConfigPath() {
        String configPath = Shell.getShellConfigPath();
        assertNotNull(configPath);
        assertFalse(configPath.isEmpty());
        assertTrue(configPath.contains(System.getProperty("user.home")));
    }

    @Test
    @DisplayName("Shell getShellType returns enum")
    void getShellType() {
        Shell.ShellType type = Shell.getShellType();
        assertNotNull(type);
        // Should be one of the known types
        assertTrue(type == Shell.ShellType.ZSH ||
                   type == Shell.ShellType.BASH ||
                   type == Shell.ShellType.FISH ||
                   type == Shell.ShellType.SH ||
                   type == Shell.ShellType.UNKNOWN);
    }

    @Test
    @DisplayName("Shell ShellType enum values")
    void shellTypeEnum() {
        Shell.ShellType[] types = Shell.ShellType.values();
        assertEquals(5, types.length);

        assertEquals("zsh", Shell.ShellType.ZSH.getName());
        assertEquals("bash", Shell.ShellType.BASH.getName());
        assertEquals("fish", Shell.ShellType.FISH.getName());
        assertEquals("sh", Shell.ShellType.SH.getName());
        assertEquals("unknown", Shell.ShellType.UNKNOWN.getName());
    }

    @Test
    @DisplayName("Shell ShellType ZSH")
    void shellTypeZsh() {
        Shell.ShellType type = Shell.ShellType.ZSH;
        assertEquals("ZSH", type.name());
        assertEquals("zsh", type.getName());
    }

    @Test
    @DisplayName("Shell ShellType BASH")
    void shellTypeBash() {
        Shell.ShellType type = Shell.ShellType.BASH;
        assertEquals("BASH", type.name());
        assertEquals("bash", type.getName());
    }

    @Test
    @DisplayName("Shell ShellType FISH")
    void shellTypeFish() {
        Shell.ShellType type = Shell.ShellType.FISH;
        assertEquals("FISH", type.name());
        assertEquals("fish", type.getName());
    }

    @Test
    @DisplayName("Shell ShellType SH")
    void shellTypeSh() {
        Shell.ShellType type = Shell.ShellType.SH;
        assertEquals("SH", type.name());
        assertEquals("sh", type.getName());
    }

    @Test
    @DisplayName("Shell ShellType UNKNOWN")
    void shellTypeUnknown() {
        Shell.ShellType type = Shell.ShellType.UNKNOWN;
        assertEquals("UNKNOWN", type.name());
        assertEquals("unknown", type.getName());
    }
}