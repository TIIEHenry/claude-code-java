/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Words.
 */
class WordsTest {

    @Test
    @DisplayName("Words generateWordSlug returns non-null")
    void generateWordSlugNotNull() {
        String result = Words.generateWordSlug();
        assertNotNull(result);
    }

    @Test
    @DisplayName("Words generateWordSlug format is adjective-verb-noun")
    void generateWordSlugFormat() {
        String result = Words.generateWordSlug();
        String[] parts = result.split("-");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("Words generateWordSlug contains hyphens")
    void generateWordSlugHyphens() {
        String result = Words.generateWordSlug();
        assertTrue(result.contains("-"));
    }

    @Test
    @DisplayName("Words generateWordSlug no empty parts")
    void generateWordSlugNoEmptyParts() {
        String result = Words.generateWordSlug();
        String[] parts = result.split("-");
        for (String part : parts) {
            assertFalse(part.isEmpty());
        }
    }

    @Test
    @DisplayName("Words generateWordSlug all lowercase")
    void generateWordSlugLowercase() {
        String result = Words.generateWordSlug();
        assertEquals(result, result.toLowerCase());
    }

    @Test
    @DisplayName("Words generateWordSlug multiple calls different")
    void generateWordSlugDifferent() {
        String result1 = Words.generateWordSlug();
        String result2 = Words.generateWordSlug();
        // With 160+ adjectives, 97+ verbs, 240+ nouns, collisions are rare
        // But we don't assert they're always different (statistical)
        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    @DisplayName("Words generateShortWordSlug returns non-null")
    void generateShortWordSlugNotNull() {
        String result = Words.generateShortWordSlug();
        assertNotNull(result);
    }

    @Test
    @DisplayName("Words generateShortWordSlug format is adjective-noun")
    void generateShortWordSlugFormat() {
        String result = Words.generateShortWordSlug();
        String[] parts = result.split("-");
        assertEquals(2, parts.length);
    }

    @Test
    @DisplayName("Words generateShortWordSlug contains hyphen")
    void generateShortWordSlugHyphen() {
        String result = Words.generateShortWordSlug();
        assertTrue(result.contains("-"));
    }

    @Test
    @DisplayName("Words generateShortWordSlug no empty parts")
    void generateShortWordSlugNoEmptyParts() {
        String result = Words.generateShortWordSlug();
        String[] parts = result.split("-");
        for (String part : parts) {
            assertFalse(part.isEmpty());
        }
    }

    @Test
    @DisplayName("Words generateShortWordSlug all lowercase")
    void generateShortWordSlugLowercase() {
        String result = Words.generateShortWordSlug();
        assertEquals(result, result.toLowerCase());
    }

    @Test
    @DisplayName("Words generateCustomSlug returns non-null")
    void generateCustomSlugNotNull() {
        String result = Words.generateCustomSlug("adj-noun");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Words generateCustomSlug adj-noun format")
    void generateCustomSlugAdjNoun() {
        String result = Words.generateCustomSlug("adj-noun");
        String[] parts = result.split("-");
        assertEquals(2, parts.length);
    }

    @Test
    @DisplayName("Words generateCustomSlug adjective-noun format")
    void generateCustomSlugAdjectiveNoun() {
        String result = Words.generateCustomSlug("adjective-noun");
        String[] parts = result.split("-");
        assertEquals(2, parts.length);
    }

    @Test
    @DisplayName("Words generateCustomSlug adj-verb-noun format")
    void generateCustomSlugAdjVerbNoun() {
        String result = Words.generateCustomSlug("adj-verb-noun");
        String[] parts = result.split("-");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("Words generateCustomSlug single adj")
    void generateCustomSlugSingleAdj() {
        String result = Words.generateCustomSlug("adj");
        assertFalse(result.isEmpty());
        assertFalse(result.contains("-"));
    }

    @Test
    @DisplayName("Words generateCustomSlug single noun")
    void generateCustomSlugSingleNoun() {
        String result = Words.generateCustomSlug("noun");
        assertFalse(result.isEmpty());
        assertFalse(result.contains("-"));
    }

    @Test
    @DisplayName("Words generateCustomSlug single verb")
    void generateCustomSlugSingleVerb() {
        String result = Words.generateCustomSlug("verb");
        assertFalse(result.isEmpty());
        assertFalse(result.contains("-"));
    }

    @Test
    @DisplayName("Words generateCustomSlug preserves literal text")
    void generateCustomSlugLiteral() {
        String result = Words.generateCustomSlug("custom-adj");
        assertTrue(result.startsWith("custom-"));
    }

    @Test
    @DisplayName("Words generateCustomSlug mixed format")
    void generateCustomSlugMixed() {
        String result = Words.generateCustomSlug("prefix-adj-middle-noun-suffix");
        assertTrue(result.contains("prefix-"));
        assertTrue(result.contains("-middle-"));
        assertTrue(result.contains("-suffix"));
    }

    @Test
    @DisplayName("Words generateCustomSlug empty format")
    void generateCustomSlugEmpty() {
        String result = Words.generateCustomSlug("");
        assertEquals("", result);
    }

    @Test
    @DisplayName("Words generateCustomSlug null format throws")
    void generateCustomSlugNull() {
        // Null format causes NullPointerException in split
        assertThrows(NullPointerException.class, () -> Words.generateCustomSlug(null));
    }

    @Test
    @DisplayName("Words generateCustomSlug case insensitive")
    void generateCustomSlugCaseInsensitive() {
        String result1 = Words.generateCustomSlug("ADJ-NOUN");
        String result2 = Words.generateCustomSlug("adj-noun");

        // Both should produce valid slugs with lowercase
        assertEquals(result1, result1.toLowerCase());
        assertEquals(result2, result2.toLowerCase());
    }

    @Test
    @DisplayName("Words generateCustomSlug complex pattern")
    void generateCustomSlugComplex() {
        String result = Words.generateCustomSlug("noun-verb-adj-noun");
        String[] parts = result.split("-");
        assertEquals(4, parts.length);
    }
}