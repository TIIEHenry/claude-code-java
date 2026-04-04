/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code trie utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.stream.*;

/**
 * Trie (prefix tree) data structure.
 */
public final class Trie {
    private final TrieNode root = new TrieNode();
    private int size = 0;

    /**
     * Insert word into trie.
     */
    public void insert(String word) {
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
        }
        if (!current.isEnd) {
            current.isEnd = true;
            size++;
        }
    }

    /**
     * Search for exact word.
     */
    public boolean search(String word) {
        TrieNode node = findNode(word);
        return node != null && node.isEnd;
    }

    /**
     * Check if any word starts with prefix.
     */
    public boolean startsWith(String prefix) {
        return findNode(prefix) != null;
    }

    /**
     * Find node for prefix.
     */
    private TrieNode findNode(String prefix) {
        TrieNode current = root;
        for (char c : prefix.toCharArray()) {
            current = current.children.get(c);
            if (current == null) return null;
        }
        return current;
    }

    /**
     * Get all words with prefix.
     */
    public List<String> getWordsWithPrefix(String prefix) {
        List<String> results = new ArrayList<>();
        TrieNode node = findNode(prefix);
        if (node != null) {
            collectWords(node, prefix, results);
        }
        return results;
    }

    /**
     * Collect all words from node.
     */
    private void collectWords(TrieNode node, String prefix, List<String> results) {
        if (node.isEnd) {
            results.add(prefix);
        }
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            collectWords(entry.getValue(), prefix + entry.getKey(), results);
        }
    }

    /**
     * Get all words.
     */
    public List<String> getAllWords() {
        List<String> results = new ArrayList<>();
        collectWords(root, "", results);
        return results;
    }

    /**
     * Delete word from trie.
     */
    public boolean delete(String word) {
        return delete(root, word, 0);
    }

    private boolean delete(TrieNode current, String word, int index) {
        if (index == word.length()) {
            if (!current.isEnd) return false;
            current.isEnd = false;
            size--;
            return current.children.isEmpty();
        }

        char c = word.charAt(index);
        TrieNode node = current.children.get(c);
        if (node == null) return false;

        boolean shouldDeleteCurrentNode = delete(node, word, index + 1);

        if (shouldDeleteCurrentNode) {
            current.children.remove(c);
            return current.children.isEmpty() && !current.isEnd;
        }

        return false;
    }

    /**
     * Get size (number of words).
     */
    public int size() {
        return size;
    }

    /**
     * Check if empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Clear trie.
     */
    public void clear() {
        root.children.clear();
        size = 0;
    }

    /**
     * Longest common prefix of all words.
     */
    public String longestCommonPrefix() {
        if (isEmpty()) return "";

        StringBuilder prefix = new StringBuilder();
        TrieNode current = root;

        while (current.children.size() == 1 && !current.isEnd) {
            Map.Entry<Character, TrieNode> entry = current.children.entrySet().iterator().next();
            prefix.append(entry.getKey());
            current = entry.getValue();
        }

        return prefix.toString();
    }

    /**
     * Count words with prefix.
     */
    public int countWordsWithPrefix(String prefix) {
        TrieNode node = findNode(prefix);
        if (node == null) return 0;

        int[] count = {0};
        countWords(node, count);
        return count[0];
    }

    private void countWords(TrieNode node, int[] count) {
        if (node.isEnd) count[0]++;
        for (TrieNode child : node.children.values()) {
            countWords(child, count);
        }
    }

    /**
     * Trie node.
     */
    private static final class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();
        private boolean isEnd = false;
    }

    /**
     * Trie utilities.
     */
    public static final class TrieUtils {
        private TrieUtils() {}

        /**
         * Create trie from words.
         */
        public static Trie fromWords(String... words) {
            return fromWords(Arrays.asList(words));
        }

        /**
         * Create trie from collection.
         */
        public static Trie fromWords(Collection<String> words) {
            Trie trie = new Trie();
            words.forEach(trie::insert);
            return trie;
        }

        /**
         * Autocomplete helper.
         */
        public static List<String> autocomplete(Trie trie, String prefix, int limit) {
            return trie.getWordsWithPrefix(prefix).stream()
                .limit(limit)
                .toList();
        }

        /**
         * Fuzzy search - find words within edit distance.
         */
        public static List<String> fuzzySearch(Trie trie, String word, int maxDistance) {
            List<String> results = new ArrayList<>();
            fuzzySearch(trie.root, "", word, maxDistance, results);
            return results;
        }

        private static void fuzzySearch(TrieNode node, String prefix, String target,
                int maxDistance, List<String> results) {
            if (node.isEnd && prefix.length() == target.length()) {
                if (editDistance(prefix, target) <= maxDistance) {
                    results.add(prefix);
                }
            }

            for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
                if (prefix.length() < target.length() + maxDistance) {
                    fuzzySearch(entry.getValue(), prefix + entry.getKey(), target, maxDistance, results);
                }
            }
        }

        private static int editDistance(String a, String b) {
            int m = a.length(), n = b.length();
            int[][] dp = new int[m + 1][n + 1];

            for (int i = 0; i <= m; i++) dp[i][0] = i;
            for (int j = 0; j <= n; j++) dp[0][j] = j;

            for (int i = 1; i <= m; i++) {
                for (int j = 1; j <= n; j++) {
                    if (a.charAt(i - 1) == b.charAt(j - 1)) {
                        dp[i][j] = dp[i - 1][j - 1];
                    } else {
                        dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i][j - 1], dp[i - 1][j]));
                    }
                }
            }

            return dp[m][n];
        }
    }
}