/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PathUtil.
 */
class PathUtilTest {

    @Test
    @DisplayName("PathUtil expandPath expands tilde")
    void expandPathTilde() {
        String result = PathUtil.expandPath("~");

        assertEquals(System.getProperty("user.home"), result);
    }

    @Test
    @DisplayName("PathUtil expandPath expands tilde with path")
    void expandPathTildeWithPath() {
        String result = PathUtil.expandPath("~/Documents");

        assertTrue(result.startsWith(System.getProperty("user.home")));
        assertTrue(result.contains("Documents"));
    }

    @Test
    @DisplayName("PathUtil expandPath null throws")
    void expandPathNull() {
        assertThrows(IllegalArgumentException.class, () -> PathUtil.expandPath(null));
    }

    @Test
    @DisplayName("PathUtil expandPath empty returns cwd")
    void expandPathEmpty() {
        String result = PathUtil.expandPath("");

        assertEquals(System.getProperty("user.dir"), result);
    }

    @Test
    @DisplayName("PathUtil expandPath absolute unchanged")
    void expandPathAbsolute() {
        String result = PathUtil.expandPath("/tmp/test");

        assertEquals("/tmp/test", result);
    }

    @Test
    @DisplayName("PathUtil expandPath relative resolved")
    void expandPathRelative() {
        String result = PathUtil.expandPath("subdir");

        assertTrue(result.contains("subdir"));
    }

    @Test
    @DisplayName("PathUtil expandPath with null bytes throws")
    void expandPathNullBytes() {
        assertThrows(IllegalArgumentException.class, () -> PathUtil.expandPath("test\0path"));
    }

    @Test
    @DisplayName("PathUtil expandPath with baseDir")
    void expandPathWithBaseDir() {
        String result = PathUtil.expandPath("subdir", "/tmp");

        assertTrue(result.startsWith("/tmp"));
    }

    @Test
    @DisplayName("PathUtil toRelativePath converts absolute")
    void toRelativePath() {
        String cwd = System.getProperty("user.dir");
        String absPath = cwd + "/subdir";
        String result = PathUtil.toRelativePath(absPath, cwd);

        assertEquals("subdir", result);
    }

    @Test
    @DisplayName("PathUtil toRelativePath outside cwd returns absolute")
    void toRelativePathOutside() {
        String cwd = System.getProperty("user.dir");
        String result = PathUtil.toRelativePath("/tmp/test", cwd);

        assertEquals("/tmp/test", result);
    }

    @Test
    @DisplayName("PathUtil getDirectoryForPath returns directory")
    void getDirectoryForPath() {
        String result = PathUtil.getDirectoryForPath("/tmp/test.txt");

        assertEquals("/tmp", result);
    }

    @Test
    @DisplayName("PathUtil containsPathTraversal detects ..")
    void containsPathTraversal() {
        assertTrue(PathUtil.containsPathTraversal("../test"));
        assertTrue(PathUtil.containsPathTraversal("test/../other"));
        assertFalse(PathUtil.containsPathTraversal("normal/path"));
    }

    @Test
    @DisplayName("PathUtil containsPathTraversal null returns false")
    void containsPathTraversalNull() {
        assertFalse(PathUtil.containsPathTraversal(null));
    }

    @Test
    @DisplayName("PathUtil sanitizePath removes null bytes")
    void sanitizePath() {
        String result = PathUtil.sanitizePath("test\0path");

        assertFalse(result.contains("\0"));
    }

    @Test
    @DisplayName("PathUtil sanitizePath null returns null")
    void sanitizePathNull() {
        assertNull(PathUtil.sanitizePath(null));
    }

    @Test
    @DisplayName("PathUtil normalizePathForConfigKey converts backslashes")
    void normalizePathForConfigKey() {
        String result = PathUtil.normalizePathForConfigKey("C:\\Users\\test");

        assertFalse(result.contains("\\"));
        assertTrue(result.contains("/"));
    }

    @Test
    @DisplayName("PathUtil normalizePathForConfigKey null returns null")
    void normalizePathForConfigKeyNull() {
        assertNull(PathUtil.normalizePathForConfigKey(null));
    }

    @Test
    @DisplayName("PathUtil isAbsolute true for absolute")
    void isAbsoluteTrue() {
        assertTrue(PathUtil.isAbsolute("/tmp/test"));
    }

    @Test
    @DisplayName("PathUtil isAbsolute false for relative")
    void isAbsoluteFalse() {
        assertFalse(PathUtil.isAbsolute("relative/path"));
    }

    @Test
    @DisplayName("PathUtil isAbsolute null returns false")
    void isAbsoluteNull() {
        assertFalse(PathUtil.isAbsolute(null));
    }

    @Test
    @DisplayName("PathUtil isAbsolute empty returns false")
    void isAbsoluteEmpty() {
        assertFalse(PathUtil.isAbsolute(""));
    }

    @Test
    @DisplayName("PathUtil getFileName extracts name")
    void getFileName() {
        String result = PathUtil.getFileName("/tmp/test.txt");

        assertEquals("test.txt", result);
    }

    @Test
    @DisplayName("PathUtil getFileName null returns null")
    void getFileNameNull() {
        assertNull(PathUtil.getFileName(null));
    }

    @Test
    @DisplayName("PathUtil getExtension extracts extension")
    void getExtension() {
        String result = PathUtil.getExtension("test.txt");

        assertEquals("txt", result);
    }

    @Test
    @DisplayName("PathUtil getExtension no extension returns null")
    void getExtensionNone() {
        assertNull(PathUtil.getExtension("noextension"));
    }

    @Test
    @DisplayName("PathUtil getExtension null returns null")
    void getExtensionNull() {
        assertNull(PathUtil.getExtension(null));
    }

    @Test
    @DisplayName("PathUtil join joins paths")
    void joinPaths() {
        String result = PathUtil.join("/tmp", "subdir", "file.txt");

        assertTrue(result.contains("tmp"));
        assertTrue(result.contains("subdir"));
        assertTrue(result.contains("file.txt"));
    }

    @Test
    @DisplayName("PathUtil join null parts handled")
    void joinNullParts() {
        String result = PathUtil.join("/tmp", null, "file.txt");

        assertNotNull(result);
    }

    @Test
    @DisplayName("PathUtil join empty returns null")
    void joinEmpty() {
        assertNull(PathUtil.join());
    }

    @Test
    @DisplayName("PathUtil resolve resolves path")
    void resolvePath() {
        String result = PathUtil.resolve("/tmp", "subdir");

        assertTrue(result.startsWith("/tmp"));
        assertTrue(result.contains("subdir"));
    }

    @Test
    @DisplayName("PathUtil resolve null path returns base")
    void resolveNullPath() {
        String result = PathUtil.resolve("/tmp", null);

        assertEquals("/tmp", result);
    }

    @Test
    @DisplayName("PathUtil isWithin true for subpath")
    void isWithinTrue() {
        assertTrue(PathUtil.isWithin("/tmp/subdir/file.txt", "/tmp"));
    }

    @Test
    @DisplayName("PathUtil isWithin false for outside")
    void isWithinFalse() {
        assertFalse(PathUtil.isWithin("/other/file.txt", "/tmp"));
    }

    @Test
    @DisplayName("PathUtil isWithin null returns false")
    void isWithinNull() {
        assertFalse(PathUtil.isWithin(null, "/tmp"));
        assertFalse(PathUtil.isWithin("/tmp", null));
    }

    @Test
    @DisplayName("PathUtil isUncPath detects UNC")
    void isUncPath() {
        assertTrue(PathUtil.isUncPath("\\\\server\\share"));
        assertTrue(PathUtil.isUncPath("//server/share"));
        assertFalse(PathUtil.isUncPath("/local/path"));
    }

    @Test
    @DisplayName("PathUtil isUncPath null returns false")
    void isUncPathNull() {
        assertFalse(PathUtil.isUncPath(null));
    }

    @Test
    @DisplayName("PathUtil isHomeRelative detects home path")
    void isHomeRelative() {
        assertTrue(PathUtil.isHomeRelative("~"));
        assertTrue(PathUtil.isHomeRelative("~/Documents"));
        assertFalse(PathUtil.isHomeRelative("/tmp"));
    }

    @Test
    @DisplayName("PathUtil isHomeRelative null returns false")
    void isHomeRelativeNull() {
        assertFalse(PathUtil.isHomeRelative(null));
    }

    @Test
    @DisplayName("PathUtil expandHome expands tilde")
    void expandHome() {
        String result = PathUtil.expandHome("~");

        assertEquals(System.getProperty("user.home"), result);
    }

    @Test
    @DisplayName("PathUtil expandHome null returns null")
    void expandHomeNull() {
        assertNull(PathUtil.expandHome(null));
    }

    @Test
    @DisplayName("PathUtil expandHome no tilde unchanged")
    void expandHomeNoTilde() {
        String result = PathUtil.expandHome("/tmp");

        assertEquals("/tmp", result);
    }
}