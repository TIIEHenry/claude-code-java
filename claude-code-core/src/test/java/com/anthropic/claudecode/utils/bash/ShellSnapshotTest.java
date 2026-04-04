/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShellSnapshot.
 */
class ShellSnapshotTest {

    @Test
    @DisplayName("ShellSnapshot builder creates snapshot")
    void builderCreatesSnapshot() {
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("ls -la")
            .output("total 0")
            .exitCode(0)
            .build();

        assertEquals("ls -la", snapshot.getCommand());
        assertEquals("total 0", snapshot.getOutput());
        assertEquals(0, snapshot.getExitCode());
    }

    @Test
    @DisplayName("ShellSnapshot isSuccessful true for exit code 0")
    void isSuccessfulTrue() {
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("ls")
            .exitCode(0)
            .build();
        assertTrue(snapshot.isSuccessful());
    }

    @Test
    @DisplayName("ShellSnapshot isSuccessful false for non-zero exit code")
    void isSuccessfulFalse() {
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("ls")
            .exitCode(1)
            .build();
        assertFalse(snapshot.isSuccessful());
    }

    @Test
    @DisplayName("ShellSnapshot getDurationMs calculates duration")
    void getDurationMs() {
        long start = System.currentTimeMillis() - 1000;
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("sleep 1")
            .startTime(start)
            .endTime(System.currentTimeMillis())
            .build();
        assertTrue(snapshot.getDurationMs() >= 1000);
    }

    @Test
    @DisplayName("ShellSnapshot getWorkingDirectory defaults to user.dir")
    void getWorkingDirectoryDefault() {
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("pwd")
            .build();
        assertEquals(System.getProperty("user.dir"), snapshot.getWorkingDirectory());
    }

    @Test
    @DisplayName("ShellSnapshot getEnvironment returns map")
    void getEnvironment() {
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("env")
            .environment(Map.of("PATH", "/usr/bin", "HOME", "/home/user"))
            .build();
        assertEquals("/usr/bin", snapshot.getEnvironment().get("PATH"));
        assertEquals("/home/user", snapshot.getEnvironment().get("HOME"));
    }

    @Test
    @DisplayName("ShellSnapshot getState returns state")
    void getState() {
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("cmd")
            .state(ShellSnapshot.ShellState.COMPLETED)
            .build();
        assertEquals(ShellSnapshot.ShellState.COMPLETED, snapshot.getState());
    }

    @Test
    @DisplayName("ShellSnapshot formatLogEntry contains command info")
    void formatLogEntry() {
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("echo hello")
            .output("hello")
            .exitCode(0)
            .build();
        String log = snapshot.formatLogEntry();
        assertTrue(log.contains("echo hello"));
        assertTrue(log.contains("Exit: 0"));
        assertTrue(log.contains("hello"));
    }

    @Test
    @DisplayName("ShellSnapshot ShellState enum values")
    void shellStateEnum() {
        ShellSnapshot.ShellState[] states = ShellSnapshot.ShellState.values();
        assertEquals(6, states.length);
        assertEquals(ShellSnapshot.ShellState.IDLE, ShellSnapshot.ShellState.valueOf("IDLE"));
        assertEquals(ShellSnapshot.ShellState.RUNNING, ShellSnapshot.ShellState.valueOf("RUNNING"));
        assertEquals(ShellSnapshot.ShellState.WAITING_INPUT, ShellSnapshot.ShellState.valueOf("WAITING_INPUT"));
        assertEquals(ShellSnapshot.ShellState.INTERRUPTED, ShellSnapshot.ShellState.valueOf("INTERRUPTED"));
        assertEquals(ShellSnapshot.ShellState.COMPLETED, ShellSnapshot.ShellState.valueOf("COMPLETED"));
        assertEquals(ShellSnapshot.ShellState.ERROR, ShellSnapshot.ShellState.valueOf("ERROR"));
    }

    @Test
    @DisplayName("ShellSnapshot constructor with all parameters")
    void constructorWithAllParameters() {
        Map<String, String> env = Map.of("VAR", "value");
        ShellSnapshot snapshot = new ShellSnapshot(
            "git status",
            "On branch main",
            0,
            1000L,
            2000L,
            "/home/user",
            env,
            ShellSnapshot.ShellState.COMPLETED
        );

        assertEquals("git status", snapshot.getCommand());
        assertEquals("On branch main", snapshot.getOutput());
        assertEquals(0, snapshot.getExitCode());
        assertEquals(1000L, snapshot.getStartTime());
        assertEquals(2000L, snapshot.getEndTime());
        assertEquals("/home/user", snapshot.getWorkingDirectory());
        assertEquals("value", snapshot.getEnvironment().get("VAR"));
        assertEquals(ShellSnapshot.ShellState.COMPLETED, snapshot.getState());
    }

    @Test
    @DisplayName("ShellSnapshot getEnvironment is immutable")
    void environmentIsImmutable() {
        ShellSnapshot snapshot = new ShellSnapshot.Builder()
            .command("cmd")
            .environment(Map.of("KEY", "VALUE"))
            .build();
        assertThrows(UnsupportedOperationException.class, () -> {
            snapshot.getEnvironment().put("NEW", "VALUE");
        });
    }
}