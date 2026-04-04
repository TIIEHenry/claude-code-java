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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FsOperations.
 */
class FsOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("FsOperations ResolvedPath record")
    void resolvedPathRecord() {
        FsOperations.ResolvedPath path = new FsOperations.ResolvedPath("/resolved", true, true);
        assertEquals("/resolved", path.resolvedPath());
        assertTrue(path.isSymlink());
        assertTrue(path.isCanonical());
    }

    @Test
    @DisplayName("FsOperations ReadRangeResult record")
    void readRangeResultRecord() {
        FsOperations.ReadRangeResult result = new FsOperations.ReadRangeResult("content", 7, 100);
        assertEquals("content", result.content());
        assertEquals(7, result.bytesRead());
        assertEquals(100, result.bytesTotal());
    }

    @Test
    @DisplayName("FsOperations getFsImplementation returns implementation")
    void getFsImplementation() {
        FsOperations.FsOperationsImpl impl = FsOperations.getFsImplementation();
        assertNotNull(impl);
    }

    @Test
    @DisplayName("FsOperations setFsImplementation")
    void setFsImplementation() {
        FsOperations.FsOperationsImpl original = FsOperations.getFsImplementation();
        FsOperations.setFsImplementation(new FsOperations.DefaultFsOperations());
        assertNotNull(FsOperations.getFsImplementation());
        FsOperations.setFsImplementation(original);
    }

    @Test
    @DisplayName("FsOperations setOriginalFsImplementation")
    void setOriginalFsImplementation() {
        FsOperations.setOriginalFsImplementation();
        assertNotNull(FsOperations.getFsImplementation());
    }

    @Test
    @DisplayName("FsOperations safeResolvePath regular file")
    void safeResolvePathRegularFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(file, "content");

        FsOperations.ResolvedPath resolved = FsOperations.safeResolvePath(
            FsOperations.getFsImplementation(), file.toString()
        );

        assertNotNull(resolved);
        assertTrue(resolved.isCanonical());
    }

    @Test
    @DisplayName("FsOperations safeResolvePath non-existent")
    void safeResolvePathNonExistent() {
        FsOperations.ResolvedPath resolved = FsOperations.safeResolvePath(
            FsOperations.getFsImplementation(), "/nonexistent/path"
        );

        assertNotNull(resolved);
        assertFalse(resolved.isCanonical());
    }

    @Test
    @DisplayName("FsOperations isDuplicatePath")
    void isDuplicatePath() throws IOException {
        Path file = tempDir.resolve("duplicate.txt");
        java.nio.file.Files.writeString(file, "content");

        Set<String> loadedPaths = new HashSet<>();
        assertFalse(FsOperations.isDuplicatePath(
            FsOperations.getFsImplementation(), file.toString(), loadedPaths
        ));
        assertTrue(FsOperations.isDuplicatePath(
            FsOperations.getFsImplementation(), file.toString(), loadedPaths
        ));
    }

    @Test
    @DisplayName("FsOperations getPathsForPermissionCheck returns list")
    void getPathsForPermissionCheck() {
        List<String> paths = FsOperations.getPathsForPermissionCheck("/tmp/test");
        assertNotNull(paths);
        assertFalse(paths.isEmpty());
    }

    @Test
    @DisplayName("FsOperations getPathsForPermissionCheck tilde")
    void getPathsForPermissionCheckTilde() {
        List<String> paths = FsOperations.getPathsForPermissionCheck("~");
        assertNotNull(paths);
        assertTrue(paths.get(0).contains(System.getProperty("user.home")));
    }

    @Test
    @DisplayName("FsOperations readFileRange")
    void readFileRange() throws IOException {
        Path file = tempDir.resolve("range.txt");
        java.nio.file.Files.writeString(file, "Hello World");

        FsOperations.ReadRangeResult result = FsOperations.readFileRange(file.toString(), 0, 5);
        assertNotNull(result);
        assertEquals("Hello", result.content());
        assertEquals(5, result.bytesRead());
    }

    @Test
    @DisplayName("FsOperations readFileRange with offset")
    void readFileRangeOffset() throws IOException {
        Path file = tempDir.resolve("offset.txt");
        java.nio.file.Files.writeString(file, "Hello World");

        FsOperations.ReadRangeResult result = FsOperations.readFileRange(file.toString(), 6, 5);
        assertNotNull(result);
        assertEquals("World", result.content());
    }

    @Test
    @DisplayName("FsOperations tailFile")
    void tailFile() throws IOException {
        Path file = tempDir.resolve("tail.txt");
        java.nio.file.Files.writeString(file, "Hello World");

        FsOperations.ReadRangeResult result = FsOperations.tailFile(file.toString(), 5);
        assertNotNull(result);
        assertEquals("World", result.content());
    }

    @Test
    @DisplayName("FsOperations tailFile empty")
    void tailFileEmpty() throws IOException {
        Path file = tempDir.resolve("empty.txt");
        java.nio.file.Files.writeString(file, "");

        FsOperations.ReadRangeResult result = FsOperations.tailFile(file.toString(), 100);
        assertNotNull(result);
        assertEquals("", result.content());
    }

    @Test
    @DisplayName("FsOperations DefaultFsOperations cwd")
    void defaultFsCwd() {
        FsOperations.DefaultFsOperations fs = new FsOperations.DefaultFsOperations();
        assertNotNull(fs.cwd());
    }

    @Test
    @DisplayName("FsOperations DefaultFsOperations existsSync")
    void defaultFsExistsSync() throws IOException {
        FsOperations.DefaultFsOperations fs = new FsOperations.DefaultFsOperations();
        Path file = tempDir.resolve("exists.txt");
        java.nio.file.Files.writeString(file, "content");

        assertTrue(fs.existsSync(file.toString()));
        assertFalse(fs.existsSync("/nonexistent"));
    }

    @Test
    @DisplayName("FsOperations DefaultFsOperations size")
    void defaultFsSize() throws IOException {
        FsOperations.DefaultFsOperations fs = new FsOperations.DefaultFsOperations();
        Path file = tempDir.resolve("size.txt");
        java.nio.file.Files.writeString(file, "12345");

        assertEquals(5, fs.size(file.toString()));
    }

    @Test
    @DisplayName("FsOperations DefaultFsOperations readFileSync")
    void defaultFsReadFileSync() throws IOException {
        FsOperations.DefaultFsOperations fs = new FsOperations.DefaultFsOperations();
        Path file = tempDir.resolve("read.txt");
        java.nio.file.Files.writeString(file, "content");

        assertEquals("content", fs.readFileSync(file.toString(), "UTF-8"));
    }

    @Test
    @DisplayName("FsOperations DefaultFsOperations writeFileSync")
    void defaultFsWriteFileSync() throws IOException {
        FsOperations.DefaultFsOperations fs = new FsOperations.DefaultFsOperations();
        Path file = tempDir.resolve("write.txt");

        fs.writeFileSync(file.toString(), "written");
        assertEquals("written", java.nio.file.Files.readString(file));
    }

    @Test
    @DisplayName("FsOperations DefaultFsOperations mkdirSync")
    void defaultFsMkdirSync() throws IOException {
        FsOperations.DefaultFsOperations fs = new FsOperations.DefaultFsOperations();
        Path dir = tempDir.resolve("newdir");

        fs.mkdirSync(dir.toString());
        assertTrue(java.nio.file.Files.isDirectory(dir));
    }

    @Test
    @DisplayName("FsOperations DefaultFsOperations readdirSync")
    void defaultFsReaddirSync() throws IOException {
        FsOperations.DefaultFsOperations fs = new FsOperations.DefaultFsOperations();
        java.nio.file.Files.writeString(tempDir.resolve("a.txt"), "");
        java.nio.file.Files.writeString(tempDir.resolve("b.txt"), "");

        List<String> files = fs.readdirSync(tempDir.toString());
        assertTrue(files.size() >= 2);
    }
}