/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code LCS utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Longest Common Subsequence utilities.
 */
public final class LcsUtils {
    private LcsUtils() {}

    /**
     * Find LCS of two lists.
     */
    public static <T> List<T> lcs(List<T> a, List<T> b) {
        return lcs(a, b, Objects::equals);
    }

    /**
     * Find LCS with custom equality.
     */
    public static <T> List<T> lcs(List<T> a, List<T> b, java.util.function.BiPredicate<T, T> equals) {
        int m = a.size();
        int n = b.size();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (equals.test(a.get(i - 1), b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Backtrack
        List<T> result = new ArrayList<>();
        int i = m, j = n;

        while (i > 0 && j > 0) {
            if (equals.test(a.get(i - 1), b.get(j - 1))) {
                result.add(0, a.get(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        return result;
    }

    /**
     * LCS length.
     */
    public static <T> int lcsLength(List<T> a, List<T> b) {
        return lcsLength(a, b, Objects::equals);
    }

    /**
     * LCS length with custom equality.
     */
    public static <T> int lcsLength(List<T> a, List<T> b, java.util.function.BiPredicate<T, T> equals) {
        int m = a.size();
        int n = b.size();

        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (equals.test(a.get(i - 1), b.get(j - 1))) {
                    curr[j] = prev[j - 1] + 1;
                } else {
                    curr[j] = Math.max(prev[j], curr[j - 1]);
                }
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[n];
    }

    /**
     * LCS of strings.
     */
    public static String lcsString(String a, String b) {
        List<Character> charsA = new ArrayList<>();
        for (char c : a.toCharArray()) charsA.add(c);

        List<Character> charsB = new ArrayList<>();
        for (char c : b.toCharArray()) charsB.add(c);

        List<Character> lcsChars = lcs(charsA, charsB);
        StringBuilder sb = new StringBuilder();
        for (char c : lcsChars) sb.append(c);
        return sb.toString();
    }

    /**
     * All LCSs (can be multiple).
     */
    public static <T> Set<List<T>> allLcs(List<T> a, List<T> b) {
        return allLcs(a, b, Objects::equals);
    }

    /**
     * All LCSs with custom equality.
     */
    public static <T> Set<List<T>> allLcs(List<T> a, List<T> b, java.util.function.BiPredicate<T, T> equals) {
        int m = a.size();
        int n = b.size();

        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (equals.test(a.get(i - 1), b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        Set<List<T>> result = new HashSet<>();
        backtrackAll(a, b, equals, dp, m, n, new ArrayList<>(), result);
        return result;
    }

    private static <T> void backtrackAll(List<T> a, List<T> b,
            java.util.function.BiPredicate<T, T> equals, int[][] dp,
            int i, int j, List<T> current, Set<List<T>> result) {

        if (i == 0 || j == 0) {
            List<T> reversed = new ArrayList<>(current);
            Collections.reverse(reversed);
            result.add(reversed);
            return;
        }

        if (equals.test(a.get(i - 1), b.get(j - 1))) {
            current.add(a.get(i - 1));
            backtrackAll(a, b, equals, dp, i - 1, j - 1, current, result);
            current.remove(current.size() - 1);
        } else {
            if (dp[i - 1][j] >= dp[i][j - 1]) {
                backtrackAll(a, b, equals, dp, i - 1, j, current, result);
            }
            if (dp[i][j - 1] >= dp[i - 1][j]) {
                backtrackAll(a, b, equals, dp, i, j - 1, current, result);
            }
        }
    }

    /**
     * Similarity ratio based on LCS.
     */
    public static <T> double similarity(List<T> a, List<T> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        int maxLen = Math.max(a.size(), b.size());
        if (maxLen == 0) return 1.0;
        return (double) lcsLength(a, b) / maxLen;
    }

    /**
     * Diff ratio (proportion of differences).
     */
    public static <T> double diffRatio(List<T> a, List<T> b) {
        return 1.0 - similarity(a, b);
    }

    /**
     * Shortest common supersequence.
     */
    public static <T> List<T> scs(List<T> a, List<T> b) {
        return scs(a, b, Objects::equals);
    }

    /**
     * Shortest common supersequence with custom equality.
     */
    public static <T> List<T> scs(List<T> a, List<T> b, java.util.function.BiPredicate<T, T> equals) {
        List<T> lcs = lcs(a, b, equals);
        List<T> result = new ArrayList<>();

        int i = 0, j = 0, k = 0;

        while (k < lcs.size()) {
            while (i < a.size() && !equals.test(a.get(i), lcs.get(k))) {
                result.add(a.get(i++));
            }
            while (j < b.size() && !equals.test(b.get(j), lcs.get(k))) {
                result.add(b.get(j++));
            }
            result.add(lcs.get(k++));
            i++;
            j++;
        }

        while (i < a.size()) result.add(a.get(i++));
        while (j < b.size()) result.add(b.get(j++));

        return result;
    }
}