/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code claude md utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;

/**
 * Claude MD memory file utilities.
 */
public final class ClaudeMd {
    private ClaudeMd() {}

    public static final int MAX_MEMORY_CHARACTER_COUNT = 40000;
    private static final int MAX_INCLUDE_DEPTH = 5;

    /**
     * Memory type enum.
     */
    public enum MemoryType {
        MANAGED, USER, PROJECT, LOCAL, AUTO_MEM, TEAM_MEM
    }

    /**
     * Memory file info.
     */
    public record MemoryFileInfo(
            String path,
            MemoryType type,
            String content,
            String parent,
            List<String> globs,
            boolean contentDiffersFromDisk,
            String rawContent
    ) {}

    /**
     * Check if path is a memory file.
     */
    public static boolean isMemoryFilePath(String filePath) {
        String name = Paths.get(filePath).getFileName().toString();

        if ("CLAUDE.md".equals(name) || "CLAUDE.local.md".equals(name)) {
            return true;
        }

        if (name.endsWith(".md") && filePath.contains("/.claude/rules/")) {
            return true;
        }

        return false;
    }

    /**
     * Strip HTML comments from content.
     */
    public static String stripHtmlComments(String content) {
        if (!content.contains("<!--")) {
            return content;
        }
        // Simple regex-based stripping for block-level comments
        return content.replaceAll("<!--[\\s\\S]*?-->", "");
    }

    /**
     * Truncate content to max character count.
     */
    public static String truncateContent(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "\n... (truncated)";
    }

    /**
     * Get memory files from project.
     */
    public static List<MemoryFileInfo> getMemoryFiles(boolean forceIncludeExternal) {
        List<MemoryFileInfo> result = new ArrayList<>();
        Set<String> processedPaths = new HashSet<>();

        // Process user memory
        Path userClaudeMd = Paths.get(System.getProperty("user.home"), ".claude", "CLAUDE.md");
        processMemoryFile(userClaudeMd, MemoryType.USER, processedPaths, result);

        // Process project memory (walk from CWD up to root)
        Path cwd = Paths.get(System.getProperty("user.dir"));
        List<Path> dirs = new ArrayList<>();
        Path current = cwd;
        while (current != null) {
            dirs.add(current);
            current = current.getParent();
        }

        // Process from root to CWD
        Collections.reverse(dirs);
        for (Path dir : dirs) {
            // CLAUDE.md
            processMemoryFile(dir.resolve("CLAUDE.md"), MemoryType.PROJECT, processedPaths, result);
            // .claude/CLAUDE.md
            processMemoryFile(dir.resolve(".claude/CLAUDE.md"), MemoryType.PROJECT, processedPaths, result);
            // CLAUDE.local.md
            processMemoryFile(dir.resolve("CLAUDE.local.md"), MemoryType.LOCAL, processedPaths, result);
        }

        return result;
    }

    /**
     * Process a single memory file.
     */
    private static void processMemoryFile(Path path, MemoryType type, Set<String> processedPaths, List<MemoryFileInfo> result) {
        String normalizedPath = path.normalize().toString();
        if (processedPaths.contains(normalizedPath)) return;

        try {
            if (!Files.exists(path)) return;

            String rawContent = Files.readString(path);
            String content = stripHtmlComments(rawContent);

            if (content.trim().isEmpty()) return;

            processedPaths.add(normalizedPath);
            result.add(new MemoryFileInfo(
                    normalizedPath,
                    type,
                    truncateContent(content, MAX_MEMORY_CHARACTER_COUNT),
                    null,
                    null,
                    !content.equals(rawContent),
                    content.equals(rawContent) ? null : rawContent
            ));
        } catch (Exception e) {
            // Ignore errors
        }
    }
}