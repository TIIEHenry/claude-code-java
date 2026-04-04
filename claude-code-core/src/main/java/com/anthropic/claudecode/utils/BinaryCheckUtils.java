/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/binaryCheck
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;

/**
 * Binary check utilities - Detect binary files.
 */
public final class BinaryCheckUtils {
    // Common binary file extensions
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
        // Images
        "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "svg", "tiff", "tif",
        // Video
        "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm",
        // Audio
        "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a",
        // Archives
        "zip", "tar", "gz", "bz2", "xz", "7z", "rar", "tgz",
        // Executables
        "exe", "dll", "so", "dylib", "bin", "app", "dmg", "deb", "rpm",
        // Documents
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
        // Fonts
        "ttf", "otf", "woff", "woff2", "eot",
        // Database
        "db", "sqlite", "sqlite3",
        // Other
        "pyc", "pyo", "class", "jar", "war", "ear", "swf", "node_modules"
    );

    // Binary magic numbers (signatures)
    private static final Map<String, byte[]> MAGIC_NUMBERS = Map.ofEntries(
        Map.entry("png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}),
        Map.entry("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        Map.entry("gif", new byte[]{'G', 'I', 'F'}),
        Map.entry("pdf", new byte[]{'%', 'P', 'D', 'F'}),
        Map.entry("zip", new byte[]{0x50, 0x4B}),
        Map.entry("rar", new byte[]{'R', 'a', 'r'}),
        Map.entry("7z", new byte[]{'7', 'z'}),
        Map.entry("class", new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE})
    );

    /**
     * Check if a file is binary by extension.
     */
    public static boolean isBinaryByExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return false;
        }

        String ext = filename.substring(dotIndex + 1).toLowerCase();
        return BINARY_EXTENSIONS.contains(ext);
    }

    /**
     * Check if a file is binary by content.
     */
    public static boolean isBinaryByContent(byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }

        // Check for null bytes in first 8KB
        int checkLength = Math.min(content.length, 8192);
        for (int i = 0; i < checkLength; i++) {
            if (content[i] == 0) {
                return true;
            }
        }

        // Check for high ratio of non-printable characters
        int nonPrintable = 0;
        for (int i = 0; i < checkLength; i++) {
            byte b = content[i];
            if (b < 32 && b != 9 && b != 10 && b != 13) {
                nonPrintable++;
            } else if (b == 127) {
                nonPrintable++;
            }
        }

        return (double) nonPrintable / checkLength > 0.3;
    }

    /**
     * Check if a file is binary.
     */
    public static boolean isBinary(Path path) {
        if (isBinaryByExtension(path.getFileName().toString())) {
            return true;
        }

        try {
            byte[] content = Files.readAllBytes(path);
            return isBinaryByContent(content);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if content is text.
     */
    public static boolean isText(byte[] content) {
        return !isBinaryByContent(content);
    }

    /**
     * Check if content is text.
     */
    public static boolean isText(String content) {
        if (content == null) return true;
        return isText(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Detect file type by magic number.
     */
    public static Optional<String> detectFileType(byte[] content) {
        if (content == null || content.length < 4) {
            return Optional.empty();
        }

        for (Map.Entry<String, byte[]> entry : MAGIC_NUMBERS.entrySet()) {
            byte[] magic = entry.getValue();
            if (content.length >= magic.length) {
                boolean matches = true;
                for (int i = 0; i < magic.length; i++) {
                    if (content[i] != magic[i]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return Optional.of(entry.getKey());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Get MIME type for a file.
     */
    public static String getMimeType(String filename) {
        if (filename == null) return "application/octet-stream";

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) return "application/octet-stream";

        String ext = filename.substring(dotIndex + 1).toLowerCase();

        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "txt", "md" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}