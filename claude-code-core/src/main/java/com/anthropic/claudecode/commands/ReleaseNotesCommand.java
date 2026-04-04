/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/release-notes
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Release notes command - View release notes.
 */
public final class ReleaseNotesCommand implements Command {
    @Override
    public String name() {
        return "release-notes";
    }

    @Override
    public List<String> aliases() {
        return List.of("releases", "whats-new", "news");
    }

    @Override
    public String description() {
        return "View release notes";
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
            return CompletableFuture.completedFuture(showLatestNotes());
        }

        String action = args.trim().toLowerCase().split("\\s+")[0];

        return CompletableFuture.completedFuture(switch (action) {
            case "latest", "current" -> showLatestNotes();
            case "list", "all" -> listAllReleases();
            default -> showVersionNotes(action);
        });
    }

    private CommandResult showLatestNotes() {
        StringBuilder sb = new StringBuilder();
        sb.append("Claude Code Release Notes\n");
        sb.append("=========================\n\n");

        sb.append("Version: 1.0.0-java\n");
        sb.append("Released: 2026-04-03\n\n");

        sb.append("Highlights:\n");
        sb.append("  • Java port of Claude Code\n");
        sb.append("  • Full tool support\n");
        sb.append("  • MCP server integration\n");

        sb.append("\nFull release notes: https://anthropic.com/claude-code/releases/\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult listAllReleases() {
        StringBuilder sb = new StringBuilder();
        sb.append("All Releases\n");
        sb.append("============\n\n");

        sb.append("* 1.0.0-java - 2026-04-03\n");

        sb.append("\nUsage: release-notes <version>\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult showVersionNotes(String version) {
        StringBuilder sb = new StringBuilder();
        sb.append("Release ").append(version).append("\n");
        sb.append("================\n\n");

        sb.append("Released: 2026-04-03\n\n");

        sb.append("Changes:\n");
        sb.append("  - Initial Java port\n");

        return CommandResult.success(sb.toString());
    }
}