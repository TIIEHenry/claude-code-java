/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/branch
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Branch command - Manage Git branches for sessions.
 */
public final class BranchCommand implements Command {
    @Override
    public String name() {
        return "branch";
    }

    @Override
    public String description() {
        return "Create or switch session branches";
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
        if (args == null || args.isEmpty()) {
            return CompletableFuture.completedFuture(listBranches(context));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "list", "ls" -> listBranches(context);
            case "delete", "rm" -> {
                if (parts.length < 2) {
                    yield CommandResult.failure("Usage: branch delete <name>");
                }
                yield deleteBranch(context, parts[1]);
            }
            case "new", "create" -> {
                if (parts.length < 2) {
                    yield CommandResult.failure("Usage: branch create <name>");
                }
                yield createBranch(context, parts[1]);
            }
            default -> switchBranch(context, parts[0]);
        });
    }

    private CommandResult listBranches(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Branches\n");
        sb.append("================\n\n");

        List<CommandContext.SessionBranch> branches = context.getSessionBranches();

        if (branches.isEmpty()) {
            sb.append("No session branches.\n\n");
            sb.append("Create a branch with: branch create <name>\n");
            return CommandResult.success(sb.toString());
        }

        String currentBranch = context.getCurrentBranch();

        for (CommandContext.SessionBranch branch : branches) {
            String marker = branch.name().equals(currentBranch) ? "* " : "  ";
            sb.append(marker).append(branch.name());
            if (branch.description() != null) {
                sb.append(" - ").append(branch.description());
            }
            sb.append("\n");
        }

        sb.append("\n").append(branches.size()).append(" branch(es)\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult createBranch(CommandContext context, String name) {
        if (name == null || name.isEmpty()) {
            return CommandResult.failure("Branch name required");
        }

        // Validate name
        if (!isValidBranchName(name)) {
            return CommandResult.failure("Invalid branch name: " + name);
        }

        boolean created = context.createSessionBranch(name);
        if (created) {
            return CommandResult.success("Created branch: " + name);
        } else {
            return CommandResult.failure("Branch already exists: " + name);
        }
    }

    private CommandResult switchBranch(CommandContext context, String name) {
        boolean switched = context.switchSessionBranch(name);
        if (switched) {
            return CommandResult.success("Switched to branch: " + name);
        } else {
            return CommandResult.failure("Branch not found: " + name);
        }
    }

    private CommandResult deleteBranch(CommandContext context, String name) {
        String currentBranch = context.getCurrentBranch();
        if (name.equals(currentBranch)) {
            return CommandResult.failure("Cannot delete current branch");
        }

        boolean deleted = context.deleteSessionBranch(name);
        if (deleted) {
            return CommandResult.success("Deleted branch: " + name);
        } else {
            return CommandResult.failure("Branch not found: " + name);
        }
    }

    private boolean isValidBranchName(String name) {
        if (name == null || name.isEmpty()) return false;
        if (name.startsWith("-") || name.startsWith(".")) return false;
        if (name.contains("..") || name.contains("~") || name.contains("^")) return false;
        if (name.contains(":") || name.contains("\\") || name.contains("?")) return false;
        if (name.contains("*") || name.contains("[") || name.contains("]")) return false;
        return name.matches("[a-zA-Z0-9/_-]+");
    }
}