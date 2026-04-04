/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/review
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Review command - Review code for issues.
 */
public final class ReviewCommand implements Command {
    @Override
    public String name() {
        return "review";
    }

    @Override
    public String description() {
        return "Review recent changes for potential issues";
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Review the recent changes in this codebase and identify:\n\n");
            prompt.append("1. **Potential bugs or errors** - Logic errors, edge cases, potential crashes\n");
            prompt.append("2. **Security concerns** - Input validation, authentication, data exposure\n");
            prompt.append("3. **Code quality issues** - Antipatterns, performance problems, maintainability\n");
            prompt.append("4. **Best practice violations** - Language/framework specific best practices\n\n");
            prompt.append("Focus on actionable feedback with specific file locations and suggested fixes.\n");
            return CommandResult.success(prompt.toString());
        });
    }
}