/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code generated files detection utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Detect generated and vendored files that should be excluded from attribution.
 * Based on GitHub Linguist vendored patterns and common generated file patterns.
 */
public final class GeneratedFiles {
    private GeneratedFiles() {}

    // Exact file name matches (case-insensitive)
    private static final Set<String> EXCLUDED_FILENAMES = Set.of(
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml",
            "bun.lockb",
            "bun.lock",
            "composer.lock",
            "gemfile.lock",
            "cargo.lock",
            "poetry.lock",
            "pipfile.lock",
            "shrinkwrap.json",
            "npm-shrinkwrap.json"
    );

    // File extension patterns (case-insensitive)
    private static final Set<String> EXCLUDED_EXTENSIONS = Set.of(
            ".lock",
            ".min.js",
            ".min.css",
            ".min.html",
            ".bundle.js",
            ".bundle.css",
            ".generated.ts",
            ".generated.js",
            ".d.ts"
    );

    // Directory patterns that indicate generated/vendored content
    private static final List<String> EXCLUDED_DIRECTORIES = List.of(
            "/dist/",
            "/build/",
            "/out/",
            "/output/",
            "/node_modules/",
            "/vendor/",
            "/vendored/",
            "/third_party/",
            "/third-party/",
            "/external/",
            "/.next/",
            "/.nuxt/",
            "/.svelte-kit/",
            "/coverage/",
            "/__pycache__/",
            "/.tox/",
            "/venv/",
            "/.venv/",
            "/target/release/",
            "/target/debug/"
    );

    // Filename patterns using regex
    private static final List<Pattern> EXCLUDED_FILENAME_PATTERNS = List.of(
            Pattern.compile("^.*\\.min\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*-min\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.bundle\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.generated\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.gen\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.auto\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*_generated\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*_gen\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.pb\\.(go|js|ts|py|rb)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*_pb2?\\.py$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.pb\\.h$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.grpc\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.swagger\\.[a-z]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^.*\\.openapi\\.[a-z]+$", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Check if a file should be excluded from attribution.
     *
     * @param filePath Relative file path from repository root
     * @return true if the file should be excluded
     */
    public static boolean isGeneratedFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        // Normalize path separators
        String normalizedPath = "/" + filePath.replace('\\', '/').replaceAll("^/+", "");
        String fileName = Paths.get(filePath).getFileName().toString().toLowerCase();
        String ext = getFileExtension(fileName);

        // Check exact filename matches
        if (EXCLUDED_FILENAMES.contains(fileName)) {
            return true;
        }

        // Check extension matches
        if (!ext.isEmpty() && EXCLUDED_EXTENSIONS.contains(ext)) {
            return true;
        }

        // Check for compound extensions like .min.js
        int dotCount = fileName.length() - fileName.replace(".", "").length();
        if (dotCount > 1) {
            int firstDot = fileName.indexOf('.');
            int lastDot = fileName.lastIndexOf('.');
            if (firstDot != lastDot) {
                String compoundExt = fileName.substring(firstDot);
                if (EXCLUDED_EXTENSIONS.contains(compoundExt)) {
                    return true;
                }
            }
        }

        // Check directory patterns
        for (String dir : EXCLUDED_DIRECTORIES) {
            if (normalizedPath.contains(dir)) {
                return true;
            }
        }

        // Check filename patterns
        for (Pattern pattern : EXCLUDED_FILENAME_PATTERNS) {
            if (pattern.matcher(fileName).matches()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get file extension (including the dot).
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex).toLowerCase();
        }
        return "";
    }

    /**
     * Filter a list of files to exclude generated files.
     *
     * @param files List of file paths
     * @return List of files that are not generated
     */
    public static List<String> filterGeneratedFiles(List<String> files) {
        List<String> result = new ArrayList<>();
        for (String file : files) {
            if (!isGeneratedFile(file)) {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Check if file is a lock file.
     */
    public static boolean isLockFile(String filePath) {
        if (filePath == null) return false;
        String fileName = Paths.get(filePath).getFileName().toString().toLowerCase();
        return EXCLUDED_FILENAMES.contains(fileName) || fileName.endsWith(".lock");
    }

    /**
     * Check if file is in a build output directory.
     */
    public static boolean isInBuildDirectory(String filePath) {
        if (filePath == null) return false;
        String normalizedPath = filePath.replace('\\', '/');

        for (String dir : EXCLUDED_DIRECTORIES) {
            if (normalizedPath.contains(dir)) {
                return true;
            }
        }
        return false;
    }
}