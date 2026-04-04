/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code file utilities
 */
package com.anthropic.claudecode.utils.file;

import java.util.concurrent.CompletableFuture;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * File utilities for path handling, encoding detection, and line number formatting.
 */
public final class FileUtils {
    private FileUtils() {}

    public static final int MAX_OUTPUT_SIZE = 256 * 1024; // 0.25MB

    /**
     * Check if a path exists.
     */
    public static boolean pathExists(String path) {
        return Files.exists(Paths.get(path));
    }

    /**
     * Check if a path exists asynchronously.
     */
    public static CompletableFuture<Boolean> pathExistsAsync(String path) {
        return CompletableFuture.supplyAsync(() -> pathExists(path));
    }

    /**
     * Read file content safely.
     */
    public static String readFileSafe(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get file modification time in milliseconds.
     */
    public static long getFileModificationTime(String filePath) {
        try {
            return (long) Math.floor(Files.getLastModifiedTime(Paths.get(filePath)).toMillis());
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Get file modification time asynchronously.
     */
    public static CompletableFuture<Long> getFileModificationTimeAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> getFileModificationTime(filePath));
    }

    /**
     * Detect file encoding.
     */
    public static Charset detectEncoding(String filePath) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));

            // Check for BOM
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }

            if (bytes.length >= 2) {
                if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                    return StandardCharsets.UTF_16BE;
                }
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                    return StandardCharsets.UTF_16LE;
                }
            }

            // Default to UTF-8
            return StandardCharsets.UTF_8;
        } catch (IOException e) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * Detect line endings in content.
     */
    public static LineEnding detectLineEndings(String content) {
        if (content == null || content.isEmpty()) {
            return LineEnding.LF;
        }

        int crlf = 0;
        int lf = 0;

        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\r') {
                if (i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    crlf++;
                    i++; // Skip the \n
                }
            } else if (content.charAt(i) == '\n') {
                lf++;
            }
        }

        return crlf > lf ? LineEnding.CRLF : LineEnding.LF;
    }

    /**
     * Line ending type.
     */
    public enum LineEnding {
        LF,
        CRLF
    }

    /**
     * Convert leading tabs to spaces.
     */
    public static String convertLeadingTabsToSpaces(String content) {
        if (content == null || !content.contains("\t")) {
            return content;
        }

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
     * Get display path (relative or tilde notation).
     */
    public static String getDisplayPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }

        String cwd = System.getProperty("user.dir");
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        Path cwdPath = Paths.get(cwd).toAbsolutePath().normalize();

        // Use relative path if within cwd
        if (path.startsWith(cwdPath)) {
            Path relative = cwdPath.relativize(path);
            String relativeStr = relative.toString();
            if (!relativeStr.startsWith("..")) {
                return relativeStr;
            }
        }

        // Use tilde notation for home directory
        String home = System.getProperty("user.home");
        if (filePath.startsWith(home + File.separator)) {
            return "~" + filePath.substring(home.length());
        }

        return filePath;
    }

    /**
     * Add cat -n style line numbers to content.
     */
    public static String addLineNumbers(String content, int startLine) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String[] lines = content.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append("\n");

            int lineNum = i + startLine;
            // Compact format: N\tline
            sb.append(lineNum).append("\t").append(lines[i]);
        }

        return sb.toString();
    }

    /**
     * Add line numbers with padded format.
     */
    public static String addLineNumbersPadded(String content, int startLine) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String[] lines = content.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append("\n");

            int lineNum = i + startLine;
            String numStr = String.valueOf(lineNum);

            if (numStr.length() >= 6) {
                sb.append(numStr).append("→").append(lines[i]);
            } else {
                sb.append(String.format("%6d→%s", lineNum, lines[i]));
            }
        }

        return sb.toString();
    }

    /**
     * Strip line number prefix from a line.
     */
    public static String stripLineNumberPrefix(String line) {
        // Match N→ or N\t prefix
        Pattern pattern = Pattern.compile("^\\s*\\d+[\\u2192\t](.*)$");
        Matcher matcher = pattern.matcher(line);
        return matcher.matches() ? matcher.group(1) : line;
    }

    /**
     * Check if a directory is empty.
     */
    public static boolean isDirEmpty(String dirPath) {
        try (var stream = Files.list(Paths.get(dirPath))) {
            return !stream.findFirst().isPresent();
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Write file content with atomic write pattern.
     */
    public static void writeFileAtomic(String filePath, String content, Charset encoding) throws IOException {
        Path targetPath = Paths.get(filePath);
        Path tempPath = Paths.get(filePath + ".tmp." + ProcessHandle.current().pid() + "." + System.currentTimeMillis());

        try {
            // Write to temp file
            Files.writeString(tempPath, content, encoding, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            // Atomic rename
            Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // Clean up temp file on error
            try {
                Files.deleteIfExists(tempPath);
            } catch (Exception ignored) {}

            // Fallback to direct write
            Files.writeString(targetPath, content, encoding, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    /**
     * Check if file is within read size limit.
     */
    public static boolean isFileWithinReadSizeLimit(String filePath, int maxSizeBytes) {
        try {
            long size = Files.size(Paths.get(filePath));
            return size <= maxSizeBytes;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Normalize path for comparison.
     */
    public static String normalizePathForComparison(String filePath) {
        if (filePath == null) return null;

        String normalized = Paths.get(filePath).normalize().toString();

        // On Windows, normalize case
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            normalized = normalized.replace('/', '\\').toLowerCase();
        }

        return normalized;
    }

    /**
     * Check if two paths are equal.
     */
    public static boolean pathsEqual(String path1, String path2) {
        return Objects.equals(normalizePathForComparison(path1), normalizePathForComparison(path2));
    }

    /**
     * Find similar files with same name but different extension.
     */
    public static Optional<String> findSimilarFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Path parent = path.getParent();
            String baseName = getBaseName(path.getFileName().toString());

            if (parent == null || !Files.isDirectory(parent)) {
                return Optional.empty();
            }

            try (var stream = Files.list(parent)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .filter(name -> getBaseName(name).equals(baseName) && !parent.resolve(name).equals(path))
                        .findFirst();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Get base name without extension.
     */
    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Get desktop path for the current platform.
     */
    public static String getDesktopPath() {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        String desktopPath;

        if (os.contains("mac")) {
            desktopPath = Paths.get(home, "Desktop").toString();
        } else if (os.contains("win")) {
            // Check for WSL Windows desktop
            String userProfile = System.getenv("USERPROFILE");
            if (userProfile != null) {
                desktopPath = userProfile.replace("\\", "/") + "/Desktop";
            } else {
                desktopPath = Paths.get(home, "Desktop").toString();
            }
        } else {
            // Linux
            desktopPath = Paths.get(home, "Desktop").toString();
        }

        if (Files.isDirectory(Paths.get(desktopPath))) {
            return desktopPath;
        }

        return home;
    }

    /**
     * Suggest path under cwd for a not-found path.
     */
    public static Optional<String> suggestPathUnderCwd(String requestedPath, String cwd) {
        Path requested = Paths.get(requestedPath).toAbsolutePath().normalize();
        Path cwdPath = Paths.get(cwd).toAbsolutePath().normalize();
        Path cwdParent = cwdPath.getParent();

        if (cwdParent == null) {
            return Optional.empty();
        }

        // Check if requested is under cwd's parent but not under cwd
        if (!requested.startsWith(cwdParent) || requested.startsWith(cwdPath)) {
            return Optional.empty();
        }

        // Get relative path from parent and check under cwd
        Path relative = cwdParent.relativize(requested);
        Path suggested = cwdPath.resolve(relative);

        if (Files.exists(suggested)) {
            return Optional.of(suggested.toString());
        }

        return Optional.empty();
    }
}