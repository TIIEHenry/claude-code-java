/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code queue processor utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Queue processor for processing queued commands.
 */
public final class QueueProcessor {
    private QueueProcessor() {}

    /**
     * Queued command record.
     */
    public record QueuedCommand(
            String value,
            String mode,
            String agentId
    ) {
        /**
         * Check if this is a slash command.
         */
        public boolean isSlashCommand() {
            if (value == null) return false;
            return value.trim().startsWith("/");
        }
    }

    private static final List<QueuedCommand> queue = new CopyOnWriteArrayList<>();

    /**
     * Add a command to the queue.
     */
    public static void enqueue(QueuedCommand command) {
        queue.add(command);
    }

    /**
     * Peek at the next command in the queue.
     */
    public static QueuedCommand peek(Predicate<QueuedCommand> filter) {
        for (QueuedCommand cmd : queue) {
            if (filter.test(cmd)) {
                return cmd;
            }
        }
        return null;
    }

    /**
     * Dequeue the next command.
     */
    public static QueuedCommand dequeue(Predicate<QueuedCommand> filter) {
        for (int i = 0; i < queue.size(); i++) {
            QueuedCommand cmd = queue.get(i);
            if (filter.test(cmd)) {
                queue.remove(i);
                return cmd;
            }
        }
        return null;
    }

    /**
     * Dequeue all matching commands.
     */
    public static List<QueuedCommand> dequeueAllMatching(Predicate<QueuedCommand> filter) {
        List<QueuedCommand> result = new ArrayList<>();
        Iterator<QueuedCommand> iter = queue.iterator();
        while (iter.hasNext()) {
            QueuedCommand cmd = iter.next();
            if (filter.test(cmd)) {
                result.add(cmd);
                iter.remove();
            }
        }
        return result;
    }

    /**
     * Check if queue has commands.
     */
    public static boolean hasCommandsInQueue() {
        return !queue.isEmpty();
    }

    /**
     * Process queue result.
     */
    public record ProcessQueueResult(boolean processed) {}

    /**
     * Process commands from the queue.
     */
    public static CompletableFuture<ProcessQueueResult> processQueueIfReady(
            Function<List<QueuedCommand>, CompletableFuture<Void>> executeInput) {

        // Check for main thread commands
        Predicate<QueuedCommand> isMainThread = cmd -> cmd.agentId() == null;

        QueuedCommand next = peek(isMainThread);
        if (next == null) {
            return CompletableFuture.completedFuture(new ProcessQueueResult(false));
        }

        // Slash commands and bash-mode commands are processed individually
        if (next.isSlashCommand() || "bash".equals(next.mode())) {
            QueuedCommand cmd = dequeue(isMainThread);
            if (cmd != null) {
                return executeInput.apply(List.of(cmd))
                        .thenApply(v -> new ProcessQueueResult(true));
            }
            return CompletableFuture.completedFuture(new ProcessQueueResult(false));
        }

        // Drain all non-slash-command items with the same mode
        String targetMode = next.mode();
        List<QueuedCommand> commands = dequeueAllMatching(c ->
                isMainThread.test(c) && !c.isSlashCommand() && targetMode.equals(c.mode()));

        if (commands.isEmpty()) {
            return CompletableFuture.completedFuture(new ProcessQueueResult(false));
        }

        return executeInput.apply(commands)
                .thenApply(v -> new ProcessQueueResult(true));
    }

    /**
     * Clear the queue.
     */
    public static void clearQueue() {
        queue.clear();
    }

    /**
     * Get queue size.
     */
    public static int queueSize() {
        return queue.size();
    }
}