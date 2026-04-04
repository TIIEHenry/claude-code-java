/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code exec sync wrapper utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Synchronous process execution wrapper with logging.
 *
 * @deprecated Use async alternatives when possible. Sync exec calls block.
 */
@Deprecated
public final class ExecSyncWrapper {
    private ExecSyncWrapper() {}

    /**
     * Execute a command synchronously and return the output.
     */
    public static String execSync(String command) throws IOException {
        return execSync(command, 30, TimeUnit.SECONDS);
    }

    /**
     * Execute a command synchronously with timeout.
     */
    public static String execSync(String command, long timeout, TimeUnit unit) throws IOException {
        long start = System.currentTimeMillis();
        String truncatedCommand = command.length() > 100 ? command.substring(0, 100) + "..." : command;

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeout, unit);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Command timed out: " + truncatedCommand);
            }

            long duration = System.currentTimeMillis() - start;
            SlowOperations.logSlowOperation("execSync", truncatedCommand, duration);

            return output.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command interrupted: " + truncatedCommand, e);
        }
    }

    /**
     * Execute a command synchronously in a specific directory.
     */
    public static String execSync(String command, File cwd, long timeout, TimeUnit unit) throws IOException {
        long start = System.currentTimeMillis();
        String truncatedCommand = command.length() > 100 ? command.substring(0, 100) + "..." : command;

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.directory(cwd);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeout, unit);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Command timed out: " + truncatedCommand);
            }

            long duration = System.currentTimeMillis() - start;
            SlowOperations.logSlowOperation("execSync", truncatedCommand, duration);

            return output.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command interrupted: " + truncatedCommand, e);
        }
    }

    /**
     * Execute a command and return exit code.
     */
    public static int execWithExitCode(String command) throws IOException {
        return execWithExitCode(command, 30, TimeUnit.SECONDS);
    }

    /**
     * Execute a command and return exit code with timeout.
     */
    public static int execWithExitCode(String command, long timeout, TimeUnit unit) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            Process process = pb.start();

            boolean finished = process.waitFor(timeout, unit);
            if (!finished) {
                process.destroyForcibly();
                return -1;
            }

            return process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /**
     * Execute a command and return result.
     */
    public static ExecResult execWithResult(String command, long timeout, TimeUnit unit) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeout, unit);
            if (!finished) {
                process.destroyForcibly();
                return new ExecResult(-1, output.toString(), "Command timed out");
            }

            return new ExecResult(process.exitValue(), output.toString(), null);
        } catch (Exception e) {
            return new ExecResult(-1, "", e.getMessage());
        }
    }

    /**
     * Execution result record.
     */
    public record ExecResult(int exitCode, String stdout, String error) {
        public boolean success() {
            return exitCode == 0;
        }
    }
}