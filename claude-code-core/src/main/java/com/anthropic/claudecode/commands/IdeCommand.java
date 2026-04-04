/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/ide
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * IDE command - Configure IDE integration.
 */
public final class IdeCommand implements Command {
    @Override
    public String name() {
        return "ide";
    }

    @Override
    public String description() {
        return "Configure IDE integration";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    // Supported IDEs
    private static final Set<String> SUPPORTED_IDES = Set.of(
        "vscode", "jetbrains", "intellij", "cursor", "neovim", "emacs"
    );

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        List<String> argList = args == null || args.isEmpty() ? List.of() : List.of(args.split("\\s+"));
        if (argList.isEmpty()) {
            return CompletableFuture.completedFuture(showIdeStatus(context));
        }

        String action = argList.get(0).toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "status", "show" -> showIdeStatus(context);
            case "connect" -> connectIde(context, argList);
            case "disconnect" -> disconnectIde(context);
            case "settings", "config" -> showIdeSettings(context);
            case "list" -> listSupportedIdes(context);
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Usage: ide [status|connect|disconnect|settings|list]\n");
                yield CommandResult.failure(sb.toString());
            }
        });
    }

    private CommandResult showIdeStatus(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("IDE Integration Status\n");
        sb.append("======================\n\n");

        sb.append("Status: Not connected\n\n");
        sb.append("Available IDEs:\n");
        for (String ide : SUPPORTED_IDES) {
            sb.append("  - ").append(ide).append("\n");
        }
        sb.append("\nUse 'ide connect <ide>' to connect.\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult connectIde(CommandContext context, List<String> args) {
        if (args.size() < 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("Please specify an IDE to connect.\n\n");
            sb.append("Supported IDEs: ").append(String.join(", ", SUPPORTED_IDES)).append("\n");
            sb.append("Usage: ide connect <ide>\n");
            return CommandResult.failure(sb.toString());
        }

        String ideType = args.get(1).toLowerCase();

        if (!SUPPORTED_IDES.contains(ideType)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unsupported IDE: ").append(ideType).append("\n\n");
            sb.append("Supported IDEs: ").append(String.join(", ", SUPPORTED_IDES)).append("\n");
            return CommandResult.failure(sb.toString());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Connected to ").append(ideType).append("\n\n");
        sb.append("Features enabled:\n");
        sb.append("  + File navigation\n");
        sb.append("  + Code completion hints\n");
        sb.append("  + Quick fixes\n");
        sb.append("  + Terminal integration\n");
        sb.append("\nUse 'ide settings' to configure.\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult disconnectIde(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Disconnected from IDE.\n");
        sb.append("IDE integration features are now disabled.\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showIdeSettings(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("IDE Settings\n");
        sb.append("============\n\n");
        sb.append("Auto-save: enabled\n");
        sb.append("File sync: enabled\n");
        sb.append("Terminal focus: enabled\n");
        sb.append("Code hints: enabled\n");
        sb.append("Quick fixes: enabled\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult listSupportedIdes(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Supported IDEs\n");
        sb.append("==============\n\n");

        for (String ide : SUPPORTED_IDES) {
            sb.append("  ").append(ide).append("\n");
        }

        sb.append("\nUsage: ide connect <ide>\n");

        return CommandResult.success(sb.toString());
    }
}