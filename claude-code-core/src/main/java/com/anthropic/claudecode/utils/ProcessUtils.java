/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code process management utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Process management utilities.
 */
public final class ProcessUtils {
    private ProcessUtils() {}

    /**
     * Execute a command and return the output.
     */
    public static ProcessResult execute(String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = readStream(process.getInputStream());
        try {
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw new IOException("Process interrupted", e);
        }
    }

    /**
     * Execute a command with timeout.
     */
    public static ProcessResult executeWithTimeout(String[] command, long timeoutMs)
            throws IOException, TimeoutException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try {
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TimeoutException("Process timed out after " + timeoutMs + "ms");
            }
            String output = readStream(process.getInputStream());
            return new ProcessResult(process.exitValue(), output);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw new IOException("Process interrupted", e);
        }
    }

    /**
     * Execute a command in a specific directory.
     */
    public static ProcessResult executeInDir(File directory, String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(directory);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = readStream(process.getInputStream());
        try {
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw new IOException("Process interrupted", e);
        }
    }

    /**
     * Execute a command with environment variables.
     */
    public static ProcessResult executeWithEnv(Map<String, String> env, String... command)
            throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(env);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = readStream(process.getInputStream());
        try {
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw new IOException("Process interrupted", e);
        }
    }

    /**
     * Execute asynchronously and return a future.
     */
    public static CompletableFuture<ProcessResult> executeAsync(String... command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(command);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Kill a process by PID.
     */
    public static boolean killProcess(long pid) {
        try {
            Process process = Runtime.getRuntime().exec("kill " + pid);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Force kill a process by PID.
     */
    public static boolean forceKillProcess(long pid) {
        try {
            Process process = Runtime.getRuntime().exec("kill -9 " + pid);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a process is running.
     */
    public static boolean isProcessRunning(long pid) {
        try {
            Process process = Runtime.getRuntime().exec("ps -p " + pid);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get process info by PID.
     */
    public static Optional<ProcessInfo> getProcessInfo(long pid) {
        try {
            ProcessResult result = execute("ps", "-p", String.valueOf(pid), "-o", "pid,ppid,user,comm");
            if (result.exitCode() == 0 && !result.output().isEmpty()) {
                String[] lines = result.output().split("\n");
                if (lines.length > 1) {
                    String[] parts = lines[1].trim().split("\\s+");
                    if (parts.length >= 4) {
                        return Optional.of(new ProcessInfo(
                            Long.parseLong(parts[0]),
                            Long.parseLong(parts[1]),
                            parts[2],
                            parts[3]
                        ));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return Optional.empty();
    }

    /**
     * Find processes by name.
     */
    public static List<ProcessInfo> findProcessesByName(String name) {
        List<ProcessInfo> processes = new ArrayList<>();
        try {
            ProcessResult result = execute("pgrep", "-l", name);
            if (result.exitCode() == 0) {
                for (String line : result.output().split("\n")) {
                    if (!line.isEmpty()) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            processes.add(new ProcessInfo(
                                Long.parseLong(parts[0]),
                                -1,
                                "",
                                parts[1]
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return processes;
    }

    /**
     * Read stream content.
     */
    private static String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Process execution result.
     */
    public record ProcessResult(int exitCode, String output) {
        public boolean success() {
            return exitCode == 0;
        }

        public boolean failure() {
            return exitCode != 0;
        }
    }

    /**
     * Process information.
     */
    public record ProcessInfo(long pid, long ppid, String user, String command) {}
}