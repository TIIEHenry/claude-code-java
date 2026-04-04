/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GlobUtils.
 */
class GlobUtilsTest {

    @Test
    @DisplayName("GlobUtils extractGlobBaseDirectory literal path")
    void extractLiteralPath() {
        GlobUtils.GlobBaseResult result = GlobUtils.extractGlobBaseDirectory("/home/user/file.txt");

        assertNotNull(result);
        assertTrue(result.baseDir().contains("home"));
        assertEquals("file.txt", result.relativePattern());
    }

    @Test
    @DisplayName("GlobUtils extractGlobBaseDirectory with wildcard")
    void extractWithWildcard() {
        GlobUtils.GlobBaseResult result = GlobUtils.extractGlobBaseDirectory("/home/user/*.txt");

        assertNotNull(result);
        assertTrue(result.baseDir().contains("user"));
        assertEquals("*.txt", result.relativePattern());
    }

    @Test
    @DisplayName("GlobUtils extractGlobBaseDirectory relative pattern")
    void extractRelativePattern() {
        GlobUtils.GlobBaseResult result = GlobUtils.extractGlobBaseDirectory("src/**/*.java");

        assertNotNull(result);
        assertEquals("src", result.baseDir());
        assertEquals("**/*.java", result.relativePattern());
    }

    @Test
    @DisplayName("GlobUtils extractGlobBaseDirectory pattern at root")
    void extractPatternAtRoot() {
        GlobUtils.GlobBaseResult result = GlobUtils.extractGlobBaseDirectory("*.txt");

        assertNotNull(result);
        assertEquals("", result.baseDir());
        assertEquals("*.txt", result.relativePattern());
    }

    @Test
    @DisplayName("GlobUtils extractGlobBaseDirectory with question mark")
    void extractWithQuestionMark() {
        GlobUtils.GlobBaseResult result = GlobUtils.extractGlobBaseDirectory("/logs/log?.txt");

        assertNotNull(result);
        assertTrue(result.baseDir().contains("logs"));
        assertEquals("log?.txt", result.relativePattern());
    }

    @Test
    @DisplayName("GlobUtils extractGlobBaseDirectory with brackets")
    void extractWithBrackets() {
        GlobUtils.GlobBaseResult result = GlobUtils.extractGlobBaseDirectory("/files/[abc].txt");

        assertNotNull(result);
        assertTrue(result.baseDir().contains("files"));
        assertEquals("[abc].txt", result.relativePattern());
    }

    @Test
    @DisplayName("GlobUtils extractGlobBaseDirectory with brace expansion")
    void extractWithBraceExpansion() {
        GlobUtils.GlobBaseResult result = GlobUtils.extractGlobBaseDirectory("/path/{a,b,c}.txt");

        assertNotNull(result);
        assertTrue(result.baseDir().contains("path"));
        assertEquals("{a,b,c}.txt", result.relativePattern());
    }

    @Test
    @DisplayName("GlobUtils GlobBaseResult record works")
    void globBaseResultRecord() {
        GlobUtils.GlobBaseResult result = new GlobUtils.GlobBaseResult("/base", "*.txt");

        assertEquals("/base", result.baseDir());
        assertEquals("*.txt", result.relativePattern());
    }

    @Test
    @DisplayName("GlobUtils GlobResult record works")
    void globResultRecord() {
        java.util.List<String> files = java.util.List.of("/path/a.txt", "/path/b.txt");
        GlobUtils.GlobResult result = new GlobUtils.GlobResult(files, true);

        assertEquals(2, result.files().size());
        assertTrue(result.truncated());
    }

    @Test
    @DisplayName("GlobUtils ToolPermissionContext record works")
    void toolPermissionContextRecord() {
        java.util.List<String> patterns = java.util.List.of(".git", "node_modules");
        GlobUtils.ToolPermissionContext ctx = new GlobUtils.ToolPermissionContext(patterns);

        assertEquals(2, ctx.ignorePatterns().size());
    }

    @Test
    @DisplayName("GlobUtils Permissions.getFileReadIgnorePatterns returns patterns")
    void permissionsGetPatterns() {
        java.util.List<String> patterns = java.util.List.of("*.log");
        GlobUtils.ToolPermissionContext ctx = new GlobUtils.ToolPermissionContext(patterns);

        java.util.List<String> result = GlobUtils.Permissions.getFileReadIgnorePatterns(ctx);

        assertEquals(1, result.size());
        assertEquals("*.log", result.get(0));
    }

    @Test
    @DisplayName("GlobUtils Permissions.getFileReadIgnorePatterns handles null")
    void permissionsGetPatternsNull() {
        java.util.List<String> result = GlobUtils.Permissions.getFileReadIgnorePatterns(null);

        assertTrue(result.isEmpty());
    }
}