/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/desktop
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Desktop command - Continue the current session in Claude Desktop.
 */
public final class DesktopCommand implements Command {
    @Override
    public String name() {
        return "desktop";
    }

    @Override
    public List<String> aliases() {
        return List.of("app");
    }

    @Override
    public String description() {
        return "Continue the current session in Claude Desktop";
    }

    @Override
    public boolean isEnabled() {
        return isSupportedPlatform();
    }

    @Override
    public boolean supportsNonInteractive() {
        return false;
    }

    private boolean isSupportedPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        // macOS or Windows x64
        return os.contains("mac") || (os.contains("win") && arch.contains("64"));
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        String sessionId = context.getSessionId();

        StringBuilder sb = new StringBuilder();
        sb.append("Claude Desktop\n");
        sb.append("==============\n\n");

        sb.append("Session: ").append(sessionId).append("\n\n");

        sb.append("Continuing this session in Claude Desktop allows:\n");
        sb.append("  - Persistent session across restarts\n");
        sb.append("  - Better UI experience\n");
        sb.append("  - System integration\n");
        sb.append("  - Offline history access\n");

        sb.append("\nTo continue in Claude Desktop:\n");
        sb.append("  1. Open Claude Desktop application\n");
        sb.append("  2. Select 'Resume Session'\n");
        sb.append("  3. Choose session ID: ").append(sessionId).append("\n");

        sb.append("\nAlternatively:\n");
        sb.append("  Click the direct link (if supported)\n");
        sb.append("  Use: claude-desktop --resume ").append(sessionId).append("\n");

        // Attempt to open desktop app
        boolean opened = context.openClaudeDesktop(sessionId);

        if (opened) {
            sb.append("\nDesktop app launched successfully.\n");
        } else {
            sb.append("\nCould not launch desktop app automatically.\n");
            sb.append("Please open Claude Desktop manually.\n");
        }

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}