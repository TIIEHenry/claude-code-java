/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code string metrics
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * String similarity metrics.
 */
public final class StringMetrics {
    private StringMetrics() {}

    /**
     * Levenshtein distance.
     */
    public static int levenshtein(String a, String b) {
        int m = a.length();
        int n = b.length();

        if (m == 0) return n;
        if (n == 0) return m;

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    dp[i - 1][j - 1] + cost,
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                );
            }
        }

        return dp[m][n];
    }

    /**
     * Damerau-Levenshtein distance (includes transpositions).
     */
    public static int damerauLevenshtein(String a, String b) {
        int m = a.length();
        int n = b.length();

        if (m == 0) return n;
        if (n == 0) return m;

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    dp[i - 1][j - 1] + cost,
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                );

                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) &&
                    a.charAt(i - 2) == b.charAt(j - 1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + cost);
                }
            }
        }

        return dp[m][n];
    }

    /**
     * Jaro similarity.
     */
    public static double jaro(String a, String b) {
        if (a.equals(b)) return 1.0;

        int m = a.length();
        int n = b.length();

        if (m == 0 || n == 0) return 0.0;

        int range = Math.max(m, n) / 2 - 1;
        range = Math.max(0, range);

        boolean[] aMatched = new boolean[m];
        boolean[] bMatched = new boolean[n];

        int matches = 0;
        int transpositions = 0;

        for (int i = 0; i < m; i++) {
            int start = Math.max(0, i - range);
            int end = Math.min(n - 1, i + range);

            for (int j = start; j <= end; j++) {
                if (bMatched[j] || a.charAt(i) != b.charAt(j)) continue;
                aMatched[i] = true;
                bMatched[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) return 0.0;

        int k = 0;
        for (int i = 0; i < m; i++) {
            if (!aMatched[i]) continue;
            while (!bMatched[k]) k++;
            if (a.charAt(i) != b.charAt(k)) transpositions++;
            k++;
        }

        return (matches / (double) m + matches / (double) n +
                (matches - transpositions / 2.0) / matches) / 3.0;
    }

    /**
     * Jaro-Winkler similarity.
     */
    public static double jaroWinkler(String a, String b) {
        double jaro = jaro(a, b);

        int prefix = 0;
        int maxPrefix = Math.min(4, Math.min(a.length(), b.length()));
        for (int i = 0; i < maxPrefix; i++) {
            if (a.charAt(i) == b.charAt(i)) {
                prefix++;
            } else {
                break;
            }
        }

        return jaro + prefix * 0.1 * (1 - jaro);
    }

    /**
     * Hamming distance (equal length strings only).
     */
    public static int hamming(String a, String b) {
        if (a.length() != b.length()) {
            throw new IllegalArgumentException("Strings must have equal length");
        }

        int distance = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) distance++;
        }
        return distance;
    }

    /**
     * Cosine similarity (using character n-grams).
     */
    public static double cosineSimilarity(String a, String b, int n) {
        Map<String, Integer> gramsA = nGrams(a, n);
        Map<String, Integer> gramsB = nGrams(b, n);

        Set<String> allGrams = new HashSet<>();
        allGrams.addAll(gramsA.keySet());
        allGrams.addAll(gramsB.keySet());

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (String gram : allGrams) {
            int countA = gramsA.getOrDefault(gram, 0);
            int countB = gramsB.getOrDefault(gram, 0);
            dotProduct += countA * countB;
            normA += countA * countA;
            normB += countB * countB;
        }

        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Generate n-grams.
     */
    private static Map<String, Integer> nGrams(String s, int n) {
        Map<String, Integer> grams = new HashMap<>();
        for (int i = 0; i <= s.length() - n; i++) {
            String gram = s.substring(i, i + n);
            grams.merge(gram, 1, Integer::sum);
        }
        return grams;
    }

    /**
     * Sørensen-Dice coefficient.
     */
    public static double sorensenDice(String a, String b) {
        Set<String> bigramsA = bigramSet(a);
        Set<String> bigramsB = bigramSet(b);

        Set<String> intersection = new HashSet<>(bigramsA);
        intersection.retainAll(bigramsB);

        if (bigramsA.isEmpty() && bigramsB.isEmpty()) return 1.0;
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0;

        return 2.0 * intersection.size() / (bigramsA.size() + bigramsB.size());
    }

    private static Set<String> bigramSet(String s) {
        Set<String> bigrams = new HashSet<>();
        for (int i = 0; i < s.length() - 1; i++) {
            bigrams.add(s.substring(i, i + 2));
        }
        return bigrams;
    }

    /**
     * Soundex code.
     */
    public static String soundex(String s) {
        if (s.isEmpty()) return "";

        s = s.toUpperCase();
        StringBuilder result = new StringBuilder();
        result.append(s.charAt(0));

        char prev = s.charAt(0);
        for (int i = 1; i < s.length() && result.length() < 4; i++) {
            char c = s.charAt(i);
            char code = soundexCode(c);
            if (code != '0' && code != soundexCode(prev)) {
                result.append(code);
            }
            prev = c;
        }

        while (result.length() < 4) {
            result.append('0');
        }

        return result.toString();
    }

    private static char soundexCode(char c) {
        return switch (c) {
            case 'B', 'F', 'P', 'V' -> '1';
            case 'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2';
            case 'D', 'T' -> '3';
            case 'L' -> '4';
            case 'M', 'N' -> '5';
            case 'R' -> '6';
            default -> '0';
        };
    }

    /**
     * Metaphone code.
     */
    public static String metaphone(String s) {
        if (s.isEmpty()) return "";

        s = s.toUpperCase();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (i == 0) {
                if (c == 'K' && i + 1 < s.length() && s.charAt(i + 1) == 'N') continue;
                if (c == 'W' && i + 1 < s.length() && s.charAt(i + 1) == 'R') continue;
            }

            switch (c) {
                case 'B', 'P' -> result.append(c);
                case 'C' -> {
                    if (i + 1 < s.length() && s.charAt(i + 1) == 'H') {
                        result.append('X');
                        i++;
                    } else {
                        result.append('K');
                    }
                }
                case 'D', 'T' -> result.append('T');
                case 'F' -> result.append('F');
                case 'G' -> result.append('K');
                case 'H' -> {
                    if (i > 0 && isVowel(s.charAt(i - 1))) result.append('H');
                }
                case 'J', 'K' -> result.append('K');
                case 'L', 'M', 'N' -> result.append(c);
                case 'R' -> result.append('R');
                case 'S' -> {
                    if (i + 1 < s.length() && s.charAt(i + 1) == 'H') {
                        result.append('X');
                        i++;
                    } else {
                        result.append('S');
                    }
                }
                case 'V' -> result.append('F');
                case 'W', 'Y' -> {}
                case 'Z' -> result.append('S');
                default -> {}
            }
        }

        return result.toString();
    }

    private static boolean isVowel(char c) {
        return "AEIOU".indexOf(c) >= 0;
    }
}