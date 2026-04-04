/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AnalyzerContext.
 */
class AnalyzerContextTest {

    private AnalyzerContext context;
    private Path projectRoot;

    @BeforeEach
    void setUp() {
        projectRoot = Paths.get("/tmp/project");
        context = new AnalyzerContext(projectRoot);
    }

    @Test
    @DisplayName("AnalyzerContext getProjectRoot")
    void getProjectRoot() {
        assertEquals(projectRoot, context.getProjectRoot());
    }

    @Test
    @DisplayName("AnalyzerContext getSetting null when not set")
    void getSettingNull() {
        assertNull(context.getSetting("nonexistent"));
    }

    @Test
    @DisplayName("AnalyzerContext getSetting with default")
    void getSettingWithDefault() {
        assertEquals("default", context.getSetting("nonexistent", "default"));
    }

    @Test
    @DisplayName("AnalyzerContext setSetting and getSetting")
    void setAndGetSetting() {
        context.setSetting("key", "value");
        assertEquals("value", context.getSetting("key"));
    }

    @Test
    @DisplayName("AnalyzerContext addIncludePath")
    void addIncludePath() {
        Path include = Paths.get("/tmp/project/src");
        context.addIncludePath(include);
        assertEquals(1, context.getIncludePaths().size());
        assertTrue(context.getIncludePaths().contains(include));
    }

    @Test
    @DisplayName("AnalyzerContext addExcludePath")
    void addExcludePath() {
        Path exclude = Paths.get("/tmp/project/build");
        context.addExcludePath(exclude);
        assertEquals(1, context.getExcludePaths().size());
        assertTrue(context.getExcludePaths().contains(exclude));
    }

    @Test
    @DisplayName("AnalyzerContext isIncluded true when empty includes")
    void isIncludedEmptyIncludes() {
        assertTrue(context.isIncluded(Paths.get("/tmp/project/file.txt")));
    }

    @Test
    @DisplayName("AnalyzerContext isIncluded true when in include path")
    void isIncludedTrue() {
        Path include = Paths.get("/tmp/project/src");
        context.addIncludePath(include);
        assertTrue(context.isIncluded(Paths.get("/tmp/project/src/main.java")));
    }

    @Test
    @DisplayName("AnalyzerContext isIncluded false when not in include path")
    void isIncludedFalse() {
        Path include = Paths.get("/tmp/project/src");
        context.addIncludePath(include);
        assertFalse(context.isIncluded(Paths.get("/tmp/project/other/file.txt")));
    }

    @Test
    @DisplayName("AnalyzerContext isExcluded true when in exclude path")
    void isExcludedTrue() {
        Path exclude = Paths.get("/tmp/project/build");
        context.addExcludePath(exclude);
        assertTrue(context.isExcluded(Paths.get("/tmp/project/build/output.class")));
    }

    @Test
    @DisplayName("AnalyzerContext isExcluded false when not in exclude path")
    void isExcludedFalse() {
        Path exclude = Paths.get("/tmp/project/build");
        context.addExcludePath(exclude);
        assertFalse(context.isExcluded(Paths.get("/tmp/project/src/main.java")));
    }

    @Test
    @DisplayName("AnalyzerContext shouldAnalyze true for included not excluded")
    void shouldAnalyzeTrue() {
        Path include = Paths.get("/tmp/project/src");
        context.addIncludePath(include);
        assertTrue(context.shouldAnalyze(Paths.get("/tmp/project/src/main.java")));
    }

    @Test
    @DisplayName("AnalyzerContext shouldAnalyze false for excluded")
    void shouldAnalyzeFalseExcluded() {
        Path include = Paths.get("/tmp/project/src");
        Path exclude = Paths.get("/tmp/project/src/generated");
        context.addIncludePath(include);
        context.addExcludePath(exclude);
        assertFalse(context.shouldAnalyze(Paths.get("/tmp/project/src/generated/Auto.java")));
    }

    @Test
    @DisplayName("AnalyzerContext defineSymbol and getSymbol")
    void defineAndGetSymbol() {
        context.defineSymbol("VERSION", "1.0");
        assertEquals("1.0", context.getSymbol("VERSION"));
    }

    @Test
    @DisplayName("AnalyzerContext isSymbolDefined")
    void isSymbolDefined() {
        assertFalse(context.isSymbolDefined("VERSION"));
        context.defineSymbol("VERSION", "1.0");
        assertTrue(context.isSymbolDefined("VERSION"));
    }

    @Test
    @DisplayName("AnalyzerContext expandSymbols with ${}")
    void expandSymbolsWithBraces() {
        context.defineSymbol("VERSION", "1.0");
        String result = context.expandSymbols("Version: ${VERSION}");
        assertEquals("Version: 1.0", result);
    }

    @Test
    @DisplayName("AnalyzerContext expandSymbols with $")
    void expandSymbolsWithoutBraces() {
        context.defineSymbol("VERSION", "1.0");
        String result = context.expandSymbols("Version: $VERSION");
        assertEquals("Version: 1.0", result);
    }

    @Test
    @DisplayName("AnalyzerContext expandSymbols null returns null")
    void expandSymbolsNull() {
        assertNull(context.expandSymbols(null));
    }

    @Test
    @DisplayName("AnalyzerContext createChild inherits settings")
    void createChildInheritsSettings() {
        context.setSetting("key", "value");
        Path subPath = Paths.get("/tmp/project/subproject");
        AnalyzerContext child = context.createChild(subPath);
        assertEquals("value", child.getSetting("key"));
    }

    @Test
    @DisplayName("AnalyzerContext createChild inherits paths")
    void createChildInheritsPaths() {
        context.addIncludePath(Paths.get("/tmp/project/src"));
        context.addExcludePath(Paths.get("/tmp/project/build"));
        
        AnalyzerContext child = context.createChild(Paths.get("/tmp/project/sub"));
        assertEquals(1, child.getIncludePaths().size());
        assertEquals(1, child.getExcludePaths().size());
    }

    @Test
    @DisplayName("AnalyzerContext createChild inherits symbols")
    void createChildInheritsSymbols() {
        context.defineSymbol("VERSION", "1.0");
        AnalyzerContext child = context.createChild(Paths.get("/tmp/project/sub"));
        assertEquals("1.0", child.getSymbol("VERSION"));
    }

    @Test
    @DisplayName("AnalyzerContext getAllSettings returns unmodifiable")
    void getAllSettingsUnmodifiable() {
        context.setSetting("key", "value");
        java.util.Map<String, Object> settings = context.getAllSettings();
        assertEquals(1, settings.size());
        assertThrows(UnsupportedOperationException.class, () -> settings.put("new", "value"));
    }

    @Test
    @DisplayName("AnalyzerContext getIncludePaths returns unmodifiable")
    void getIncludePathsUnmodifiable() {
        context.addIncludePath(Paths.get("/tmp/project/src"));
        java.util.List<Path> paths = context.getIncludePaths();
        assertThrows(UnsupportedOperationException.class, () -> paths.add(Paths.get("/other")));
    }

    @Test
    @DisplayName("AnalyzerContext getExcludePaths returns unmodifiable")
    void getExcludePathsUnmodifiable() {
        context.addExcludePath(Paths.get("/tmp/project/build"));
        java.util.List<Path> paths = context.getExcludePaths();
        assertThrows(UnsupportedOperationException.class, () -> paths.add(Paths.get("/other")));
    }
}
