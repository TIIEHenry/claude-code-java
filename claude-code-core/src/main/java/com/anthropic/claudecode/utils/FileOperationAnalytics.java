/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code file operation analytics
 */
package com.anthropic.claudecode.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * File operation analytics for logging file operations.
 */
public final class FileOperationAnalytics {
    private FileOperationAnalytics() {}

    // Maximum content size to hash (100KB)
    private static final int MAX_CONTENT_HASH_SIZE = 100 * 1024;

    /**
     * Creates a truncated SHA256 hash (16 chars) for file paths.
     * Used for privacy-preserving analytics on file operations.
     */
    public static String hashFilePath(String filePath) {
        return hashTruncated(filePath, 16);
    }

    /**
     * Creates a full SHA256 hash (64 chars) for file contents.
     * Used for deduplication and change detection analytics.
     */
    public static String hashFileContent(String content) {
        return hashFull(content);
    }

    /**
     * Compute SHA256 hash and return truncated hex string.
     */
    private static String hashTruncated(String input, int length) {
        String fullHash = hashFull(input);
        return fullHash.substring(0, Math.min(length, fullHash.length()));
    }

    /**
     * Compute SHA256 hash and return full hex string.
     */
    private static String hashFull(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Log file operation analytics.
     */
    public static void logFileOperation(FileOperationParams params) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("operation", params.operation);
        metadata.put("tool", params.tool);
        metadata.put("filePathHash", hashFilePath(params.filePath));

        // Only hash content if it's provided and below size limit
        if (params.content != null && params.content.length() <= MAX_CONTENT_HASH_SIZE) {
            metadata.put("contentHash", hashFileContent(params.content));
        }

        if (params.type != null) {
            metadata.put("type", params.type);
        }

        Analytics.logEvent("tengu_file_operation", metadata);
    }

    /**
     * File operation parameters.
     */
    public record FileOperationParams(
            String operation,
            String tool,
            String filePath,
            String content,
            String type
    ) {}
}