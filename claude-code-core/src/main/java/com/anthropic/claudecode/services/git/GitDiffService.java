/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/git/diff
 */
package com.anthropic.claudecode.services.git;

import java.util.*;
import java.nio.file.*;

/**
 * Git diff service - Git diff operations.
 */
public final class GitDiffService {
    private final GitService gitService;

    /**
     * Create git diff service.
     */
    public GitDiffService(GitService gitService) {
        this.gitService = gitService;
    }

    /**
     * Diff type enum.
     */
    public enum DiffType {
        WORKING_DIRECTORY,
        STAGED,
        COMMIT_TO_COMMIT,
        BRANCH_TO_BRANCH
    }

    /**
     * Get diff for file.
     */
    public FileDiff getFileDiff(Path filePath, DiffType type) {
        try {
            List<String> command = new ArrayList<>();
            command.add("git");
            command.add("diff");

            switch (type) {
                case STAGED -> command.add("--staged");
                case COMMIT_TO_COMMIT -> {
                    // Would need commit hashes
                    command.add("HEAD");
                }
                case BRANCH_TO_BRANCH -> {
                    // Would need branch names
                    command.add("HEAD");
                }
                default -> {} // WORKING_DIRECTORY - no extra args
            }

            command.add("--");
            command.add(filePath.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(gitService.getRepositoryPath().toFile());
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            List<HunkDiff> hunks = parseDiff(output);
            int additions = 0, deletions = 0;

            for (HunkDiff hunk : hunks) {
                additions += hunk.getAdditions();
                deletions += hunk.getDeletions();
            }

            return new FileDiff(
                filePath.toString(),
                filePath.toString(),
                hunks,
                additions,
                deletions
            );
        } catch (Exception e) {
            return new FileDiff(
                filePath.toString(),
                filePath.toString(),
                Collections.emptyList(),
                0, 0
            );
        }
    }

    /**
     * Get diff summary.
     */
    public DiffSummary getDiffSummary(DiffType type) {
        try {
            List<String> command = new ArrayList<>();
            command.add("git");
            command.add("diff");
            command.add("--stat");

            switch (type) {
                case STAGED -> command.add("--staged");
                case COMMIT_TO_COMMIT -> command.add("HEAD");
                case BRANCH_TO_BRANCH -> command.add("HEAD");
                default -> {}
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(gitService.getRepositoryPath().toFile());
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            return parseDiffStat(output);
        } catch (Exception e) {
            return new DiffSummary(
                Collections.emptyList(),
                0, 0, 0
            );
        }
    }

    /**
     * Parse diff --stat output.
     */
    private DiffSummary parseDiffStat(String output) {
        List<FileDiffSummary> files = new ArrayList<>();
        int totalAdditions = 0, totalDeletions = 0;

        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.isEmpty() || line.contains("files changed")) continue;

            // Format: " path/to/file | 10 ++++----"
            int pipeIdx = line.indexOf('|');
            if (pipeIdx < 0) continue;

            String path = line.substring(0, pipeIdx).trim();
            String stats = line.substring(pipeIdx + 1).trim();

            // Count + and - characters
            int additions = 0, deletions = 0;
            boolean isBinary = stats.contains("Bin");

            if (!isBinary) {
                for (char c : stats.toCharArray()) {
                    if (c == '+') additions++;
                    else if (c == '-') deletions++;
                }
            }

            files.add(new FileDiffSummary(path, additions, deletions, isBinary));
            totalAdditions += additions;
            totalDeletions += deletions;
        }

        return new DiffSummary(files, totalAdditions, totalDeletions, files.size());
    }

    /**
     * Parse diff output.
     */
    public List<HunkDiff> parseDiff(String diffOutput) {
        List<HunkDiff> hunks = new ArrayList<>();
        String[] lines = diffOutput.split("\n");

        int oldStart = 0, oldCount = 0, newStart = 0, newCount = 0;
        List<DiffLine> currentLines = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("@@")) {
                if (!currentLines.isEmpty()) {
                    hunks.add(new HunkDiff(oldStart, oldCount, newStart, newCount, currentLines));
                    currentLines = new ArrayList<>();
                }

                // Parse hunk header
                String[] parts = line.split("\\s+");
                for (String part : parts) {
                    if (part.startsWith("-")) {
                        String[] range = part.substring(1).split(",");
                        oldStart = Integer.parseInt(range[0]);
                        oldCount = range.length > 1 ? Integer.parseInt(range[1]) : 1;
                    } else if (part.startsWith("+")) {
                        String[] range = part.substring(1).split(",");
                        newStart = Integer.parseInt(range[0]);
                        newCount = range.length > 1 ? Integer.parseInt(range[1]) : 1;
                    }
                }
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                currentLines.add(new DiffLine(DiffLineType.ADD, line.substring(1)));
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                currentLines.add(new DiffLine(DiffLineType.REMOVE, line.substring(1)));
            } else if (!line.startsWith("\\") && !line.startsWith("diff ") && !line.startsWith("index ")) {
                if (!line.isEmpty()) {
                    currentLines.add(new DiffLine(DiffLineType.CONTEXT, line.startsWith(" ") ? line.substring(1) : line));
                }
            }
        }

        if (!currentLines.isEmpty()) {
            hunks.add(new HunkDiff(oldStart, oldCount, newStart, newCount, currentLines));
        }

        return hunks;
    }

    /**
     * File diff record.
     */
    public record FileDiff(
        String oldPath,
        String newPath,
        List<HunkDiff> hunks,
        int additions,
        int deletions
    ) {
        public boolean isNewFile() { return oldPath.equals("/dev/null"); }
        public boolean isDeletedFile() { return newPath.equals("/dev/null"); }
        public boolean isModified() { return !isNewFile() && !isDeletedFile(); }
    }

    /**
     * Hunk diff record.
     */
    public record HunkDiff(
        int oldStart,
        int oldCount,
        int newStart,
        int newCount,
        List<DiffLine> lines
    ) {
        public int getAdditions() {
            return (int) lines.stream().filter(l -> l.type() == DiffLineType.ADD).count();
        }

        public int getDeletions() {
            return (int) lines.stream().filter(l -> l.type() == DiffLineType.REMOVE).count();
        }

        public String formatHeader() {
            return String.format("@@ -%d,%d +%d,%d @@", oldStart, oldCount, newStart, newCount);
        }
    }

    /**
     * Diff line type enum.
     */
    public enum DiffLineType {
        ADD("+"),
        REMOVE("-"),
        CONTEXT(" ");

        private final String prefix;

        DiffLineType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() { return prefix; }
    }

    /**
     * Diff line record.
     */
    public record DiffLine(DiffLineType type, String content) {
        public String format() {
            return type.getPrefix() + content;
        }
    }

    /**
     * Diff summary record.
     */
    public record DiffSummary(
        List<FileDiffSummary> files,
        int totalAdditions,
        int totalDeletions,
        int filesChanged
    ) {
        public String format() {
            return String.format("%d files changed, %d insertions(+), %d deletions(-)",
                filesChanged, totalAdditions, totalDeletions);
        }
    }

    /**
     * File diff summary record.
     */
    public record FileDiffSummary(
        String path,
        int additions,
        int deletions,
        boolean isBinary
    ) {
        public String format() {
            return String.format("%s | %d +++, %d ---", path, additions, deletions);
        }
    }
}