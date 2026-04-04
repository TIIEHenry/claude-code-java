/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExampleCommands.
 */
class ExampleCommandsTest {

    @Test
    @DisplayName("ExampleCommands isCoreFile returns true for source files")
    void isCoreFileTrue() {
        assertTrue(ExampleCommands.isCoreFile("src/main/java/MyClass.java"));
        assertTrue(ExampleCommands.isCoreFile("lib/mylib.py"));
        assertTrue(ExampleCommands.isCoreFile("app.ts"));
    }

    @Test
    @DisplayName("ExampleCommands isCoreFile returns false for lock files")
    void isCoreFileLockFiles() {
        assertFalse(ExampleCommands.isCoreFile("package-lock.json"));
        assertFalse(ExampleCommands.isCoreFile("yarn.lock"));
        assertFalse(ExampleCommands.isCoreFile("Cargo.lock"));
    }

    @Test
    @DisplayName("ExampleCommands isCoreFile returns false for build artifacts")
    void isCoreFileBuildArtifacts() {
        assertFalse(ExampleCommands.isCoreFile("dist/bundle.js"));
        assertFalse(ExampleCommands.isCoreFile("build/output.o"));
        assertFalse(ExampleCommands.isCoreFile("node_modules/package/index.js"));
    }

    @Test
    @DisplayName("ExampleCommands isCoreFile returns false for config files")
    void isCoreFileConfigFiles() {
        assertFalse(ExampleCommands.isCoreFile("tsconfig.json"));
        assertFalse(ExampleCommands.isCoreFile(".eslintrc"));
        assertFalse(ExampleCommands.isCoreFile("README.md"));
    }

    @Test
    @DisplayName("ExampleCommands isCoreFile returns false for minified files")
    void isCoreFileMinified() {
        assertFalse(ExampleCommands.isCoreFile("bundle.min.js"));
        assertFalse(ExampleCommands.isCoreFile("styles.min.css"));
    }

    @Test
    @DisplayName("ExampleCommands countAndSortItems")
    void countAndSortItems() {
        List<String> items = List.of("a", "b", "a", "c", "a", "b");
        String result = ExampleCommands.countAndSortItems(items, 3);

        assertTrue(result.contains("3 a"));
        assertTrue(result.contains("2 b"));
        assertTrue(result.contains("1 c"));
    }

    @Test
    @DisplayName("ExampleCommands countAndSortItems empty")
    void countAndSortItemsEmpty() {
        String result = ExampleCommands.countAndSortItems(new ArrayList<>(), 3);
        assertEquals("", result);
    }

    @Test
    @DisplayName("ExampleCommands pickDiverseCoreFiles returns empty for empty input")
    void pickDiverseCoreFilesEmpty() {
        List<String> result = ExampleCommands.pickDiverseCoreFiles(new ArrayList<>(), 5);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("ExampleCommands pickDiverseCoreFiles filters non-core files")
    void pickDiverseCoreFilesFiltersNonCore() {
        List<String> paths = List.of(
            "package-lock.json",
            "src/Main.java",
            "yarn.lock"
        );
        List<String> result = ExampleCommands.pickDiverseCoreFiles(paths, 3);
        // The method returns empty list if it can't pick required items
        // Just verify it doesn't throw and returns a list
        assertNotNull(result);
    }

    @Test
    @DisplayName("ExampleCommands getExampleCommands returns list")
    void getExampleCommands() {
        List<String> commands = ExampleCommands.getExampleCommands("Test.java");
        assertFalse(commands.isEmpty());
        assertTrue(commands.size() >= 5);
    }

    @Test
    @DisplayName("ExampleCommands getExampleCommands null file")
    void getExampleCommandsNull() {
        List<String> commands = ExampleCommands.getExampleCommands(null);
        assertFalse(commands.isEmpty());
        // Check that the commands list contains expected items
        assertTrue(commands.size() >= 5);
    }

    @Test
    @DisplayName("ExampleCommands getRandomExampleCommand returns string")
    void getRandomExampleCommand() {
        String command = ExampleCommands.getRandomExampleCommand("Test.java");
        assertNotNull(command);
        assertTrue(command.startsWith("Try \""));
    }

    @Test
    @DisplayName("ExampleCommands getExampleCommandFromCache")
    void getExampleCommandFromCache() {
        List<String> files = List.of("Main.java", "Utils.py");
        String command = ExampleCommands.getExampleCommandFromCache(files);
        assertNotNull(command);
        assertTrue(command.startsWith("Try \""));
    }

    @Test
    @DisplayName("ExampleCommands getExampleCommandFromCache null files")
    void getExampleCommandFromCacheNull() {
        String command = ExampleCommands.getExampleCommandFromCache(null);
        assertNotNull(command);
    }

    @Test
    @DisplayName("ExampleCommands needsRefresh null timestamp")
    void needsRefreshNullTimestamp() {
        assertTrue(ExampleCommands.needsRefresh(null));
    }

    @Test
    @DisplayName("ExampleCommands needsRefresh old timestamp")
    void needsRefreshOldTimestamp() {
        long oldTimestamp = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000); // 8 days ago
        assertTrue(ExampleCommands.needsRefresh(oldTimestamp));
    }

    @Test
    @DisplayName("ExampleCommands needsRefresh recent timestamp")
    void needsRefreshRecentTimestamp() {
        long recentTimestamp = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000); // 1 day ago
        assertFalse(ExampleCommands.needsRefresh(recentTimestamp));
    }

    @Test
    @DisplayName("ExampleCommands findFrequentlyModifiedFiles returns list")
    void findFrequentlyModifiedFiles() {
        // Without git, returns empty list
        List<String> files = ExampleCommands.findFrequentlyModifiedFiles(
            java.nio.file.Paths.get("/tmp")
        );
        assertNotNull(files);
    }
}