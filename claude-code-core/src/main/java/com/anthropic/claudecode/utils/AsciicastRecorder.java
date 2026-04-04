/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/asciicast.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Asciicast recorder for terminal session recording.
 * Records terminal output in asciicast v2 format.
 */
public final class AsciicastRecorder {
    private AsciicastRecorder() {}

    private static final AtomicReference<RecordingState> recordingState = new AtomicReference<>();

    private static class RecordingState {
        String filePath;
        long timestamp;
        BufferedWriter writer;
        long startTimeNanos;
        int cols = 80;
        int rows = 24;
    }

    /**
     * Get the recording file path.
     */
    public static String getRecordFilePath() {
        RecordingState state = recordingState.get();
        if (state != null && state.filePath != null) {
            return state.filePath;
        }

        if (!EnvUtils.isUserTypeAnt()) {
            return null;
        }

        if (!EnvUtils.isTruthy(System.getenv("CLAUDE_CODE_TERMINAL_RECORDING"))) {
            return null;
        }

        return state != null ? state.filePath : null;
    }

    /**
     * Install the asciicast recorder.
     */
    public static void installAsciicastRecorder(String outputPath) {
        if (outputPath == null || outputPath.isEmpty()) {
            return;
        }

        RecordingState state = new RecordingState();
        state.filePath = outputPath;
        state.timestamp = System.currentTimeMillis();
        state.startTimeNanos = System.nanoTime();
        state.cols = getTerminalCols();
        state.rows = getTerminalRows();

        try {
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());

            // Write asciicast v2 header
            String header = String.format(
                "{\"version\":2,\"width\":%d,\"height\":%d,\"timestamp\":%d,\"env\":{\"SHELL\":\"%s\",\"TERM\":\"%s\"}}\n",
                state.cols,
                state.rows,
                Instant.now().getEpochSecond(),
                System.getenv("SHELL") != null ? System.getenv("SHELL") : "",
                System.getenv("TERM") != null ? System.getenv("TERM") : ""
            );

            state.writer = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(outputPath),
                    StandardCharsets.UTF_8
                )
            );
            state.writer.write(header);

            recordingState.set(state);
        } catch (IOException e) {
            // Silently fail
        }
    }

    /**
     * Record output to the asciicast file.
     */
    public static void recordOutput(String text) {
        RecordingState state = recordingState.get();
        if (state == null || state.writer == null || text == null) {
            return;
        }

        try {
            double elapsed = (System.nanoTime() - state.startTimeNanos) / 1_000_000_000.0;
            String jsonEntry = String.format("[%.6f,\"o\",%s]\n",
                elapsed,
                escapeJson(text)
            );
            state.writer.write(jsonEntry);
        } catch (IOException e) {
            // Silently fail
        }
    }

    /**
     * Flush pending writes.
     */
    public static void flush() {
        RecordingState state = recordingState.get();
        if (state != null && state.writer != null) {
            try {
                state.writer.flush();
            } catch (IOException e) {
                // Silently fail
            }
        }
    }

    /**
     * Dispose the recorder.
     */
    public static void dispose() {
        RecordingState state = recordingState.getAndSet(null);
        if (state != null && state.writer != null) {
            try {
                state.writer.flush();
                state.writer.close();
            } catch (IOException e) {
                // Silently fail
            }
        }
    }

    /**
     * Reset recording state for testing.
     */
    public static void resetRecordingStateForTesting() {
        dispose();
    }

    private static int getTerminalCols() {
        try {
            return System.getenv("COLUMNS") != null
                ? Integer.parseInt(System.getenv("COLUMNS"))
                : 80;
        } catch (NumberFormatException e) {
            return 80;
        }
    }

    private static int getTerminalRows() {
        try {
            return System.getenv("LINES") != null
                ? Integer.parseInt(System.getenv("LINES"))
                : 24;
        } catch (NumberFormatException e) {
            return 24;
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32 || c > 126) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}