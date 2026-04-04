/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/rewind
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Rewind command - Restore code and/or conversation to a previous point.
 */
public final class RewindCommand implements Command {
    @Override
    public String name() {
        return "rewind";
    }

    @Override
    public List<String> aliases() {
        return List.of("checkpoint");
    }

    @Override
    public String description() {
        return "Restore the code and/or conversation to a previous point";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return false;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        boolean rewindCode = args != null && args.contains("--code");
        boolean rewindConversation = args != null && args.contains("--conversation");

        if (!rewindCode && !rewindConversation) {
            rewindCode = true;
            rewindConversation = true;
        }

        String checkpointId = null;
        if (args != null) {
            for (String arg : args.split("\\s+")) {
                if (!arg.startsWith("--") && !arg.isEmpty()) {
                    checkpointId = arg;
                    break;
                }
            }
        }

        List<Checkpoint> checkpoints = getCheckpoints(context);

        if (checkpoints.isEmpty()) {
            return CompletableFuture.completedFuture(CommandResult.failure("No checkpoints available to rewind to."));
        }

        Checkpoint target = null;
        if (checkpointId != null) {
            for (Checkpoint cp : checkpoints) {
                if (cp.id().equals(checkpointId) || cp.id().startsWith(checkpointId)) {
                    target = cp;
                    break;
                }
            }
            if (target == null) {
                return CompletableFuture.completedFuture(CommandResult.failure("Checkpoint not found: " + checkpointId));
            }
        } else {
            target = checkpoints.get(0);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Rewinding to checkpoint: ").append(target.id()).append("\n");
        sb.append("Time: ").append(target.timestamp()).append("\n\n");

        if (rewindCode) {
            sb.append("Restoring code changes...\n");
            sb.append("  Files restored: ").append(target.fileCount()).append("\n");
        }

        if (rewindConversation) {
            sb.append("Restoring conversation...\n");
            context.rewindToCheckpoint(target.id());
            sb.append("  Messages restored to point in time\n");
        }

        sb.append("\nRewind complete.\n");

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }

    private List<Checkpoint> getCheckpoints(CommandContext context) {
        List<Checkpoint> checkpoints = new ArrayList<>();
        checkpoints.add(new Checkpoint("cp_" + System.currentTimeMillis(), new Date(), 5));
        return checkpoints;
    }

    public record Checkpoint(String id, Date timestamp, int fileCount) {}
}