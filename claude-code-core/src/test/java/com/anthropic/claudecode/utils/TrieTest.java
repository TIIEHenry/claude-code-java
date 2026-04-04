/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Trie.
 */
class TrieTest {

    @Test
    @DisplayName("Trie creates empty")
    void createsEmpty() {
        Trie trie = new Trie();

        assertTrue(trie.isEmpty());
        assertEquals(0, trie.size());
    }

    @Test
    @DisplayName("Trie insert adds word")
    void insertAddsWord() {
        Trie trie = new Trie();

        trie.insert("hello");

        assertEquals(1, trie.size());
        assertTrue(trie.search("hello"));
    }

    @Test
    @DisplayName("Trie insert doesn't duplicate")
    void insertNoDuplicate() {
        Trie trie = new Trie();

        trie.insert("hello");
        trie.insert("hello");

        assertEquals(1, trie.size());
    }

    @Test
    @DisplayName("Trie search finds exact word")
    void searchExact() {
        Trie trie = new Trie();

        trie.insert("hello");
        trie.insert("hell");

        assertTrue(trie.search("hello"));
        assertTrue(trie.search("hell"));
        assertFalse(trie.search("he")); // Not a complete word
    }

    @Test
    @DisplayName("Trie search returns false for missing")
    void searchMissing() {
        Trie trie = new Trie();

        trie.insert("hello");

        assertFalse(trie.search("world"));
        assertFalse(trie.search("hel"));
    }

    @Test
    @DisplayName("Trie startsWith checks prefix")
    void startsWithWorks() {
        Trie trie = new Trie();

        trie.insert("hello");

        assertTrue(trie.startsWith("hel"));
        assertTrue(trie.startsWith("hello"));
        assertFalse(trie.startsWith("world"));
    }

    @Test
    @DisplayName("Trie getWordsWithPrefix returns matches")
    void getWordsWithPrefix() {
        Trie trie = new Trie();

        trie.insert("hello");
        trie.insert("hell");
        trie.insert("help");
        trie.insert("world");

        List<String> words = trie.getWordsWithPrefix("hel");

        assertEquals(3, words.size());
        assertTrue(words.contains("hello"));
        assertTrue(words.contains("hell"));
        assertTrue(words.contains("help"));
    }

    @Test
    @DisplayName("Trie getAllWords returns all")
    void getAllWords() {
        Trie trie = new Trie();

        trie.insert("a");
        trie.insert("b");
        trie.insert("c");

        List<String> words = trie.getAllWords();

        assertEquals(3, words.size());
        assertTrue(words.contains("a"));
        assertTrue(words.contains("b"));
        assertTrue(words.contains("c"));
    }

    @Test
    @DisplayName("Trie delete removes word")
    void deleteWorks() {
        Trie trie = new Trie();

        trie.insert("hello");
        trie.insert("hell");

        // delete returns true if nodes were deleted, false if only isEnd was cleared
        trie.delete("hello");
        assertFalse(trie.search("hello"));
        assertTrue(trie.search("hell")); // Still exists
        assertEquals(1, trie.size());
    }

    @Test
    @DisplayName("Trie delete returns false for missing")
    void deleteMissing() {
        Trie trie = new Trie();

        trie.insert("hello");

        assertFalse(trie.delete("world"));
        assertEquals(1, trie.size());
    }

    @Test
    @DisplayName("Trie clear removes all")
    void clearWorks() {
        Trie trie = new Trie();

        trie.insert("hello");
        trie.insert("world");
        trie.clear();

        assertTrue(trie.isEmpty());
        assertEquals(0, trie.size());
    }

    @Test
    @DisplayName("Trie longestCommonPrefix returns common prefix")
    void longestCommonPrefix() {
        Trie trie = new Trie();

        trie.insert("flower");
        trie.insert("flow");
        trie.insert("flight");

        assertEquals("fl", trie.longestCommonPrefix());
    }

    @Test
    @DisplayName("Trie longestCommonPrefix empty when trie empty")
    void longestCommonPrefixEmpty() {
        Trie trie = new Trie();

        assertEquals("", trie.longestCommonPrefix());
    }

    @Test
    @DisplayName("Trie countWordsWithPrefix counts matches")
    void countWordsWithPrefix() {
        Trie trie = new Trie();

        trie.insert("hello");
        trie.insert("hell");
        trie.insert("help");
        trie.insert("world");

        assertEquals(3, trie.countWordsWithPrefix("hel"));
        assertEquals(1, trie.countWordsWithPrefix("world"));
        assertEquals(0, trie.countWordsWithPrefix("xyz"));
    }

    @Test
    @DisplayName("Trie TrieUtils fromWords creates trie")
    void trieUtilsFromWords() {
        Trie trie = Trie.TrieUtils.fromWords("hello", "world");

        assertEquals(2, trie.size());
        assertTrue(trie.search("hello"));
        assertTrue(trie.search("world"));
    }

    @Test
    @DisplayName("Trie TrieUtils autocomplete returns limited")
    void trieUtilsAutocomplete() {
        Trie trie = new Trie();
        trie.insert("hello");
        trie.insert("hell");
        trie.insert("help");
        trie.insert("helper");

        List<String> results = Trie.TrieUtils.autocomplete(trie, "hel", 2);

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Trie TrieUtils fuzzySearch finds close matches")
    void trieUtilsFuzzySearch() {
        Trie trie = Trie.TrieUtils.fromWords("hello", "world", "help");

        List<String> results = Trie.TrieUtils.fuzzySearch(trie, "hello", 1);

        assertTrue(results.contains("hello"));
        // May also contain close matches like "help" depending on edit distance
    }

    @Test
    @DisplayName("Trie handles empty string")
    void handlesEmptyString() {
        Trie trie = new Trie();

        trie.insert("");

        assertTrue(trie.search(""));
        assertTrue(trie.startsWith(""));
    }
}