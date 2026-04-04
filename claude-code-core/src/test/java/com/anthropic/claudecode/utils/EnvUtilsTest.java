/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EnvUtils.
 */
class EnvUtilsTest {

    @Test
    @DisplayName("isEnvTruthyValue works for true values")
    void isEnvTruthyValueWorksForTrue() {
        assertTrue(EnvUtils.isEnvTruthyValue("true"));
        assertTrue(EnvUtils.isEnvTruthyValue("TRUE"));
        assertTrue(EnvUtils.isEnvTruthyValue("1"));
        assertTrue(EnvUtils.isEnvTruthyValue("yes"));
        assertTrue(EnvUtils.isEnvTruthyValue("YES"));
        assertTrue(EnvUtils.isEnvTruthyValue("on"));
        assertTrue(EnvUtils.isEnvTruthyValue("ON"));
    }

    @Test
    @DisplayName("isEnvTruthyValue works for false values")
    void isEnvTruthyValueWorksForFalse() {
        assertFalse(EnvUtils.isEnvTruthyValue("false"));
        assertFalse(EnvUtils.isEnvTruthyValue("0"));
        assertFalse(EnvUtils.isEnvTruthyValue("no"));
        assertFalse(EnvUtils.isEnvTruthyValue(null));
        assertFalse(EnvUtils.isEnvTruthyValue(""));
        assertFalse(EnvUtils.isEnvTruthyValue("random"));
    }

    @Test
    @DisplayName("getClaudeConfigHomeDir returns valid path")
    void getClaudeConfigHomeDirWorks() {
        String configDir = EnvUtils.getClaudeConfigHomeDir();
        assertNotNull(configDir);
        assertTrue(configDir.contains(".claude"));
    }

    @Test
    @DisplayName("getPlatform returns valid platform")
    void getPlatformWorks() {
        String platform = EnvUtils.getPlatform();
        assertNotNull(platform);
        assertTrue(platform.length() > 0);
    }

    @Test
    @DisplayName("isWindows returns correct value")
    void isWindowsWorks() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        assertEquals(isWindows, EnvUtils.isWindows());
    }

    @Test
    @DisplayName("isMac returns correct value")
    void isMacWorks() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isMac = os.contains("mac");
        assertEquals(isMac, EnvUtils.isMac());
    }

    @Test
    @DisplayName("isLinux returns correct value")
    void isLinuxWorks() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isLinux = os.contains("linux") && !EnvUtils.isWSL();
        assertEquals(isLinux, EnvUtils.isLinux());
    }
}