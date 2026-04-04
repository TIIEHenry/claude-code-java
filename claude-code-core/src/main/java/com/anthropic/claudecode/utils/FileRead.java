/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code file read utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

/**
 * Sync file-read utilities with encoding and line ending detection.
 */
public final class FileRead {
    private FileRead() {}

    /**
     * Line ending type enum.
     */
    public enum LineEndingType {
        CRLF, LF
    }

    /**
     * File read result with metadata.
     */
    public record FileReadResult(
            String content,
            Charset encoding,
            LineEndingType lineEndings
    ) {}

    /**
     * Detect file encoding from file path.
     */
    public static Charset detectEncoding(Path path) throws IOException {
        byte[] bytes = new byte[4096];
        try (InputStream is = Files.newInputStream(path)) {
            int bytesRead = is.read(bytes);
            return detectEncoding(bytes, bytesRead);
        }
    }

    /**
     * Detect encoding from byte buffer.
     */
    public static Charset detectEncoding(byte[] buffer, int bytesRead) {
        // Empty files should default to UTF-8
        if (bytesRead == 0) {
            return StandardCharsets.UTF_8;
        }

        // Check for BOM
        if (bytesRead >= 2) {
            // UTF-16 LE: FF FE
            if ((buffer[0] & 0xFF) == 0xFF && (buffer[1] & 0xFF) == 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
        }

        if (bytesRead >= 3) {
            // UTF-8 BOM: EF BB BF
            if ((buffer[0] & 0xFF) == 0xEF &&
                (buffer[1] & 0xFF) == 0xBB &&
                (buffer[2] & 0xFF) == 0xBF) {
                return StandardCharsets.UTF_8;
            }
        }

        // Default to UTF-8
        return StandardCharsets.UTF_8;
    }

    /**
     * Detect line endings from string content.
     */
    public static LineEndingType detectLineEndings(String content) {
        int crlfCount = 0;
        int lfCount = 0;

        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                if (i > 0 && content.charAt(i - 1) == '\r') {
                    crlfCount++;
                } else {
                    lfCount++;
                }
            }
        }

        return crlfCount > lfCount ? LineEndingType.CRLF : LineEndingType.LF;
    }

    /**
     * Read file with metadata (encoding and line endings).
     */
    public static FileReadResult readWithMetadata(Path path) throws IOException {
        // Detect encoding first
        Charset encoding = detectEncoding(path);

        // Read content
        String raw = Files.readString(path, encoding);

        // Detect line endings from first 4096 chars
        String sample = raw.length() > 4096 ? raw.substring(0, 4096) : raw;
        LineEndingType lineEndings = detectLineEndings(sample);

        // Normalize line endings to LF
        String content = raw.replace("\r\n", "\n");

        return new FileReadResult(content, encoding, lineEndings);
    }

    /**
     * Read file content as string.
     */
    public static String readFile(Path path) throws IOException {
        return readWithMetadata(path).content();
    }

    /**
     * Read file content as string with path string.
     */
    public static String readFile(String path) throws IOException {
        return readFile(Paths.get(path));
    }

    /**
     * Check if file exists.
     */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    /**
     * Check if file is a symlink.
     */
    public static boolean isSymlink(Path path) {
        return Files.isSymbolicLink(path);
    }

    /**
     * Resolve symlink to real path.
     */
    public static Path resolveSymlink(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            return Files.readSymbolicLink(path);
        }
        return path;
    }
}