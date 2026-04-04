/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/session
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Session command - Show remote session URL and QR code.
 */
public final class SessionCommand implements Command {
    @Override
    public String name() {
        return "session";
    }

    @Override
    public List<String> aliases() {
        return List.of("remote");
    }

    @Override
    public String description() {
        return "Show remote session URL and QR code";
    }

    @Override
    public boolean isEnabled() {
        // Only enabled in remote mode
        String remoteMode = System.getenv("CLAUDE_CODE_REMOTE");
        return "true".equalsIgnoreCase(remoteMode);
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Remote Session\n");
        sb.append("==============\n\n");

        // Get session URL
        String sessionUrl = getSessionUrl(context);

        if (sessionUrl == null || sessionUrl.isEmpty()) {
            return CompletableFuture.completedFuture(CommandResult.failure("No active remote session."));
        }

        sb.append("Session URL:\n");
        sb.append("  ").append(sessionUrl).append("\n\n");

        // Get session ID
        String sessionId = System.getenv("CLAUDE_CODE_REMOTE_SESSION_ID");
        if (sessionId != null) {
            sb.append("Session ID: ").append(sessionId).append("\n\n");
        }

        // Get container ID
        String containerId = System.getenv("CLAUDE_CODE_CONTAINER_ID");
        if (containerId != null) {
            sb.append("Container ID: ").append(containerId).append("\n\n");
        }

        // Generate QR code (ASCII art)
        sb.append("QR Code:\n");
        sb.append(generateQRCode(sessionUrl));

        sb.append("\nScan the QR code or visit the URL to connect.\n");

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }

    private String getSessionUrl(CommandContext context) {
        // In real implementation, would get from remote session manager
        String baseUrl = System.getenv("CLAUDE_CODE_REMOTE_URL");
        String sessionId = System.getenv("CLAUDE_CODE_REMOTE_SESSION_ID");

        if (baseUrl != null && sessionId != null) {
            return baseUrl + "/session/" + sessionId;
        }

        return null;
    }

    private String generateQRCode(String url) {
        // Simple ASCII QR code placeholder
        StringBuilder sb = new StringBuilder();
        sb.append("┌──────────────────┐\n");
        sb.append("│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓  │\n");
        sb.append("│  ▓▓         ▓▓▓  │\n");
        sb.append("│  ▓▓  QR CODE ▓▓▓  │\n");
        sb.append("│  ▓▓         ▓▓▓  │\n");
        sb.append("│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓  │\n");
        sb.append("└──────────────────┘\n");
        return sb.toString();
    }
}