/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code file utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File utilities for path handling and file operations.
 */
public final class FileUtils2 {
    private FileUtils2() {}

    public static final int MAX_OUTPUT_SIZE = 256 * 1024; // 0.25MB

    /**
     * Check if path exists.
     */
    public static boolean pathExists(Path path) {
        return Files.exists(path);
    }

    /**
     * Get file modification time.
     */
    public static long getFileModificationTime(Path path) throws Exception {
        return Files.getLastModifiedTime(path).toMillis();
    }

    /**
     * Get display path (relative or tilde notation).
     */
    public static String getDisplayPath(String filePath) {
        String cwd = System.getProperty("user.dir");
        String home = System.getProperty("user.home");

        // Use relative path if in cwd
        if (filePath.startsWith(cwd + "/")) {
            return filePath.substring(cwd.length() + 1);
        }

        // Use tilde for home directory
        if (filePath.startsWith(home + "/")) {
            return "~" + filePath.substring(home.length());
        }

        return filePath;
    }

    /**
     * Add line numbers to content.
     */
    public static String addLineNumbers(String content, int startLine) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String[] lines = content.split("\n", -1);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            sb.append(i + startLine).append("\t").append(lines[i]);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Strip line number prefix from a line.
     */
    public static String stripLineNumberPrefix(String line) {
        Pattern pattern = Pattern.compile("^\\s*\\d+[\u2192\t](.*)$");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return line;
    }

    /**
     * Check if directory is empty.
     */
    public static boolean isDirEmpty(Path path) {
        try {
            try (var stream = Files.list(path)) {
                return !stream.findFirst().isPresent();
            }
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Convert leading tabs to spaces.
     */
    public static String convertLeadingTabsToSpaces(String content) {
        if (!content.contains("\t")) return content;

        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) result.append("\n");
            String line = lines[i];
            int tabCount = 0;
            while (tabCount < line.length() && line.charAt(tabCount) == '\t') {
                tabCount++;
            }
            result.append("  ".repeat(tabCount));
            result.append(line.substring(tabCount));
        }
        return result.toString();
    }

    /**
     * Get absolute and relative paths.
     */
    public static PathPair getAbsoluteAndRelativePaths(String path) {
        Path absolute = Paths.get(path).toAbsolutePath();
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path relative = cwd.relativize(absolute);
        return new PathPair(absolute.toString(), relative.toString());
    }

    /**
     * Normalize path for comparison.
     */
    public static String normalizePathForComparison(String filePath) {
        String normalized = Paths.get(filePath).normalize().toString();
        // On Windows, normalize for case-insensitive comparison
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            normalized = normalized.replace("/", "\\").toLowerCase();
        }
        return normalized;
    }

    /**
     * Check if paths are equal.
     */
    public static boolean pathsEqual(String path1, String path2) {
        return normalizePathForComparison(path1).equals(normalizePathForComparison(path2));
    }

    /**
     * Check if file is within read size limit.
     */
    public static boolean isFileWithinReadSizeLimit(Path path, int maxSizeBytes) {
        try {
            return Files.size(path) <= maxSizeBytes;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find similar file with different extension.
     */
    public static String findSimilarFile(Path filePath) {
        try {
            Path dir = filePath.getParent();
            String baseNameRaw = filePath.getFileName().toString();
            int dotIndex = baseNameRaw.lastIndexOf('.');
            final String baseName = dotIndex > 0 ? baseNameRaw.substring(0, dotIndex) : baseNameRaw;
            final String originalFileName = filePath.getFileName().toString();

            try (var stream = Files.list(dir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .filter(name -> {
                            int dot = name.lastIndexOf('.');
                            String nameBase = dot > 0 ? name.substring(0, dot) : name;
                            return nameBase.equals(baseName) && !name.equals(originalFileName);
                        })
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get desktop path.
     */
    public static String getDesktopPath() {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("mac")) {
            return home + "/Desktop";
        }

        if (os.contains("win")) {
            // WSL path
            String userProfile = System.getenv("USERPROFILE");
            if (userProfile != null) {
                String wslPath = userProfile.replace("\\", "/").replaceFirst("^[A-Za-z]:", "");
                String drive = userProfile.substring(0, 1).toLowerCase();
                String desktop = "/mnt/" + drive + wslPath + "/Desktop";
                if (Files.exists(Paths.get(desktop))) {
                    return desktop;
                }
            }
        }

        // Linux fallback
        String desktop = home + "/Desktop";
        if (Files.exists(Paths.get(desktop))) {
            return desktop;
        }

        return home;
    }

    /**
     * Path pair record.
     */
    public record PathPair(String absolutePath, String relativePath) {}
}