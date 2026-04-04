/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code path matcher utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Path matching utilities.
 */
public final class PathMatcherUtils {
    private PathMatcherUtils() {}

    /**
     * Create glob pattern matcher.
     */
    public static Predicate<Path> glob(String pattern) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        return matcher::matches;
    }

    /**
     * Create regex pattern matcher.
     */
    public static Predicate<Path> regex(String pattern) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
        return matcher::matches;
    }

    /**
     * Match against multiple patterns (any match).
     */
    public static Predicate<Path> anyMatch(List<String> patterns) {
        List<Predicate<Path>> matchers = patterns.stream()
            .map(PathMatcherUtils::glob)
            .toList();
        return path -> matchers.stream().anyMatch(m -> m.test(path));
    }

    /**
     * Match against multiple patterns (all match).
     */
    public static Predicate<Path> allMatch(List<String> patterns) {
        List<Predicate<Path>> matchers = patterns.stream()
            .map(PathMatcherUtils::glob)
            .toList();
        return path -> matchers.stream().allMatch(m -> m.test(path));
    }

    /**
     * Match against include patterns, excluding exclude patterns.
     */
    public static Predicate<Path> matchIncludeExclude(List<String> include, List<String> exclude) {
        Predicate<Path> includeMatcher = include.isEmpty() ? p -> true : anyMatch(include);
        Predicate<Path> excludeMatcher = exclude.isEmpty() ? p -> false : anyMatch(exclude);
        return path -> includeMatcher.test(path) && !excludeMatcher.test(path);
    }

    /**
     * Match file extension.
     */
    public static Predicate<Path> extension(String... extensions) {
        Set<String> exts = Arrays.stream(extensions)
            .map(e -> e.startsWith(".") ? e : "." + e)
            .collect(Collectors.toSet());
        return path -> exts.contains(getExtension(path));
    }

    /**
     * Get file extension.
     */
    public static String getExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot) : "";
    }

    /**
     * Get file name without extension.
     */
    public static String getBaseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * Match by file name.
     */
    public static Predicate<Path> fileName(String name) {
        return path -> path.getFileName().toString().equals(name);
    }

    /**
     * Match by file name pattern.
     */
    public static Predicate<Path> fileNameGlob(String pattern) {
        Pattern regexPattern = Pattern.compile(
            globToRegex(pattern),
            Pattern.CASE_INSENSITIVE
        );
        return path -> regexPattern.matcher(path.getFileName().toString()).matches();
    }

    /**
     * Match by directory name.
     */
    public static Predicate<Path> inDirectory(String dirName) {
        return path -> {
            for (Path parent : path) {
                if (parent.getFileName() != null &&
                    parent.getFileName().toString().equals(dirName)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Match by depth level.
     */
    public static Predicate<Path> depth(int min, int max) {
        return path -> {
            int depth = path.getNameCount();
            return depth >= min && depth <= max;
        };
    }

    /**
     * Match hidden files.
     */
    public static Predicate<Path> hidden() {
        return path -> path.getFileName().toString().startsWith(".");
    }

    /**
     * Match visible (non-hidden) files.
     */
    public static Predicate<Path> visible() {
        return path -> !path.getFileName().toString().startsWith(".");
    }

    /**
     * Match directories.
     */
    public static Predicate<Path> isDirectory() {
        return Files::isDirectory;
    }

    /**
     * Match regular files.
     */
    public static Predicate<Path> isRegularFile() {
        return Files::isRegularFile;
    }

    /**
     * Match symbolic links.
     */
    public static Predicate<Path> isSymbolicLink() {
        return Files::isSymbolicLink;
    }

    /**
     * Match readable files.
     */
    public static Predicate<Path> isReadable() {
        return Files::isReadable;
    }

    /**
     * Match writable files.
     */
    public static Predicate<Path> isWritable() {
        return Files::isWritable;
    }

    /**
     * Match executable files.
     */
    public static Predicate<Path> isExecutable() {
        return Files::isExecutable;
    }

    /**
     * Match by size range.
     */
    public static Predicate<Path> sizeRange(long minBytes, long maxBytes) {
        return path -> {
            try {
                long size = Files.size(path);
                return size >= minBytes && size <= maxBytes;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Match empty files.
     */
    public static Predicate<Path> isEmpty() {
        return path -> {
            try {
                return Files.size(path) == 0;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Match non-empty files.
     */
    public static Predicate<Path> nonEmpty() {
        return path -> {
            try {
                return Files.size(path) > 0;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Match by last modified time.
     */
    public static Predicate<Path> modifiedWithin(long millis) {
        return path -> {
            try {
                long lastModified = Files.getLastModifiedTime(path).toMillis();
                long now = System.currentTimeMillis();
                return (now - lastModified) <= millis;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Match files modified before given time.
     */
    public static Predicate<Path> modifiedBefore(long timestamp) {
        return path -> {
            try {
                return Files.getLastModifiedTime(path).toMillis() < timestamp;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Match files modified after given time.
     */
    public static Predicate<Path> modifiedAfter(long timestamp) {
        return path -> {
            try {
                return Files.getLastModifiedTime(path).toMillis() > timestamp;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Convert glob pattern to regex.
     */
    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> {
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        regex.append(".*");
                        i++;
                    } else {
                        regex.append("[^/]*");
                    }
                }
                case '?' -> regex.append("[^/]");
                case '[' -> {
                    regex.append('[');
                    while (i + 1 < glob.length() && glob.charAt(++i) != ']') {
                        regex.append(glob.charAt(i));
                    }
                    regex.append(']');
                }
                case '{' -> {
                    regex.append('(');
                    int j = i + 1;
                    while (j < glob.length() && glob.charAt(j) != '}') {
                        if (glob.charAt(j) == ',') {
                            regex.append('|');
                        } else {
                            regex.append(glob.charAt(j));
                        }
                        j++;
                    }
                    regex.append(')');
                    i = j;
                }
                case '.', '$', '^', '+', '|' -> regex.append('\\').append(c);
                default -> regex.append(c);
            }
        }
        return regex.toString();
    }

    /**
     * Simplified glob pattern (like simple glob, not path glob).
     */
    public static Predicate<String> simpleGlob(String pattern) {
        Pattern regex = Pattern.compile(
            simpleGlobToRegex(pattern),
            Pattern.CASE_INSENSITIVE
        );
        return regex.asMatchPredicate();
    }

    /**
     * Convert simple glob to regex (no path separators).
     */
    private static String simpleGlobToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        regex.append('^');
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                case '[' -> {
                    regex.append('[');
                    while (i + 1 < glob.length() && glob.charAt(++i) != ']') {
                        regex.append(glob.charAt(i));
                    }
                    regex.append(']');
                }
                case '{' -> {
                    regex.append('(');
                    int j = i + 1;
                    while (j < glob.length() && glob.charAt(j) != '}') {
                        if (glob.charAt(j) == ',') {
                            regex.append('|');
                        } else {
                            regex.append(glob.charAt(j));
                        }
                        j++;
                    }
                    regex.append(')');
                    i = j;
                }
                case '.', '$', '^', '+', '|' -> regex.append('\\').append(c);
                default -> regex.append(c);
            }
        }
        regex.append('$');
        return regex.toString();
    }

    /**
     * Combine matchers.
     */
    public static Predicate<Path> combine(Predicate<Path>... matchers) {
        return path -> Arrays.stream(matchers).allMatch(m -> m.test(path));
    }

    /**
     * Negate matcher.
     */
    public static Predicate<Path> not(Predicate<Path> matcher) {
        return matcher.negate();
    }

    /**
     * Path starts with.
     */
    public static Predicate<Path> startsWith(Path prefix) {
        return path -> path.startsWith(prefix);
    }

    /**
     * Path ends with.
     */
    public static Predicate<Path> endsWith(String suffix) {
        return path -> path.toString().endsWith(suffix);
    }

    /**
     * Path contains.
     */
    public static Predicate<Path> contains(String substring) {
        return path -> path.toString().contains(substring);
    }
}