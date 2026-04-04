/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/btw
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * BTW command - Ask a quick side question without interrupting main conversation.
 */
public final class BtwCommand implements Command {
    @Override
    public String name() {
        return "btw";
    }

    @Override
    public String description() {
        return "Ask a quick side question without interrupting the main conversation";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return false;
    }

    public String argumentHint() {
        return "<question>";
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        if (args == null || args.isEmpty()) {
            return CompletableFuture.completedFuture(
                CommandResult.failure("Please provide a question.\nUsage: btw <question>\n")
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Side Question\n");
        sb.append("============\n\n");

        sb.append("Question: ").append(args).append("\n\n");

        sb.append("This question has been noted as a side inquiry.\n");
        sb.append("It will be addressed without interrupting the current task.\n\n");

        sb.append("Note: Use '/btw' for quick clarifications.\n");
        sb.append("The main conversation flow continues.\n");

        // Queue the question for later processing
        context.queueSideQuestion(args);

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}