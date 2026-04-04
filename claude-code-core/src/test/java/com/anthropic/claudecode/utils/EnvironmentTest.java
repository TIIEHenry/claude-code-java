/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Environment.
 */
class EnvironmentTest {

    @Test
    @DisplayName("Environment getPlatform returns non-null")
    void getPlatform() {
        Environment.Platform platform = Environment.getPlatform();

        assertNotNull(platform);
        assertTrue(platform == Environment.Platform.DARWIN ||
                   platform == Environment.Platform.LINUX ||
                   platform == Environment.Platform.WIN32);
    }

    @Test
    @DisplayName("Environment getArch returns non-null")
    void getArch() {
        String arch = Environment.getArch();

        assertNotNull(arch);
        assertFalse(arch.isEmpty());
    }

    @Test
    @DisplayName("Environment getJavaVersion returns non-null")
    void getJavaVersion() {
        String version = Environment.getJavaVersion();

        assertNotNull(version);
        assertFalse(version.isEmpty());
    }

    @Test
    @DisplayName("Environment detectTerminal returns non-null")
    void detectTerminal() {
        Environment.Terminal terminal = Environment.detectTerminal();

        assertNotNull(terminal);
    }

    @Test
    @DisplayName("Environment isTTY returns boolean")
    void isTTY() {
        boolean tty = Environment.isTTY();

        // Just check it doesn't throw
        assertTrue(tty || !tty);
    }

    @Test
    @DisplayName("Environment isSSHSession returns boolean")
    void isSSHSession() {
        boolean ssh = Environment.isSSHSession();

        // Just check it doesn't throw
        assertTrue(ssh || !ssh);
    }

    @Test
    @DisplayName("Environment detectDeploymentEnvironment returns non-null")
    void detectDeploymentEnvironment() {
        Environment.DeploymentEnvironment env = Environment.detectDeploymentEnvironment();

        assertNotNull(env);
    }

    @Test
    @DisplayName("Environment isWslEnvironment returns boolean")
    void isWslEnvironment() {
        boolean wsl = Environment.isWslEnvironment();

        // Just check it doesn't throw
        assertTrue(wsl || !wsl);
    }

    @Test
    @DisplayName("Environment isConductor returns boolean")
    void isConductor() {
        boolean conductor = Environment.isConductor();

        // Just check it doesn't throw
        assertTrue(conductor || !conductor);
    }

    @Test
    @DisplayName("Environment getHostPlatformForAnalytics returns non-null")
    void getHostPlatformForAnalytics() {
        Environment.Platform platform = Environment.getHostPlatformForAnalytics();

        assertNotNull(platform);
    }

    @Test
    @DisplayName("Environment getTerminalName returns non-null")
    void getTerminalName() {
        String name = Environment.getTerminalName();

        assertNotNull(name);
        assertFalse(name.isEmpty());
    }

    @Test
    @DisplayName("Environment isCommandAvailable ls returns true")
    void isCommandAvailableLs() {
        boolean available = Environment.isCommandAvailable("ls");

        assertTrue(available);
    }

    @Test
    @DisplayName("Environment isCommandAvailable non-existent returns false")
    void isCommandAvailableNonExistent() {
        boolean available = Environment.isCommandAvailable("thisCommandDefinitelyDoesNotExist12345");

        assertFalse(available);
    }

    @Test
    @DisplayName("Environment Platform enum values")
    void platformEnumValues() {
        Environment.Platform[] values = Environment.Platform.values();

        assertEquals(3, values.length);
    }

    @Test
    @DisplayName("Environment Terminal enum values")
    void terminalEnumValues() {
        Environment.Terminal[] values = Environment.Terminal.values();

        assertTrue(values.length > 10);
    }

    @Test
    @DisplayName("Environment DeploymentEnvironment enum values")
    void deploymentEnvironmentEnumValues() {
        Environment.DeploymentEnvironment[] values = Environment.DeploymentEnvironment.values();

        assertTrue(values.length > 20);
    }

    @Test
    @DisplayName("Environment JETBRAINS_IDES list is not empty")
    void jetbrainsIdesList() {
        assertFalse(Environment.JETBRAINS_IDES.isEmpty());
        assertTrue(Environment.JETBRAINS_IDES.contains("intellij"));
    }

    @Test
    @DisplayName("Environment getPackageManagers returns list")
    void getPackageManagers() throws Exception {
        var future = Environment.getPackageManagers();
        var managers = future.get();

        assertNotNull(managers);
    }

    @Test
    @DisplayName("Environment getRuntimes returns list")
    void getRuntimes() throws Exception {
        var future = Environment.getRuntimes();
        var runtimes = future.get();

        assertNotNull(runtimes);
    }

    @Test
    @DisplayName("Environment getEnvironmentInfo returns info")
    void getEnvironmentInfo() throws Exception {
        var future = Environment.getEnvironmentInfo();
        var info = future.get();

        assertNotNull(info);
        assertNotNull(info.platform());
        assertNotNull(info.arch());
        assertNotNull(info.javaVersion());
        assertNotNull(info.terminal());
        assertNotNull(info.deployment());
    }

    @Test
    @DisplayName("Environment EnvironmentInfo record works")
    void environmentInfoRecord() {
        Environment.EnvironmentInfo info = new Environment.EnvironmentInfo(
            Environment.Platform.DARWIN,
            "aarch64",
            "17.0.1",
            Environment.Terminal.ITERM2,
            Environment.DeploymentEnvironment.UNKNOWN_DARWIN,
            false,
            false,
            false,
            false,
            List.of("npm"),
            List.of("node")
        );

        assertEquals(Environment.Platform.DARWIN, info.platform());
        assertEquals("aarch64", info.arch());
        assertEquals("17.0.1", info.javaVersion());
        assertEquals(Environment.Terminal.ITERM2, info.terminal());
        assertFalse(info.isSSH());
    }
}