/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.permissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DangerousPatterns.
 */
class DangerousPatternsTest {

    @Test
    @DisplayName("DangerousPatterns CROSS_PLATFORM_CODE_EXEC contains expected items")
    void crossPlatformCodeExec() {
        List<String> patterns = DangerousPatterns.CROSS_PLATFORM_CODE_EXEC;
        assertTrue(patterns.contains("python"));
        assertTrue(patterns.contains("python3"));
        assertTrue(patterns.contains("node"));
        assertTrue(patterns.contains("bash"));
        assertTrue(patterns.contains("sh"));
    }

    @Test
    @DisplayName("DangerousPatterns DANGEROUS_BASH_PATTERNS contains expected items")
    void dangerousBashPatterns() {
        List<String> patterns = DangerousPatterns.DANGEROUS_BASH_PATTERNS;
        assertTrue(patterns.contains("python"));
        assertTrue(patterns.contains("sudo"));
        assertTrue(patterns.contains("eval"));
        assertTrue(patterns.contains("exec"));
    }

    @Test
    @DisplayName("DangerousPatterns DANGEROUS_POWERSHELL_PATTERNS contains expected items")
    void dangerousPowerShellPatterns() {
        List<String> patterns = DangerousPatterns.DANGEROUS_POWERSHELL_PATTERNS;
        assertTrue(patterns.contains("powershell"));
        assertTrue(patterns.contains("pwsh"));
        assertTrue(patterns.contains("iex"));
        assertTrue(patterns.contains("cmd"));
    }

    @Test
    @DisplayName("DangerousPatterns NETWORK_PATTERNS contains expected items")
    void networkPatterns() {
        List<String> patterns = DangerousPatterns.NETWORK_PATTERNS;
        assertTrue(patterns.contains("curl"));
        assertTrue(patterns.contains("wget"));
        assertTrue(patterns.contains("ssh"));
    }

    @Test
    @DisplayName("DangerousPatterns FILESYSTEM_MODIFY_PATTERNS contains expected items")
    void filesystemModifyPatterns() {
        List<String> patterns = DangerousPatterns.FILESYSTEM_MODIFY_PATTERNS;
        assertTrue(patterns.contains("rm"));
        assertTrue(patterns.contains("chmod"));
        assertTrue(patterns.contains("dd"));
    }

    @Test
    @DisplayName("DangerousPatterns matchesDangerousPattern true for matching command")
    void matchesDangerousPatternTrue() {
        assertTrue(DangerousPatterns.matchesDangerousPattern("python script.py", DangerousPatterns.DANGEROUS_BASH_PATTERNS));
        assertTrue(DangerousPatterns.matchesDangerousPattern("node app.js", DangerousPatterns.DANGEROUS_BASH_PATTERNS));
        assertTrue(DangerousPatterns.matchesDangerousPattern("sudo rm -rf /", DangerousPatterns.DANGEROUS_BASH_PATTERNS));
    }

    @Test
    @DisplayName("DangerousPatterns matchesDangerousPattern false for safe command")
    void matchesDangerousPatternFalse() {
        assertFalse(DangerousPatterns.matchesDangerousPattern("ls -la", DangerousPatterns.DANGEROUS_BASH_PATTERNS));
        assertFalse(DangerousPatterns.matchesDangerousPattern("echo hello", DangerousPatterns.DANGEROUS_BASH_PATTERNS));
    }

    @Test
    @DisplayName("DangerousPatterns matchesDangerousPattern null returns false")
    void matchesDangerousPatternNull() {
        assertFalse(DangerousPatterns.matchesDangerousPattern(null, DangerousPatterns.DANGEROUS_BASH_PATTERNS));
        assertFalse(DangerousPatterns.matchesDangerousPattern("ls", null));
    }

    @Test
    @DisplayName("DangerousPatterns isDangerousBashCommand true")
    void isDangerousBashCommandTrue() {
        assertTrue(DangerousPatterns.isDangerousBashCommand("python"));
        assertTrue(DangerousPatterns.isDangerousBashCommand("node"));
        assertTrue(DangerousPatterns.isDangerousBashCommand("sudo ls"));
    }

    @Test
    @DisplayName("DangerousPatterns isDangerousBashCommand false")
    void isDangerousBashCommandFalse() {
        assertFalse(DangerousPatterns.isDangerousBashCommand("ls"));
        assertFalse(DangerousPatterns.isDangerousBashCommand("cat file.txt"));
    }

    @Test
    @DisplayName("DangerousPatterns isDangerousPowerShellCommand true")
    void isDangerousPowerShellCommandTrue() {
        assertTrue(DangerousPatterns.isDangerousPowerShellCommand("powershell"));
        assertTrue(DangerousPatterns.isDangerousPowerShellCommand("pwsh"));
        assertTrue(DangerousPatterns.isDangerousPowerShellCommand("iex"));
    }

    @Test
    @DisplayName("DangerousPatterns isDangerousPowerShellCommand false")
    void isDangerousPowerShellCommandFalse() {
        assertFalse(DangerousPatterns.isDangerousPowerShellCommand("Get-ChildItem"));
        assertFalse(DangerousPatterns.isDangerousPowerShellCommand("Write-Host hello"));
    }

    @Test
    @DisplayName("DangerousPatterns isOverlyBroadBashPermission true for wildcard")
    void isOverlyBroadBashPermissionWildcard() {
        assertTrue(DangerousPatterns.isOverlyBroadBashPermission("*"));
    }

    @Test
    @DisplayName("DangerousPatterns isOverlyBroadBashPermission true for dangerous pattern")
    void isOverlyBroadBashPermissionDangerous() {
        assertTrue(DangerousPatterns.isOverlyBroadBashPermission("python"));
    }

    @Test
    @DisplayName("DangerousPatterns isOverlyBroadBashPermission false for safe pattern")
    void isOverlyBroadBashPermissionSafe() {
        assertFalse(DangerousPatterns.isOverlyBroadBashPermission("ls"));
    }

    @Test
    @DisplayName("DangerousPatterns isOverlyBroadBashPermission null returns false")
    void isOverlyBroadBashPermissionNull() {
        assertFalse(DangerousPatterns.isOverlyBroadBashPermission(null));
    }

    @Test
    @DisplayName("DangerousPatterns isOverlyBroadPowerShellPermission true for wildcard")
    void isOverlyBroadPowerShellPermissionWildcard() {
        assertTrue(DangerousPatterns.isOverlyBroadPowerShellPermission("*"));
    }

    @Test
    @DisplayName("DangerousPatterns isOverlyBroadPowerShellPermission true for dangerous pattern")
    void isOverlyBroadPowerShellPermissionDangerous() {
        assertTrue(DangerousPatterns.isOverlyBroadPowerShellPermission("powershell"));
    }

    @Test
    @DisplayName("DangerousPatterns isOverlyBroadPowerShellPermission false for safe pattern")
    void isOverlyBroadPowerShellPermissionSafe() {
        assertFalse(DangerousPatterns.isOverlyBroadPowerShellPermission("Get-ChildItem"));
    }

    @Test
    @DisplayName("DangerousPatterns matches with colon separator")
    void matchesWithColonSeparator() {
        assertTrue(DangerousPatterns.matchesDangerousPattern("python:script.py", DangerousPatterns.DANGEROUS_BASH_PATTERNS));
    }

    @Test
    @DisplayName("DangerousPatterns case insensitive matching")
    void caseInsensitiveMatching() {
        assertTrue(DangerousPatterns.matchesDangerousPattern("PYTHON script.py", DangerousPatterns.DANGEROUS_BASH_PATTERNS));
        assertTrue(DangerousPatterns.matchesDangerousPattern("Node app.js", DangerousPatterns.DANGEROUS_BASH_PATTERNS));
    }
}