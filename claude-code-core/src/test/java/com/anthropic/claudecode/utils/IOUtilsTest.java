/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IOUtils.
 */
class IOUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("IOUtils readAllBytes")
    void readAllBytes() throws Exception {
        byte[] data = "Hello, World!".getBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        byte[] result = IOUtils.readAllBytes(bis);
        assertArrayEquals(data, result);
    }

    @Test
    @DisplayName("IOUtils readAllLines")
    void readAllLines() throws Exception {
        String text = "Line 1\nLine 2\nLine 3";
        ByteArrayInputStream bis = new ByteArrayInputStream(text.getBytes());

        List<String> lines = IOUtils.readAllLines(bis);
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
    }

    @Test
    @DisplayName("IOUtils readString")
    void readString() throws Exception {
        String text = "Test content";
        ByteArrayInputStream bis = new ByteArrayInputStream(text.getBytes());

        String result = IOUtils.readString(bis);
        assertEquals(text, result);
    }

    @Test
    @DisplayName("IOUtils writeString")
    void writeString() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.writeString(bos, "Test");

        assertEquals("Test", bos.toString());
    }

    @Test
    @DisplayName("IOUtils writeBytes")
    void writeBytes() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.writeBytes(bos, new byte[]{1, 2, 3});

        assertArrayEquals(new byte[]{1, 2, 3}, bos.toByteArray());
    }

    @Test
    @DisplayName("IOUtils copy")
    void copy() throws Exception {
        byte[] data = "Source data".getBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        long copied = IOUtils.copy(bis, bos);
        assertEquals(data.length, copied);
        assertArrayEquals(data, bos.toByteArray());
    }

    @Test
    @DisplayName("IOUtils closeQuietly")
    void closeQuietly() {
        assertDoesNotThrow(() -> IOUtils.closeQuietly((Closeable) null));
    }

    @Test
    @DisplayName("IOUtils closeQuietly multiple")
    void closeQuietlyMultiple() {
        assertDoesNotThrow(() -> IOUtils.closeQuietly(null, null, null));
    }

    @Test
    @DisplayName("IOUtils readFileToString")
    void readFileToString() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "File content");

        String result = IOUtils.readFileToString(file);
        assertEquals("File content", result);
    }

    @Test
    @DisplayName("IOUtils readFileToBytes")
    void readFileToBytes() throws Exception {
        Path file = tempDir.resolve("test.bin");
        Files.write(file, new byte[]{1, 2, 3, 4, 5});

        byte[] result = IOUtils.readFileToBytes(file);
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    @DisplayName("IOUtils readFileToLines")
    void readFileToLines() throws Exception {
        Path file = tempDir.resolve("lines.txt");
        Files.writeString(file, "A\nB\nC");

        List<String> lines = IOUtils.readFileToLines(file);
        assertEquals(3, lines.size());
    }

    @Test
    @DisplayName("IOUtils writeStringToFile")
    void writeStringToFile() throws Exception {
        Path file = tempDir.resolve("output.txt");
        IOUtils.writeStringToFile(file, "Written content");

        assertEquals("Written content", Files.readString(file));
    }

    @Test
    @DisplayName("IOUtils writeBytesToFile")
    void writeBytesToFile() throws Exception {
        Path file = tempDir.resolve("output.bin");
        IOUtils.writeBytesToFile(file, new byte[]{10, 20, 30});

        assertArrayEquals(new byte[]{10, 20, 30}, Files.readAllBytes(file));
    }

    @Test
    @DisplayName("IOUtils writeLinesToFile")
    void writeLinesToFile() throws Exception {
        Path file = tempDir.resolve("lines.txt");
        IOUtils.writeLinesToFile(file, List.of("X", "Y", "Z"));

        assertEquals("X\nY\nZ\n", Files.readString(file));
    }

    @Test
    @DisplayName("IOUtils appendStringToFile")
    void appendStringToFile() throws Exception {
        Path file = tempDir.resolve("append.txt");
        IOUtils.writeStringToFile(file, "Initial");
        IOUtils.appendStringToFile(file, " Appended");

        assertEquals("Initial Appended", Files.readString(file));
    }

    @Test
    @DisplayName("IOUtils fileExists")
    void fileExists() throws Exception {
        Path file = tempDir.resolve("exists.txt");
        assertFalse(IOUtils.fileExists(file));

        Files.writeString(file, "content");
        assertTrue(IOUtils.fileExists(file));
    }

    @Test
    @DisplayName("IOUtils directoryExists")
    void directoryExists() {
        assertTrue(IOUtils.directoryExists(tempDir));
        assertFalse(IOUtils.directoryExists(tempDir.resolve("nonexistent")));
    }

    @Test
    @DisplayName("IOUtils createDirectoryIfNotExists")
    void createDirectoryIfNotExists() throws Exception {
        Path dir = tempDir.resolve("newdir");
        assertFalse(Files.exists(dir));

        IOUtils.createDirectoryIfNotExists(dir);
        assertTrue(Files.isDirectory(dir));
    }

    @Test
    @DisplayName("IOUtils deleteFileIfExists")
    void deleteFileIfExists() throws Exception {
        Path file = tempDir.resolve("todelete.txt");
        Files.writeString(file, "content");

        assertTrue(IOUtils.deleteFileIfExists(file));
        assertFalse(Files.exists(file));
        assertFalse(IOUtils.deleteFileIfExists(file)); // Already deleted
    }

    @Test
    @DisplayName("IOUtils getFileExtension")
    void getFileExtension() {
        assertEquals("txt", IOUtils.getFileExtension(Path.of("file.txt")));
        assertEquals("java", IOUtils.getFileExtension(Path.of("Test.java")));
        assertEquals("", IOUtils.getFileExtension(Path.of("noext")));
    }

    @Test
    @DisplayName("IOUtils getFileNameWithoutExtension")
    void getFileNameWithoutExtension() {
        assertEquals("file", IOUtils.getFileNameWithoutExtension(Path.of("file.txt")));
        assertEquals("Test", IOUtils.getFileNameWithoutExtension(Path.of("Test.java")));
        assertEquals("noext", IOUtils.getFileNameWithoutExtension(Path.of("noext")));
    }

    @Test
    @DisplayName("IOUtils getFileSize")
    void getFileSize() throws Exception {
        Path file = tempDir.resolve("size.txt");
        Files.writeString(file, "12345");

        assertEquals(5, IOUtils.getFileSize(file));
    }

    @Test
    @DisplayName("IOUtils formatFileSize")
    void formatFileSize() {
        assertEquals("500 B", IOUtils.formatFileSize(500));
        assertEquals("1.0 KB", IOUtils.formatFileSize(1024));
        assertEquals("1.5 KB", IOUtils.formatFileSize(1536));
        assertEquals("1.0 MB", IOUtils.formatFileSize(1024 * 1024));
    }

    @Test
    @DisplayName("IOUtils getTempDirectory")
    void getTempDirectory() {
        assertNotNull(IOUtils.getTempDirectory());
    }

    @Test
    @DisplayName("IOUtils createTempFile")
    void createTempFile() throws Exception {
        Path temp = IOUtils.createTempFile("test", ".tmp");
        assertTrue(Files.exists(temp));
        Files.delete(temp);
    }

    @Test
    @DisplayName("IOUtils createTempDirectory")
    void createTempDirectory() throws Exception {
        Path temp = IOUtils.createTempDirectory("testdir");
        assertTrue(Files.isDirectory(temp));
        Files.delete(temp);
    }

    @Test
    @DisplayName("IOUtils listFiles")
    void listFiles() throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "");
        Files.writeString(tempDir.resolve("b.txt"), "");
        Files.createDirectory(tempDir.resolve("subdir"));

        List<Path> files = IOUtils.listFiles(tempDir);
        assertEquals(2, files.size());
    }

    @Test
    @DisplayName("IOUtils listDirectories")
    void listDirectories() throws Exception {
        Files.writeString(tempDir.resolve("file.txt"), "");
        Files.createDirectory(tempDir.resolve("dir1"));
        Files.createDirectory(tempDir.resolve("dir2"));

        List<Path> dirs = IOUtils.listDirectories(tempDir);
        assertEquals(2, dirs.size());
    }

    @Test
    @DisplayName("IOUtils touchFile creates file")
    void touchFileCreates() throws Exception {
        Path file = tempDir.resolve("touched.txt");
        assertFalse(Files.exists(file));

        IOUtils.touchFile(file);
        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("IOUtils touchFile updates mtime")
    void touchFileUpdatesMtime() throws Exception {
        Path file = tempDir.resolve("touched2.txt");
        Files.writeString(file, "content");
        long oldTime = Files.getLastModifiedTime(file).toMillis();

        Thread.sleep(100);
        IOUtils.touchFile(file);

        assertTrue(Files.getLastModifiedTime(file).toMillis() > oldTime);
    }
}