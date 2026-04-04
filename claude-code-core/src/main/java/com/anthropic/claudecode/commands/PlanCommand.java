/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/plan
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Plan command - Enable plan mode or view the current session plan.
 */
public final class PlanCommand implements Command {
    @Override
    public String name() {
        return "plan";
    }

    @Override
    public String description() {
        return "Enable plan mode or view the current session plan";
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
            return CompletableFuture.completedFuture(showCurrentPlan(context));
        }

        String arg = args.trim();

        if ("open".equalsIgnoreCase(arg)) {
            return CompletableFuture.completedFuture(openPlanEditor(context));
        }

        return CompletableFuture.completedFuture(createPlan(context, arg));
    }

    private CommandResult showCurrentPlan(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Plan\n");
        sb.append("============\n\n");

        String plan = context.getCurrentPlan();

        if (plan == null) {
            sb.append("No plan set for this session.\n\n");
            sb.append("To create a plan:\n");
            sb.append("  plan <description>  - Create a plan with description\n");
            sb.append("  plan open           - Open plan editor\n\n");
            sb.append("Plan mode helps Claude work systematically through complex tasks.\n");
            return CommandResult.success(sb.toString());
        }

        sb.append("Description: ").append(plan).append("\n\n");
        sb.append("\nUse 'plan open' to edit the plan.\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult openPlanEditor(CommandContext context) {
        context.openPlanEditor();
        return CommandResult.success("Plan editor opened.\n");
    }

    private CommandResult createPlan(CommandContext context, String description) {
        if (description == null || description.trim().isEmpty()) {
            return CommandResult.failure("Please provide a plan description");
        }

        context.createPlan(description.trim());

        StringBuilder sb = new StringBuilder();
        sb.append("Plan created: ").append(description).append("\n\n");
        sb.append("Claude will now work through this systematically.\n");
        sb.append("Use 'plan' to view progress, 'plan open' to edit.\n");
        return CommandResult.success(sb.toString());
    }
}