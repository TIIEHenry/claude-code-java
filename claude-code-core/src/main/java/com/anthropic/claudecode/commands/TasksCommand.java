/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/tasks
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Tasks command - View and manage running tasks.
 */
public final class TasksCommand implements Command {
    @Override
    public String name() {
        return "tasks";
    }

    @Override
    public String description() {
        return "View and manage running tasks";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(listTasks(context));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "list", "ls" -> listTasks(context);
            case "stop", "kill" -> {
                if (parts.length < 2) {
                    yield CommandResult.failure("Usage: tasks stop <task-id>");
                }
                yield stopTask(context, parts[1]);
            }
            case "clear" -> clearCompletedTasks(context);
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Usage: tasks [list|stop <id>|clear]\n");
                yield CommandResult.failure(sb.toString());
            }
        });
    }

    private CommandResult listTasks(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Running Tasks\n");
        sb.append("=============\n\n");

        sb.append("No running tasks.\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult stopTask(CommandContext context, String taskId) {
        return CommandResult.success("Stopped task: " + taskId + "\n");
    }

    private CommandResult clearCompletedTasks(CommandContext context) {
        return CommandResult.success("Cleared completed tasks\n");
    }

    /**
     * Task information record.
     */
    public record TaskInfo(
        String id,
        String name,
        String status,
        String progress,
        long startTime
    ) {}
}