/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code markdown configuration loader
 */
package com.anthropic.claudecode.utils.config;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Markdown configuration loader for agents, commands, skills, etc.
 * Loads markdown files from managed, user, and project directories.
 */
public final class MarkdownConfigLoader {
    private MarkdownConfigLoader() {}

    // Claude configuration directory names
    public static final List<String> CLAUDE_CONFIG_DIRECTORIES = List.of(
            "commands",
            "agents",
            "output-styles",
            "skills",
            "workflows"
    );

    /**
     * Markdown file with metadata.
     */
    public record MarkdownFile(
            String filePath,
            String baseDir,
            FrontmatterData frontmatter,
            String content,
            String source
    ) {}

    /**
     * Frontmatter data parsed from markdown.
     */
    public record FrontmatterData(
            String name,
            String description,
            List<String> tools,
            Map<String, Object> additionalFields
    ) {}

    // Cache for loaded markdown files
    private static final ConcurrentHashMap<String, List<MarkdownFile>> cache = new ConcurrentHashMap<>();

    /**
     * Extract description from markdown content.
     */
    public static String extractDescriptionFromMarkdown(String content, String defaultDescription) {
        if (content == null || content.isEmpty()) {
            return defaultDescription;
        }

        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                // If it's a header, strip the header prefix
                Pattern headerPattern = Pattern.compile("^#+\\s+(.+)$");
                Matcher m = headerPattern.matcher(trimmed);
                String text = m.matches() ? m.group(1) : trimmed;

                // Return the text, limited to reasonable length
                return text.length() > 100 ? text.substring(0, 97) + "..." : text;
            }
        }

        return defaultDescription;
    }

    /**
     * Parse tools from frontmatter.
     */
    public static List<String> parseToolListString(Object toolsValue) {
        if (toolsValue == null) {
            return null;
        }

        if (toolsValue instanceof String) {
            String str = (String) toolsValue;
            if (str.isEmpty()) {
                return List.of();
            }
            return parseToolListFromCLI(List.of(str));
        }

        if (toolsValue instanceof List) {
            List<?> list = (List<?>) toolsValue;
            List<String> toolsArray = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String) {
                    toolsArray.add((String) item);
                }
            }
            if (toolsArray.isEmpty()) {
                return List.of();
            }
            return parseToolListFromCLI(toolsArray);
        }

        return List.of();
    }

    /**
     * Parse tools from agent frontmatter.
     */
    public static List<String> parseAgentToolsFromFrontmatter(Object toolsValue) {
        List<String> parsed = parseToolListString(toolsValue);
        if (parsed == null) {
            // For agents: undefined = all tools
            return null;
        }
        // If parsed contains '*', return null (all tools)
        if (parsed.contains("*")) {
            return null;
        }
        return parsed;
    }

    /**
     * Parse allowed-tools from slash command frontmatter.
     */
    public static List<String> parseSlashCommandToolsFromFrontmatter(Object toolsValue) {
        List<String> parsed = parseToolListString(toolsValue);
        if (parsed == null) {
            return List.of();
        }
        return parsed;
    }

    /**
     * Parse tool list from CLI-style input.
     */
    private static List<String> parseToolListFromCLI(List<String> toolStrings) {
        List<String> result = new ArrayList<>();
        for (String str : toolStrings) {
            // Split by comma or space
            String[] parts = str.split("[,\\s]+");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    result.add(part.trim());
                }
            }
        }
        if (result.contains("*")) {
            return List.of("*");
        }
        return result;
    }

    /**
     * Get project directories up to home/git root.
     */
    public static List<String> getProjectDirsUpToHome(String subdir, String cwd) {
        String home = resolveHomeDir();
        String gitRoot = findGitRoot(cwd);
        Path current = Paths.get(cwd).normalize();
        List<String> dirs = new ArrayList<>();

        while (true) {
            // Stop if we've reached the home directory
            if (current.normalize().toString().equals(home)) {
                break;
            }

            Path claudeSubdir = current.resolve(".claude").resolve(subdir);
            if (Files.isDirectory(claudeSubdir)) {
                dirs.add(claudeSubdir.toString());
            }

            // Stop after processing the git root directory
            if (gitRoot != null && current.normalize().toString().equals(gitRoot)) {
                break;
            }

            // Move to parent directory
            Path parent = current.getParent();
            if (parent == null || parent.equals(current)) {
                break;
            }

            current = parent;
        }

        return dirs;
    }

    /**
     * Load markdown files for a subdirectory.
     */
    public static List<MarkdownFile> loadMarkdownFilesForSubdir(String subdir, String cwd) {
        String cacheKey = subdir + ":" + cwd;
        return cache.computeIfAbsent(cacheKey, k -> loadMarkdownFilesInternal(subdir, cwd));
    }

    /**
     * Internal loading implementation.
     */
    private static List<MarkdownFile> loadMarkdownFilesInternal(String subdir, String cwd) {
        long startTime = System.currentTimeMillis();

        String userDir = resolveUserDir(subdir);
        String managedDir = resolveManagedDir(subdir);
        List<String> projectDirs = getProjectDirsUpToHome(subdir, cwd);

        List<MarkdownFile> managedFiles = loadMarkdownFilesFromDir(managedDir, "policySettings");
        List<MarkdownFile> userFiles = isSettingSourceEnabled("userSettings")
                ? loadMarkdownFilesFromDir(userDir, "userSettings")
                : List.of();

        List<MarkdownFile> projectFiles = new ArrayList<>();
        if (isSettingSourceEnabled("projectSettings")) {
            for (String projectDir : projectDirs) {
                projectFiles.addAll(loadMarkdownFilesFromDir(projectDir, "projectSettings"));
            }
        }

        // Combine all files with priority: managed > user > project
        List<MarkdownFile> allFiles = new ArrayList<>();
        allFiles.addAll(managedFiles);
        allFiles.addAll(userFiles);
        allFiles.addAll(projectFiles);

        // Deduplicate files by identity
        List<MarkdownFile> deduplicatedFiles = deduplicateFiles(allFiles);

        long duration = System.currentTimeMillis() - startTime;

        return deduplicatedFiles;
    }

    /**
     * Load markdown files from a directory.
     */
    private static List<MarkdownFile> loadMarkdownFilesFromDir(String dir, String source) {
        if (dir == null || !Files.isDirectory(Paths.get(dir))) {
            return List.of();
        }

        List<MarkdownFile> files = new ArrayList<>();
        try {
            Files.walk(Paths.get(dir))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            FrontmatterData frontmatter = parseFrontmatter(content, path.toString());

                            files.add(new MarkdownFile(
                                    path.toString(),
                                    dir,
                                    frontmatter,
                                    content,
                                    source
                            ));
                        } catch (Exception e) {
                            // Skip files that can't be read
                        }
                    });
        } catch (IOException e) {
            // Directory not accessible
        }

        return files;
    }

    /**
     * Parse frontmatter from markdown content.
     */
    public static FrontmatterData parseFrontmatter(String content, String filePath) {
        Map<String, Object> fields = new LinkedHashMap<>();
        String name = null;
        String description = null;
        List<String> tools = null;

        // Check for YAML frontmatter between --- markers
        Pattern frontmatterPattern = Pattern.compile("^---\\s*\n(.*?)\n---\\s*\n", Pattern.DOTALL);
        Matcher m = frontmatterPattern.matcher(content);

        if (m.find()) {
            String frontmatterText = m.group(1);
            String[] lines = frontmatterText.split("\n");

            for (String line : lines) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    switch (key) {
                        case "name" -> name = value;
                        case "description" -> description = value;
                        case "tools", "allowed-tools" -> {
                            // Parse tools list
                            if (value.startsWith("[") && value.endsWith("]")) {
                                String inner = value.substring(1, value.length() - 1);
                                tools = Arrays.stream(inner.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .toList();
                            } else {
                                tools = List.of(value);
                            }
                        }
                        default -> fields.put(key, parseValue(value));
                    }
                }
            }

            // Remove frontmatter from content
            content = content.substring(m.end());
        }

        // Use filename as name if not specified
        if (name == null) {
            Path path = Paths.get(filePath);
            name = path.getFileName().toString().replace(".md", "");
        }

        // Extract description from content if not specified
        if (description == null) {
            description = extractDescriptionFromMarkdown(content, "Custom item");
        }

        return new FrontmatterData(name, description, tools, fields);
    }

    /**
     * Parse a value from frontmatter.
     */
    private static Object parseValue(String value) {
        // Remove quotes
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        // Boolean
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;

        // Number
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                // Not a number
            }
        }

        // Array
        if (value.startsWith("[") && value.endsWith("]")) {
            String inner = value.substring(1, value.length() - 1);
            return Arrays.stream(inner.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        return value;
    }

    /**
     * Deduplicate files by file identity.
     */
    private static List<MarkdownFile> deduplicateFiles(List<MarkdownFile> files) {
        Map<String, String> seenFileIds = new HashMap<>();
        List<MarkdownFile> result = new ArrayList<>();

        for (MarkdownFile file : files) {
            String fileId = getFileIdentity(file.filePath());
            if (fileId == null) {
                // Can't identify, include it
                result.add(file);
                continue;
            }

            if (!seenFileIds.containsKey(fileId)) {
                seenFileIds.put(fileId, file.source());
                result.add(file);
            }
        }

        return result;
    }

    /**
     * Get file identity (device:inode).
     */
    private static String getFileIdentity(String filePath) {
        try {
            var attrs = Files.getAttribute(Paths.get(filePath), "unix:device", java.nio.file.LinkOption.NOFOLLOW_LINKS);
            var inode = Files.getAttribute(Paths.get(filePath), "unix:ino", java.nio.file.LinkOption.NOFOLLOW_LINKS);
            return attrs + ":" + inode;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolve home directory.
     */
    private static String resolveHomeDir() {
        return System.getProperty("user.home");
    }

    /**
     * Resolve user config directory.
     */
    private static String resolveUserDir(String subdir) {
        return Paths.get(System.getProperty("user.home"), ".claude", subdir).toString();
    }

    /**
     * Resolve managed config directory.
     */
    private static String resolveManagedDir(String subdir) {
        // Read managed path from settings
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(home, ".claude", "settings.json");

            if (java.nio.file.Files.exists(settingsPath)) {
                String content = java.nio.file.Files.readString(settingsPath);

                // Find managedPath setting
                int managedIdx = content.indexOf("\"managedPath\"");
                if (managedIdx >= 0) {
                    int valStart = content.indexOf("\"", managedIdx + 13) + 1;
                    int valEnd = content.indexOf("\"", valStart);
                    if (valStart > 0 && valEnd > valStart) {
                        String managedPath = content.substring(valStart, valEnd);
                        return Paths.get(managedPath, subdir).toString();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return null;
    }

    /**
     * Find git root.
     */
    private static String findGitRoot(String cwd) {
        Path current = Paths.get(cwd).normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve(".git"))) {
                return current.toString();
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Check if setting source is enabled.
     */
    private static boolean isSettingSourceEnabled(String source) {
        // Check if source is in enabled sources list
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(home, ".claude", "settings.json");

            if (java.nio.file.Files.exists(settingsPath)) {
                String content = java.nio.file.Files.readString(settingsPath);

                // Find enabledSources setting
                int sourcesIdx = content.indexOf("\"enabledSources\"");
                if (sourcesIdx >= 0) {
                    int arrStart = content.indexOf("[", sourcesIdx);
                    int arrEnd = content.indexOf("]", arrStart);
                    if (arrStart >= 0 && arrEnd > arrStart) {
                        String arr = content.substring(arrStart, arrEnd + 1);
                        return arr.contains("\"" + source + "\"");
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return true; // Default to enabled
    }

    /**
     * Clear cache.
     */
    public static void clearCache() {
        cache.clear();
    }

    /**
     * Get cache size.
     */
    public static int getCacheSize() {
        return cache.size();
    }
}