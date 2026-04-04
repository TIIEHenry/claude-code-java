/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/export
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Export command - Export conversation to file or clipboard.
 */
public final class ExportCommand implements Command {
    @Override
    public String name() {
        return "export";
    }

    @Override
    public String description() {
        return "Export the current conversation to a file or clipboard";
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
        List<String> argList = args == null || args.isEmpty() ? List.of() : List.of(args.split("\\s+"));

        // Parse arguments
        String filename = null;
        String format = "markdown"; // default

        for (String arg : argList) {
            if (arg.equals("--format") || arg.equals("-f")) {
                int idx = argList.indexOf(arg);
                if (idx + 1 < argList.size()) {
                    format = argList.get(idx + 1);
                }
            } else if (!arg.startsWith("-")) {
                filename = arg;
            }
        }

        // Get conversation content (placeholder)
        String content = "No conversation to export.";

        if (content == null || content.isEmpty()) {
            return CompletableFuture.completedFuture(CommandResult.failure("No conversation to export."));
        }

        // If no filename specified, copy to clipboard
        if (filename == null) {
            return CompletableFuture.completedFuture(copyToClipboard(content));
        }

        // Write to file
        return CompletableFuture.completedFuture(writeToFile(filename, content, format));
    }

    private CommandResult copyToClipboard(String content) {
        try {
            // Platform-specific clipboard copy
            String osName = System.getProperty("os.name").toLowerCase();

            ProcessBuilder pb;
            if (osName.contains("mac")) {
                pb = new ProcessBuilder("pbcopy");
            } else if (osName.contains("win")) {
                pb = new ProcessBuilder("clip");
            } else {
                // Linux
                pb = new ProcessBuilder("xclip", "-selection", "clipboard");
            }

            Process p = pb.start();
            try (OutputStream os = p.getOutputStream()) {
                os.write(content.getBytes());
            }

            p.waitFor(5, TimeUnit.SECONDS);

            return CommandResult.success("Conversation copied to clipboard.");
        } catch (Exception e) {
            return CommandResult.failure("Failed to copy to clipboard: " + e.getMessage());
        }
    }

    private CommandResult writeToFile(String filename, String content, String format) {
        try {
            // Add extension if not present
            if (!filename.contains(".")) {
                filename = filename + "." + getExtension(format);
            }

            Path path = Paths.get(filename);

            // Check if file exists
            if (Files.exists(path)) {
                // In real implementation, would prompt user
                return CommandResult.failure("File already exists: " + filename);
            }

            Files.writeString(path, content);

            return CommandResult.success(
                "Conversation exported to: " + path.toAbsolutePath() +
                "\nSize: " + content.length() + " characters"
            );
        } catch (Exception e) {
            return CommandResult.failure("Failed to export: " + e.getMessage());
        }
    }

    private String getExtension(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> "json";
            case "markdown", "md" -> "md";
            default -> "txt";
        };
    }
}