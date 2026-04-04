/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/permissions
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Permissions command - Manage allow & deny tool permission rules.
 */
public final class PermissionsCommand implements Command {
    @Override
    public String name() {
        return "permissions";
    }

    @Override
    public List<String> aliases() {
        return List.of("allowed-tools");
    }

    @Override
    public String description() {
        return "Manage allow & deny tool permission rules";
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
        List<String> argList = parseArgs(args);

        if (argList.isEmpty()) {
            return CompletableFuture.completedFuture(listPermissions(context));
        }

        String subcommand = argList.get(0).toLowerCase();

        CommandResult result = switch (subcommand) {
            case "list", "ls" -> listPermissions(context);
            case "allow" -> {
                if (argList.size() < 2) {
                    yield CommandResult.failure("Usage: /permissions allow <tool-pattern>");
                }
                yield addAllowRule(context, argList.get(1));
            }
            case "deny" -> {
                if (argList.size() < 2) {
                    yield CommandResult.failure("Usage: /permissions deny <tool-pattern>");
                }
                yield addDenyRule(context, argList.get(1));
            }
            case "reset", "clear" -> resetPermissions(context);
            default -> CommandResult.failure(
                "Unknown subcommand: " + subcommand + "\n" +
                "Available: list, allow, deny, reset"
            );
        };

        return CompletableFuture.completedFuture(result);
    }

    private List<String> parseArgs(String args) {
        if (args == null || args.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.asList(args.trim().split("\\s+"));
    }

    private CommandResult listPermissions(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Permission Rules\n");
        sb.append("================\n\n");

        // Get current permission rules
        List<PermissionRule> allowRules = context.getAllowRules();
        List<PermissionRule> denyRules = context.getDenyRules();

        sb.append("Allow Rules:\n");
        if (allowRules.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (PermissionRule rule : allowRules) {
                sb.append("  ✓ ").append(rule.pattern()).append("\n");
            }
        }

        sb.append("\nDeny Rules:\n");
        if (denyRules.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (PermissionRule rule : denyRules) {
                sb.append("  ✗ ").append(rule.pattern()).append("\n");
            }
        }

        sb.append("\nPermission Mode: ").append(context.getPermissionMode()).append("\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult addAllowRule(CommandContext context, String pattern) {
        context.addAllowRule(pattern);
        return CommandResult.success("Added allow rule: " + pattern);
    }

    private CommandResult addDenyRule(CommandContext context, String pattern) {
        context.addDenyRule(pattern);
        return CommandResult.success("Added deny rule: " + pattern);
    }

    private CommandResult resetPermissions(CommandContext context) {
        context.resetPermissionRules();
        return CommandResult.success("All permission rules cleared.");
    }

    /**
     * Permission rule record.
     */
    public record PermissionRule(String pattern, String source) {}
}