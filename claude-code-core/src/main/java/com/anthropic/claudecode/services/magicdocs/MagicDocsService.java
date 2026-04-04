/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/magicDocs
 */
package com.anthropic.claudecode.services.magicdocs;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

/**
 * Magic docs service - Document processing and analysis.
 */
public final class MagicDocsService {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Document type enum.
     */
    public enum DocumentType {
        README,
        API_DOC,
        TUTORIAL,
        CHANGELOG,
        LICENSE,
        CONFIG,
        CODE,
        MARKDOWN,
        JSON,
        YAML,
        XML,
        UNKNOWN
    }

    /**
     * Document analysis result record.
     */
    public record DocumentAnalysis(
        String title,
        String summary,
        DocumentType type,
        List<String> sections,
        List<String> keywords,
        Map<String, String> metadata,
        double relevanceScore
    ) {
        public static DocumentAnalysis empty() {
            return new DocumentAnalysis("", "", DocumentType.UNKNOWN,
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), 0.0);
        }
    }

    /**
     * Analyze document.
     */
    public CompletableFuture<DocumentAnalysis> analyzeDocument(Path path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.exists(path)) {
                    return DocumentAnalysis.empty();
                }

                String content = Files.readString(path);
                return analyzeContent(content, path.getFileName().toString());
            } catch (Exception e) {
                return DocumentAnalysis.empty();
            }
        }, executor);
    }

    /**
     * Analyze content.
     */
    private DocumentAnalysis analyzeContent(String content, String fileName) {
        DocumentType type = detectDocumentType(fileName, content);
        String title = extractTitle(content);
        List<String> sections = extractSections(content);
        List<String> keywords = extractKeywords(content);
        String summary = generateSummary(content);

        return new DocumentAnalysis(
            title,
            summary,
            type,
            sections,
            keywords,
            new HashMap<>(),
            calculateRelevance(content)
        );
    }

    /**
     * Detect document type.
     */
    private DocumentType detectDocumentType(String fileName, String content) {
        String lower = fileName.toLowerCase();

        if (lower.contains("readme")) return DocumentType.README;
        if (lower.contains("changelog") || lower.contains("changes")) return DocumentType.CHANGELOG;
        if (lower.contains("license")) return DocumentType.LICENSE;
        if (lower.contains("api")) return DocumentType.API_DOC;
        if (lower.contains("tutorial") || lower.contains("guide")) return DocumentType.TUTORIAL;
        if (lower.endsWith(".json")) return DocumentType.JSON;
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return DocumentType.YAML;
        if (lower.endsWith(".xml")) return DocumentType.XML;
        if (lower.endsWith(".md")) return DocumentType.MARKDOWN;
        if (lower.endsWith(".java") || lower.endsWith(".ts") || lower.endsWith(".js")) return DocumentType.CODE;

        if (content.contains("api") || content.contains("endpoint")) return DocumentType.API_DOC;
        if (content.contains("tutorial") || content.contains("getting started")) return DocumentType.TUTORIAL;

        return DocumentType.UNKNOWN;
    }

    /**
     * Extract title.
     */
    private String extractTitle(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("# ")) {
                return line.substring(2);
            }
        }
        return "";
    }

    /**
     * Extract sections.
     */
    private List<String> extractSections(String content) {
        List<String> sections = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("## ")) {
                sections.add(line.substring(3));
            } else if (line.startsWith("### ")) {
                sections.add("  " + line.substring(4));
            }
        }

        return sections;
    }

    /**
     * Extract keywords.
     */
    private List<String> extractKeywords(String content) {
        Map<String, Integer> wordCount = new HashMap<>();

        String[] words = content.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .split("\\s+");

        for (String word : words) {
            if (word.length() > 3) {
                wordCount.merge(word, 1, Integer::sum);
            }
        }

        return wordCount.entrySet()
            .stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(10)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Generate summary.
     */
    private String generateSummary(String content) {
        // Simple summary: first non-empty paragraph
        String[] paragraphs = content.split("\n\n");
        for (String para : paragraphs) {
            para = para.trim();
            if (!para.isEmpty() && !para.startsWith("#") && !para.startsWith("```")) {
                if (para.length() > 200) {
                    return para.substring(0, 200) + "...";
                }
                return para;
            }
        }
        return "";
    }

    /**
     * Calculate relevance.
     */
    private double calculateRelevance(String content) {
        int score = 0;

        if (content.contains("API")) score += 20;
        if (content.contains("usage")) score += 10;
        if (content.contains("example")) score += 15;
        if (content.contains("install")) score += 10;
        if (content.contains("config")) score += 10;

        return Math.min(100, score);
    }

    /**
     * Batch analyze documents.
     */
    public CompletableFuture<Map<Path, DocumentAnalysis>> analyzeDocuments(List<Path> paths) {
        Map<Path, DocumentAnalysis> results = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = paths.stream()
            .map(path -> analyzeDocument(path)
                .thenAccept(analysis -> results.put(path, analysis)))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> results);
    }

    /**
     * Shutdown service.
     */
    public void shutdown() {
        executor.shutdown();
    }
}