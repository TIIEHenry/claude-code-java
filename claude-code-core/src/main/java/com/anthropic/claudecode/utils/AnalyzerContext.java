/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/analyzerContext
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.file.*;

/**
 * Analyzer context - Context for code analysis operations.
 */
public final class AnalyzerContext {
    private final Path projectRoot;
    private final Map<String, Object> settings;
    private final List<Path> includePaths;
    private final List<Path> excludePaths;
    private final Map<String, String> definedSymbols;

    public AnalyzerContext(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.settings = new HashMap<>();
        this.includePaths = new ArrayList<>();
        this.excludePaths = new ArrayList<>();
        this.definedSymbols = new HashMap<>();
    }

    /**
     * Get project root.
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /**
     * Get a setting.
     */
    public Object getSetting(String key) {
        return settings.get(key);
    }

    /**
     * Get a setting with default.
     */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, T defaultValue) {
        Object value = settings.get(key);
        if (value == null) return defaultValue;
        return (T) value;
    }

    /**
     * Set a setting.
     */
    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    /**
     * Add include path.
     */
    public void addIncludePath(Path path) {
        includePaths.add(path);
    }

    /**
     * Add exclude path.
     */
    public void addExcludePath(Path path) {
        excludePaths.add(path);
    }

    /**
     * Check if path is included.
     */
    public boolean isIncluded(Path path) {
        if (includePaths.isEmpty()) return true;
        return includePaths.stream().anyMatch(p -> path.startsWith(p));
    }

    /**
     * Check if path is excluded.
     */
    public boolean isExcluded(Path path) {
        return excludePaths.stream().anyMatch(p -> path.startsWith(p));
    }

    /**
     * Check if path should be analyzed.
     */
    public boolean shouldAnalyze(Path path) {
        return isIncluded(path) && !isExcluded(path);
    }

    /**
     * Define a symbol.
     */
    public void defineSymbol(String name, String value) {
        definedSymbols.put(name, value);
    }

    /**
     * Get symbol value.
     */
    public String getSymbol(String name) {
        return definedSymbols.get(name);
    }

    /**
     * Check if symbol is defined.
     */
    public boolean isSymbolDefined(String name) {
        return definedSymbols.containsKey(name);
    }

    /**
     * Expand symbols in text.
     */
    public String expandSymbols(String text) {
        if (text == null) return null;

        String result = text;
        for (Map.Entry<String, String> entry : definedSymbols.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
            result = result.replace("$" + entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Create a child context.
     */
    public AnalyzerContext createChild(Path subPath) {
        AnalyzerContext child = new AnalyzerContext(subPath);
        child.settings.putAll(this.settings);
        child.includePaths.addAll(this.includePaths);
        child.excludePaths.addAll(this.excludePaths);
        child.definedSymbols.putAll(this.definedSymbols);
        return child;
    }

    /**
     * Get all settings.
     */
    public Map<String, Object> getAllSettings() {
        return Collections.unmodifiableMap(settings);
    }

    /**
     * Get include paths.
     */
    public List<Path> getIncludePaths() {
        return Collections.unmodifiableList(includePaths);
    }

    /**
     * Get exclude paths.
     */
    public List<Path> getExcludePaths() {
        return Collections.unmodifiableList(excludePaths);
    }
}