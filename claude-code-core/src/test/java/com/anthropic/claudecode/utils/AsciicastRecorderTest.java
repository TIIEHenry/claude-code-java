/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AsciicastRecorder.
 */
class AsciicastRecorderTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AsciicastRecorder.resetRecordingStateForTesting();
    }

    @Test
    @DisplayName("AsciicastRecorder getRecordFilePath returns null when not recording")
    void getRecordFilePathNotRecording() {
        String path = AsciicastRecorder.getRecordFilePath();
        assertNull(path);
    }

    @Test
    @DisplayName("AsciicastRecorder installAsciicastRecorder null path")
    void installNullPath() {
        AsciicastRecorder.installAsciicastRecorder(null);
        assertNull(AsciicastRecorder.getRecordFilePath());
    }

    @Test
    @DisplayName("AsciicastRecorder installAsciicastRecorder empty path")
    void installEmptyPath() {
        AsciicastRecorder.installAsciicastRecorder("");
        assertNull(AsciicastRecorder.getRecordFilePath());
    }

    @Test
    @DisplayName("AsciicastRecorder installAsciicastRecorder creates file")
    void installCreatesFile() throws Exception {
        Path outputPath = tempDir.resolve("test.cast");
        AsciicastRecorder.installAsciicastRecorder(outputPath.toString());

        // File should be created with header
        assertTrue(Files.exists(outputPath));
        String content = Files.readString(outputPath);
        assertTrue(content.contains("{\"version\":2"));
    }

    @Test
    @DisplayName("AsciicastRecorder recordOutput without installation")
    void recordOutputWithoutInstallation() {
        // Should not throw
        assertDoesNotThrow(() -> AsciicastRecorder.recordOutput("test"));
    }

    @Test
    @DisplayName("AsciicastRecorder recordOutput null text")
    void recordOutputNullText() throws Exception {
        Path outputPath = tempDir.resolve("test.cast");
        AsciicastRecorder.installAsciicastRecorder(outputPath.toString());

        assertDoesNotThrow(() -> AsciicastRecorder.recordOutput(null));
    }

    @Test
    @DisplayName("AsciicastRecorder recordOutput writes output")
    void recordOutputWrites() throws Exception {
        Path outputPath = tempDir.resolve("test.cast");
        AsciicastRecorder.installAsciicastRecorder(outputPath.toString());
        AsciicastRecorder.recordOutput("Hello World");

        AsciicastRecorder.flush();
        String content = Files.readString(outputPath);
        assertTrue(content.contains("["));
        assertTrue(content.contains("\"o\""));
    }

    @Test
    @DisplayName("AsciicastRecorder flush does not throw")
    void flushDoesNotThrow() {
        assertDoesNotThrow(() -> AsciicastRecorder.flush());
    }

    @Test
    @DisplayName("AsciicastRecorder dispose stops recording")
    void disposeStopsRecording() throws Exception {
        Path outputPath = tempDir.resolve("test.cast");
        AsciicastRecorder.installAsciicastRecorder(outputPath.toString());
        AsciicastRecorder.recordOutput("Before dispose");

        AsciicastRecorder.dispose();

        // After dispose, recordOutput should not write
        AsciicastRecorder.recordOutput("After dispose");

        String content = Files.readString(outputPath);
        assertTrue(content.contains("Before dispose"));
        // After dispose, the writer is closed, so we can't check further
    }

    @Test
    @DisplayName("AsciicastRecorder dispose without installation")
    void disposeWithoutInstallation() {
        assertDoesNotThrow(() -> AsciicastRecorder.dispose());
    }

    @Test
    @DisplayName("AsciicastRecorder resetRecordingStateForTesting")
    void resetRecordingStateForTesting() {
        // Should not throw
        assertDoesNotThrow(() -> AsciicastRecorder.resetRecordingStateForTesting());
        assertNull(AsciicastRecorder.getRecordFilePath());
    }

    @Test
    @DisplayName("AsciicastRecorder multiple install calls")
    void multipleInstallCalls() throws Exception {
        Path outputPath1 = tempDir.resolve("test1.cast");
        Path outputPath2 = tempDir.resolve("test2.cast");

        AsciicastRecorder.installAsciicastRecorder(outputPath1.toString());
        AsciicastRecorder.installAsciicastRecorder(outputPath2.toString());

        // Second install should create new file
        assertTrue(Files.exists(outputPath2));
    }
}