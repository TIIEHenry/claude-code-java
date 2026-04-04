/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/diff
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Diff command - Show differences in files or sessions.
 */
public final class DiffCommand implements Command {
    @Override
    public String name() {
        return "diff";
    }

    @Override
    public String description() {
        return "Show differences in files or sessions";
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
            return CompletableFuture.completedFuture(showSessionDiff(context));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "session" -> showSessionDiff(context);
            case "last", "last-commit" -> showLastCommitDiff(context);
            case "unstaged" -> showUnstagedDiff(context);
            case "staged" -> showStagedDiff(context);
            default -> {
                if (parts.length >= 2) {
                    yield showFileDiff(context, parts[0], parts[1]);
                }
                yield showFileDiff(context, parts[0], null);
            }
        });
    }

    private CommandResult showSessionDiff(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Changes\n");
        sb.append("===============\n\n");

        List<FileChange> changes = getSessionChanges(context);

        if (changes.isEmpty()) {
            sb.append("No file changes in this session.\n");
            return CommandResult.success(sb.toString());
        }

        int added = 0, modified = 0, deleted = 0;

        for (FileChange change : changes) {
            String symbol = switch (change.type()) {
                case "added" -> "+";
                case "modified" -> "~";
                case "deleted" -> "-";
                default -> "?";
            };

            sb.append(symbol).append(" ").append(change.path());

            if (change.linesAdded() > 0 || change.linesRemoved() > 0) {
                sb.append(" (+").append(change.linesAdded());
                sb.append(" -").append(change.linesRemoved()).append(")");
            }
            sb.append("\n");

            switch (change.type()) {
                case "added" -> added++;
                case "modified" -> modified++;
                case "deleted" -> deleted++;
            }
        }

        sb.append("\nSummary: ");
        if (added > 0) sb.append(added).append(" added, ");
        if (modified > 0) sb.append(modified).append(" modified, ");
        if (deleted > 0) sb.append(deleted).append(" deleted, ");
        sb.append(changes.size()).append(" total\n");

        return CommandResult.success(sb.toString());
    }

    private List<FileChange> getSessionChanges(CommandContext context) {
        // Track actual file changes from session
        List<FileChange> changes = new ArrayList<>();

        try {
            // Read session file changes from session state file
            String home = System.getProperty("user.home");
            String cwd = System.getProperty("user.dir");
            String slug = cwd.replaceAll("[^a-zA-Z0-9]", "-");
            java.nio.file.Path sessionPath = java.nio.file.Paths.get(home, ".claude", "projects", slug, "file-changes.json");

            if (java.nio.file.Files.exists(sessionPath)) {
                String content = java.nio.file.Files.readString(sessionPath);

                // Parse file changes array
                int arrStart = content.indexOf("[");
                int arrEnd = content.lastIndexOf("]");
                if (arrStart >= 0 && arrEnd > arrStart) {
                    String arr = content.substring(arrStart + 1, arrEnd);

                    int i = 0;
                    while (i < arr.length()) {
                        int objStart = arr.indexOf("{", i);
                        if (objStart < 0) break;

                        int depth = 1;
                        int objEnd = objStart + 1;
                        while (objEnd < arr.length() && depth > 0) {
                            char c = arr.charAt(objEnd);
                            if (c == '{') depth++;
                            else if (c == '}') depth--;
                            objEnd++;
                        }

                        String obj = arr.substring(objStart, objEnd);

                        String path = extractJsonValueString(obj, "path");
                        String type = extractJsonValueString(obj, "type");

                        if (path != null && type != null) {
                            changes.add(new FileChange(path, type, 0, 0));
                        }

                        i = objEnd;
                    }
                }
            }

            // Also check git status for uncommitted changes
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
            pb.directory(new java.io.File(cwd));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            if (process.exitValue() == 0) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.length() >= 3) {
                            String statusCode = line.substring(0, 2).trim();
                            String filePath = line.substring(3);

                            // Check if already in changes
                            boolean exists = changes.stream()
                                .anyMatch(c -> c.path().equals(filePath));

                            if (!exists) {
                                String type = switch (statusCode) {
                                    case "A", "??", "!!" -> "added";
                                    case "M", "MM", "AM" -> "modified";
                                    case "D" -> "deleted";
                                    default -> "modified";
                                };
                                changes.add(new FileChange(filePath, type, 0, 0));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list on error
        }

        return changes;
    }

    private String extractJsonValueString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int valStart = json.indexOf("\"", idx + key.length() + 3) + 1;
        if (valStart < 1) return null;
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    private CommandResult showLastCommitDiff(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Last Commit Diff\n");
        sb.append("================\n\n");

        sb.append("Use git diff HEAD~1 HEAD to see last commit changes.\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showUnstagedDiff(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unstaged Changes\n");
        sb.append("================\n\n");

        sb.append("Use git diff to see unstaged changes.\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showStagedDiff(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Staged Changes\n");
        sb.append("==============\n\n");

        sb.append("Use git diff --cached to see staged changes.\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult showFileDiff(CommandContext context, String file1, String file2) {
        StringBuilder sb = new StringBuilder();
        sb.append("File Diff\n");
        sb.append("=========\n\n");

        if (file2 == null) {
            sb.append("Comparing ").append(file1).append(" with HEAD\n\n");
            sb.append("Use git diff HEAD -- ").append(file1).append(" to see changes.\n");
        } else {
            sb.append("Comparing ").append(file1).append(" with ").append(file2).append("\n\n");
            sb.append("Use git diff --no-index ").append(file1).append(" ").append(file2).append("\n");
        }

        return CommandResult.success(sb.toString());
    }

    /**
     * File change record.
     */
    public record FileChange(
        String path,
        String type,
        int linesAdded,
        int linesRemoved
    ) {}
}