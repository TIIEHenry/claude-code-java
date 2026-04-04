/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileUtils.
 */
class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("getExtension extracts file extension")
    void getExtensionWorks() {
        assertEquals(".txt", FileUtils.getExtension("file.txt"));
        assertEquals(".java", FileUtils.getExtension("Test.java"));
        assertEquals(".json", FileUtils.getExtension("/path/to/file.json"));
        assertEquals("", FileUtils.getExtension("noextension"));
        assertEquals("", FileUtils.getExtension(null));
        assertEquals("", FileUtils.getExtension(""));
    }

    @Test
    @DisplayName("getBaseName extracts name without extension")
    void getBaseNameWorks() {
        assertEquals("file", FileUtils.getBaseName("file.txt"));
        assertEquals("Test", FileUtils.getBaseName("Test.java"));
        assertEquals("config", FileUtils.getBaseName("/path/to/config.json"));
        assertEquals("noext", FileUtils.getBaseName("noext"));
    }

    @Test
    @DisplayName("getFileName extracts file name")
    void getFileNameWorks() {
        assertEquals("file.txt", FileUtils.getFileName("file.txt"));
        assertEquals("Test.java", FileUtils.getFileName("/path/to/Test.java"));
        assertEquals("", FileUtils.getFileName(null));
    }

    @Test
    @DisplayName("getParent extracts parent directory")
    void getParentWorks() {
        assertEquals("/path/to", FileUtils.getParent("/path/to/file.txt"));
        assertEquals("", FileUtils.getParent("file.txt"));
        assertEquals("", FileUtils.getParent(null));
    }

    @Test
    @DisplayName("isTextFile detects text extensions")
    void isTextFileWorks() {
        assertTrue(FileUtils.isTextFile("file.txt"));
        assertTrue(FileUtils.isTextFile("file.md"));
        assertTrue(FileUtils.isTextFile("file.java"));
        assertTrue(FileUtils.isTextFile("file.json"));
        assertFalse(FileUtils.isTextFile("file.exe"));
        assertFalse(FileUtils.isTextFile("file.png"));
        assertFalse(FileUtils.isTextFile("noextension"));
    }

    @Test
    @DisplayName("isBinaryFile detects binary extensions")
    void isBinaryFileWorks() {
        assertTrue(FileUtils.isBinaryFile("file.exe"));
        assertTrue(FileUtils.isBinaryFile("file.dll"));
        assertTrue(FileUtils.isBinaryFile("file.zip"));
        assertTrue(FileUtils.isBinaryFile("file.png"));
        assertTrue(FileUtils.isBinaryFile("file.pdf"));
        assertFalse(FileUtils.isBinaryFile("file.txt"));
        assertFalse(FileUtils.isBinaryFile("file.java"));
    }

    @Test
    @DisplayName("formatSize formats file sizes correctly")
    void formatSizeWorks() {
        assertEquals("500 B", FileUtils.formatSize(500));
        assertTrue(FileUtils.formatSize(1024).contains("KB"));
        assertTrue(FileUtils.formatSize(2_000_000).contains("MB"));
        assertTrue(FileUtils.formatSize(2_000_000_000).contains("GB"));
    }

    @Test
    @DisplayName("writeFile and readFile work together")
    void writeAndReadWorks() throws IOException {
        Path file = tempDir.resolve("test.txt");
        String content = "Hello, World!\nTest content.";

        FileUtils.writeFile(file.toString(), content);
        assertTrue(FileUtils.exists(file.toString()));

        String read = FileUtils.readFile(file.toString());
        assertEquals(content, read);
    }

    @Test
    @DisplayName("writeBytes and readBytes work together")
    void writeAndReadBytesWorks() throws IOException {
        Path file = tempDir.resolve("test.bin");
        byte[] content = new byte[]{0x01, 0x02, 0x03, 0x04};

        FileUtils.writeBytes(file.toString(), content);
        assertTrue(FileUtils.exists(file.toString()));

        byte[] read = FileUtils.readBytes(file.toString());
        assertArrayEquals(content, read);
    }

    @Test
    @DisplayName("delete removes file")
    void deleteWorks() throws IOException {
        Path file = tempDir.resolve("todelete.txt");
        FileUtils.writeFile(file.toString(), "content");

        assertTrue(FileUtils.exists(file.toString()));
        FileUtils.delete(file.toString());
        assertFalse(FileUtils.exists(file.toString()));
    }

    @Test
    @DisplayName("mkdir creates directories")
    void mkdirWorks() throws IOException {
        Path dir = tempDir.resolve("newdir/nested");

        FileUtils.mkdir(dir.toString());
        assertTrue(FileUtils.isDirectory(dir.toString()));
    }

    @Test
    @DisplayName("copy copies file")
    void copyWorks() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");

        FileUtils.writeFile(source.toString(), "content");
        FileUtils.copy(source.toString(), target.toString());

        assertTrue(FileUtils.exists(target.toString()));
        assertEquals("content", FileUtils.readFile(target.toString()));
    }

    @Test
    @DisplayName("move moves file")
    void moveWorks() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");

        FileUtils.writeFile(source.toString(), "content");
        FileUtils.move(source.toString(), target.toString());

        assertFalse(FileUtils.exists(source.toString()));
        assertTrue(FileUtils.exists(target.toString()));
        assertEquals("content", FileUtils.readFile(target.toString()));
    }

    @Test
    @DisplayName("readLines and writeLines work together")
    void readWriteLinesWorks() throws IOException {
        Path file = tempDir.resolve("lines.txt");
        List<String> lines = List.of("line1", "line2", "line3");

        FileUtils.writeLines(file.toString(), lines);
        List<String> read = FileUtils.readLines(file.toString());

        assertEquals(lines, read);
    }

    @Test
    @DisplayName("append adds content to file")
    void appendWorks() throws IOException {
        Path file = tempDir.resolve("append.txt");

        FileUtils.writeFile(file.toString(), "initial");
        FileUtils.append(file.toString(), " appended");

        assertEquals("initial appended", FileUtils.readFile(file.toString()));
    }

    @Test
    @DisplayName("getAbsolutePath returns absolute path")
    void getAbsolutePathWorks() {
        String abs = FileUtils.getAbsolutePath("test.txt");
        assertTrue(abs.contains("test.txt"));
        // On Mac/Linux, absolute path starts with /
        assertTrue(abs.startsWith("/") || abs.startsWith("C:") || abs.startsWith("D:"));
    }

    @Test
    @DisplayName("normalize normalizes path")
    void normalizeWorks() {
        assertEquals("a/b", FileUtils.normalize("a/./b"));
        assertEquals("a", FileUtils.normalize("a/b/.."));
    }

    @Test
    @DisplayName("exists returns correct status")
    void existsWorks() throws IOException {
        Path existing = tempDir.resolve("exists.txt");
        FileUtils.writeFile(existing.toString(), "content");

        assertTrue(FileUtils.exists(existing.toString()));
        assertFalse(FileUtils.exists(tempDir.resolve("nonexistent.txt").toString()));
    }

    @Test
    @DisplayName("isDirectory and isFile work correctly")
    void isDirectoryAndFileWorks() throws IOException {
        Path file = tempDir.resolve("file.txt");
        FileUtils.writeFile(file.toString(), "content");

        assertTrue(FileUtils.isFile(file.toString()));
        assertFalse(FileUtils.isDirectory(file.toString()));
        assertTrue(FileUtils.isDirectory(tempDir.toString()));
        assertFalse(FileUtils.isFile(tempDir.toString()));
    }

    @Test
    @DisplayName("getSize returns file size")
    void getSizeWorks() throws IOException {
        Path file = tempDir.resolve("size.txt");
        String content = "1234567890";  // 10 bytes
        FileUtils.writeFile(file.toString(), content);

        assertEquals(10, FileUtils.getSize(file.toString()));
    }
}