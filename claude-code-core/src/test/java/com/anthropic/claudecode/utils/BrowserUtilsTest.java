/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BrowserUtils.
 */
class BrowserUtilsTest {

    @Test
    @DisplayName("BrowserUtils validateUrl accepts http URLs")
    void validateUrlHttp() {
        assertDoesNotThrow(() -> BrowserUtils.validateUrl("http://example.com"));
    }

    @Test
    @DisplayName("BrowserUtils validateUrl accepts https URLs")
    void validateUrlHttps() {
        assertDoesNotThrow(() -> BrowserUtils.validateUrl("https://example.com"));
    }

    @Test
    @DisplayName("BrowserUtils validateUrl rejects ftp URLs")
    void validateUrlFtp() {
        assertThrows(IllegalArgumentException.class, () -> BrowserUtils.validateUrl("ftp://example.com"));
    }

    @Test
    @DisplayName("BrowserUtils validateUrl rejects URLs without protocol")
    void validateUrlNoProtocol() {
        assertThrows(IllegalArgumentException.class, () -> BrowserUtils.validateUrl("example.com"));
    }

    @Test
    @DisplayName("BrowserUtils validateUrl rejects invalid URL format")
    void validateUrlInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> BrowserUtils.validateUrl("not a url"));
    }

    @Test
    @DisplayName("BrowserUtils isBrowserAvailable returns boolean")
    void isBrowserAvailable() {
        // Just check it doesn't throw
        assertDoesNotThrow(() -> BrowserUtils.isBrowserAvailable());
    }

    @Test
    @DisplayName("BrowserUtils getDefaultBrowser returns non-null")
    void getDefaultBrowser() {
        String browser = BrowserUtils.getDefaultBrowser();

        assertNotNull(browser);
        assertFalse(browser.isEmpty());
    }

    @Test
    @DisplayName("BrowserUtils openBrowser returns false for invalid URL")
    void openBrowserInvalid() {
        boolean result = BrowserUtils.openBrowser("not-a-url");

        assertFalse(result);
    }

    @Test
    @DisplayName("BrowserUtils openPath returns false for non-existent path")
    void openPathNonExistent() {
        boolean result = BrowserUtils.openPath("/non/existent/path/that/does/not/exist");

        assertFalse(result);
    }

    @Test
    @DisplayName("BrowserUtils openInEditor returns false for non-existent file")
    void openInEditorNonExistent() {
        boolean result = BrowserUtils.openInEditor("/non/existent/file.txt");

        assertFalse(result);
    }
}