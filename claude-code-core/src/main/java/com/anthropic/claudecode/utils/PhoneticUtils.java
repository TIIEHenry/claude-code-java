/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code soundex
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Phonetic encoding utilities.
 */
public final class PhoneticUtils {
    private PhoneticUtils() {}

    /**
     * Soundex encoding.
     */
    public static String soundex(String s) {
        if (s == null || s.isEmpty()) return "0000";

        s = s.toUpperCase().replaceAll("[^A-Z]", "");
        if (s.isEmpty()) return "0000";

        char first = s.charAt(0);
        StringBuilder result = new StringBuilder();
        result.append(first);

        char prevCode = soundexCode(first);
        for (int i = 1; i < s.length() && result.length() < 4; i++) {
            char code = soundexCode(s.charAt(i));
            if (code != '0' && code != prevCode) {
                result.append(code);
            }
            prevCode = code;
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
     * Metaphone encoding.
     */
    public static String metaphone(String s) {
        if (s == null || s.isEmpty()) return "";

        s = s.toUpperCase().replaceAll("[^A-Z]", "");
        if (s.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        int length = s.length();

        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            char prev = i > 0 ? s.charAt(i - 1) : '\0';
            char next = i < length - 1 ? s.charAt(i + 1) : '\0';

            switch (c) {
                case 'A', 'E', 'I', 'O', 'U' -> {
                    if (i == 0) result.append(c);
                }
                case 'B' -> {
                    if (!(i == length - 1 && prev == 'M')) result.append('B');
                }
                case 'C' -> {
                    if (next == 'H') {
                        result.append('X');
                        i++;
                    } else if (next == 'I' && i + 2 < length && s.charAt(i + 2) == 'A') {
                        result.append('X');
                        i += 2;
                    } else {
                        result.append('K');
                    }
                }
                case 'D' -> {
                    if (next == 'G' && i + 2 < length && "EIY".indexOf(s.charAt(i + 2)) >= 0) {
                        result.append('J');
                        i++;
                    } else {
                        result.append('T');
                    }
                }
                case 'F' -> result.append('F');
                case 'G' -> {
                    if (next == 'H') {
                        if (i + 2 < length && "AEIOU".indexOf(s.charAt(i + 2)) >= 0) {
                            result.append('F');
                        }
                        i++;
                    } else if (next != 'N' && (i == length - 1 || next != 'E' || i + 2 < length)) {
                        if (next == 'I' && i + 2 < length && "AEIOU".indexOf(s.charAt(i + 2)) >= 0) {
                            result.append('J');
                        } else {
                            result.append('K');
                        }
                    }
                }
                case 'H' -> {
                    if (i == 0 || "AEIOU".indexOf(prev) >= 0) {
                        if ("AEIOU".indexOf(next) >= 0) {
                            result.append('H');
                        }
                    }
                }
                case 'J' -> result.append('J');
                case 'K' -> {
                    if (prev != 'C') result.append('K');
                }
                case 'L' -> result.append('L');
                case 'M' -> result.append('M');
                case 'N' -> result.append('N');
                case 'P' -> {
                    if (next == 'H') {
                        result.append('F');
                        i++;
                    } else {
                        result.append('P');
                    }
                }
                case 'Q' -> result.append('K');
                case 'R' -> result.append('R');
                case 'S' -> {
                    if (next == 'H') {
                        result.append('X');
                        i++;
                    } else if (next == 'I' && i + 2 < length && "AO".indexOf(s.charAt(i + 2)) >= 0) {
                        result.append('X');
                        i += 2;
                    } else {
                        result.append('S');
                    }
                }
                case 'T' -> {
                    if (next == 'H') {
                        result.append('0');
                        i++;
                    } else if (!(next == 'I' && i + 2 < length && "AO".indexOf(s.charAt(i + 2)) >= 0)) {
                        result.append('T');
                    }
                }
                case 'V' -> result.append('F');
                case 'W', 'Y' -> {
                    if (i == 0 || "AEIOU".indexOf(next) >= 0) {
                        result.append(c);
                    }
                }
                case 'X' -> result.append("KS");
                case 'Z' -> result.append('S');
            }
        }

        return result.toString();
    }

    /**
     * Double Metaphone encoding.
     */
    public static String[] doubleMetaphone(String s) {
        // Simplified implementation returns primary and alternate
        String primary = metaphone(s);
        return new String[]{primary, primary};
    }

    /**
     * NYSIIS encoding.
     */
    public static String nysiis(String s) {
        if (s == null || s.isEmpty()) return "";

        s = s.toUpperCase().replaceAll("[^A-Z]", "");
        if (s.isEmpty()) return "";

        // First character transformations
        if (s.startsWith("MAC")) s = "MCC" + s.substring(3);
        else if (s.startsWith("KN")) s = "NN" + s.substring(2);
        else if (s.startsWith("K")) s = "C" + s.substring(1);
        else if (s.startsWith("PH") || s.startsWith("PF")) s = "FF" + s.substring(2);
        else if (s.startsWith("SCH")) s = "SSS" + s.substring(3);

        // Last character transformations
        if (s.endsWith("EE") || s.endsWith("IE")) {
            s = s.substring(0, s.length() - 2) + "Y";
        } else if (s.endsWith("DT") || s.endsWith("RT") || s.endsWith("RD") || s.endsWith("NT") || s.endsWith("ND")) {
            s = s.substring(0, s.length() - 2) + "D";
        }

        StringBuilder result = new StringBuilder();
        result.append(s.charAt(0));

        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            char prev = s.charAt(i - 1);

            // Skip vowels
            if ("AEIOU".indexOf(c) >= 0) continue;

            // Transformations
            if (c == 'E' && i + 1 < s.length() && s.charAt(i + 1) == 'V') {
                result.append("AF");
                i++;
            } else if (c == 'Q') {
                result.append('G');
            } else if (c == 'Z') {
                result.append('S');
            } else if (c == 'M') {
                result.append('N');
            } else if (c == 'K') {
                if (i + 1 < s.length() && s.charAt(i + 1) == 'N') {
                    result.append('N');
                    i++;
                } else {
                    result.append('C');
                }
            } else if (c == 'S' && i + 2 < s.length() && s.charAt(i + 1) == 'C' && s.charAt(i + 2) == 'H') {
                result.append("SSS");
                i += 2;
            } else if (c == 'P' && i + 1 < s.length() && s.charAt(i + 1) == 'H') {
                result.append('F');
                i++;
            } else if (c == 'H' && "AEIOU".indexOf(prev) < 0) {
                result.append(prev);
            } else if (c == 'W' && "AEIOU".indexOf(prev) >= 0) {
                result.append(prev);
            } else if (c != prev) {
                result.append(c);
            }
        }

        // Truncate if too long
        String resultStr = result.toString();
        if (resultStr.length() > 6) {
            resultStr = resultStr.substring(0, 6);
        }

        return resultStr;
    }

    /**
     * Caverphone encoding.
     */
    public static String caverphone(String s) {
        if (s == null || s.isEmpty()) return "1111111111";

        s = s.toLowerCase().replaceAll("[^a-z]", "");
        if (s.isEmpty()) return "1111111111";

        // Simplified Caverphone 1.0
        s = s.replaceAll("e$", "");
        s = s.replaceAll("^[aeiou]", "A");
        s = s.replaceAll("^c", "K");
        s = s.replaceAll("^g", "K");
        s = s.replaceAll("ph", "F");
        s = s.replaceAll("v", "F");
        s = s.replaceAll("b", "P");
        s = s.replaceAll("q", "K");
        s = s.replaceAll("x", "K");
        s = s.replaceAll("j", "K");
        s = s.replaceAll("z", "S");

        // Pad to 10 characters
        while (s.length() < 10) {
            s += "1";
        }

        return s.substring(0, 10);
    }

    /**
     * Match rating approach.
     */
    public static String matchRating(String s) {
        if (s == null || s.isEmpty()) return "";

        s = s.toUpperCase().replaceAll("[^A-Z]", "");
        if (s.isEmpty()) return "";

        // Remove vowels
        s = s.replaceAll("[AEIOU]", "");

        // Remove duplicate adjacent consonants
        StringBuilder sb = new StringBuilder();
        char prev = '\0';
        for (char c : s.toCharArray()) {
            if (c != prev) {
                sb.append(c);
                prev = c;
            }
        }

        return sb.toString();
    }

    /**
     * Compare phonetic codes.
     */
    public static boolean phoneticallySimilar(String a, String b, PhoneticAlgorithm algorithm) {
        String codeA = switch (algorithm) {
            case SOUNDEX -> soundex(a);
            case METAPHONE -> metaphone(a);
            case NYSIIS -> nysiis(a);
            case CAVERPHONE -> caverphone(a);
            case MATCH_RATING -> matchRating(a);
        };

        String codeB = switch (algorithm) {
            case SOUNDEX -> soundex(b);
            case METAPHONE -> metaphone(b);
            case NYSIIS -> nysiis(b);
            case CAVERPHONE -> caverphone(b);
            case MATCH_RATING -> matchRating(b);
        };

        return codeA.equals(codeB);
    }

    /**
     * Phonetic algorithm enum.
     */
    public enum PhoneticAlgorithm {
        SOUNDEX, METAPHONE, NYSIIS, CAVERPHONE, MATCH_RATING
    }
}