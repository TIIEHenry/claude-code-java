/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/fileOperations
 */
package com.anthropic.claudecode.services.fileops;

import java.util.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.*;

/**
 * File operations service - File manipulation utilities.
 */
public final class FileOperationsService {
    private final int maxFileSize;
    private final Set<String> binaryExtensions;

    /**
     * Create file operations service.
     */
    public FileOperationsService(int maxFileSize) {
        this.maxFileSize = maxFileSize;
        this.binaryExtensions = Set.of(
            ".exe", ".dll", ".so", ".dylib", ".class", ".jar",
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico",
            ".pdf", ".zip", ".tar", ".gz", ".7z", ".rar",
            ".mp3", ".mp4", ".wav", ".avi", ".mov", ".mkv",
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
        );
    }

    /**
     * Read file content.
     */
    public FileContent readFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            return FileContent.notFound(path);
        }

        if (Files.size(path) > maxFileSize) {
            return FileContent.tooLarge(path, Files.size(path));
        }

        if (isBinaryFile(path)) {
            return FileContent.binary(path);
        }

        String content = Files.readString(path, StandardCharsets.UTF_8);
        return FileContent.text(path, content);
    }

    /**
     * Write file content.
     */
    public void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /**
     * Append to file.
     */
    public void appendFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Delete file or directory.
     */
    public DeleteResult delete(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new DeleteResult(false, "Path does not exist", 0);
        }

        int deletedCount = 0;
        if (Files.isDirectory(path)) {
            deletedCount = deleteDirectoryRecursive(path);
        } else {
            Files.delete(path);
            deletedCount = 1;
        }

        return new DeleteResult(true, null, deletedCount);
    }

    /**
     * Delete directory recursively.
     */
    private int deleteDirectoryRecursive(Path path) throws IOException {
        int[] count = {0};
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                count[0]++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        return count[0];
    }

    /**
     * Copy file or directory.
     */
    public CopyResult copy(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return new CopyResult(false, "Source does not exist", 0);
        }

        int copiedCount = 0;
        if (Files.isDirectory(source)) {
            copiedCount = copyDirectoryRecursive(source, target);
        } else {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            copiedCount = 1;
        }

        return new CopyResult(true, null, copiedCount);
    }

    /**
     * Copy directory recursively.
     */
    private int copyDirectoryRecursive(Path source, Path target) throws IOException {
        int[] count = {0};
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                count[0]++;
                return FileVisitResult.CONTINUE;
            }
        });
        return count[0];
    }

    /**
     * Move file or directory.
     */
    public MoveResult move(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return new MoveResult(false, "Source does not exist");
        }

        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

        return new MoveResult(true, null);
    }

    /**
     * Check if binary file.
     */
    public boolean isBinaryFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        for (String ext : binaryExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get file info.
     */
    public FileInfo getFileInfo(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        String mimeType = detectMimeType(path);

        return new FileInfo(
            path.toString(),
            path.getFileName().toString(),
            attrs.size(),
            attrs.lastModifiedTime().toMillis(),
            attrs.isDirectory(),
            mimeType,
            isBinaryFile(path)
        );
    }

    /**
     * Detect MIME type.
     */
    private String detectMimeType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".java")) return "text/x-java";
            if (name.endsWith(".ts")) return "text/typescript";
            if (name.endsWith(".js")) return "text/javascript";
            if (name.endsWith(".py")) return "text/x-python";
            if (name.endsWith(".json")) return "application/json";
            if (name.endsWith(".md")) return "text/markdown";
            return "text/plain";
        }
    }

    /**
     * List directory contents.
     */
    public List<FileInfo> listDirectory(Path path, boolean recursive) throws IOException {
        if (!Files.isDirectory(path)) {
            return Collections.emptyList();
        }

        List<FileInfo> files = new ArrayList<>();
        if (recursive) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        files.add(getFileInfo(file));
                    } catch (IOException e) {
                        // Skip
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    files.add(getFileInfo(entry));
                }
            }
        }

        return files;
    }

    /**
     * File content record.
     */
    public record FileContent(
        Path path,
        String content,
        ReadStatus status,
        String errorMessage,
        long size
    ) {
        public static FileContent text(Path path, String content) {
            return new FileContent(path, content, ReadStatus.SUCCESS, null, content.length());
        }

        public static FileContent notFound(Path path) {
            return new FileContent(path, null, ReadStatus.NOT_FOUND, "File not found", 0);
        }

        public static FileContent tooLarge(Path path, long size) {
            return new FileContent(path, null, ReadStatus.TOO_LARGE, "File too large", size);
        }

        public static FileContent binary(Path path) {
            return new FileContent(path, null, ReadStatus.BINARY, "Binary file", 0);
        }

        public boolean isSuccess() { return status == ReadStatus.SUCCESS; }
    }

    /**
     * Read status enum.
     */
    public enum ReadStatus {
        SUCCESS,
        NOT_FOUND,
        TOO_LARGE,
        BINARY,
        ERROR
    }

    /**
     * Delete result record.
     */
    public record DeleteResult(boolean success, String error, int deletedCount) {}

    /**
     * Copy result record.
     */
    public record CopyResult(boolean success, String error, int copiedCount) {}

    /**
     * Move result record.
     */
    public record MoveResult(boolean success, String error) {}

    /**
     * File info record.
     */
    public record FileInfo(
        String path,
        String name,
        long size,
        long lastModified,
        boolean isDirectory,
        String mimeType,
        boolean isBinary
    ) {
        public String formatSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return (size / 1024) + " KB";
            return (size / (1024 * 1024)) + " MB";
        }
    }
}