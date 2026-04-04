/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/insights
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Insights command - View usage insights and statistics.
 */
public final class InsightsCommand implements Command {
    @Override
    public String name() {
        return "insights";
    }

    @Override
    public String description() {
        return "View usage insights and statistics";
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
            return CompletableFuture.completedFuture(showAllInsights(context));
        }

        String category = args.trim().split("\\s+")[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (category) {
            case "tokens", "token" -> showTokenInsights(context);
            case "cost", "money" -> showCostInsights(context);
            case "sessions", "session" -> showSessionInsights(context);
            case "tools", "tool" -> showToolInsights(context);
            case "all", "summary" -> showAllInsights(context);
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown category: ").append(category).append("\n\n");
                sb.append("Available categories: tokens, cost, sessions, tools, all\n");
                yield CommandResult.failure(sb.toString());
            }
        });
    }

    private CommandResult showTokenInsights(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Token Usage Insights\n");
        sb.append("====================\n\n");
        sb.append("Total Input Tokens: ").append(context.getTotalInputTokens()).append("\n");
        sb.append("Total Output Tokens: ").append(context.getTotalOutputTokens()).append("\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showCostInsights(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cost Insights\n");
        sb.append("============\n\n");
        sb.append("Total Cost: $").append(String.format("%.2f", context.getTotalCost())).append("\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showSessionInsights(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Insights\n");
        sb.append("===============\n\n");
        sb.append("Message Count: ").append(context.getMessageCount()).append("\n");
        sb.append("Command Count: ").append(context.getCommandCount()).append("\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showToolInsights(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool Usage Insights\n");
        sb.append("==================\n\n");
        sb.append("Tool Call Count: ").append(context.getToolCallCount()).append("\n");
        sb.append("Modified Files: ").append(context.getModifiedFileCount()).append("\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showAllInsights(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage Insights Summary\n");
        sb.append("======================\n\n");
        sb.append("Messages: ").append(context.getMessageCount()).append("\n");
        sb.append("Tool Calls: ").append(context.getToolCallCount()).append("\n");
        sb.append("Input Tokens: ").append(context.getTotalInputTokens()).append("\n");
        sb.append("Output Tokens: ").append(context.getTotalOutputTokens()).append("\n");
        sb.append("Total Cost: $").append(String.format("%.2f", context.getTotalCost())).append("\n");
        return CommandResult.success(sb.toString());
    }
}