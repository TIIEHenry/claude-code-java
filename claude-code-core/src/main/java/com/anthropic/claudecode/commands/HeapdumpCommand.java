/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/heapdump
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Heapdump command - Dump the heap for debugging (hidden command).
 */
public final class HeapdumpCommand implements Command {
    @Override
    public String name() {
        return "heapdump";
    }

    @Override
    public String description() {
        return "Dump the memory heap for debugging";
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
        if (args == null || args.isEmpty()) {
            return CompletableFuture.completedFuture(dumpHeap(context));
        }

        String action = args.trim().toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "status" -> showHeapStatus(context);
            case "force" -> forceDumpHeap(context);
            default -> dumpHeap(context);
        });
    }

    private CommandResult dumpHeap(CommandContext context) {
        String outputPath = context.getHeapDumpPath();

        StringBuilder sb = new StringBuilder();
        sb.append("Heap Dump\n");
        sb.append("=========\n\n");

        try {
            // Request heap dump
            CommandContext.HeapDumpResult result = context.dumpHeap(outputPath);

            sb.append("Heap dump created.\n\n");
            sb.append("Path: ").append(result.path()).append("\n");
            sb.append("Size: ").append(formatSize(result.size())).append("\n");
            sb.append("Format: ").append(result.format()).append("\n");
            sb.append("Timestamp: ").append(result.timestamp()).append("\n");

            sb.append("\nTo analyze:\n");
            sb.append("  - Use VisualVM or JProfiler to open the dump\n");
            sb.append("  - Use 'jhat' command-line tool\n");

        } catch (Exception e) {
            sb.append("Failed to create heap dump: ").append(e.getMessage()).append("\n");
            return CommandResult.failure(sb.toString());
        }

        return CommandResult.success(sb.toString());
    }

    private CommandResult showHeapStatus(CommandContext context) {
        CommandContext.HeapInfo heapInfo = context.getHeapInfo();

        StringBuilder sb = new StringBuilder();
        sb.append("Heap Status\n");
        sb.append("===========\n\n");

        sb.append("Memory Usage:\n");
        sb.append("  Used: ").append(formatSize(heapInfo.used())).append("\n");
        sb.append("  Free: ").append(formatSize(heapInfo.free())).append("\n");
        sb.append("  Total: ").append(formatSize(heapInfo.total())).append("\n");
        sb.append("  Max: ").append(formatSize(heapInfo.max())).append("\n");
        sb.append("  Usage: ").append(heapInfo.usagePercent()).append("%\n");

        sb.append("\nGC Stats:\n");
        sb.append("  GC Count: ").append(heapInfo.gcCount()).append("\n");
        sb.append("  GC Time: ").append(heapInfo.gcTimeMs()).append(" ms\n");

        sb.append("\nObjects:\n");
        sb.append("  Total Objects: ").append(heapInfo.objectCount()).append("\n");
        sb.append("  Classes Loaded: ").append(heapInfo.classCount()).append("\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult forceDumpHeap(CommandContext context) {
        context.triggerGc();

        StringBuilder sb = new StringBuilder();
        sb.append("Triggered full GC before heap dump.\n\n");

        CommandResult dumpResult = dumpHeap(context);
        sb.append(dumpResult.getText().orElse(""));

        return CommandResult.success(sb.toString());
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}