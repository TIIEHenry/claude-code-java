/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/systemPromptSections.ts
 */
package com.anthropic.claudecode.constants;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * System prompt section utilities.
 */
public final class SystemPromptSections {
    private SystemPromptSections() {}

    // Cache for system prompt sections
    private static final Map<String, String> sectionCache = new HashMap<>();

    /**
     * System prompt section record.
     */
    public record SystemPromptSection(
        String name,
        Supplier<String> compute,
        boolean cacheBreak
    ) {}

    /**
     * Create a memoized system prompt section.
     * Computed once, cached until /clear or /compact.
     */
    public static SystemPromptSection systemPromptSection(
        String name,
        Supplier<String> compute
    ) {
        return new SystemPromptSection(name, compute, false);
    }

    /**
     * Create a volatile system prompt section that recomputes every turn.
     * This WILL break the prompt cache when the value changes.
     */
    public static SystemPromptSection dangerousUncachedSystemPromptSection(
        String name,
        Supplier<String> compute,
        String reason
    ) {
        return new SystemPromptSection(name, compute, true);
    }

    /**
     * Resolve all system prompt sections, returning prompt strings.
     */
    public static List<String> resolveSystemPromptSections(
        List<SystemPromptSection> sections
    ) {
        List<String> results = new ArrayList<>();

        for (SystemPromptSection section : sections) {
            if (!section.cacheBreak() && sectionCache.containsKey(section.name())) {
                results.add(sectionCache.get(section.name()));
            } else {
                String value = section.compute().get();
                sectionCache.put(section.name(), value);
                results.add(value);
            }
        }

        return results;
    }

    /**
     * Clear all system prompt section state.
     * Called on /clear and /compact.
     */
    public static void clearSystemPromptSections() {
        sectionCache.clear();
    }
}