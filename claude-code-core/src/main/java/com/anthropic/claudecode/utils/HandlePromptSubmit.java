/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code handle prompt submit utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Handle prompt submit utilities.
 * This is a simplified port - the full implementation depends on React/UI components.
 */
public final class HandlePromptSubmit {
    private HandlePromptSubmit() {}

    /**
     * Prompt input mode.
     */
    public enum PromptInputMode {
        PROMPT,
        BASH,
        TASK_NOTIFICATION
    }

    /**
     * Queued command.
     */
    public record QueuedCommand(
            String value,
            String preExpansionValue,
            PromptInputMode mode,
            Map<String, Object> pastedContents,
            boolean skipSlashCommands,
            String uuid,
            String workload,
            String origin
    ) {}

    /**
     * Prompt input helpers.
     */
    public interface PromptInputHelpers {
        void setCursorOffset(int offset);
        void clearBuffer();
        void resetHistory();
    }

    /**
     * Handle prompt submit parameters.
     */
    public record HandlePromptSubmitParams(
            String input,
            PromptInputMode mode,
            Map<String, Object> pastedContents,
            PromptInputHelpers helpers,
            List<QueuedCommand> queuedCommands,
            boolean skipSlashCommands,
            String uuid
    ) {}

    /**
     * Handle prompt submit.
     * This is a simplified implementation - the full version has many more parameters.
     */
    public static CompletableFuture<Void> handlePromptSubmit(HandlePromptSubmitParams params) {
        return CompletableFuture.runAsync(() -> {
            // Queue processor path
            if (params.queuedCommands() != null && !params.queuedCommands().isEmpty()) {
                executeUserInput(params.queuedCommands());
                return;
            }

            String input = params.input() != null ? params.input() : "";
            PromptInputMode mode = params.mode() != null ? params.mode() : PromptInputMode.PROMPT;

            if (input.trim().isEmpty()) {
                return;
            }

            // Handle exit commands
            if (!params.skipSlashCommands() &&
                Arrays.asList("exit", "quit", ":q", ":q!", ":wq", ":wq!").contains(input.trim())) {
                System.exit(0);
                return;
            }

            // Create queued command and execute
            QueuedCommand cmd = new QueuedCommand(
                    input,
                    input,
                    mode,
                    params.pastedContents(),
                    params.skipSlashCommands(),
                    params.uuid(),
                    null,
                    null
            );

            executeUserInput(List.of(cmd));
        });
    }

    /**
     * Execute user input.
     */
    private static void executeUserInput(List<QueuedCommand> commands) {
        for (QueuedCommand cmd : commands) {
            Debug.log("Executing command: " + cmd.value().substring(0, Math.min(100, cmd.value().length())));
            // Process the command - simplified implementation
            processUserInput(cmd);
        }
    }

    /**
     * Process user input.
     */
    private static void processUserInput(QueuedCommand cmd) {
        // This would be the main entry point for processing user input
        // In a full implementation, this would call the query engine
        Debug.log("Processing user input: mode=" + cmd.mode());
    }
}