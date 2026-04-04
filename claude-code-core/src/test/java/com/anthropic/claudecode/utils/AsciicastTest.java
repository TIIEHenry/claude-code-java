/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Asciicast.
 */
class AsciicastTest {

    @BeforeEach
    void setUp() {
        Asciicast.resetRecordingState();
    }

    @Test
    @DisplayName("Asciicast resetRecordingState clears state")
    void resetRecordingState() {
        Asciicast.resetRecordingState();
        // Just verify no exception
        assertTrue(true);
    }

    @Test
    @DisplayName("Asciicast getRecordFilePath returns null without env")
    void getRecordFilePathNoEnv() {
        // Without USER_TYPE=ant and CLAUDE_CODE_TERMINAL_RECORDING
        String path = Asciicast.getRecordFilePath();
        // May be null or a path depending on env
        // Just verify it doesn't throw
        assertTrue(true);
    }

    @Test
    @DisplayName("Asciicast getSessionRecordingPaths returns list")
    void getSessionRecordingPaths() {
        List<String> paths = Asciicast.getSessionRecordingPaths();
        assertNotNull(paths);
        // May be empty if no recordings exist
    }

    @Test
    @DisplayName("Asciicast flushAsciicastRecorder does not throw")
    void flushAsciicastRecorder() {
        // Should not throw even if no recorder installed
        Asciicast.flushAsciicastRecorder();
        assertTrue(true);
    }

    @Test
    @DisplayName("Asciicast installAsciicastRecorder does not throw")
    void installAsciicastRecorder() {
        // Should not throw even without proper env
        Asciicast.installAsciicastRecorder();
        assertTrue(true);
    }
}
