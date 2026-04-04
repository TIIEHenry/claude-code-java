/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.magicdocs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MagicDocsService.
 */
class MagicDocsServiceTest {

    private final MagicDocsService service = new MagicDocsService();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("MagicDocsService DocumentType enum")
    void documentTypeEnum() {
        MagicDocsService.DocumentType[] types = MagicDocsService.DocumentType.values();
        assertEquals(12, types.length);
        assertEquals(MagicDocsService.DocumentType.README,
            MagicDocsService.DocumentType.valueOf("README"));
        assertEquals(MagicDocsService.DocumentType.API_DOC,
            MagicDocsService.DocumentType.valueOf("API_DOC"));
    }

    @Test
    @DisplayName("MagicDocsService DocumentAnalysis empty")
    void documentAnalysisEmpty() {
        MagicDocsService.DocumentAnalysis empty = MagicDocsService.DocumentAnalysis.empty();

        assertEquals("", empty.title());
        assertEquals("", empty.summary());
        assertEquals(MagicDocsService.DocumentType.UNKNOWN, empty.type());
        assertTrue(empty.sections().isEmpty());
        assertTrue(empty.keywords().isEmpty());
        assertEquals(0.0, empty.relevanceScore(), 0.01);
    }

    @Test
    @DisplayName("MagicDocsService DocumentAnalysis record")
    void documentAnalysisRecord() {
        MagicDocsService.DocumentAnalysis analysis = new MagicDocsService.DocumentAnalysis(
            "Test Title",
            "Test summary",
            MagicDocsService.DocumentType.README,
            List.of("Section 1", "Section 2"),
            List.of("keyword1", "keyword2"),
            Map.of("key", "value"),
            50.0
        );

        assertEquals("Test Title", analysis.title());
        assertEquals("Test summary", analysis.summary());
        assertEquals(MagicDocsService.DocumentType.README, analysis.type());
        assertEquals(2, analysis.sections().size());
        assertEquals(2, analysis.keywords().size());
        assertEquals(50.0, analysis.relevanceScore(), 0.01);
    }

    @Test
    @DisplayName("MagicDocsService analyzeDocument nonexistent file")
    void analyzeDocumentNonexistent() throws Exception {
        Path path = tempDir.resolve("nonexistent.md");

        MagicDocsService.DocumentAnalysis result = service.analyzeDocument(path).get();

        assertEquals(MagicDocsService.DocumentType.UNKNOWN, result.type());
    }

    @Test
    @DisplayName("MagicDocsService analyzeDocument markdown file")
    void analyzeDocumentMarkdown() throws Exception {
        Path path = tempDir.resolve("README.md");
        String content = "# Test Title\n\nThis is a test document.\n\n## Section 1\n\nContent here.";
        java.nio.file.Files.writeString(path, content);

        MagicDocsService.DocumentAnalysis result = service.analyzeDocument(path).get();

        assertEquals("Test Title", result.title());
        assertEquals(MagicDocsService.DocumentType.README, result.type());
        assertFalse(result.sections().isEmpty());
    }

    @Test
    @DisplayName("MagicDocsService analyzeDocument JSON file")
    void analyzeDocumentJson() throws Exception {
        Path path = tempDir.resolve("config.json");
        java.nio.file.Files.writeString(path, "{\"key\": \"value\"}");

        MagicDocsService.DocumentAnalysis result = service.analyzeDocument(path).get();

        assertEquals(MagicDocsService.DocumentType.JSON, result.type());
    }

    @Test
    @DisplayName("MagicDocsService analyzeDocument YAML file")
    void analyzeDocumentYaml() throws Exception {
        Path path = tempDir.resolve("config.yaml");
        java.nio.file.Files.writeString(path, "key: value");

        MagicDocsService.DocumentAnalysis result = service.analyzeDocument(path).get();

        assertEquals(MagicDocsService.DocumentType.YAML, result.type());
    }

    @Test
    @DisplayName("MagicDocsService analyzeDocuments batch")
    void analyzeDocumentsBatch() throws Exception {
        Path file1 = tempDir.resolve("README.md");
        Path file2 = tempDir.resolve("config.json");

        java.nio.file.Files.writeString(file1, "# Test\n\nContent");
        java.nio.file.Files.writeString(file2, "{\"key\": \"value\"}");

        Map<Path, MagicDocsService.DocumentAnalysis> results = service.analyzeDocuments(
            List.of(file1, file2)
        ).get();

        assertEquals(2, results.size());
        assertEquals(MagicDocsService.DocumentType.README, results.get(file1).type());
        assertEquals(MagicDocsService.DocumentType.JSON, results.get(file2).type());
    }

    @Test
    @DisplayName("MagicDocsService shutdown does not throw")
    void shutdown() {
        service.shutdown();
    }
}