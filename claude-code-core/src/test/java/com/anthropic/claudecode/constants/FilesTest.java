/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Files constants.
 */
class FilesTest {

    @Test
    @DisplayName("Files CLAUDE_MD constant")
    void claudeMdConstant() {
        assertEquals("CLAUDE.md", Files.CLAUDE_MD);
    }

    @Test
    @DisplayName("Files CLAUDE_MD_LOCAL constant")
    void claudeMdLocalConstant() {
        assertEquals("CLAUDE.local.md", Files.CLAUDE_MD_LOCAL);
    }

    @Test
    @DisplayName("Files CONFIG_JSON constant")
    void configJsonConstant() {
        assertEquals("config.json", Files.CONFIG_JSON);
    }

    @Test
    @DisplayName("Files SETTINGS_JSON constant")
    void settingsJsonConstant() {
        assertEquals("settings.json", Files.SETTINGS_JSON);
    }

    @Test
    @DisplayName("Files PACKAGE_JSON constant")
    void packageJsonConstant() {
        assertEquals("package.json", Files.PACKAGE_JSON);
    }

    @Test
    @DisplayName("Files CLAUDE_DIR constant")
    void claudeDirConstant() {
        assertEquals(".claude", Files.CLAUDE_DIR);
    }

    @Test
    @DisplayName("Files GIT_DIR constant")
    void gitDirConstant() {
        assertEquals(".git", Files.GIT_DIR);
    }

    @Test
    @DisplayName("Files NODE_MODULES constant")
    void nodeModulesConstant() {
        assertEquals("node_modules", Files.NODE_MODULES);
    }

    @Test
    @DisplayName("Files SKILLS_DIR constant")
    void skillsDirConstant() {
        assertEquals("skills", Files.SKILLS_DIR);
    }

    @Test
    @DisplayName("Files PLUGINS_DIR constant")
    void pluginsDirConstant() {
        assertEquals("plugins", Files.PLUGINS_DIR);
    }

    @Test
    @DisplayName("Files TEXT_EXTENSIONS not empty")
    void textExtensionsNotEmpty() {
        assertTrue(Files.TEXT_EXTENSIONS.length > 0);
    }

    @Test
    @DisplayName("Files TEXT_EXTENSIONS contains common extensions")
    void textExtensionsContainsCommon() {
        String[] expected = {".txt", ".md", ".json", ".java", ".py", ".js"};
        for (String ext : expected) {
            boolean found = false;
            for (String textExt : Files.TEXT_EXTENSIONS) {
                if (textExt.equals(ext)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Should contain " + ext);
        }
    }

    @Test
    @DisplayName("Files BINARY_EXTENSIONS not empty")
    void binaryExtensionsNotEmpty() {
        assertTrue(Files.BINARY_EXTENSIONS.length > 0);
    }

    @Test
    @DisplayName("Files BINARY_EXTENSIONS contains common extensions")
    void binaryExtensionsContainsCommon() {
        String[] expected = {".exe", ".dll", ".png", ".jpg", ".pdf", ".zip"};
        for (String ext : expected) {
            boolean found = false;
            for (String binExt : Files.BINARY_EXTENSIONS) {
                if (binExt.equals(ext)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Should contain " + ext);
        }
    }

    @Test
    @DisplayName("Files IGNORED_DIRS not empty")
    void ignoredDirsNotEmpty() {
        assertTrue(Files.IGNORED_DIRS.length > 0);
    }

    @Test
    @DisplayName("Files IGNORED_DIRS contains common directories")
    void ignoredDirsContainsCommon() {
        String[] expected = {"node_modules", ".git", "target", "build"};
        for (String dir : expected) {
            boolean found = false;
            for (String ignored : Files.IGNORED_DIRS) {
                if (ignored.equals(dir)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Should contain " + dir);
        }
    }

    @Test
    @DisplayName("Files HIDDEN_FILES not empty")
    void hiddenFilesNotEmpty() {
        assertTrue(Files.HIDDEN_FILES.length > 0);
    }

    @Test
    @DisplayName("Files isTextExtension returns true for text extensions")
    void isTextExtensionTrue() {
        assertTrue(Files.isTextExtension(".txt"));
        assertTrue(Files.isTextExtension(".md"));
        assertTrue(Files.isTextExtension(".json"));
        assertTrue(Files.isTextExtension(".java"));
        assertTrue(Files.isTextExtension(".py"));
        assertTrue(Files.isTextExtension(".js"));
    }

    @Test
    @DisplayName("Files isTextExtension returns true regardless of case")
    void isTextExtensionCaseInsensitive() {
        assertTrue(Files.isTextExtension(".TXT"));
        assertTrue(Files.isTextExtension(".MD"));
        assertTrue(Files.isTextExtension(".JSON"));
        assertTrue(Files.isTextExtension(".Java"));
    }

    @Test
    @DisplayName("Files isTextExtension returns false for binary extensions")
    void isTextExtensionFalseForBinary() {
        assertFalse(Files.isTextExtension(".exe"));
        assertFalse(Files.isTextExtension(".png"));
        assertFalse(Files.isTextExtension(".pdf"));
    }

    @Test
    @DisplayName("Files isTextExtension returns false for unknown extensions")
    void isTextExtensionFalseForUnknown() {
        assertFalse(Files.isTextExtension(".xyz"));
        assertFalse(Files.isTextExtension(".unknown"));
    }

    @Test
    @DisplayName("Files isBinaryExtension returns true for binary extensions")
    void isBinaryExtensionTrue() {
        assertTrue(Files.isBinaryExtension(".exe"));
        assertTrue(Files.isBinaryExtension(".dll"));
        assertTrue(Files.isBinaryExtension(".png"));
        assertTrue(Files.isBinaryExtension(".jpg"));
        assertTrue(Files.isBinaryExtension(".pdf"));
    }

    @Test
    @DisplayName("Files isBinaryExtension returns true regardless of case")
    void isBinaryExtensionCaseInsensitive() {
        assertTrue(Files.isBinaryExtension(".EXE"));
        assertTrue(Files.isBinaryExtension(".PNG"));
        assertTrue(Files.isBinaryExtension(".PDF"));
    }

    @Test
    @DisplayName("Files isBinaryExtension returns false for text extensions")
    void isBinaryExtensionFalseForText() {
        assertFalse(Files.isBinaryExtension(".txt"));
        assertFalse(Files.isBinaryExtension(".md"));
        assertFalse(Files.isBinaryExtension(".java"));
    }

    @Test
    @DisplayName("Files isBinaryExtension returns false for unknown extensions")
    void isBinaryExtensionFalseForUnknown() {
        assertFalse(Files.isBinaryExtension(".xyz"));
        assertFalse(Files.isBinaryExtension(".unknown"));
    }

    @Test
    @DisplayName("Files isIgnoredDir returns true for ignored directories")
    void isIgnoredDirTrue() {
        assertTrue(Files.isIgnoredDir("node_modules"));
        assertTrue(Files.isIgnoredDir(".git"));
        assertTrue(Files.isIgnoredDir("target"));
        assertTrue(Files.isIgnoredDir("build"));
        assertTrue(Files.isIgnoredDir("__pycache__"));
    }

    @Test
    @DisplayName("Files isIgnoredDir returns false for normal directories")
    void isIgnoredDirFalse() {
        assertFalse(Files.isIgnoredDir("src"));
        assertFalse(Files.isIgnoredDir("main"));
        assertFalse(Files.isIgnoredDir("test"));
        assertFalse(Files.isIgnoredDir("lib"));
    }

    @Test
    @DisplayName("Files isIgnoredDir exact match only")
    void isIgnoredDirExactMatch() {
        // Should not match partial or different case
        assertFalse(Files.isIgnoredDir("Node_Modules"));
        assertFalse(Files.isIgnoredDir("node_modules/subdir"));
        assertFalse(Files.isIgnoredDir(".gitignore"));
    }
}