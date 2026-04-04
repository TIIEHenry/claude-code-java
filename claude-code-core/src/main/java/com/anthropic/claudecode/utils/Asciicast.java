/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code asciicast recording utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/**
 * Asciicast recording utilities for terminal session recording.
 */
public final class Asciicast {
    private Asciicast() {}

    private static String recordFilePath = null;
    private static long timestamp = 0;
    private static AsciicastRecorder recorder = null;

    /**
     * Get the recording file path.
     */
    public static String getRecordFilePath() {
        if (recordFilePath != null) {
            return recordFilePath;
        }

        if (!"ant".equals(System.getenv("USER_TYPE"))) {
            return null;
        }

        if (!EnvUtils.isEnvTruthy(System.getenv("CLAUDE_CODE_TERMINAL_RECORDING"))) {
            return null;
        }

        Path projectsDir = Paths.get(EnvUtils.getClaudeConfigHomeDir()).resolve("projects");
        Path projectDir = projectsDir.resolve(PathUtils.sanitizePath(System.getProperty("user.dir")));

        timestamp = System.currentTimeMillis();
        recordFilePath = projectDir.resolve(getSessionId() + "-" + timestamp + ".cast").toString();

        return recordFilePath;
    }

    /**
     * Reset recording state for testing.
     */
    public static void resetRecordingState() {
        recordFilePath = null;
        timestamp = 0;
    }

    /**
     * Find all .cast files for the current session.
     */
    public static List<String> getSessionRecordingPaths() {
        String sessionId = getSessionId();
        Path projectsDir = Paths.get(EnvUtils.getClaudeConfigHomeDir()).resolve("projects");
        Path projectDir = projectsDir.resolve(PathUtils.sanitizePath(System.getProperty("user.dir")));

        try {
            if (!Files.isDirectory(projectDir)) {
                return List.of();
            }

            return Files.list(projectDir)
                    .filter(p -> p.getFileName().toString().startsWith(sessionId) &&
                                 p.getFileName().toString().endsWith(".cast"))
                    .sorted()
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Flush pending recording data.
     */
    public static void flushAsciicastRecorder() {
        if (recorder != null) {
            recorder.flush();
        }
    }

    /**
     * Install the asciicast recorder.
     */
    public static void installAsciicastRecorder() {
        String filePath = getRecordFilePath();
        if (filePath == null) {
            return;
        }

        int cols = 80;
        int rows = 24;
        long startTime = System.nanoTime();

        // Write asciicast v2 header
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("version", 2);
        header.put("width", cols);
        header.put("height", rows);
        header.put("timestamp", Instant.now().getEpochSecond());
        header.put("env", Map.of(
                "SHELL", System.getenv().getOrDefault("SHELL", ""),
                "TERM", System.getenv().getOrDefault("TERM", "")
        ));

        try {
            Files.createDirectories(Paths.get(filePath).getParent());
            Files.writeString(Paths.get(filePath), JsonUtils.toJson(header) + "\n");
        } catch (IOException e) {
            // Ignore
        }

        recorder = new AsciicastRecorder(Paths.get(filePath), startTime);
    }

    /**
     * Get session ID (placeholder).
     */
    private static String getSessionId() {
        String sessionId = System.getenv("CLAUDE_SESSION_ID");
        return sessionId != null ? sessionId : UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Asciicast recorder implementation.
     */
    private static class AsciicastRecorder {
        private final Path filePath;
        private final long startTime;
        private final List<String> buffer = new ArrayList<>();
        private volatile boolean disposed = false;

        AsciicastRecorder(Path filePath, long startTime) {
            this.filePath = filePath;
            this.startTime = startTime;
        }

        void write(String text) {
            if (disposed) return;

            double elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0;
            List<Object> event = List.of(elapsed, "o", text);

            synchronized (buffer) {
                buffer.add(JsonUtils.toJson(event) + "\n");
                if (buffer.size() >= 50) {
                    flush();
                }
            }
        }

        void flush() {
            List<String> toWrite;
            synchronized (buffer) {
                toWrite = new ArrayList<>(buffer);
                buffer.clear();
            }

            try {
                Files.write(filePath, toWrite, StandardOpenOption.APPEND);
            } catch (IOException e) {
                // Ignore
            }
        }

        void dispose() {
            disposed = true;
            flush();
        }
    }
}