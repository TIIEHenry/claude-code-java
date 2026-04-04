/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/ShellSnapshot
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.time.*;

/**
 * Shell snapshot - Capture shell state.
 */
public final class ShellSnapshot {
    private final String command;
    private final String output;
    private final int exitCode;
    private final long startTime;
    private final long endTime;
    private final String workingDirectory;
    private final Map<String, String> environment;
    private final ShellState state;

    /**
     * Create shell snapshot.
     */
    public ShellSnapshot(
        String command,
        String output,
        int exitCode,
        long startTime,
        long endTime,
        String workingDirectory,
        Map<String, String> environment,
        ShellState state
    ) {
        this.command = command;
        this.output = output;
        this.exitCode = exitCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.workingDirectory = workingDirectory;
        this.environment = new HashMap<>(environment);
        this.state = state;
    }

    /**
     * Get command.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Get output.
     */
    public String getOutput() {
        return output;
    }

    /**
     * Get exit code.
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Check if successful.
     */
    public boolean isSuccessful() {
        return exitCode == 0;
    }

    /**
     * Get duration in ms.
     */
    public long getDurationMs() {
        return endTime - startTime;
    }

    /**
     * Get start time.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get end time.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Get working directory.
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Get environment.
     */
    public Map<String, String> getEnvironment() {
        return Collections.unmodifiableMap(environment);
    }

    /**
     * Get state.
     */
    public ShellState getState() {
        return state;
    }

    /**
     * Format as log entry.
     */
    public String formatLogEntry() {
        return String.format(
            "[%s] Command: %s\nExit: %d | Duration: %dms\nOutput:\n%s",
            Instant.ofEpochMilli(startTime),
            command,
            exitCode,
            getDurationMs(),
            output
        );
    }

    /**
     * Shell state enum.
     */
    public enum ShellState {
        IDLE,
        RUNNING,
        WAITING_INPUT,
        INTERRUPTED,
        COMPLETED,
        ERROR
    }

    /**
     * Snapshot builder.
     */
    public static class Builder {
        private String command;
        private String output = "";
        private int exitCode = 0;
        private long startTime = System.currentTimeMillis();
        private long endTime;
        private String workingDirectory = System.getProperty("user.dir");
        private Map<String, String> environment = new HashMap<>();
        private ShellState state = ShellState.IDLE;

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder output(String output) {
            this.output = output;
            return this;
        }

        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment = new HashMap<>(environment);
            return this;
        }

        public Builder state(ShellState state) {
            this.state = state;
            return this;
        }

        public ShellSnapshot build() {
            if (endTime == 0) {
                endTime = System.currentTimeMillis();
            }
            return new ShellSnapshot(
                command, output, exitCode,
                startTime, endTime,
                workingDirectory, environment, state
            );
        }
    }
}