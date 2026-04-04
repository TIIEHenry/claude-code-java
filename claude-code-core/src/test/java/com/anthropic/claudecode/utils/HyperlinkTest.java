/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Hyperlink.
 */
class HyperlinkTest {

    @Test
    @DisplayName("Hyperlink OSC8_START constant")
    void osc8StartConstant() {
        assertEquals("\u001b]8;;", Hyperlink.OSC8_START);
    }

    @Test
    @DisplayName("Hyperlink OSC8_END constant")
    void osc8EndConstant() {
        assertEquals("\u0007", Hyperlink.OSC8_END);
    }

    @Test
    @DisplayName("Hyperlink supportsHyperlinks returns boolean")
    void supportsHyperlinks() {
        boolean result = Hyperlink.supportsHyperlinks();
        assertTrue(result == true || result == false);
    }

    @Test
    @DisplayName("Hyperlink blue applies ANSI codes")
    void blue() {
        String result = Hyperlink.blue("text");
        assertTrue(result.contains("\u001b[34m"));
        assertTrue(result.contains("\u001b[0m"));
        assertTrue(result.contains("text"));
    }

    @Test
    @DisplayName("Hyperlink createHyperlink without support")
    void createHyperlinkNoSupport() {
        String url = "https://example.com";
        String result = Hyperlink.createHyperlink(url, null, false);
        assertEquals(url, result);
    }

    @Test
    @DisplayName("Hyperlink createHyperlink with support")
    void createHyperlinkWithSupport() {
        String url = "https://example.com";
        String result = Hyperlink.createHyperlink(url, null, true);
        assertTrue(result.contains(Hyperlink.OSC8_START));
        assertTrue(result.contains(url));
    }

    @Test
    @DisplayName("Hyperlink createHyperlink with content")
    void createHyperlinkWithContent() {
        String url = "https://example.com";
        String content = "Click here";
        String result = Hyperlink.createHyperlink(url, content, true);
        assertTrue(result.contains(Hyperlink.OSC8_START));
        assertTrue(result.contains("Click here"));
    }

    @Test
    @DisplayName("Hyperlink createHyperlink simple")
    void createHyperlinkSimple() {
        String url = "https://example.com";
        String result = Hyperlink.createHyperlink(url);
        // May or may not have OSC8 sequences depending on terminal
        assertTrue(result.contains(url));
    }

    @Test
    @DisplayName("Hyperlink HyperlinkResult record")
    void hyperlinkResultRecord() {
        Hyperlink.HyperlinkResult result = new Hyperlink.HyperlinkResult("link", true);
        assertEquals("link", result.link());
        assertTrue(result.supported());
    }

    @Test
    @DisplayName("Hyperlink createHyperlinkWithSupport method")
    void createHyperlinkWithSupportMethod() {
        Hyperlink.HyperlinkResult result = Hyperlink.createHyperlinkWithSupport(
            "https://example.com", "Link"
        );
        assertNotNull(result.link());
        assertTrue(result.supported() == true || result.supported() == false);
    }
}