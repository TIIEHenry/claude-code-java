/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/add-dir
 */
package com.anthropic.claudecode.commands;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Add-dir command - Add directories to the working context.
 */
public final class AddDirCommand implements Command {
    @Override
    public String name() {
        return "add-dir";
    }

    @Override
    public String description() {
        return "Add directories to the working context";
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
            return CompletableFuture.completedFuture(
                CommandResult.failure("Usage: add-dir <directory> [--recursive]")
            );
        }

        String[] parts = args.trim().split("\\s+");
        boolean recursive = false;
        List<String> dirs = new ArrayList<>();

        for (String arg : parts) {
            if ("--recursive".equals(arg) || "-r".equals(arg)) {
                recursive = true;
            } else {
                dirs.add(arg);
            }
        }

        if (dirs.isEmpty()) {
            return CompletableFuture.completedFuture(
                CommandResult.failure("Please specify a directory to add")
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Adding Directories\n");
        sb.append("==================\n\n");

        int added = 0;
        int skipped = 0;

        for (String dirPath : dirs) {
            Path path = Paths.get(dirPath);

            // Resolve relative to current working directory
            if (!path.isAbsolute()) {
                path = Paths.get(context.cwd()).resolve(path);
            }

            // Normalize and check existence
            path = path.normalize();

            if (!Files.exists(path)) {
                sb.append("✗ ").append(dirPath).append(" (not found)\n");
                skipped++;
                continue;
            }

            if (!Files.isDirectory(path)) {
                sb.append("✗ ").append(dirPath).append(" (not a directory)\n");
                skipped++;
                continue;
            }

            // Add to context
            context.addContextFile(path.toString());
            sb.append("✓ ").append(dirPath);
            if (recursive) {
                sb.append(" (recursive)");
            }
            sb.append("\n");
            added++;
        }

        sb.append("\n");
        sb.append("Added: ").append(added).append(", Skipped: ").append(skipped).append("\n");

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }
}