/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Attribution.
 */
class AttributionTest {

    private Attribution attribution;

    @BeforeEach
    void setUp() {
        attribution = new Attribution();
    }

    @Test
    @DisplayName("Attribution default author")
    void defaultAuthor() {
        assertNotNull(attribution.getCurrentAuthor());
    }

    @Test
    @DisplayName("Attribution setAuthor changes author")
    void setAuthor() {
        attribution.setAuthor("Test Author", "test@example.com");
        assertEquals("Test Author", attribution.getCurrentAuthor());
    }

    @Test
    @DisplayName("Attribution addAttribution adds entry")
    void addAttribution() {
        attribution.addAttribution("Test description", "test.java", 1, 10, Attribution.AttributionType.CREATED);
        assertEquals(1, attribution.getEntries().size());
    }

    @Test
    @DisplayName("Attribution addFileAttribution adds entry")
    void addFileAttribution() {
        attribution.addFileAttribution("test.java", Attribution.AttributionType.MODIFIED);
        assertEquals(1, attribution.getEntries().size());
    }

    @Test
    @DisplayName("Attribution getEntriesForFile filters")
    void getEntriesForFile() {
        attribution.addFileAttribution("file1.java", Attribution.AttributionType.CREATED);
        attribution.addFileAttribution("file2.java", Attribution.AttributionType.MODIFIED);
        
        List<Attribution.AttributionEntry> entries = attribution.getEntriesForFile("file1.java");
        assertEquals(1, entries.size());
    }

    @Test
    @DisplayName("Attribution getEntriesByAuthor filters")
    void getEntriesByAuthor() {
        attribution.setAuthor("Author1", "a1@example.com");
        attribution.addFileAttribution("file1.java", Attribution.AttributionType.CREATED);
        attribution.setAuthor("Author2", "a2@example.com");
        attribution.addFileAttribution("file2.java", Attribution.AttributionType.MODIFIED);
        
        List<Attribution.AttributionEntry> entries = attribution.getEntriesByAuthor("Author1");
        assertEquals(1, entries.size());
    }

    @Test
    @DisplayName("Attribution getGitTrailers returns co-authors")
    void getGitTrailers() {
        // Add entries with different authors
        attribution.setAuthor("Co-Author", "co@example.com");
        attribution.addFileAttribution("file1.java", Attribution.AttributionType.CREATED);
        // Now set main author - entries with other authors will be co-authors
        attribution.setAuthor("Main Author", "main@example.com");
        attribution.addFileAttribution("file2.java", Attribution.AttributionType.MODIFIED);

        List<String> trailers = attribution.getGitTrailers();
        // Co-Author is not the current author, so should be included
        assertEquals(1, trailers.size());
        assertTrue(trailers.get(0).contains("Co-Author"));
    }

    @Test
    @DisplayName("Attribution clear removes entries")
    void clear() {
        attribution.addFileAttribution("test.java", Attribution.AttributionType.CREATED);
        attribution.clear();
        assertTrue(attribution.getEntries().isEmpty());
    }

    @Test
    @DisplayName("Attribution getSummary returns correct counts")
    void getSummary() {
        attribution.addFileAttribution("file1.java", Attribution.AttributionType.CREATED);
        attribution.addFileAttribution("file2.java", Attribution.AttributionType.MODIFIED);
        
        Attribution.AttributionSummary summary = attribution.getSummary();
        assertEquals(2, summary.totalEntries());
        assertEquals(1, summary.uniqueAuthors());
    }

    @Test
    @DisplayName("Attribution AttributionEntry formatGit")
    void attributionEntryFormatGit() {
        Attribution.AttributionEntry entry = new Attribution.AttributionEntry(
            "Test Author", "test@example.com", "desc", "file.java", 1, 10, 0, Attribution.AttributionType.CREATED
        );
        String formatted = entry.formatGit();
        assertTrue(formatted.contains("Test Author"));
        assertTrue(formatted.contains("test@example.com"));
    }

    @Test
    @DisplayName("Attribution AttributionEntry formatGit with empty email")
    void attributionEntryFormatGitEmptyEmail() {
        Attribution.AttributionEntry entry = new Attribution.AttributionEntry(
            "Test Author", "", "desc", "file.java", 1, 10, 0, Attribution.AttributionType.CREATED
        );
        String formatted = entry.formatGit();
        assertTrue(formatted.contains("no-email@example.com"));
    }

    @Test
    @DisplayName("Attribution AttributionType enum values")
    void attributionTypeEnum() {
        Attribution.AttributionType[] types = Attribution.AttributionType.values();
        assertEquals(7, types.length);
    }

    @Test
    @DisplayName("Attribution AttributionSummary format")
    void attributionSummaryFormat() {
        Map<String, Integer> authorCounts = Map.of("Author1", 2);
        Map<Attribution.AttributionType, Integer> typeCounts = Map.of(Attribution.AttributionType.CREATED, 2);
        
        Attribution.AttributionSummary summary = new Attribution.AttributionSummary(2, 1, authorCounts, typeCounts);
        String formatted = summary.format();
        assertTrue(formatted.contains("Total entries: 2"));
        assertTrue(formatted.contains("Unique authors: 1"));
    }
}
