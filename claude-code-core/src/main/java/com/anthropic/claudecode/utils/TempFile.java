/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/tempfile.ts
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.security.*;
import java.util.UUID;

/**
 * Temporary file utilities.
 */
public final class TempFile {
    private TempFile() {}

    /**
     * Generate a temporary file path.
     *
     * @param prefix Optional prefix for the temp file name
     * @param extension Optional file extension (defaults to ".md")
     * @param contentHash When provided, the identifier is derived from a SHA-256 hash
     * @return Temp file path
     */
    public static String generateTempFilePath(String prefix, String extension, String contentHash) {
        String id;
        if (contentHash != null && !contentHash.isEmpty()) {
            id = hashContent(contentHash);
        } else {
            id = UUID.randomUUID().toString();
        }

        String actualPrefix = prefix != null ? prefix : "claude-prompt";
        String actualExtension = extension != null ? extension : ".md";

        String tempDir = System.getProperty("java.io.tmpdir");
        return Paths.get(tempDir, actualPrefix + "-" + id + actualExtension).toString();
    }

    /**
     * Generate a temporary file path with default settings.
     */
    public static String generateTempFilePath() {
        return generateTempFilePath(null, null, null);
    }

    /**
     * Generate a temporary file path with prefix.
     */
    public static String generateTempFilePath(String prefix) {
        return generateTempFilePath(prefix, null, null);
    }

    /**
     * Generate a stable temporary file path based on content hash.
     * This produces a path that is stable across process boundaries.
     */
    public static String generateStableTempFilePath(String contentHash) {
        return generateTempFilePath(null, null, contentHash);
    }

    /**
     * Generate a stable temporary file path based on content hash with prefix.
     */
    public static String generateStableTempFilePath(String prefix, String contentHash) {
        return generateTempFilePath(prefix, null, contentHash);
    }

    private static String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}