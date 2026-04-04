/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GeneratedFiles.
 */
class GeneratedFilesTest {

    @Test
    @DisplayName("GeneratedFiles isGeneratedFile lock files")
    void isGeneratedFileLockFiles() {
        assertTrue(GeneratedFiles.isGeneratedFile("package-lock.json"));
        assertTrue(GeneratedFiles.isGeneratedFile("yarn.lock"));
        assertTrue(GeneratedFiles.isGeneratedFile("pnpm-lock.yaml"));
        assertTrue(GeneratedFiles.isGeneratedFile("cargo.lock"));
    }

    @Test
    @DisplayName("GeneratedFiles isGeneratedFile minified files")
    void isGeneratedFileMinified() {
        assertTrue(GeneratedFiles.isGeneratedFile("bundle.min.js"));
        assertTrue(GeneratedFiles.isGeneratedFile("styles.min.css"));
    }

    @Test
    @DisplayName("GeneratedFiles isGeneratedFile build directories")
    void isGeneratedFileBuildDirs() {
        assertTrue(GeneratedFiles.isGeneratedFile("dist/bundle.js"));
        assertTrue(GeneratedFiles.isGeneratedFile("build/output.o"));
        assertTrue(GeneratedFiles.isGeneratedFile("node_modules/package/index.js"));
    }

    @Test
    @DisplayName("GeneratedFiles isGeneratedFile generated patterns")
    void isGeneratedFileGeneratedPatterns() {
        assertTrue(GeneratedFiles.isGeneratedFile("file.generated.ts"));
        assertTrue(GeneratedFiles.isGeneratedFile("file.gen.js"));
        assertTrue(GeneratedFiles.isGeneratedFile("file.pb.go"));
        assertTrue(GeneratedFiles.isGeneratedFile("file_pb2.py"));
    }

    @Test
    @DisplayName("GeneratedFiles isGeneratedFile regular files")
    void isGeneratedFileRegularFiles() {
        assertFalse(GeneratedFiles.isGeneratedFile("src/Main.java"));
        assertFalse(GeneratedFiles.isGeneratedFile("lib/utils.py"));
        assertFalse(GeneratedFiles.isGeneratedFile("index.ts"));
    }

    @Test
    @DisplayName("GeneratedFiles isGeneratedFile null")
    void isGeneratedFileNull() {
        assertFalse(GeneratedFiles.isGeneratedFile(null));
    }

    @Test
    @DisplayName("GeneratedFiles isGeneratedFile empty")
    void isGeneratedFileEmpty() {
        assertFalse(GeneratedFiles.isGeneratedFile(""));
    }

    @Test
    @DisplayName("GeneratedFiles filterGeneratedFiles")
    void filterGeneratedFiles() {
        List<String> files = List.of(
            "src/Main.java",
            "package-lock.json",
            "lib/utils.py",
            "dist/bundle.js"
        );
        List<String> filtered = GeneratedFiles.filterGeneratedFiles(files);

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains("src/Main.java"));
        assertTrue(filtered.contains("lib/utils.py"));
    }

    @Test
    @DisplayName("GeneratedFiles isLockFile true")
    void isLockFileTrue() {
        assertTrue(GeneratedFiles.isLockFile("package-lock.json"));
        assertTrue(GeneratedFiles.isLockFile("yarn.lock"));
    }

    @Test
    @DisplayName("GeneratedFiles isLockFile false")
    void isLockFileFalse() {
        assertFalse(GeneratedFiles.isLockFile("src/Main.java"));
        assertFalse(GeneratedFiles.isLockFile(null));
    }

    @Test
    @DisplayName("GeneratedFiles isInBuildDirectory true")
    void isInBuildDirectoryTrue() {
        assertTrue(GeneratedFiles.isInBuildDirectory("dist/bundle.js"));
        assertTrue(GeneratedFiles.isInBuildDirectory("node_modules/pkg/index.js"));
        assertTrue(GeneratedFiles.isInBuildDirectory("build/output"));
    }

    @Test
    @DisplayName("GeneratedFiles isInBuildDirectory false")
    void isInBuildDirectoryFalse() {
        assertFalse(GeneratedFiles.isInBuildDirectory("src/Main.java"));
        assertFalse(GeneratedFiles.isInBuildDirectory(null));
    }
}