/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code IO utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

/**
 * IO utilities.
 */
public final class IOUtils {
    private IOUtils() {}

    private static final int BUFFER_SIZE = 8192;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Read all bytes from input stream.
     */
    public static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    /**
     * Read all lines from input stream.
     */
    public static List<String> readAllLines(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    /**
     * Read all lines from input stream with encoding.
     */
    public static List<String> readAllLines(InputStream is, Charset charset) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    /**
     * Read string from input stream.
     */
    public static String readString(InputStream is) throws IOException {
        return new String(readAllBytes(is), StandardCharsets.UTF_8);
    }

    /**
     * Read string from input stream with encoding.
     */
    public static String readString(InputStream is, Charset charset) throws IOException {
        return new String(readAllBytes(is), charset);
    }

    /**
     * Write string to output stream.
     */
    public static void writeString(OutputStream os, String content) throws IOException {
        os.write(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write string to output stream with encoding.
     */
    public static void writeString(OutputStream os, String content, Charset charset) throws IOException {
        os.write(content.getBytes(charset));
    }

    /**
     * Write bytes to output stream.
     */
    public static void writeBytes(OutputStream os, byte[] data) throws IOException {
        os.write(data);
    }

    /**
     * Copy stream.
     */
    public static long copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long total = 0;
        int n;
        while ((n = is.read(buffer)) != -1) {
            os.write(buffer, 0, n);
            total += n;
        }
        return total;
    }

    /**
     * Copy stream with progress callback.
     */
    public static long copy(InputStream is, OutputStream os, LongConsumer progressCallback) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long total = 0;
        int n;
        while ((n = is.read(buffer)) != -1) {
            os.write(buffer, 0, n);
            total += n;
            progressCallback.accept(total);
        }
        return total;
    }

    @FunctionalInterface
    public interface LongConsumer {
        void accept(long value);
    }

    /**
     * Close quietly (ignoring exceptions).
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Close multiple quietly.
     */
    public static void closeQuietly(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    /**
     * Read file to string.
     */
    public static String readFileToString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * Read file to string with encoding.
     */
    public static String readFileToString(Path path, Charset charset) throws IOException {
        return Files.readString(path, charset);
    }

    /**
     * Read file to bytes.
     */
    public static byte[] readFileToBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * Read file to lines.
     */
    public static List<String> readFileToLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    /**
     * Write string to file.
     */
    public static void writeStringToFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /**
     * Write string to file with encoding.
     */
    public static void writeStringToFile(Path path, String content, Charset charset) throws IOException {
        Files.writeString(path, content, charset);
    }

    /**
     * Write bytes to file.
     */
    public static void writeBytesToFile(Path path, byte[] data) throws IOException {
        Files.write(path, data);
    }

    /**
     * Write lines to file.
     */
    public static void writeLinesToFile(Path path, List<String> lines) throws IOException {
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    /**
     * Append string to file.
     */
    public static void appendStringToFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    /**
     * Check if file exists.
     */
    public static boolean fileExists(Path path) {
        return Files.exists(path);
    }

    /**
     * Check if directory exists.
     */
    public static boolean directoryExists(Path path) {
        return Files.isDirectory(path);
    }

    /**
     * Create directory if not exists.
     */
    public static void createDirectoryIfNotExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Create parent directories.
     */
    public static void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Delete file if exists.
     */
    public static boolean deleteFileIfExists(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    /**
     * Delete directory recursively.
     */
    public static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }

    /**
     * Copy directory.
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Get file extension.
     */
    public static String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 && dotIndex < fileName.length() - 1
            ? fileName.substring(dotIndex + 1)
            : "";
    }

    /**
     * Get file name without extension.
     */
    public static String getFileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Get file size.
     */
    public static long getFileSize(Path path) throws IOException {
        return Files.size(path);
    }

    /**
     * Get file size formatted.
     */
    public static String getFileSizeFormatted(Path path) throws IOException {
        return formatFileSize(getFileSize(path));
    }

    /**
     * Format file size.
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIndex = (int) (Math.log10(bytes) / Math.log10(1024));
        unitIndex = Math.min(unitIndex, units.length - 1);
        double size = bytes / Math.pow(1024, unitIndex);
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * Get temp directory.
     */
    public static Path getTempDirectory() {
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Create temp file.
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix);
    }

    /**
     * Create temp directory.
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * List files in directory.
     */
    public static List<Path> listFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile).collect(Collectors.toList());
        }
    }

    /**
     * List directories in directory.
     */
    public static List<Path> listDirectories(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isDirectory).collect(Collectors.toList());
        }
    }

    /**
     * List all files recursively.
     */
    public static List<Path> listAllFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        try (java.util.stream.Stream<Path> stream = Files.walk(directory)) {
            return stream.filter(Files::isRegularFile).collect(Collectors.toList());
        }
    }

    /**
     * Find files matching pattern.
     */
    public static List<Path> findFiles(Path directory, String glob) throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        try (java.util.stream.Stream<Path> stream = Files.walk(directory)) {
            return stream.filter(Files::isRegularFile)
                        .filter(matcher::matches)
                        .collect(Collectors.toList());
        }
    }

    /**
     * Touch file (create if not exists, update modification time if exists).
     */
    public static void touchFile(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.setLastModifiedTime(path, FileTime.from(java.time.Instant.now()));
        } else {
            createParentDirectories(path);
            Files.createFile(path);
        }
    }

    /**
     * Read resource as string.
     */
    public static String readResourceAsString(String name) throws IOException {
        try (InputStream is = IOUtils.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + name);
            }
            return readString(is);
        }
    }

    /**
     * Read resource as bytes.
     */
    public static byte[] readResourceAsBytes(String name) throws IOException {
        try (InputStream is = IOUtils.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + name);
            }
            return readAllBytes(is);
        }
    }

    /**
     * Buffered reader from file.
     */
    public static BufferedReader bufferedReader(Path path) throws IOException {
        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    /**
     * Buffered writer to file.
     */
    public static BufferedWriter bufferedWriter(Path path) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8);
    }

    /**
     * Input stream from file.
     */
    public static InputStream inputStream(Path path) throws IOException {
        return Files.newInputStream(path);
    }

    /**
     * Output stream to file.
     */
    public static OutputStream outputStream(Path path) throws IOException {
        return Files.newOutputStream(path);
    }
}