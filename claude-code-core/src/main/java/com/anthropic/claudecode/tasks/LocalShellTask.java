/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code LocalShellTask
 */
package com.anthropic.claudecode.tasks;

import java.util.concurrent.*;

/**
 * Local shell task - executes shell commands locally.
 */
public final class LocalShellTask implements Task {
    private static final LocalShellTask INSTANCE = new LocalShellTask();

    private LocalShellTask() {}

    public static LocalShellTask getInstance() {
        return INSTANCE;
    }

    @Override
    public TaskType getType() {
        return TaskType.LOCAL_BASH;
    }

    @Override
    public String getName() {
        return "LocalShell";
    }

    @Override
    public String getDescription() {
        return "Execute shell commands locally";
    }

    @Override
    public CompletableFuture<TaskResult> execute(TaskContext context) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                // Execute shell command
                String command = (String) context.getProperty("command");
                if (command == null || command.isEmpty()) {
                    return TaskResult.failure("No command specified");
                }

                ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                long duration = System.currentTimeMillis() - start;

                if (exitCode == 0) {
                    return TaskResult.success(output.toString(), duration);
                } else {
                    return TaskResult.failure("Exit code: " + exitCode + "\n" + output, duration);
                }
            } catch (Exception e) {
                return TaskResult.failure(e.getMessage(), System.currentTimeMillis() - start);
            }
        });
    }
}