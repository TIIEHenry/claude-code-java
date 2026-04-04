/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/attachments
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.file.*;

/**
 * Attachments - Attachment handling utilities.
 */
public final class Attachments {
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
        "image/png", "image/jpeg", "image/gif", "image/webp"
    );
    private static final Set<String> SUPPORTED_DOC_TYPES = Set.of(
        "application/pdf", "text/plain", "text/markdown",
        "application/json", "text/csv"
    );
    private static final long MAX_ATTACHMENT_SIZE = 20 * 1024 * 1024; // 20MB

    /**
     * Attachment type enum.
     */
    public enum AttachmentType {
        IMAGE,
        DOCUMENT,
        FILE,
        URL,
        UNKNOWN
    }

    /**
     * Attachment info record.
     */
    public record AttachmentInfo(
        String id,
        String name,
        String path,
        AttachmentType type,
        String mimeType,
        long size,
        String content
    ) {
        public boolean isImage() {
            return type == AttachmentType.IMAGE;
        }

        public boolean isDocument() {
            return type == AttachmentType.DOCUMENT;
        }

        public boolean isSupported() {
            return SUPPORTED_IMAGE_TYPES.contains(mimeType) ||
                   SUPPORTED_DOC_TYPES.contains(mimeType);
        }
    }

    /**
     * Detect attachment type.
     */
    public static AttachmentType detectType(String mimeType) {
        if (SUPPORTED_IMAGE_TYPES.contains(mimeType)) {
            return AttachmentType.IMAGE;
        }
        if (SUPPORTED_DOC_TYPES.contains(mimeType)) {
            return AttachmentType.DOCUMENT;
        }
        if (mimeType != null && !mimeType.isEmpty()) {
            return AttachmentType.FILE;
        }
        return AttachmentType.UNKNOWN;
    }

    /**
     * Detect mime type from path.
     */
    public static String detectMimeType(String path) {
        if (path == null) return "application/octet-stream";

        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".md")) return "text/markdown";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".csv")) return "text/csv";
        if (lower.endsWith(".html")) return "text/html";
        if (lower.endsWith(".xml")) return "application/xml";

        return "application/octet-stream";
    }

    /**
     * Check if size is valid.
     */
    public static boolean isValidSize(long size) {
        return size > 0 && size <= MAX_ATTACHMENT_SIZE;
    }

    /**
     * Check if type is supported.
     */
    public static boolean isSupportedType(String mimeType) {
        return SUPPORTED_IMAGE_TYPES.contains(mimeType) ||
               SUPPORTED_DOC_TYPES.contains(mimeType);
    }

    /**
     * Create attachment from path.
     */
    public static AttachmentInfo fromPath(String path) {
        String mimeType = detectMimeType(path);
        AttachmentType type = detectType(mimeType);

        Path filePath = Paths.get(path);
        String name = filePath.getFileName().toString();
        long size = 0;
        try {
            size = Files.size(filePath);
        } catch (Exception e) {
            // Size unknown
        }

        return new AttachmentInfo(
            UUID.randomUUID().toString(),
            name,
            path,
            type,
            mimeType,
            size,
            null
        );
    }

    /**
     * Create attachment from content.
     */
    public static AttachmentInfo fromContent(String name, String content, String mimeType) {
        AttachmentType type = detectType(mimeType);

        return new AttachmentInfo(
            UUID.randomUUID().toString(),
            name,
            null,
            type,
            mimeType,
            content.length(),
            content
        );
    }

    /**
     * Attachment collection record.
     */
    public record AttachmentCollection(
        List<AttachmentInfo> attachments,
        int totalCount,
        long totalSize
    ) {
        public static AttachmentCollection empty() {
            return new AttachmentCollection(Collections.emptyList(), 0, 0);
        }

        public AttachmentCollection add(AttachmentInfo attachment) {
            List<AttachmentInfo> newList = new ArrayList<>(attachments);
            newList.add(attachment);
            return new AttachmentCollection(
                newList,
                totalCount + 1,
                totalSize + attachment.size()
            );
        }

        public List<AttachmentInfo> getImages() {
            return attachments.stream()
                .filter(AttachmentInfo::isImage)
                .toList();
        }

        public List<AttachmentInfo> getDocuments() {
            return attachments.stream()
                .filter(AttachmentInfo::isDocument)
                .toList();
        }
    }
}