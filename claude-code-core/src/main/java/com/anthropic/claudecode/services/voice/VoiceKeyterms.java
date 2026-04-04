/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/voiceKeyterms.ts
 */
package com.anthropic.claudecode.services.voice;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Voice keyterms for improving STT accuracy.
 *
 * Provides domain-specific vocabulary hints so the STT engine correctly
 * recognises coding terminology, project names, and branch names.
 */
public final class VoiceKeyterms {
    private VoiceKeyterms() {}

    // Global keyterms
    private static final List<String> GLOBAL_KEYTERMS = List.of(
        "MCP",
        "symlink",
        "grep",
        "regex",
        "localhost",
        "codebase",
        "TypeScript",
        "JSON",
        "OAuth",
        "webhook",
        "gRPC",
        "dotfiles",
        "subagent",
        "worktree"
    );

    private static final int MAX_KEYTERMS = 50;

    /**
     * Split an identifier into individual words.
     *
     * Handles camelCase, PascalCase, kebab-case, snake_case, or path segments.
     * Fragments of 2 chars or fewer are discarded.
     */
    public static List<String> splitIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return Collections.emptyList();
        }

        // Insert space before uppercase letters (camelCase/PascalCase)
        String separated = name.replaceAll("([a-z])([A-Z])", "$1 $2");

        // Split on separators
        String[] parts = separated.split("[-_./\\s]+");

        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() > 2 && trimmed.length() <= 20) {
                result.add(trimmed);
            }
        }

        return result;
    }

    /**
     * Extract words from file name.
     */
    public static List<String> fileNameWords(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return Collections.emptyList();
        }

        // Get basename without extension
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }

        return splitIdentifier(fileName);
    }

    /**
     * Build a list of keyterms for the STT endpoint.
     *
     * Combines hardcoded global coding terms with session context
     * (project name, git branch, recent files) without any model calls.
     */
    public static CompletableFuture<List<String>> getVoiceKeyterms(Set<String> recentFiles) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> terms = new LinkedHashSet<>(GLOBAL_KEYTERMS);

            // Project root basename
            try {
                String projectRoot = getProjectRoot();
                if (projectRoot != null) {
                    Path path = Paths.get(projectRoot);
                    String name = path.getFileName().toString();
                    if (name.length() > 2 && name.length() <= 50) {
                        terms.add(name);
                    }
                }
            } catch (Exception e) {
                // Ignore if project root not initialized
            }

            // Git branch words
            try {
                String branch = getBranch();
                if (branch != null) {
                    for (String word : splitIdentifier(branch)) {
                        terms.add(word);
                    }
                }
            } catch (Exception e) {
                // Ignore if not in git repo
            }

            // Recent file names
            if (recentFiles != null) {
                for (String filePath : recentFiles) {
                    if (terms.size() >= MAX_KEYTERMS) break;
                    for (String word : fileNameWords(filePath)) {
                        terms.add(word);
                    }
                }
            }

            // Limit to MAX_KEYTERMS
            List<String> result = new ArrayList<>(terms);
            if (result.size() > MAX_KEYTERMS) {
                result = result.subList(0, MAX_KEYTERMS);
            }

            return result;
        });
    }

    /**
     * Get voice keyterms without recent files.
     */
    public static CompletableFuture<List<String>> getVoiceKeyterms() {
        return getVoiceKeyterms(null);
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private static String getProjectRoot() {
        // Return current working directory as project root
        String cwd = System.getProperty("user.dir");
        return cwd;
    }

    private static String getBranch() {
        // Get current git branch name
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "branch", "--show-current");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.waitFor();
                if (process.exitValue() == 0 && line != null) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            // Not in git repo or git not available
        }
        return null;
    }
}