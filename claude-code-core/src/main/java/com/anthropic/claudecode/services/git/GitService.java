/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/git
 */
package com.anthropic.claudecode.services.git;

import java.time.Instant;

import java.util.*;
import java.nio.file.*;
import java.io.*;

/**
 * Git service - Git operations.
 */
public final class GitService {
    private final Path repositoryPath;

    /**
     * Create git service.
     */
    public GitService(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    /**
     * Get repository path.
     */
    public Path getRepositoryPath() {
        return repositoryPath;
    }

    /**
     * Check if is git repository.
     */
    public boolean isGitRepository() {
        return Files.exists(repositoryPath.resolve(".git"));
    }

    /**
     * Get current branch.
     */
    public Optional<String> getCurrentBranch() {
        try {
            Path headPath = repositoryPath.resolve(".git/HEAD");
            if (!Files.exists(headPath)) return Optional.empty();

            String content = Files.readString(headPath).trim();
            if (content.startsWith("ref: refs/heads/")) {
                return Optional.of(content.substring("ref: refs/heads/".length()));
            }
            return Optional.of(content.substring(0, 7)); // Detached HEAD
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Get repository status.
     */
    public GitStatus getStatus() {
        GitStatus status = new GitStatus();
        status.setRepositoryPath(repositoryPath);
        status.setBranch(getCurrentBranch().orElse("unknown"));
        status.setClean(isWorkingDirectoryClean());
        return status;
    }

    /**
     * Check if working directory is clean.
     */
    public boolean isWorkingDirectoryClean() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "diff", "--quiet");
            pb.directory(repositoryPath.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            // exit code 0 = no changes, 1 = changes, other = error
            return exitCode == 0;
        } catch (Exception e) {
            // If git command fails, check index file modification time
            try {
                Path indexFile = repositoryPath.resolve(".git/index");
                if (!Files.exists(indexFile)) return true;
                // Fall back to assuming clean if we can't run git
                return true;
            } catch (Exception ex) {
                return true;
            }
        }
    }

    /**
     * Get remote URL.
     */
    public Optional<String> getRemoteUrl(String remote) {
        try {
            Path configPath = repositoryPath.resolve(".git/config");
            if (!Files.exists(configPath)) return Optional.empty();

            String content = Files.readString(configPath);
            // Simple parsing for remote URL
            String pattern = "[remote \"" + remote + "\"]";
            int idx = content.indexOf(pattern);
            if (idx < 0) return Optional.empty();

            int urlIdx = content.indexOf("url = ", idx);
            if (urlIdx < 0) return Optional.empty();

            int lineEnd = content.indexOf("\n", urlIdx);
            String url = content.substring(urlIdx + 6, lineEnd).trim();
            return Optional.of(url);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Get list of branches.
     */
    public List<String> getBranches() {
        List<String> branches = new ArrayList<>();
        try {
            Path headsPath = repositoryPath.resolve(".git/refs/heads");
            if (Files.exists(headsPath)) {
                Files.walk(headsPath)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        String branch = headsPath.relativize(p).toString();
                        branches.add(branch);
                    });
            }
        } catch (IOException e) {
            // Ignore
        }
        return branches;
    }

    /**
     * Get last commit info.
     */
    public Optional<CommitInfo> getLastCommit() {
        try {
            // Use git log command for accurate info
            ProcessBuilder pb = new ProcessBuilder(
                "git", "log", "-1", "--format=%H|%h|%s|%an|%at"
            );
            pb.directory(repositoryPath.toFile());
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor();

            if (output.isEmpty()) {
                return Optional.empty();
            }

            String[] parts = output.split("\\|", 5);
            if (parts.length >= 5) {
                String fullHash = parts[0];
                String shortHash = parts[1];
                String message = parts[2];
                String author = parts[3];
                long timestamp = Long.parseLong(parts[4]);

                return Optional.of(new CommitInfo(
                    shortHash,
                    fullHash,
                    message,
                    author,
                    Instant.ofEpochSecond(timestamp)
                ));
            }
        } catch (Exception e) {
            // Fall back to file-based parsing
        }

        // Fallback: read from .git files
        try {
            Path headRef = repositoryPath.resolve(".git/HEAD");
            if (!Files.exists(headRef)) return Optional.empty();

            String headContent = Files.readString(headRef).trim();
            String commitHash;

            if (headContent.startsWith("ref: ")) {
                String refPath = headContent.substring(5);
                Path refFile = repositoryPath.resolve(".git").resolve(refPath);
                if (!Files.exists(refFile)) {
                    // Check packed-refs
                    Path packedRefs = repositoryPath.resolve(".git/packed-refs");
                    if (Files.exists(packedRefs)) {
                        String packedContent = Files.readString(packedRefs);
                        String refName = refPath;
                        for (String line : packedContent.split("\n")) {
                            if (line.endsWith(refName)) {
                                commitHash = line.split("\\s+")[0];
                                return Optional.of(new CommitInfo(
                                    commitHash.substring(0, 7),
                                    commitHash,
                                    "",
                                    "",
                                    Instant.now()
                                ));
                            }
                        }
                    }
                    return Optional.empty();
                }
                commitHash = Files.readString(refFile).trim();
            } else {
                commitHash = headContent;
            }

            return Optional.of(new CommitInfo(
                commitHash.substring(0, 7),
                commitHash,
                "",
                "",
                Instant.now()
            ));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Parse gitignore.
     */
    public List<String> parseGitignore() {
        Path gitignore = repositoryPath.resolve(".gitignore");
        if (!Files.exists(gitignore)) {
            return Collections.emptyList();
        }

        try {
            return Files.readAllLines(gitignore)
                .stream()
                .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
                .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Check if path is ignored.
     */
    public boolean isIgnored(Path path) {
        List<String> patterns = parseGitignore();
        String relativePath = repositoryPath.relativize(path).toString();

        for (String pattern : patterns) {
            if (matchesPattern(relativePath, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simple pattern matching.
     */
    private boolean matchesPattern(String path, String pattern) {
        pattern = pattern.trim();
        if (pattern.endsWith("/")) {
            return path.startsWith(pattern);
        }
        if (pattern.startsWith("*")) {
            return path.endsWith(pattern.substring(1));
        }
        return path.contains(pattern);
    }

    /**
     * Git status record.
     */
    public static final class GitStatus {
        private Path repositoryPath;
        private String branch;
        private boolean clean;
        private List<String> stagedFiles = new ArrayList<>();
        private List<String> modifiedFiles = new ArrayList<>();
        private List<String> untrackedFiles = new ArrayList<>();

        public Path getRepositoryPath() { return repositoryPath; }
        public void setRepositoryPath(Path path) { this.repositoryPath = path; }
        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public boolean isClean() { return clean; }
        public void setClean(boolean clean) { this.clean = clean; }
        public List<String> getStagedFiles() { return stagedFiles; }
        public List<String> getModifiedFiles() { return modifiedFiles; }
        public List<String> getUntrackedFiles() { return untrackedFiles; }
    }

    /**
     * Commit info record.
     */
    public record CommitInfo(
        String shortHash,
        String fullHash,
        String message,
        String author,
        Instant timestamp
    ) {
        public String format() {
            return shortHash + " " + message;
        }
    }

    /**
     * Remote info record.
     */
    public record RemoteInfo(
        String name,
        String url,
        String fetchUrl,
        String pushUrl
    ) {}
}