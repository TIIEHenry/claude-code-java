/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/files
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Files command - List all files currently in context.
 */
public final class FilesCommand implements Command {
    @Override
    public String name() {
        return "files";
    }

    @Override
    public String description() {
        return "List all files currently in context";
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String format = (args == null || args.trim().isEmpty()) ? "paths" : args.trim().toLowerCase();

            return switch (format) {
                case "detail", "full" -> showDetailedFiles(context);
                case "paths", "list" -> showFilePaths(context);
                case "clear" -> clearFiles(context);
                default -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown format: ").append(format).append("\n\n");
                    sb.append("Usage: files [detail|paths|clear]\n");
                    yield CommandResult.failure(sb.toString());
                }
            };
        });
    }

    private CommandResult showFilePaths(CommandContext context) {
        List<String> files = context.getContextFiles();

        StringBuilder sb = new StringBuilder();
        sb.append("Files in Context\n");
        sb.append("================\n\n");

        if (files.isEmpty()) {
            sb.append("No files in context.\n");
            sb.append("Use 'Read' or 'Glob' to add files to context.\n");
        } else {
            sb.append("Total: ").append(files.size()).append(" files\n\n");
            for (String file : files) {
                sb.append("  ").append(file).append("\n");
            }
        }

        return CommandResult.success(sb.toString());
    }

    private CommandResult showDetailedFiles(CommandContext context) {
        List<FileContextInfo> files = context.getContextFileDetails();

        StringBuilder sb = new StringBuilder();
        sb.append("Files in Context (Detailed)\n");
        sb.append("===========================\n\n");

        if (files.isEmpty()) {
            sb.append("No files in context.\n");
        } else {
            sb.append("Total: ").append(files.size()).append(" files\n\n");

            for (FileContextInfo info : files) {
                sb.append(info.path()).append("\n");
                sb.append("  Size: ").append(formatSize(info.size())).append("\n");
                sb.append("  Lines: ").append(info.lines()).append("\n");
                sb.append("  Tokens: ").append(info.tokens()).append("\n");
                sb.append("  Added: ").append(info.addedAt()).append("\n");
                sb.append("\n");
            }

            long totalTokens = files.stream().mapToLong(FileContextInfo::tokens).sum();
            sb.append("Total tokens in context: ").append(totalTokens).append("\n");
        }

        return CommandResult.success(sb.toString());
    }

    private CommandResult clearFiles(CommandContext context) {
        int count = context.getContextFiles().size();
        context.clearContextFiles();

        StringBuilder sb = new StringBuilder();
        sb.append("Cleared ").append(count).append(" files from context.\n");
        return CommandResult.success(sb.toString());
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        } else {
            return (bytes / (1024 * 1024)) + " MB";
        }
    }

    /**
     * File context information record.
     */
    public record FileContextInfo(
        String path,
        long size,
        int lines,
        long tokens,
        long addedAt
    ) {}
}