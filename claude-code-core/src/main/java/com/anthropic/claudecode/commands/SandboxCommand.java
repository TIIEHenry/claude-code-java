/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/sandbox-toggle
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Sandbox command - Configure sandbox settings for command execution.
 */
public final class SandboxCommand implements Command {
    @Override
    public String name() {
        return "sandbox";
    }

    @Override
    public String description() {
        return "Configure sandbox settings for command execution";
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
            return CompletableFuture.completedFuture(showSandboxStatus(context));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return switch (action) {
            case "on", "enable" -> CompletableFuture.completedFuture(enableSandbox(context));
            case "off", "disable" -> CompletableFuture.completedFuture(disableSandbox(context));
            case "status", "show" -> CompletableFuture.completedFuture(showSandboxStatus(context));
            case "exclude", "allow" -> CompletableFuture.completedFuture(excludeCommand(context, parts));
            case "auto" -> CompletableFuture.completedFuture(setAutoAllow(context, parts));
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Usage: sandbox [on|off|status|exclude|auto]\n");
                yield CompletableFuture.completedFuture(CommandResult.failure(sb.toString()));
            }
        };
    }

    private CommandResult showSandboxStatus(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sandbox Configuration\n");
        sb.append("====================\n\n");
        sb.append("Status: sandbox disabled (placeholder)\n\n");
        sb.append("Usage:\n");
        sb.append("  sandbox on      - Enable sandbox\n");
        sb.append("  sandbox off     - Disable sandbox\n");
        sb.append("  sandbox exclude <pattern> - Exclude command from sandbox\n");
        sb.append("  sandbox auto on|off - Toggle auto-allow\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult enableSandbox(CommandContext context) {
        context.setSandboxEnabled(true);
        StringBuilder sb = new StringBuilder();
        sb.append("Sandbox enabled.\n\n");
        sb.append("All Bash commands will now run in sandboxed environment.\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult disableSandbox(CommandContext context) {
        context.setSandboxEnabled(false);
        StringBuilder sb = new StringBuilder();
        sb.append("Sandbox disabled.\n\n");
        sb.append("Bash commands will run without sandbox restrictions.\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult excludeCommand(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("Please provide a command pattern to exclude.\n\nUsage: sandbox exclude <pattern>\n");
        }
        context.excludeCommandFromSandbox(args[1]);
        return CommandResult.success("Command excluded from sandbox: " + args[1] + "\n");
    }

    private CommandResult setAutoAllow(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("Please specify 'on' or 'off'.\n\nUsage: sandbox auto [on|off]\n");
        }
        boolean autoAllow = "on".equalsIgnoreCase(args[1]) || "true".equalsIgnoreCase(args[1]);
        context.setSandboxAutoAllow(autoAllow);
        return CommandResult.success("Auto-allow set to: " + (autoAllow ? "enabled" : "disabled") + "\n");
    }
}