/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TempFile.
 */
class TempFileTest {

    @Test
    @DisplayName("TempFile generateTempFilePath returns valid path")
    void generateTempFilePathDefault() {
        String result = TempFile.generateTempFilePath();

        assertNotNull(result);
        assertTrue(result.contains("claude-prompt"));
        assertTrue(result.endsWith(".md"));
        assertTrue(result.contains(System.getProperty("java.io.tmpdir")));
    }

    @Test
    @DisplayName("TempFile generateTempFilePath with prefix")
    void generateTempFilePathWithPrefix() {
        String result = TempFile.generateTempFilePath("my-prefix");

        assertTrue(result.contains("my-prefix"));
        assertTrue(result.endsWith(".md"));
    }

    @Test
    @DisplayName("TempFile generateTempFilePath with extension")
    void generateTempFilePathWithExtension() {
        String result = TempFile.generateTempFilePath("prefix", ".txt", null);

        assertTrue(result.contains("prefix"));
        assertTrue(result.endsWith(".txt"));
    }

    @Test
    @DisplayName("TempFile generateTempFilePath null prefix uses default")
    void generateTempFilePathNullPrefix() {
        String result = TempFile.generateTempFilePath(null, ".txt", null);

        assertTrue(result.contains("claude-prompt"));
        assertTrue(result.endsWith(".txt"));
    }

    @Test
    @DisplayName("TempFile generateTempFilePath null extension uses default")
    void generateTempFilePathNullExtension() {
        String result = TempFile.generateTempFilePath("prefix", null, null);

        assertTrue(result.endsWith(".md"));
    }

    @Test
    @DisplayName("TempFile generateStableTempFilePath with content hash")
    void generateStableTempFilePath() {
        String result1 = TempFile.generateStableTempFilePath("content-hash");
        String result2 = TempFile.generateStableTempFilePath("content-hash");

        // Same content hash should produce same path
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("TempFile generateStableTempFilePath different hash different path")
    void generateStableTempFilePathDifferent() {
        String result1 = TempFile.generateStableTempFilePath("hash1");
        String result2 = TempFile.generateStableTempFilePath("hash2");

        // Different content hash should produce different paths
        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("TempFile generateStableTempFilePath with prefix")
    void generateStableTempFilePathWithPrefix() {
        String result = TempFile.generateStableTempFilePath("my-prefix", "content-hash");

        assertTrue(result.contains("my-prefix"));
        assertTrue(result.endsWith(".md"));
    }

    @Test
    @DisplayName("TempFile generateTempFilePath with content hash is stable")
    void generateTempFilePathWithHash() {
        String result1 = TempFile.generateTempFilePath("prefix", ".txt", "hash");
        String result2 = TempFile.generateTempFilePath("prefix", ".txt", "hash");

        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("TempFile generateTempFilePath without hash is random")
    void generateTempFilePathRandom() {
        String result1 = TempFile.generateTempFilePath("prefix", ".txt", null);
        String result2 = TempFile.generateTempFilePath("prefix", ".txt", null);

        // Should generate different UUIDs
        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("TempFile generateTempFilePath empty hash treated as null")
    void generateTempFilePathEmptyHash() {
        String result1 = TempFile.generateTempFilePath("prefix", ".txt", "");
        String result2 = TempFile.generateTempFilePath("prefix", ".txt", "");

        // Empty hash treated as null, should be random
        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("TempFile path contains temp directory")
    void tempDirInPath() {
        String result = TempFile.generateTempFilePath();
        String tempDir = System.getProperty("java.io.tmpdir");

        assertTrue(result.startsWith(tempDir));
    }

    @Test
    @DisplayName("TempFile path is valid path format")
    void validPathFormat() {
        String result = TempFile.generateTempFilePath("test", ".md", "hash");

        // Should be parseable as a path
        assertNotNull(Paths.get(result));
    }

    @Test
    @DisplayName("TempFile content hash produces consistent identifier")
    void contentHashConsistent() {
        // The hash should produce the same 16 character hex string
        String result = TempFile.generateTempFilePath("test", ".txt", "test-content");

        // Extract the identifier part (between prefix and extension)
        int start = result.indexOf("test-") + 5;
        int end = result.indexOf(".txt");
        String identifier = result.substring(start, end);

        // Identifier should be 16 hex characters (8 bytes)
        assertEquals(16, identifier.length());
        assertTrue(identifier.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("TempFile different content different hash")
    void differentContentDifferentHash() {
        String result1 = TempFile.generateTempFilePath("test", ".txt", "content1");
        String result2 = TempFile.generateTempFilePath("test", ".txt", "content2");

        // Extract identifiers
        String id1 = extractIdentifier(result1);
        String id2 = extractIdentifier(result2);

        assertNotEquals(id1, id2);
    }

    private String extractIdentifier(String path) {
        int start = path.indexOf("test-") + 5;
        int end = path.indexOf(".txt");
        return path.substring(start, end);
    }
}