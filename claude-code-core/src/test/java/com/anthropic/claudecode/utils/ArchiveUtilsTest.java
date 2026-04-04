/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArchiveUtils.
 */
class ArchiveUtilsTest {

    @Test
    @DisplayName("ArchiveUtils detectArchiveType detects ZIP")
    void detectZip() {
        assertEquals(ArchiveUtils.ArchiveType.ZIP,
            ArchiveUtils.detectArchiveType(Path.of("test.zip")));
    }

    @Test
    @DisplayName("ArchiveUtils detectArchiveType detects TAR")
    void detectTar() {
        assertEquals(ArchiveUtils.ArchiveType.TAR,
            ArchiveUtils.detectArchiveType(Path.of("test.tar")));
    }

    @Test
    @DisplayName("ArchiveUtils detectArchiveType detects TAR_GZ")
    void detectTarGz() {
        assertEquals(ArchiveUtils.ArchiveType.TAR_GZ,
            ArchiveUtils.detectArchiveType(Path.of("test.tar.gz")));
        assertEquals(ArchiveUtils.ArchiveType.TAR_GZ,
            ArchiveUtils.detectArchiveType(Path.of("test.tgz")));
    }

    @Test
    @DisplayName("ArchiveUtils detectArchiveType detects TAR_BZ2")
    void detectTarBz2() {
        assertEquals(ArchiveUtils.ArchiveType.TAR_BZ2,
            ArchiveUtils.detectArchiveType(Path.of("test.tar.bz2")));
        assertEquals(ArchiveUtils.ArchiveType.TAR_BZ2,
            ArchiveUtils.detectArchiveType(Path.of("test.tbz2")));
    }

    @Test
    @DisplayName("ArchiveUtils detectArchiveType detects GZIP")
    void detectGzip() {
        assertEquals(ArchiveUtils.ArchiveType.GZIP,
            ArchiveUtils.detectArchiveType(Path.of("test.gz")));
    }

    @Test
    @DisplayName("ArchiveUtils detectArchiveType returns UNKNOWN for others")
    void detectUnknown() {
        assertEquals(ArchiveUtils.ArchiveType.UNKNOWN,
            ArchiveUtils.detectArchiveType(Path.of("test.txt")));
    }

    @Test
    @DisplayName("ArchiveUtils createZip creates ZIP file")
    void createZip() throws IOException {
        Path tempDir = Files.createTempDirectory("archive-test");
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "test content");

        Path zipFile = tempDir.resolve("test.zip");
        ArchiveUtils.createZip(tempDir, zipFile);

        assertTrue(Files.exists(zipFile));
        assertTrue(zipFile.toFile().length() > 0);

        // Cleanup
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(sourceFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("ArchiveUtils listZipContents lists contents")
    void listZipContents() throws IOException {
        Path tempDir = Files.createTempDirectory("archive-test");
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "test content");

        Path zipFile = tempDir.resolve("test.zip");
        ArchiveUtils.createZip(tempDir, zipFile);

        List<String> contents = ArchiveUtils.listZipContents(zipFile);

        assertFalse(contents.isEmpty());

        // Cleanup
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(sourceFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("ArchiveUtils extractZip extracts files")
    void extractZip() throws IOException {
        Path tempDir = Files.createTempDirectory("archive-test");
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "test content");

        Path zipFile = tempDir.resolve("test.zip");
        ArchiveUtils.createZipFromFiles(List.of(sourceFile), zipFile);

        Path extractDir = Files.createTempDirectory("extract-test");
        ArchiveUtils.extractZip(zipFile, extractDir);

        assertTrue(Files.exists(extractDir.resolve("test.txt")));
        assertEquals("test content", Files.readString(extractDir.resolve("test.txt")));

        // Cleanup - delete files first, then directories
        Files.deleteIfExists(extractDir.resolve("test.txt"));
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(sourceFile);
        Files.deleteIfExists(extractDir);
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("ArchiveUtils createZipFromFiles creates from file list")
    void createZipFromFiles() throws IOException {
        Path tempDir = Files.createTempDirectory("archive-test");
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        Path zipFile = tempDir.resolve("multi.zip");
        ArchiveUtils.createZipFromFiles(List.of(file1, file2), zipFile);

        assertTrue(Files.exists(zipFile));

        // Cleanup
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(file1);
        Files.deleteIfExists(file2);
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("ArchiveUtils isValidZip checks validity")
    void isValidZip() throws IOException {
        Path tempDir = Files.createTempDirectory("archive-test");
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "test");

        Path zipFile = tempDir.resolve("test.zip");
        ArchiveUtils.createZip(tempDir, zipFile);

        assertTrue(ArchiveUtils.isValidZip(zipFile));
        assertFalse(ArchiveUtils.isValidZip(sourceFile));

        // Cleanup
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(sourceFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("ArchiveUtils gzipFile compresses file")
    void gzipFile() throws IOException {
        Path tempDir = Files.createTempDirectory("gzip-test");
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "test content for gzip compression");

        Path gzipFile = ArchiveUtils.gzipFile(sourceFile);

        assertTrue(Files.exists(gzipFile));
        assertTrue(gzipFile.toString().endsWith(".gz"));

        // Cleanup
        Files.deleteIfExists(gzipFile);
        Files.deleteIfExists(sourceFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("ArchiveUtils gunzipFile decompresses file")
    void gunzipFile() throws IOException {
        Path tempDir = Files.createTempDirectory("gzip-test");
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "test content");

        Path gzipFile = ArchiveUtils.gzipFile(sourceFile);
        Files.deleteIfExists(sourceFile); // Remove original

        Path decompressed = ArchiveUtils.gunzipFile(gzipFile);

        assertTrue(Files.exists(decompressed));
        assertEquals("test content", Files.readString(decompressed));

        // Cleanup
        Files.deleteIfExists(decompressed);
        Files.deleteIfExists(gzipFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("ArchiveUtils ArchiveType enum values")
    void archiveTypeEnum() {
        ArchiveUtils.ArchiveType[] values = ArchiveUtils.ArchiveType.values();

        assertTrue(values.length >= 6);
        assertEquals(ArchiveUtils.ArchiveType.ZIP, ArchiveUtils.ArchiveType.valueOf("ZIP"));
    }
}