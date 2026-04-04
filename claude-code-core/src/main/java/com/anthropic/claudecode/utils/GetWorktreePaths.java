/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code worktree paths utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Returns the paths of all worktrees for the current git repository.
 * If git is not available, not in a git repo, or only has one worktree,
 * returns an empty array.
 */
public final class GetWorktreePaths {
    private GetWorktreePaths() {}

    /**
     * Get all worktree paths for a git repository.
     */
    public static CompletableFuture<List<String>> getWorktreePaths(Path cwd) {
        long startTime = System.currentTimeMillis();

        return ExecFileNoThrow.execFileNoThrowWithCwd(
                Git.gitExe(),
                List.of("worktree", "list", "--porcelain").toArray(new String[0]),
                cwd.toString()
        ).thenApply(result -> {
            long durationMs = System.currentTimeMillis() - startTime;

            if (result.code() != 0) {
                Analytics.logEvent("tengu_worktree_detection", Map.of(
                        "duration_ms", durationMs,
                        "worktree_count", 0,
                        "success", false
                ));
                return List.of();
            }

            // Parse porcelain output - lines starting with "worktree " contain paths
            List<String> worktreePaths = Arrays.stream(result.stdout().split("\n"))
                    .filter(line -> line.startsWith("worktree "))
                    .map(line -> line.substring("worktree ".length()))
                    .map(line -> line.trim())
                    .toList();

            Analytics.logEvent("tengu_worktree_detection", Map.of(
                    "duration_ms", durationMs,
                    "worktree_count", worktreePaths.size(),
                    "success", true
            ));

            // Sort worktrees: current worktree first, then alphabetically
            String cwdStr = cwd.toString();
            String fileSep = FileSystems.getDefault().getSeparator();

            Optional<String> currentWorktree = worktreePaths.stream()
                    .filter(path -> cwdStr.equals(path) || cwdStr.startsWith(path + fileSep))
                    .findFirst();

            List<String> otherWorktrees = worktreePaths.stream()
                    .filter(path -> currentWorktree.isEmpty() || !path.equals(currentWorktree.get()))
                    .sorted(String::compareTo)
                    .toList();

            if (currentWorktree.isPresent()) {
                List<String> resultPaths = new ArrayList<>();
                resultPaths.add(currentWorktree.get());
                resultPaths.addAll(otherWorktrees);
                return resultPaths;
            }
            return otherWorktrees;
        });
    }
}