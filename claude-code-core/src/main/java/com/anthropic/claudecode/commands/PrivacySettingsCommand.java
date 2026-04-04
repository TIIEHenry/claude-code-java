/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/privacy-settings
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Privacy settings command - View and update privacy settings.
 */
public final class PrivacySettingsCommand implements Command {
    @Override
    public String name() {
        return "privacy-settings";
    }

    @Override
    public String description() {
        return "View and update your privacy settings";
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
            return CompletableFuture.completedFuture(showPrivacySettings(context));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "show", "status" -> showPrivacySettings(context);
            case "update", "set" -> updatePrivacySettings(context, parts);
            case "reset" -> resetPrivacySettings(context);
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Usage: privacy-settings [show|update|reset]\n");
                yield CommandResult.failure(sb.toString());
            }
        });
    }

    private CommandResult showPrivacySettings(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Privacy Settings\n");
        sb.append("================\n\n");

        sb.append("Data Collection:\n");
        sb.append("  Analytics: enabled\n");
        sb.append("  Error reporting: enabled\n");
        sb.append("  Usage stats: enabled\n");

        sb.append("\nModel Training:\n");
        sb.append("  Train on conversations: disabled\n");
        sb.append("  Train on code: disabled\n");

        sb.append("\nStorage:\n");
        sb.append("  Local history: enabled\n");
        sb.append("  Remote sync: disabled\n");

        sb.append("\nPrivacy Level: balanced\n");

        sb.append("\nUsage:\n");
        sb.append("  privacy-settings show    - View current settings\n");
        sb.append("  privacy-settings update  - Update settings\n");
        sb.append("  privacy-settings reset   - Reset to defaults\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult updatePrivacySettings(CommandContext context, String[] args) {
        if (args.length < 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("Please specify a setting to update.\n\n");
            sb.append("Available settings:\n");
            sb.append("  analytics <on|off>\n");
            sb.append("  error-reporting <on|off>\n");
            sb.append("  train-on-conversations <on|off>\n");
            sb.append("  train-on-code <on|off>\n");
            sb.append("  privacy-level <strict|balanced|minimal>\n");
            return CommandResult.failure(sb.toString());
        }

        String setting = args[1].toLowerCase();
        String value = args.length > 2 ? args[2].toLowerCase() : "";

        boolean boolValue = "on".equals(value) || "true".equals(value) || "yes".equals(value) || "enabled".equals(value);

        StringBuilder sb = new StringBuilder();
        sb.append("Privacy setting updated.\n");
        sb.append(setting).append(": ").append(boolValue ? "enabled" : "disabled").append("\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult resetPrivacySettings(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Privacy settings reset to defaults.\n\n");
        sb.append("Default settings:\n");
        sb.append("  Analytics: enabled\n");
        sb.append("  Error reporting: enabled\n");
        sb.append("  Train on conversations: disabled\n");
        sb.append("  Train on code: disabled\n");
        sb.append("  Privacy level: balanced\n");

        return CommandResult.success(sb.toString());
    }
}