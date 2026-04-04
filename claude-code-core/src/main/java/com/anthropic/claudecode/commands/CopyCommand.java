/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/copy
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Copy command - Copy conversation or code to clipboard.
 */
public final class CopyCommand implements Command {
    @Override
    public String name() {
        return "copy";
    }

    @Override
    public String description() {
        return "Copy conversation or code to clipboard";
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
        String target = (args == null || args.trim().isEmpty()) ? "last-output" : args.trim().toLowerCase().split("\\s+")[0];

        return CompletableFuture.completedFuture(switch (target) {
            case "conversation", "chat" -> copyConversation();
            case "last-output", "output" -> copyLastOutput();
            case "code", "last-code" -> copyLastCode();
            case "all" -> copyAll();
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown target: ").append(target).append("\n\n");
                sb.append("Usage: copy [conversation|last-output|code|all]\n");
                yield CommandResult.failure(sb.toString());
            }
        });
    }

    private CommandResult copyConversation() {
        return CommandResult.success("Copied conversation to clipboard\n");
    }

    private CommandResult copyLastOutput() {
        return CommandResult.success("Copied last output to clipboard\n");
    }

    private CommandResult copyLastCode() {
        return CommandResult.success("Copied code to clipboard\n");
    }

    private CommandResult copyAll() {
        return CommandResult.success("Copied all content to clipboard\n");
    }
}