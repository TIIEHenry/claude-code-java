/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CompletableFuture;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CliHighlight.
 */
class CliHighlightTest {

    @BeforeEach
    void setUp() {
        CliHighlight.reset();
    }

    @Test
    @DisplayName("CliHighlight LanguageInfo record")
    void languageInfoRecord() {
        CliHighlight.LanguageInfo info = new CliHighlight.LanguageInfo(
            "Java", Set.of("java", "jav")
        );

        assertEquals("Java", info.name());
        assertEquals(2, info.aliases().size());
        assertTrue(info.aliases().contains("java"));
    }

    @Test
    @DisplayName("CliHighlight getCliHighlightPromise returns future")
    void getCliHighlightPromise() {
        CompletableFuture<CliHighlight.CliHighlightInstance> future =
            CliHighlight.getCliHighlightPromise();

        assertNotNull(future);
        // Future may already be completed or still running
    }

    @Test
    @DisplayName("CliHighlight getCliHighlightPromise cached")
    void getCliHighlightPromiseCached() {
        CompletableFuture<CliHighlight.CliHighlightInstance> future1 =
            CliHighlight.getCliHighlightPromise();
        CompletableFuture<CliHighlight.CliHighlightInstance> future2 =
            CliHighlight.getCliHighlightPromise();

        assertSame(future1, future2);
    }

    @Test
    @DisplayName("CliHighlight getLanguageName")
    void getLanguageName() throws Exception {
        CompletableFuture<String> langFuture = CliHighlight.getLanguageName("foo/bar.ts");
        String lang = langFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals("TypeScript", lang);
    }

    @Test
    @DisplayName("CliHighlight getLanguageName java")
    void getLanguageNameJava() throws Exception {
        CompletableFuture<String> langFuture = CliHighlight.getLanguageName("Test.java");
        String lang = langFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals("Java", lang);
    }

    @Test
    @DisplayName("CliHighlight getLanguageName no extension")
    void getLanguageNameNoExtension() throws Exception {
        CompletableFuture<String> langFuture = CliHighlight.getLanguageName("README");
        String lang = langFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals("unknown", lang);
    }

    @Test
    @DisplayName("CliHighlight getLanguageName unknown extension")
    void getLanguageNameUnknownExtension() throws Exception {
        CompletableFuture<String> langFuture = CliHighlight.getLanguageName("file.xyz");
        String lang = langFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals("unknown", lang);
    }

    @Test
    @DisplayName("CliHighlight supportsLanguage java")
    void supportsLanguageJava() throws Exception {
        // Load the highlighter first
        CliHighlight.getCliHighlightPromise().get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(CliHighlight.supportsLanguage("java"));
        assertTrue(CliHighlight.supportsLanguage("Java"));
    }

    @Test
    @DisplayName("CliHighlight supportsLanguage js")
    void supportsLanguageJs() throws Exception {
        CliHighlight.getCliHighlightPromise().get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(CliHighlight.supportsLanguage("js"));
        assertTrue(CliHighlight.supportsLanguage("javascript"));
    }

    @Test
    @DisplayName("CliHighlight supportsLanguage unknown")
    void supportsLanguageUnknown() throws Exception {
        CliHighlight.getCliHighlightPromise().get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertFalse(CliHighlight.supportsLanguage("xyz"));
    }

    @Test
    @DisplayName("CliHighlight supportsLanguage before load")
    void supportsLanguageBeforeLoad() {
        CliHighlight.reset();
        assertFalse(CliHighlight.supportsLanguage("java"));
    }

    @Test
    @DisplayName("CliHighlight reset clears cache")
    void resetClearsCache() throws Exception {
        CliHighlight.getCliHighlightPromise().get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(CliHighlight.supportsLanguage("java"));

        CliHighlight.reset();
        assertFalse(CliHighlight.supportsLanguage("java"));

        // After reset, getCliHighlightPromise will create a new future
        CompletableFuture<CliHighlight.CliHighlightInstance> newFuture =
            CliHighlight.getCliHighlightPromise();
        assertNotNull(newFuture);
    }

    @Test
    @DisplayName("CliHighlight CliHighlightInstance highlight")
    void highlightInstanceHighlight() throws Exception {
        CliHighlight.CliHighlightInstance instance =
            CliHighlight.getCliHighlightPromise()
                .get(5, java.util.concurrent.TimeUnit.SECONDS);

        String result = instance.highlight("public class Test {}", "java");
        assertEquals("public class Test {}", result); // Basic impl returns unchanged
    }

    @Test
    @DisplayName("CliHighlight CliHighlightInstance supportsLanguage")
    void highlightInstanceSupportsLanguage() throws Exception {
        CliHighlight.CliHighlightInstance instance =
            CliHighlight.getCliHighlightPromise()
                .get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(instance.supportsLanguage("java"));
        assertFalse(instance.supportsLanguage("xyz"));
    }
}