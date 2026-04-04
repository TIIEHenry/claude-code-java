/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/attribution
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Attribution - Attribution tracking for commits and changes.
 */
public final class Attribution {
    private final List<AttributionEntry> entries = new ArrayList<>();
    private String currentAuthor = System.getProperty("user.name", "unknown");
    private String currentEmail = "";

    /**
     * Attribution entry record.
     */
    public record AttributionEntry(
        String author,
        String email,
        String description,
        String filePath,
        int lineStart,
        int lineEnd,
        long timestamp,
        AttributionType type
    ) {
        public String formatGit() {
            return String.format(
                "Co-authored-by: %s <%s>",
                author,
                email.isEmpty() ? "no-email@example.com" : email
            );
        }
    }

    /**
     * Attribution type enum.
     */
    public enum AttributionType {
        CREATED,
        MODIFIED,
        REFACTORED,
        DOCUMENTED,
        TESTED,
        REVIEWED,
        GENERATED
    }

    /**
     * Set current author.
     */
    public void setAuthor(String author, String email) {
        this.currentAuthor = author;
        this.currentEmail = email;
    }

    /**
     * Get current author.
     */
    public String getCurrentAuthor() {
        return currentAuthor;
    }

    /**
     * Add attribution.
     */
    public void addAttribution(
        String description,
        String filePath,
        int lineStart,
        int lineEnd,
        AttributionType type
    ) {
        entries.add(new AttributionEntry(
            currentAuthor,
            currentEmail,
            description,
            filePath,
            lineStart,
            lineEnd,
            System.currentTimeMillis(),
            type
        ));
    }

    /**
     * Add file attribution.
     */
    public void addFileAttribution(String filePath, AttributionType type) {
        addAttribution("", filePath, 0, 0, type);
    }

    /**
     * Get all entries.
     */
    public List<AttributionEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Get entries for file.
     */
    public List<AttributionEntry> getEntriesForFile(String filePath) {
        return entries.stream()
            .filter(e -> e.filePath().equals(filePath))
            .toList();
    }

    /**
     * Get entries by author.
     */
    public List<AttributionEntry> getEntriesByAuthor(String author) {
        return entries.stream()
            .filter(e -> e.author().equals(author))
            .toList();
    }

    /**
     * Get git co-author trailers.
     */
    public List<String> getGitTrailers() {
        Set<String> authors = new HashSet<>();
        List<String> trailers = new ArrayList<>();

        for (AttributionEntry entry : entries) {
            if (!entry.author().equals(currentAuthor) && !authors.contains(entry.author())) {
                authors.add(entry.author());
                trailers.add(entry.formatGit());
            }
        }

        return trailers;
    }

    /**
     * Clear attributions.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Attribution summary.
     */
    public record AttributionSummary(
        int totalEntries,
        int uniqueAuthors,
        Map<String, Integer> authorCounts,
        Map<AttributionType, Integer> typeCounts
    ) {
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("Total entries: ").append(totalEntries).append("\n");
            sb.append("Unique authors: ").append(uniqueAuthors).append("\n");
            sb.append("By type:\n");
            typeCounts.forEach((type, count) ->
                sb.append("  ").append(type.name()).append(": ").append(count).append("\n")
            );
            return sb.toString();
        }
    }

    /**
     * Get summary.
     */
    public AttributionSummary getSummary() {
        Map<String, Integer> authorCounts = new HashMap<>();
        Map<AttributionType, Integer> typeCounts = new HashMap<>();

        for (AttributionEntry entry : entries) {
            authorCounts.merge(entry.author(), 1, Integer::sum);
            typeCounts.merge(entry.type(), 1, Integer::sum);
        }

        return new AttributionSummary(
            entries.size(),
            authorCounts.size(),
            authorCounts,
            typeCounts
        );
    }
}