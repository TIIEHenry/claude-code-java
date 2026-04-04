/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/file.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * File operation utilities.
 */
public final class FileUtils {
    private FileUtils() {}

    // Common file extensions
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
        ".txt", ".md", ".json", ".xml", ".yaml", ".yml",
        ".js", ".ts", ".jsx", ".tsx", ".py", ".java", ".go", ".rs",
        ".c", ".cpp", ".h", ".hpp", ".cs", ".rb", ".php", ".swift",
        ".kt", ".scala", ".sh", ".bash", ".zsh", ".fish",
        ".css", ".scss", ".less", ".html", ".svg",
        ".sql", ".graphql", ".proto", ".toml", ".ini", ".cfg"
    );

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
        ".exe", ".dll", ".so", ".dylib",
        ".zip", ".tar", ".gz", ".rar", ".7z",
        ".pdf", ".doc", ".docx", ".xls", ".xlsx",
        ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico",
        ".mp3", ".mp4", ".wav", ".avi", ".mov",
        ".db", ".sqlite", ".sqlite3"
    );

    /**
     * Read file content as string.
     */
    public static String readFile(String path) throws IOException {
        return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    }

    /**
     * Read file content as bytes.
     */
    public static byte[] readBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    /**
     * Write string to file.
     */
    public static void writeFile(String path, String content) throws IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content, StandardCharsets.UTF_8);
    }

    /**
     * Write bytes to file.
     */
    public static void writeBytes(String path, byte[] content) throws IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent());
        Files.write(p, content);
    }

    /**
     * Check if file exists.
     */
    public static boolean exists(String path) {
        return Files.exists(Paths.get(path));
    }

    /**
     * Check if path is a directory.
     */
    public static boolean isDirectory(String path) {
        return Files.isDirectory(Paths.get(path));
    }

    /**
     * Check if path is a regular file.
     */
    public static boolean isFile(String path) {
        return Files.isRegularFile(Paths.get(path));
    }

    /**
     * Delete a file.
     */
    public static boolean delete(String path) throws IOException {
        return Files.deleteIfExists(Paths.get(path));
    }

    /**
     * Create directory.
     */
    public static void mkdir(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }

    /**
     * List files in a directory.
     */
    public static List<String> listFiles(String dir) throws IOException {
        List<String> files = new ArrayList<>();
        try (var stream = Files.list(Paths.get(dir))) {
            stream.forEach(p -> files.add(p.toString()));
        }
        return files;
    }

    /**
     * Copy file.
     */
    public static void copy(String source, String target) throws IOException {
        Path src = Paths.get(source);
        Path dst = Paths.get(target);
        Files.createDirectories(dst.getParent());
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Move file.
     */
    public static void move(String source, String target) throws IOException {
        Path src = Paths.get(source);
        Path dst = Paths.get(target);
        Files.createDirectories(dst.getParent());
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Get file extension.
     */
    public static String getExtension(String path) {
        if (path == null || path.isEmpty()) return "";
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) return "";
        return path.substring(dot).toLowerCase();
    }

    /**
     * Get file name without extension.
     */
    public static String getBaseName(String path) {
        if (path == null || path.isEmpty()) return "";
        String name = Paths.get(path).getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return name;
        return name.substring(0, dot);
    }

    /**
     * Get file name.
     */
    public static String getFileName(String path) {
        if (path == null || path.isEmpty()) return "";
        return Paths.get(path).getFileName().toString();
    }

    /**
     * Get parent directory.
     */
    public static String getParent(String path) {
        if (path == null || path.isEmpty()) return "";
        Path parent = Paths.get(path).getParent();
        return parent != null ? parent.toString() : "";
    }

    /**
     * Get file size.
     */
    public static long getSize(String path) throws IOException {
        return Files.size(Paths.get(path));
    }

    /**
     * Check if file is text.
     */
    public static boolean isTextFile(String path) {
        String ext = getExtension(path);
        return TEXT_EXTENSIONS.contains(ext);
    }

    /**
     * Check if file is binary.
     */
    public static boolean isBinaryFile(String path) {
        String ext = getExtension(path);
        return BINARY_EXTENSIONS.contains(ext);
    }

    /**
     * Format file size.
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Read file lines.
     */
    public static List<String> readLines(String path) throws IOException {
        return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
    }

    /**
     * Write file lines.
     */
    public static void writeLines(String path, List<String> lines) throws IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent());
        Files.write(p, lines, StandardCharsets.UTF_8);
    }

    /**
     * Append to file.
     */
    public static void append(String path, String content) throws IOException {
        Files.writeString(Paths.get(path), content, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    /**
     * Get absolute path.
     */
    public static String getAbsolutePath(String path) {
        return Paths.get(path).toAbsolutePath().normalize().toString();
    }

    /**
     * Normalize path.
     */
    public static String normalize(String path) {
        return Paths.get(path).normalize().toString();
    }

    /**
     * Join path components.
     */
    public static String join(String... parts) {
        return Paths.get("", parts).toString();
    }
}