/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/advisor
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Advisor command - Configure the advisor model.
 */
public final class AdvisorCommand implements Command {
    @Override
    public String name() {
        return "advisor";
    }

    @Override
    public String description() {
        return "Configure the advisor model";
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String arg = (args == null || args.trim().isEmpty()) ? "" : args.trim().toLowerCase();

            StringBuilder sb = new StringBuilder();
            sb.append("Advisor Configuration\n");
            sb.append("====================\n\n");

            if (arg.isEmpty() || "status".equals(arg)) {
                sb.append("Current advisor: ").append(context.getAdvisorModel() != null ? context.getAdvisorModel() : "disabled").append("\n");
                sb.append("\nUsage: advisor [<model>|off]\n");
                sb.append("\nAvailable models:\n");
                sb.append("  claude-opus-4-6\n");
                sb.append("  claude-sonnet-4-6\n");
                sb.append("  claude-sonnet-4-5\n");
            } else if ("off".equals(arg) || "disable".equals(arg)) {
                context.setAdvisorModel(null);
                sb.append("Advisor disabled.\n");
            } else {
                context.setAdvisorModel(arg);
                sb.append("Advisor set to: ").append(arg).append("\n");
            }

            return CommandResult.success(sb.toString());
        });
    }
}