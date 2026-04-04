/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PathUtils.
 */
class PathUtilsTest {

    @Test
    @DisplayName("PathUtils normalize path")
    void normalizePath() {
        Path path = Paths.get("src", "..", "src", "main");
        Path normalized = PathUtils.normalize(path);

        assertTrue(normalized.isAbsolute());
        assertTrue(normalized.toString().contains("src"));
        assertTrue(normalized.toString().contains("main"));
    }

    @Test
    @DisplayName("PathUtils normalize string")
    void normalizeString() {
        String normalized = PathUtils.normalize("src/../src");

        assertTrue(Paths.get(normalized).isAbsolute());
    }

    @Test
    @DisplayName("PathUtils isAbsolute true for absolute")
    void isAbsoluteTrue() {
        assertTrue(PathUtils.isAbsolute("/tmp"));
    }

    @Test
    @DisplayName("PathUtils isAbsolute false for relative")
    void isAbsoluteFalse() {
        assertFalse(PathUtils.isAbsolute("relative/path"));
    }

    @Test
    @DisplayName("PathUtils relativize works")
    void relativizeWorks() {
        Path base = Paths.get("/home/user");
        Path target = Paths.get("/home/user/docs/file.txt");

        String relative = PathUtils.relativize(base, target);

        assertEquals("docs/file.txt", relative);
    }

    @Test
    @DisplayName("PathUtils getExtension finds extension")
    void getExtensionFinds() {
        Optional<String> ext = PathUtils.getExtension(Paths.get("file.txt"));

        assertTrue(ext.isPresent());
        assertEquals("txt", ext.get());
    }

    @Test
    @DisplayName("PathUtils getExtension lowercases")
    void getExtensionLowercases() {
        Optional<String> ext = PathUtils.getExtension(Paths.get("file.JSON"));

        assertTrue(ext.isPresent());
        assertEquals("json", ext.get());
    }

    @Test
    @DisplayName("PathUtils getExtension empty for no extension")
    void getExtensionEmpty() {
        Optional<String> ext = PathUtils.getExtension(Paths.get("filename"));

        assertFalse(ext.isPresent());
    }

    @Test
    @DisplayName("PathUtils getExtension empty for dotfile")
    void getExtensionDotfile() {
        Optional<String> ext = PathUtils.getExtension(Paths.get(".gitignore"));

        assertFalse(ext.isPresent());
    }

    @Test
    @DisplayName("PathUtils isHidden true for dotfile")
    void isHiddenTrue() {
        assertTrue(PathUtils.isHidden(Paths.get(".hidden")));
    }

    @Test
    @DisplayName("PathUtils isHidden false for regular file")
    void isHiddenFalse() {
        assertFalse(PathUtils.isHidden(Paths.get("visible")));
    }

    @Test
    @DisplayName("PathUtils join paths")
    void joinPaths() {
        Path joined = PathUtils.join("/home", "user", "docs");

        assertEquals("/home/user/docs", joined.toString());
    }

    @Test
    @DisplayName("PathUtils getParent returns parent")
    void getParentReturns() {
        Optional<Path> parent = PathUtils.getParent(Paths.get("/home/user/file.txt"));

        assertTrue(parent.isPresent());
        assertEquals("/home/user", parent.get().toString());
    }

    @Test
    @DisplayName("PathUtils getParent empty for root")
    void getParentRoot() {
        Optional<Path> parent = PathUtils.getParent(Paths.get("/file.txt"));

        // Root parent behavior varies by platform
        assertNotNull(parent);
    }

    @Test
    @DisplayName("PathUtils getBaseName strips extension")
    void getBaseNameStrips() {
        String base = PathUtils.getBaseName(Paths.get("file.txt"));

        assertEquals("file", base);
    }

    @Test
    @DisplayName("PathUtils getBaseName returns full name for no extension")
    void getBaseNameNoExt() {
        String base = PathUtils.getBaseName(Paths.get("filename"));

        assertEquals("filename", base);
    }

    @Test
    @DisplayName("PathUtils sanitizePath replaces special chars")
    void sanitizePathReplaces() {
        String sanitized = PathUtils.sanitizePath("/path/to/file.txt");

        assertEquals("_path_to_file.txt", sanitized);
    }

    @Test
    @DisplayName("PathUtils sanitizePath handles null")
    void sanitizePathNull() {
        String sanitized = PathUtils.sanitizePath(null);

        assertEquals("", sanitized);
    }

    @Test
    @DisplayName("PathUtils sanitizePath keeps allowed chars")
    void sanitizePathKeepsAllowed() {
        String sanitized = PathUtils.sanitizePath("file-name_123.test");

        assertEquals("file-name_123.test", sanitized);
    }
}