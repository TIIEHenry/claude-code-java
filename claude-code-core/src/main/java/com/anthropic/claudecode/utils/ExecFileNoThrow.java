/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code exec file utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Utility wrappers for executing external processes without throwing exceptions.
 */
public final class ExecFileNoThrow {
    private ExecFileNoThrow() {}

    private static final int DEFAULT_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes
    private static final int DEFAULT_MAX_BUFFER = 1_000_000;

    /**
     * Execution result record.
     */
    public record ExecResult(
            String stdout,
            String stderr,
            int code,
            String error
    ) {
        public boolean success() {
            return code == 0 && error == null;
        }
    }

    /**
     * Execution options.
     */
    public record ExecOptions(
            long timeoutMs,
            boolean preserveOutputOnError,
            String cwd,
            Map<String, String> env,
            boolean shell,
            String input
    ) {
        public static ExecOptions defaults() {
            return new ExecOptions(
                    DEFAULT_TIMEOUT_MS,
                    true,
                    null,
                    null,
                    false,
                    null
            );
        }
    }

    /**
     * Execute a file without throwing exceptions.
     */
    public static CompletableFuture<ExecResult> execFileNoThrow(
            String file,
            String[] args) {
        return execFileNoThrow(file, args, ExecOptions.defaults());
    }

    /**
     * Execute a file without throwing exceptions with options.
     */
    public static CompletableFuture<ExecResult> execFileNoThrow(
            String file,
            String[] args,
            ExecOptions options) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(file);
                for (String arg : args) {
                    pb.command().add(arg);
                }

                // Set directory
                if (options.cwd() != null) {
                    pb.directory(new File(options.cwd()));
                } else {
                    pb.directory(new File(System.getProperty("user.dir")));
                }

                // Set environment
                if (options.env() != null) {
                    Map<String, String> env = pb.environment();
                    env.putAll(options.env());
                }

                // Redirect error stream
                pb.redirectErrorStream(false);

                // Start process
                Process process = pb.start();

                // Write input if provided
                if (options.input() != null) {
                    try (OutputStream os = process.getOutputStream()) {
                        os.write(options.input().getBytes());
                    }
                }

                // Read stdout and stderr
                StringBuilder stdout = new StringBuilder();
                StringBuilder stderr = new StringBuilder();

                Thread stdoutThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stdout.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        // Ignore
                    }
                });

                Thread stderrThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()))) {
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

                // Wait with timeout
                boolean finished = process.waitFor(options.timeoutMs(), TimeUnit.MILLISECONDS);

                stdoutThread.join(1000);
                stderrThread.join(1000);

                if (!finished) {
                    process.destroyForcibly();
                    return new ExecResult(
                            stdout.toString(),
                            stderr.toString(),
                            -1,
                            "Process timed out"
                    );
                }

                int exitCode = process.exitValue();

                if (exitCode != 0) {
                    if (options.preserveOutputOnError()) {
                        return new ExecResult(
                                stdout.toString(),
                                stderr.toString(),
                                exitCode,
                                "Exit code: " + exitCode
                        );
                    } else {
                        return new ExecResult("", "", exitCode, "Exit code: " + exitCode);
                    }
                }

                return new ExecResult(stdout.toString(), stderr.toString(), 0, null);

            } catch (Exception e) {
                return new ExecResult("", "", 1, e.getMessage());
            }
        });
    }

    /**
     * Execute a file with cwd.
     */
    public static CompletableFuture<ExecResult> execFileNoThrowWithCwd(
            String file,
            String[] args,
            String cwd) {

        ExecOptions options = new ExecOptions(
                DEFAULT_TIMEOUT_MS,
                true,
                cwd,
                null,
                false,
                null
        );

        return execFileNoThrow(file, args, options);
    }

    /**
     * Execute a shell command.
     */
    public static CompletableFuture<ExecResult> execShellCommand(
            String command,
            String cwd) {

        String os = System.getProperty("os.name", "").toLowerCase();
        String[] cmd;

        if (os.contains("win")) {
            cmd = new String[]{"cmd", "/c", command};
        } else {
            cmd = new String[]{"sh", "-c", command};
        }

        return execFileNoThrowWithCwd(cmd[0], Arrays.copyOfRange(cmd, 1, cmd.length), cwd);
    }
}