/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code regex utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;
import java.util.function.*;

/**
 * Regex pattern utilities.
 */
public final class RegexUtils {
    private RegexUtils() {}

    /**
     * Compile pattern with default flags.
     */
    public static Pattern compile(String pattern) {
        return Pattern.compile(pattern);
    }

    /**
     * Compile pattern with case insensitive flag.
     */
    public static Pattern compileIgnoreCase(String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Compile pattern with multiline flag.
     */
    public static Pattern compileMultiline(String pattern) {
        return Pattern.compile(pattern, Pattern.MULTILINE);
    }

    /**
     * Compile pattern with dotall flag.
     */
    public static Pattern compileDotall(String pattern) {
        return Pattern.compile(pattern, Pattern.DOTALL);
    }

    /**
     * Check if pattern matches entire string.
     */
    public static boolean matches(String input, String pattern) {
        return Pattern.matches(pattern, input);
    }

    /**
     * Check if pattern matches entire string.
     */
    public static boolean matches(String input, Pattern pattern) {
        return pattern.matcher(input).matches();
    }

    /**
     * Check if string contains pattern.
     */
    public static boolean contains(String input, String pattern) {
        return Pattern.compile(pattern).matcher(input).find();
    }

    /**
     * Check if string contains pattern.
     */
    public static boolean contains(String input, Pattern pattern) {
        return pattern.matcher(input).find();
    }

    /**
     * Find first match.
     */
    public static Optional<String> findFirst(String input, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    /**
     * Find first match with group.
     */
    public static Optional<String> findFirstGroup(String input, String pattern, int group) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        return matcher.find() ? Optional.of(matcher.group(group)) : Optional.empty();
    }

    /**
     * Find all matches.
     */
    public static List<String> findAll(String input, String pattern) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    /**
     * Find all matches with groups.
     */
    public static List<List<String>> findAllGroups(String input, String pattern) {
        List<List<String>> results = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        while (matcher.find()) {
            List<String> groups = new ArrayList<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                groups.add(matcher.group(i));
            }
            results.add(groups);
        }
        return results;
    }

    /**
     * Find all group matches for specific group.
     */
    public static List<String> findAllGroup(String input, String pattern, int group) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        while (matcher.find()) {
            matches.add(matcher.group(group));
        }
        return matches;
    }

    /**
     * Replace all matches.
     */
    public static String replaceAll(String input, String pattern, String replacement) {
        return input.replaceAll(pattern, replacement);
    }

    /**
     * Replace all matches.
     */
    public static String replaceAll(String input, Pattern pattern, String replacement) {
        return pattern.matcher(input).replaceAll(replacement);
    }

    /**
     * Replace first match.
     */
    public static String replaceFirst(String input, String pattern, String replacement) {
        return input.replaceFirst(pattern, replacement);
    }

    /**
     * Replace with function.
     */
    public static String replaceAll(String input, String pattern, Function<Matcher, String> replacer) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, replacer.apply(matcher));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Split string.
     */
    public static String[] split(String input, String pattern) {
        return input.split(pattern);
    }

    /**
     * Split string with limit.
     */
    public static String[] split(String input, String pattern, int limit) {
        return input.split(pattern, limit);
    }

    /**
     * Split and trim.
     */
    public static List<String> splitAndTrim(String input, String pattern) {
        return Arrays.stream(input.split(pattern))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * Count matches.
     */
    public static int countMatches(String input, String pattern) {
        int count = 0;
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Extract groups as map.
     */
    public static Map<String, String> extractGroups(String input, String pattern, String... groupNames) {
        Map<String, String> result = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        if (matcher.find()) {
            for (int i = 0; i < groupNames.length && i < matcher.groupCount(); i++) {
                result.put(groupNames[i], matcher.group(i + 1));
            }
        }
        return result;
    }

    /**
     * Named groups extraction (simulated).
     */
    public static Map<String, String> namedGroups(String input, Pattern pattern) {
        Map<String, String> result = new LinkedHashMap<>();
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            // Java doesn't support getting named groups by reflection easily
            // This is a simplified version
            for (int i = 1; i <= matcher.groupCount(); i++) {
                result.put("group" + i, matcher.group(i));
            }
        }
        return result;
    }

    /**
     * Escape regex special characters.
     */
    public static String escape(String literal) {
        return Pattern.quote(literal);
    }

    /**
     * Create glob pattern.
     */
    public static Pattern globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                case '.', '^', '$', '+', '|', '(', ')', '[', ']', '{', '}', '\\' ->
                    regex.append("\\").append(c);
                default -> regex.append(c);
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }

    /**
     * Test glob pattern.
     */
    public static boolean globMatches(String input, String glob) {
        return globToRegex(glob).matcher(input).matches();
    }

    /**
     * Match groups and transform.
     */
    public static <T> Optional<T> matchAndTransform(String input, String pattern,
            Function<Matcher, T> transform) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        if (matcher.matches()) {
            return Optional.of(transform.apply(matcher));
        }
        return Optional.empty();
    }

    /**
     * Find and process each match.
     */
    public static void forEachMatch(String input, String pattern, Consumer<Matcher> consumer) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        while (matcher.find()) {
            consumer.accept(matcher);
        }
    }

    /**
     * Find match positions.
     */
    public static List<MatchPosition> findMatchPositions(String input, String pattern) {
        List<MatchPosition> positions = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        while (matcher.find()) {
            positions.add(new MatchPosition(matcher.start(), matcher.end(), matcher.group()));
        }
        return positions;
    }

    /**
     * Match position record.
     */
    public record MatchPosition(int start, int end, String match) {
        public int length() {
            return end - start;
        }
    }

    /**
     * Validate pattern.
     */
    public static boolean isValidPattern(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * Get pattern error message.
     */
    public static String getPatternError(String pattern) {
        try {
            Pattern.compile(pattern);
            return null;
        } catch (PatternSyntaxException e) {
            return e.getMessage();
        }
    }

    /**
     * Common patterns.
     */
    public static final class Patterns {
        public static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        );
        public static final Pattern URL = Pattern.compile(
            "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$"
        );
        public static final Pattern IP_ADDRESS = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        );
        public static final Pattern IPV6 = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
        );
        public static final Pattern UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        );
        public static final Pattern INTEGER = Pattern.compile("^-?\\d+$");
        public static final Pattern DECIMAL = Pattern.compile("^-?\\d+(\\.\\d+)?$");
        public static final Pattern ALPHANUMERIC = Pattern.compile("^[A-Za-z0-9]+$");
        public static final Pattern ALPHA = Pattern.compile("^[A-Za-z]+$");
        public static final Pattern NUMERIC = Pattern.compile("^\\d+$");
        public static final Pattern WHITESPACE = Pattern.compile("\\s+");
        public static final Pattern NON_WHITESPACE = Pattern.compile("\\S+");
        public static final Pattern WORD = Pattern.compile("\\w+");
        public static final Pattern CREDIT_CARD = Pattern.compile(
            "^\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}$"
        );
        public static final Pattern PHONE = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$"
        );
        public static final Pattern DATE_ISO = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}$"
        );
        public static final Pattern DATETIME_ISO = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"
        );
        public static final Pattern TIME_24H = Pattern.compile(
            "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$"
        );
        public static final Pattern HEX_COLOR = Pattern.compile(
            "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"
        );
        public static final Pattern USERNAME = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_]{2,19}$"
        );
        public static final Pattern PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"
        );
        public static final Pattern JSON_STRING = Pattern.compile(
            "^\"(?:[^\"\\\\]|\\\\.)*\"$"
        );
        public static final Pattern SEMVER = Pattern.compile(
            "^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z0-9.-]+))?(?:\\+([a-zA-Z0-9.-]+))?$"
        );
        public static final Pattern ANSI_ESCAPE = Pattern.compile(
            "\u001B\\[[;\\d]*m"
        );
    }

    /**
     * Test email pattern.
     */
    public static boolean isEmail(String input) {
        return Patterns.EMAIL.matcher(input).matches();
    }

    /**
     * Test URL pattern.
     */
    public static boolean isUrl(String input) {
        return Patterns.URL.matcher(input).matches();
    }

    /**
     * Test UUID pattern.
     */
    public static boolean isUuid(String input) {
        return Patterns.UUID.matcher(input).matches();
    }

    /**
     * Test integer pattern.
     */
    public static boolean isInteger(String input) {
        return Patterns.INTEGER.matcher(input).matches();
    }

    /**
     * Test decimal pattern.
     */
    public static boolean isDecimal(String input) {
        return Patterns.DECIMAL.matcher(input).matches();
    }
}