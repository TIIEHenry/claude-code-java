/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/doctor
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Doctor command - Diagnose and verify installation and settings.
 */
public final class DoctorCommand implements Command {
    @Override
    public String name() {
        return "doctor";
    }

    @Override
    public String description() {
        return "Diagnose and verify your Claude Code installation and settings";
    }

    @Override
    public boolean isEnabled() {
        String disabled = System.getenv("DISABLE_DOCTOR_COMMAND");
        return disabled == null || !"true".equalsIgnoreCase(disabled);
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Claude Code Diagnostics\n");
        sb.append("=======================\n\n");

        sb.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("Java Home: ").append(System.getProperty("java.home")).append("\n\n");

        sb.append("Operating System: ").append(System.getProperty("os.name")).append("\n");
        sb.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
        sb.append("OS Architecture: ").append(System.getProperty("os.arch")).append("\n\n");

        sb.append("Working Directory: ").append(System.getProperty("user.dir")).append("\n");
        sb.append("User Home: ").append(System.getProperty("user.home")).append("\n\n");

        sb.append("Environment Variables:\n");
        checkEnvVar(sb, "ANTHROPIC_API_KEY", true);
        checkEnvVar(sb, "ANTHROPIC_AUTH_TOKEN", true);
        checkEnvVar(sb, "ANTHROPIC_MODEL", false);
        checkEnvVar(sb, "CLAUDE_CODE_USE_BEDROCK", false);
        checkEnvVar(sb, "CLAUDE_CODE_USE_VERTEX", false);
        checkEnvVar(sb, "CLAUDE_CODE_USE_FOUNDRY", false);
        checkEnvVar(sb, "MCP_CLIENT_SECRET", true);

        sb.append("\n");

        sb.append("Configuration:\n");
        Map<String, Object> config = context.getConfiguration();
        if (config != null && !config.isEmpty()) {
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        } else {
            sb.append("  No configuration loaded\n");
        }

        sb.append("\n");

        sb.append("Git Status:\n");
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--short");
            pb.directory(new java.io.File(System.getProperty("user.dir")));
            Process p = pb.start();
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (p.exitValue() == 0) {
                sb.append("  Git repository detected\n");
            } else {
                sb.append("  Not a git repository\n");
            }
        } catch (Exception e) {
            sb.append("  Git not available or not a repository\n");
        }

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }

    private void checkEnvVar(StringBuilder sb, String name, boolean sensitive) {
        String value = System.getenv(name);
        if (value != null) {
            if (sensitive) {
                sb.append(String.format("  %-25s = [SET] (%d chars)\n", name, value.length()));
            } else {
                sb.append(String.format("  %-25s = %s\n", name, value));
            }
        } else {
            sb.append(String.format("  %-25s = [NOT SET]\n", name));
        }
    }
}