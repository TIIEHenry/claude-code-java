/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code exec file no throw portable
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.concurrent.*;

/**
 * Portable exec utilities that don't throw exceptions.
 * @deprecated Use async alternatives when possible. Sync exec calls block.
 */
@Deprecated
public final class ExecFileNoThrowPortable {
    private ExecFileNoThrowPortable() {}

    private static final long DEFAULT_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes

    /**
     * Execute result record.
     */
    public record ExecResult(int code, String stdout, String stderr, String error) {
        public boolean success() {
            return code == 0;
        }
    }

    /**
     * Execute a command synchronously without throwing.
     */
    public static ExecResult execSync(String command) {
        return execSync(command, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Execute a command synchronously with timeout.
     */
    public static ExecResult execSync(String command, long timeoutMs) {
        long start = System.currentTimeMillis();
        String truncatedCommand = command.length() > 200
                ? command.substring(0, 200) + "..."
                : command;

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Read stdout and stderr in parallel
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // Ignore
                }
            });

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // Ignore
                }
            });

            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            stdoutThread.join(1000);
            stderrThread.join(1000);

            if (!finished) {
                process.destroyForcibly();
                return new ExecResult(-1, stdout.toString(), stderr.toString(), "Command timed out");
            }

            long duration = System.currentTimeMillis() - start;
            SlowOperations.logSlowOperation("execSync", truncatedCommand, duration);

            return new ExecResult(process.exitValue(), stdout.toString(), stderr.toString(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecResult(-1, "", "", "Command interrupted");
        } catch (Exception e) {
            return new ExecResult(-1, "", "", e.getMessage());
        }
    }

    /**
     * Execute a command in a specific directory.
     */
    public static ExecResult execSync(String command, File cwd, long timeoutMs) {
        long start = System.currentTimeMillis();
        String truncatedCommand = command.length() > 200
                ? command.substring(0, 200) + "..."
                : command;

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.directory(cwd);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // Ignore
                }
            });

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // Ignore
                }
            });

            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            stdoutThread.join(1000);
            stderrThread.join(1000);

            if (!finished) {
                process.destroyForcibly();
                return new ExecResult(-1, stdout.toString(), stderr.toString(), "Command timed out");
            }

            long duration = System.currentTimeMillis() - start;
            SlowOperations.logSlowOperation("execSync", truncatedCommand, duration);

            return new ExecResult(process.exitValue(), stdout.toString(), stderr.toString(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecResult(-1, "", "", "Command interrupted");
        } catch (Exception e) {
            return new ExecResult(-1, "", "", e.getMessage());
        }
    }
}