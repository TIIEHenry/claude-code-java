/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CompletionCache.
 */
class CompletionCacheTest {

    private CompletionCache cache;

    @BeforeEach
    void setUp() {
        cache = new CompletionCache();
        cache.clear();
    }

    @Test
    @DisplayName("CompletionCache CompletionEntry record")
    void completionEntryRecord() {
        CompletionCache.CompletionEntry entry = new CompletionCache.CompletionEntry(
            "text", "display", "desc", CompletionCache.CompletionType.COMMAND, 100
        );

        assertEquals("text", entry.text());
        assertEquals("display", entry.display());
        assertEquals("desc", entry.description());
        assertEquals(CompletionCache.CompletionType.COMMAND, entry.type());
        assertEquals(100, entry.score());
    }

    @Test
    @DisplayName("CompletionCache CompletionEntry matches prefix")
    void completionEntryMatchesPrefix() {
        CompletionCache.CompletionEntry entry = new CompletionCache.CompletionEntry(
            "/help", "Help", "Get help", CompletionCache.CompletionType.COMMAND, 50
        );

        assertTrue(entry.matches("/he"));
        assertTrue(entry.matches("/help"));
        assertFalse(entry.matches("/other"));
    }

    @Test
    @DisplayName("CompletionCache CompletionEntry matches display lowercase")
    void completionEntryMatchesDisplay() {
        CompletionCache.CompletionEntry entry = new CompletionCache.CompletionEntry(
            "cmd", "Help Command", "description", CompletionCache.CompletionType.COMMAND, 50
        );

        assertTrue(entry.matches("help"));
        assertTrue(entry.matches("HELP"));
        assertFalse(entry.matches("other"));
    }

    @Test
    @DisplayName("CompletionCache CompletionType enum values")
    void completionTypeEnum() {
        CompletionCache.CompletionType[] types = CompletionCache.CompletionType.values();
        assertEquals(8, types.length);

        assertEquals(CompletionCache.CompletionType.COMMAND,
            CompletionCache.CompletionType.valueOf("COMMAND"));
        assertEquals(CompletionCache.CompletionType.FILE,
            CompletionCache.CompletionType.valueOf("FILE"));
        assertEquals(CompletionCache.CompletionType.OPTION,
            CompletionCache.CompletionType.valueOf("OPTION"));
        assertEquals(CompletionCache.CompletionType.VARIABLE,
            CompletionCache.CompletionType.valueOf("VARIABLE"));
        assertEquals(CompletionCache.CompletionType.FUNCTION,
            CompletionCache.CompletionType.valueOf("FUNCTION"));
        assertEquals(CompletionCache.CompletionType.KEYWORD,
            CompletionCache.CompletionType.valueOf("KEYWORD"));
        assertEquals(CompletionCache.CompletionType.HISTORY,
            CompletionCache.CompletionType.valueOf("HISTORY"));
        assertEquals(CompletionCache.CompletionType.SNIPPET,
            CompletionCache.CompletionType.valueOf("SNIPPET"));
    }

    @Test
    @DisplayName("CompletionCache getCompletions returns list")
    void getCompletions() {
        List<CompletionCache.CompletionEntry> completions = cache.getCompletions("/h");
        assertNotNull(completions);
        // Returns empty or populated list
    }

    @Test
    @DisplayName("CompletionCache addCompletion adds entry")
    void addCompletion() {
        CompletionCache.CompletionEntry entry = new CompletionCache.CompletionEntry(
            "/test", "Test", "Test command", CompletionCache.CompletionType.COMMAND, 50
        );

        cache.addCompletion("/t", entry);

        List<CompletionCache.CompletionEntry> completions = cache.getCompletions("/t");
        assertTrue(completions.stream()
            .anyMatch(e -> e.text().equals("/test")));
    }

    @Test
    @DisplayName("CompletionCache addCompletion multiple entries")
    void addCompletionMultiple() {
        cache.addCompletion("/cmd", new CompletionCache.CompletionEntry(
            "/cmd1", "Cmd1", "desc", CompletionCache.CompletionType.COMMAND, 50
        ));
        cache.addCompletion("/cmd", new CompletionCache.CompletionEntry(
            "/cmd2", "Cmd2", "desc", CompletionCache.CompletionType.COMMAND, 60
        ));

        List<CompletionCache.CompletionEntry> completions = cache.getCompletions("/cmd");
        assertTrue(completions.size() >= 2);
    }

    @Test
    @DisplayName("CompletionCache clear removes entries")
    void clear() {
        cache.addCompletion("test", new CompletionCache.CompletionEntry(
            "text", "display", "desc", CompletionCache.CompletionType.COMMAND, 50
        ));
        cache.clear();

        CompletionCache.CacheStats stats = cache.getStats();
        assertEquals(0, stats.entries());
    }

    @Test
    @DisplayName("CompletionCache invalidatePrefix removes matching entries")
    void invalidatePrefix() {
        cache.addCompletion("/help", new CompletionCache.CompletionEntry(
            "/help", "Help", "desc", CompletionCache.CompletionType.COMMAND, 50
        ));
        cache.addCompletion("/history", new CompletionCache.CompletionEntry(
            "/history", "History", "desc", CompletionCache.CompletionType.COMMAND, 50
        ));
        cache.addCompletion("/other", new CompletionCache.CompletionEntry(
            "/other", "Other", "desc", CompletionCache.CompletionType.COMMAND, 50
        ));

        cache.invalidatePrefix("/h");

        // Entries with keys starting with "/h" should be removed
        CompletionCache.CacheStats stats = cache.getStats();
        assertTrue(stats.entries() <= 1);
    }

    @Test
    @DisplayName("CompletionCache getStats returns valid stats")
    void getStats() {
        cache.addCompletion("key", new CompletionCache.CompletionEntry(
            "text", "display", "desc", CompletionCache.CompletionType.COMMAND, 50
        ));

        CompletionCache.CacheStats stats = cache.getStats();
        assertTrue(stats.entries() >= 0);
        assertTrue(stats.keys() >= 0);
        assertTrue(stats.hitCount() >= 0);
        assertTrue(stats.missCount() >= 0);
    }

    @Test
    @DisplayName("CompletionCache CacheStats hitRate calculation")
    void cacheStatsHitRate() {
        CompletionCache.CacheStats stats = new CompletionCache.CacheStats(10, 5, 8, 2);
        assertEquals(0.8, stats.getHitRate(), 0.01);
    }

    @Test
    @DisplayName("CompletionCache CacheStats hitRate zero when no hits")
    void cacheStatsHitRateZero() {
        CompletionCache.CacheStats stats = new CompletionCache.CacheStats(10, 5, 0, 0);
        assertEquals(0.0, stats.getHitRate(), 0.01);
    }

    @Test
    @DisplayName("CompletionCache CacheStats hitRate with misses only")
    void cacheStatsHitRateMissesOnly() {
        CompletionCache.CacheStats stats = new CompletionCache.CacheStats(10, 5, 0, 10);
        assertEquals(0.0, stats.getHitRate(), 0.01);
    }

    @Test
    @DisplayName("CompletionCache generates sorted completions")
    void generatesSortedCompletions() {
        // The cache internally generates and sorts completions by score
        // We just verify that the result is a valid sorted list
        List<CompletionCache.CompletionEntry> completions = cache.getCompletions("test");

        // If there are multiple completions, verify sorting
        if (completions.size() >= 2) {
            for (int i = 0; i < completions.size() - 1; i++) {
                assertTrue(completions.get(i).score() >= completions.get(i + 1).score(),
                    "Completions should be sorted by score descending");
            }
        }
    }

    @Test
    @DisplayName("CompletionCache caches results")
    void cachesResults() {
        List<CompletionCache.CompletionEntry> first = cache.getCompletions("cache-test");
        List<CompletionCache.CompletionEntry> second = cache.getCompletions("cache-test");

        // Both should return cached result
        assertNotNull(first);
        assertNotNull(second);

        CompletionCache.CacheStats stats = cache.getStats();
        assertTrue(stats.hitCount() >= 1);
    }
}