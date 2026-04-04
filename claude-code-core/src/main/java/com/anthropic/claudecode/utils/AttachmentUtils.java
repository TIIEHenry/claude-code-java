/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/attachments
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.file.*;

/**
 * Attachment utilities - File attachment handling.
 */
public final class AttachmentUtils {
    // Supported image extensions
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
        "png", "jpg", "jpeg", "gif", "webp", "svg", "bmp"
    );

    // Supported document extensions
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
        "pdf", "txt", "md", "csv", "json", "xml", "html"
    );

    // Max attachment size (20MB)
    private static final long MAX_ATTACHMENT_SIZE = 20 * 1024 * 1024;

    /**
     * Check if file is an image.
     */
    public static boolean isImage(String filename) {
        return hasExtension(filename, IMAGE_EXTENSIONS);
    }

    /**
     * Check if file is a document.
     */
    public static boolean isDocument(String filename) {
        return hasExtension(filename, DOCUMENT_EXTENSIONS);
    }

    /**
     * Check if file is attachable.
     */
    public static boolean isAttachable(String filename) {
        return isImage(filename) || isDocument(filename);
    }

    /**
     * Check if file size is within limits.
     */
    public static boolean isWithinSizeLimit(Path path) {
        try {
            long size = Files.size(path);
            return size <= MAX_ATTACHMENT_SIZE;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get attachment type.
     */
    public static AttachmentType getType(String filename) {
        if (isImage(filename)) return AttachmentType.IMAGE;
        if (isDocument(filename)) return AttachmentType.DOCUMENT;
        return AttachmentType.OTHER;
    }

    /**
     * Get MIME type for attachment.
     */
    public static String getMimeType(String filename) {
        return BinaryCheckUtils.getMimeType(filename);
    }

    /**
     * Create attachment info.
     */
    public static AttachmentInfo createAttachmentInfo(Path path) {
        try {
            String filename = path.getFileName().toString();
            String mimeType = getMimeType(filename);
            long size = Files.size(path);
            AttachmentType type = getType(filename);

            return new AttachmentInfo(
                filename,
                path.toString(),
                mimeType,
                size,
                type,
                isWithinSizeLimit(path)
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read attachment content.
     */
    public static byte[] readContent(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read attachment as base64.
     */
    public static String readAsBase64(Path path) {
        byte[] content = readContent(path);
        if (content == null) return null;
        return Base64.getEncoder().encodeToString(content);
    }

    /**
     * Read attachment as text.
     */
    public static String readAsText(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Format attachment size.
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private static boolean hasExtension(String filename, Set<String> extensions) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return false;
        }

        String ext = filename.substring(dotIndex + 1).toLowerCase();
        return extensions.contains(ext);
    }

    /**
     * Attachment type enum.
     */
    public enum AttachmentType {
        IMAGE,
        DOCUMENT,
        OTHER
    }

    /**
     * Attachment info record.
     */
    public record AttachmentInfo(
        String filename,
        String path,
        String mimeType,
        long size,
        AttachmentType type,
        boolean withinSizeLimit
    ) {
        public String formattedSize() {
            return formatSize(size);
        }
    }
}