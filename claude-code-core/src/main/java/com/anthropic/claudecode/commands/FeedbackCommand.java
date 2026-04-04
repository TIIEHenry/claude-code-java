/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/feedback
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Feedback command - Send feedback about Claude Code.
 */
public final class FeedbackCommand implements Command {
    @Override
    public String name() {
        return "feedback";
    }

    @Override
    public String description() {
        return "Send feedback about Claude Code";
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
        StringBuilder sb = new StringBuilder();
        sb.append("Send Feedback\n");
        sb.append("=============\n\n");

        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(showFeedbackOptions(sb));
        }

        String[] parts = args.trim().split("\\s+", 2);
        String type = parts[0].toLowerCase();
        String message = parts.length > 1 ? parts[1] : null;

        return switch (type) {
            case "bug", "issue" -> CompletableFuture.completedFuture(submitFeedback(context, "bug", message, sb));
            case "feature", "request" -> CompletableFuture.completedFuture(submitFeedback(context, "feature", message, sb));
            case "general", "comment" -> CompletableFuture.completedFuture(submitFeedback(context, "general", message, sb));
            default -> CompletableFuture.completedFuture(submitFeedback(context, "general", args.trim(), sb));
        };
    }

    private CommandResult showFeedbackOptions(StringBuilder sb) {
        sb.append("We'd love to hear from you!\n\n");
        sb.append("Submit feedback:\n");
        sb.append("  feedback bug <description>     - Report a bug\n");
        sb.append("  feedback feature <description> - Request a feature\n");
        sb.append("  feedback <message>             - Send general feedback\n\n");
        sb.append("Or visit:\n");
        sb.append("  GitHub Issues: https://github.com/anthropics/claude-code/issues\n");
        sb.append("  Documentation: https://docs.anthropic.com/claude-code\n\n");
        sb.append("Your feedback helps us improve Claude Code!\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult submitFeedback(CommandContext context, String type, String message, StringBuilder sb) {
        if (message == null || message.isEmpty()) {
            sb.append("Please provide a message with your feedback.\n\n");
            sb.append("Example: feedback bug The tool doesn't work with large files\n");
            return CommandResult.failure(sb.toString());
        }

        sb.append("Thank you for your feedback!\n\n");
        sb.append("Type: ").append(type).append("\n");
        sb.append("Message: ").append(message).append("\n\n");
        sb.append("We appreciate you taking the time to help improve Claude Code.\n");
        return CommandResult.success(sb.toString());
    }
}