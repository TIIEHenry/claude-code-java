/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code CLI highlight utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * CLI syntax highlighting utilities.
 * Provides async loading of highlight.js for code highlighting.
 */
public final class CliHighlight {
    private CliHighlight() {}

    private static volatile CompletableFuture<CliHighlightInstance> cliHighlightPromise = null;
    private static volatile Map<String, LanguageInfo> languageRegistry = null;

    /**
     * CLI highlight instance.
     */
    public interface CliHighlightInstance {
        String highlight(String code, String language);
        boolean supportsLanguage(String language);
    }

    /**
     * Language info from highlight.js registry.
     */
    public record LanguageInfo(String name, Set<String> aliases) {}

    /**
     * Get the CLI highlight promise (lazy loaded).
     */
    public static CompletableFuture<CliHighlightInstance> getCliHighlightPromise() {
        if (cliHighlightPromise == null) {
            cliHighlightPromise = loadCliHighlight();
        }
        return cliHighlightPromise;
    }

    /**
     * Load CLI highlight module.
     */
    private static CompletableFuture<CliHighlightInstance> loadCliHighlight() {
        return CompletableFuture.supplyAsync(() -> {
            // In Java, we don't have cli-highlight, but we can provide
            // a basic implementation or use a library like pygments via subprocess
            languageRegistry = loadLanguageRegistry();
            return new BasicCliHighlight();
        });
    }

    /**
     * Load language registry with common file extensions.
     */
    private static Map<String, LanguageInfo> loadLanguageRegistry() {
        Map<String, LanguageInfo> registry = new HashMap<>();

        // Common languages
        registry.put("js", new LanguageInfo("JavaScript", Set.of("js", "javascript")));
        registry.put("ts", new LanguageInfo("TypeScript", Set.of("ts", "typescript")));
        registry.put("java", new LanguageInfo("Java", Set.of("java")));
        registry.put("py", new LanguageInfo("Python", Set.of("py", "python")));
        registry.put("rb", new LanguageInfo("Ruby", Set.of("rb", "ruby")));
        registry.put("go", new LanguageInfo("Go", Set.of("go")));
        registry.put("rs", new LanguageInfo("Rust", Set.of("rs", "rust")));
        registry.put("c", new LanguageInfo("C", Set.of("c")));
        registry.put("cpp", new LanguageInfo("C++", Set.of("cpp", "c++")));
        registry.put("cs", new LanguageInfo("C#", Set.of("cs", "csharp")));
        registry.put("swift", new LanguageInfo("Swift", Set.of("swift")));
        registry.put("kt", new LanguageInfo("Kotlin", Set.of("kt", "kotlin")));
        registry.put("scala", new LanguageInfo("Scala", Set.of("scala")));
        registry.put("php", new LanguageInfo("PHP", Set.of("php")));
        registry.put("html", new LanguageInfo("HTML", Set.of("html")));
        registry.put("css", new LanguageInfo("CSS", Set.of("css")));
        registry.put("json", new LanguageInfo("JSON", Set.of("json")));
        registry.put("yaml", new LanguageInfo("YAML", Set.of("yaml", "yml")));
        registry.put("xml", new LanguageInfo("XML", Set.of("xml")));
        registry.put("sql", new LanguageInfo("SQL", Set.of("sql")));
        registry.put("sh", new LanguageInfo("Shell", Set.of("sh", "bash", "zsh")));
        registry.put("md", new LanguageInfo("Markdown", Set.of("md", "markdown")));
        registry.put("tsx", new LanguageInfo("TypeScriptReact", Set.of("tsx")));
        registry.put("jsx", new LanguageInfo("JavaScriptReact", Set.of("jsx")));

        return registry;
    }

    /**
     * Get language name from file path.
     * e.g., "foo/bar.ts" → "TypeScript"
     */
    public static CompletableFuture<String> getLanguageName(String filePath) {
        return getCliHighlightPromise().thenApply(instance -> {
            String ext = getFileExtension(filePath);
            if (ext.isEmpty()) return "unknown";

            LanguageInfo info = languageRegistry.get(ext.toLowerCase());
            return info != null ? info.name() : "unknown";
        });
    }

    /**
     * Get file extension without the dot.
     */
    private static String getFileExtension(String filePath) {
        String fileName = filePath;
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = filePath.substring(lastSlash + 1);
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    /**
     * Check if a language is supported.
     */
    public static boolean supportsLanguage(String language) {
        if (languageRegistry == null) return false;
        String lower = language.toLowerCase();
        return languageRegistry.containsKey(lower) ||
                languageRegistry.values().stream()
                        .anyMatch(info -> info.aliases().contains(lower));
    }

    /**
     * Basic CLI highlight implementation.
     * In real implementation would use a proper highlighting library.
     */
    private static class BasicCliHighlight implements CliHighlightInstance {
        @Override
        public String highlight(String code, String language) {
            // Basic implementation - no actual highlighting
            // Real implementation would use ANSI escape codes
            return code;
        }

        @Override
        public boolean supportsLanguage(String language) {
            return CliHighlight.supportsLanguage(language);
        }
    }

    /**
     * Reset for testing.
     */
    public static void reset() {
        cliHighlightPromise = null;
        languageRegistry = null;
    }
}