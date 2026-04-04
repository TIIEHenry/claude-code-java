/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.voice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VoiceKeyterms.
 */
class VoiceKeytermsTest {

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier handles camelCase")
    void splitIdentifierCamelCase() {
        List<String> parts = VoiceKeyterms.splitIdentifier("myVariableName");

        // Fragments of 2 chars or less are discarded: "my" is discarded
        assertTrue(parts.contains("Variable"));
        assertTrue(parts.contains("Name"));
    }

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier handles PascalCase")
    void splitIdentifierPascalCase() {
        List<String> parts = VoiceKeyterms.splitIdentifier("MyClassName");

        // "My" (2 chars) is discarded
        assertTrue(parts.contains("Class"));
        assertTrue(parts.contains("Name"));
    }

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier handles kebab-case")
    void splitIdentifierKebabCase() {
        List<String> parts = VoiceKeyterms.splitIdentifier("my-component-name");

        // "my" (2 chars) is discarded
        assertTrue(parts.contains("component"));
        assertTrue(parts.contains("name"));
    }

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier handles snake_case")
    void splitIdentifierSnakeCase() {
        List<String> parts = VoiceKeyterms.splitIdentifier("my_function_name");

        // "my" (2 chars) is discarded
        assertTrue(parts.contains("function"));
        assertTrue(parts.contains("name"));
    }

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier handles path separators")
    void splitIdentifierPathSeparators() {
        List<String> parts = VoiceKeyterms.splitIdentifier("src/main/java/Service");

        assertTrue(parts.contains("src"));
        assertTrue(parts.contains("main"));
        assertTrue(parts.contains("java"));
        assertTrue(parts.contains("Service"));
    }

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier filters short fragments")
    void splitIdentifierFiltersShort() {
        List<String> parts = VoiceKeyterms.splitIdentifier("aBcDe");

        // Fragments of 2 chars or less are discarded
        // "a" (1 char) is discarded, "Bc" (2 chars) is discarded, "De" (2 chars) is discarded
        assertTrue(parts.stream().allMatch(p -> p.length() > 2));
    }

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier handles null")
    void splitIdentifierNull() {
        List<String> parts = VoiceKeyterms.splitIdentifier(null);

        assertTrue(parts.isEmpty());
    }

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier handles empty string")
    void splitIdentifierEmpty() {
        List<String> parts = VoiceKeyterms.splitIdentifier("");

        assertTrue(parts.isEmpty());
    }

    @Test
    @DisplayName("VoiceKeyterms splitIdentifier filters fragments longer than 20")
    void splitIdentifierFiltersLong() {
        String longName = "a".repeat(25);
        List<String> parts = VoiceKeyterms.splitIdentifier(longName);

        // 25 char fragment is filtered out (> 20)
        assertTrue(parts.isEmpty());
    }

    @Test
    @DisplayName("VoiceKeyterms fileNameWords extracts words from file name")
    void fileNameWords() {
        List<String> words = VoiceKeyterms.fileNameWords("/path/to/MyComponent.tsx");

        // "My" (2 chars) is discarded
        assertTrue(words.contains("Component"));
    }

    @Test
    @DisplayName("VoiceKeyterms fileNameWords removes extension")
    void fileNameWordsRemovesExtension() {
        List<String> words = VoiceKeyterms.fileNameWords("Service.java");

        assertTrue(words.contains("Service"));
        assertFalse(words.contains("java"));
    }

    @Test
    @DisplayName("VoiceKeyterms fileNameWords handles null")
    void fileNameWordsNull() {
        List<String> words = VoiceKeyterms.fileNameWords(null);

        assertTrue(words.isEmpty());
    }

    @Test
    @DisplayName("VoiceKeyterms fileNameWords handles empty")
    void fileNameWordsEmpty() {
        List<String> words = VoiceKeyterms.fileNameWords("");

        assertTrue(words.isEmpty());
    }

    @Test
    @DisplayName("VoiceKeyterms getVoiceKeyterms returns global keyterms")
    void getVoiceKeytermsGlobal() throws Exception {
        CompletableFuture<List<String>> future = VoiceKeyterms.getVoiceKeyterms();
        List<String> keyterms = future.get();

        // Should contain global keyterms
        assertTrue(keyterms.contains("MCP"));
        assertTrue(keyterms.contains("grep"));
        assertTrue(keyterms.contains("regex"));
        assertTrue(keyterms.contains("localhost"));
    }

    @Test
    @DisplayName("VoiceKeyterms getVoiceKeyterms with recent files")
    void getVoiceKeytermsWithRecentFiles() throws Exception {
        Set<String> recentFiles = Set.of(
            "/project/src/UserService.java",
            "/project/src/DataRepository.ts"
        );

        CompletableFuture<List<String>> future = VoiceKeyterms.getVoiceKeyterms(recentFiles);
        List<String> keyterms = future.get();

        // Should contain words from file names
        assertTrue(keyterms.stream().anyMatch(k -> k.contains("User") || k.contains("Service") || k.contains("Data")));
    }

    @Test
    @DisplayName("VoiceKeyterms getVoiceKeyterms limits to MAX_KEYTERMS")
    void getVoiceKeytermsLimited() throws Exception {
        // Create many recent files
        java.util.Set<String> recentFiles = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            recentFiles.add("/project/Service" + i + ".java");
        }

        CompletableFuture<List<String>> future = VoiceKeyterms.getVoiceKeyterms(recentFiles);
        List<String> keyterms = future.get();

        assertTrue(keyterms.size() <= 50); // MAX_KEYTERMS = 50
    }
}