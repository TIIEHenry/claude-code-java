/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/login
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Login command - Sign in with Anthropic account.
 */
public final class LoginCommand implements Command {
    @Override
    public String name() {
        return "login";
    }

    @Override
    public String description() {
        return hasApiKeyAuth()
            ? "Switch Anthropic accounts"
            : "Sign in with your Anthropic account";
    }

    @Override
    public boolean isEnabled() {
        String disabled = System.getenv("DISABLE_LOGIN_COMMAND");
        return !isEnvTruthy(disabled);
    }

    @Override
    public boolean supportsNonInteractive() {
        return false;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();

        // Check current auth status
        String currentAuth = getAuthStatus();

        if (hasApiKeyAuth()) {
            sb.append("Current Authentication\n");
            sb.append("=====================\n\n");
            sb.append("You are currently authenticated");
            if (currentAuth != null) {
                sb.append(" as: ").append(currentAuth);
            }
            sb.append("\n\n");
            sb.append("To switch accounts:\n");
            sb.append("1. Run: logout\n");
            sb.append("2. Run: login again\n");
            sb.append("\n");
            sb.append("Or set a new ANTHROPIC_API_KEY environment variable.\n");
        } else {
            sb.append("Anthropic Login\n");
            sb.append("===============\n\n");
            sb.append("To authenticate with Anthropic:\n\n");
            sb.append("Option 1: API Key\n");
            sb.append("  Set the ANTHROPIC_API_KEY environment variable:\n");
            sb.append("  export ANTHROPIC_API_KEY=your-api-key\n\n");
            sb.append("Option 2: OAuth (coming soon)\n");
            sb.append("  Run: claude login --oauth\n\n");
            sb.append("Get your API key from: https://console.anthropic.com/\n");
        }

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }

    private boolean hasApiKeyAuth() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        return apiKey != null && !apiKey.isEmpty();
    }

    private String getAuthStatus() {
        String userType = System.getenv("USER_TYPE");
        if (userType != null) {
            return userType;
        }
        return null;
    }

    private boolean isEnvTruthy(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }
}