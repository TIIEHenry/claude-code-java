/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/model
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Model command - Set or show the AI model.
 */
public final class ModelCommand implements Command {
    private static final Map<String, String> AVAILABLE_MODELS = Map.of(
        "claude-opus-4-6", "Claude Opus 4.6 (most capable)",
        "claude-sonnet-4-6", "Claude Sonnet 4.6 (balanced)",
        "claude-haiku-4-5-20251001", "Claude Haiku 4.5 (fast)"
    );

    @Override
    public String name() {
        return "model";
    }

    @Override
    public String description() {
        return "Set the AI model for Claude Code";
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String[] parts = args != null ? args.trim().split("\\s+") : new String[0];

            if (parts.length == 0 || parts[0].isEmpty()) {
                // Show current model
                String currentModel = context.getCurrentModel();
                StringBuilder sb = new StringBuilder();

                sb.append("Current model: ").append(currentModel).append("\n\n");
                sb.append("Available models:\n");

                for (Map.Entry<String, String> entry : AVAILABLE_MODELS.entrySet()) {
                    String marker = entry.getKey().equals(currentModel) ? " (current)" : "";
                    sb.append(String.format("  %-30s %s%s\n", entry.getKey(), entry.getValue(), marker));
                }

                return CommandResult.success(sb.toString());
            }

            // Set new model
            String newModel = parts[0];
            if (!AVAILABLE_MODELS.containsKey(newModel)) {
                return CommandResult.failure("Unknown model: " + newModel +
                    "\nAvailable models: " + String.join(", ", AVAILABLE_MODELS.keySet()));
            }

            context.setCurrentModel(newModel);
            return CommandResult.success("Model set to: " + newModel);
        });
    }
}