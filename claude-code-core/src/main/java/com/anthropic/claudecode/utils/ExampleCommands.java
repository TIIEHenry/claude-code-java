/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code example commands utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

/**
 * Example commands utilities for generating helpful command suggestions.
 */
public final class ExampleCommands {
    private ExampleCommands() {}

    // Patterns that mark a file as non-core
    private static final List<Pattern> NON_CORE_PATTERNS = List.of(
            // lock / dependency manifests
            Pattern.compile("(?:^|/)(?:package-lock\\.json|yarn\\.lock|bun\\.lock|bun\\.lockb|pnpm-lock\\.yaml|Pipfile\\.lock|poetry\\.lock|Cargo\\.lock|Gemfile\\.lock|go\\.sum|composer\\.lock|uv\\.lock)$"),
            // generated / build artifacts
            Pattern.compile("\\.generated\\."),
            Pattern.compile("(?:^|/)(?:dist|build|out|target|node_modules|\\.next|__pycache__)/"),
            Pattern.compile("\\.(?:min\\.js|min\\.css|map|pyc|pyo)$"),
            // data / docs / config extensions
            Pattern.compile("\\.(?:json|ya?ml|toml|xml|ini|cfg|conf|env|lock|txt|md|mdx|rst|csv|log|svg)$", Pattern.CASE_INSENSITIVE),
            // configuration / metadata
            Pattern.compile("(?:^|/)\\.?(?:eslintrc|prettierrc|babelrc|editorconfig|gitignore|gitattributes|dockerignore|npmrc)"),
            Pattern.compile("(?:^|/)(?:tsconfig|jsconfig|biome|vitest\\.config|jest\\.config|webpack\\.config|vite\\.config|rollup\\.config)\\.[a-z]+$"),
            Pattern.compile("(?:^|/)\\.(?:github|vscode|idea|claude)/"),
            // docs / changelogs
            Pattern.compile("(?:^|/)(?:CHANGELOG|LICENSE|CONTRIBUTING|CODEOWNERS|README)(?:\\.[a-z]+)?$", Pattern.CASE_INSENSITIVE)
    );

    private static final long ONE_WEEK_MS = 7L * 24 * 60 * 60 * 1000;

    /**
     * Check if a file is a core file (not auto-generated, dependency, or config).
     */
    public static boolean isCoreFile(String path) {
        for (Pattern pattern : NON_CORE_PATTERNS) {
            if (pattern.matcher(path).find()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Count and sort items by frequency.
     */
    public static String countAndSortItems(List<String> items, int topN) {
        Map<String, Integer> counts = new HashMap<>();
        for (String item : items) {
            counts.merge(item, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topN)
                .map(e -> String.format("%6d %s", e.getValue(), e.getKey()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    /**
     * Pick diverse core files from a sorted list.
     */
    public static List<String> pickDiverseCoreFiles(List<String> sortedPaths, int want) {
        List<String> picked = new ArrayList<>();
        Set<String> seenBasenames = new HashSet<>();
        Map<String, Integer> dirTally = new HashMap<>();

        for (int cap = 1; picked.size() < want && cap <= want; cap++) {
            for (String p : sortedPaths) {
                if (picked.size() >= want) break;
                if (!isCoreFile(p)) continue;

                int lastSep = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
                String base = lastSep >= 0 ? p.substring(lastSep + 1) : p;
                if (base.isEmpty() || seenBasenames.contains(base)) continue;

                String dir = lastSep >= 0 ? p.substring(0, lastSep) : ".";
                int dirCount = dirTally.getOrDefault(dir, 0);
                if (dirCount >= cap) continue;

                picked.add(base);
                seenBasenames.add(base);
                dirTally.put(dir, dirCount + 1);
            }
        }

        return picked.size() >= want ? picked : List.of();
    }

    /**
     * Get example commands for a file.
     */
    public static List<String> getExampleCommands(String frequentFile) {
        String file = frequentFile != null ? frequentFile : "<filepath>";

        return List.of(
                "fix lint errors",
                "fix typecheck errors",
                "how does " + file + " work?",
                "refactor " + file,
                "how do I log an error?",
                "edit " + file + " to...",
                "write a test for " + file,
                "create a util logging.py that..."
        );
    }

    /**
     * Get a random example command.
     */
    public static String getRandomExampleCommand(String frequentFile) {
        List<String> commands = getExampleCommands(frequentFile);
        Random random = new Random();
        return "Try \"" + commands.get(random.nextInt(commands.size())) + "\"";
    }

    /**
     * Get example command from cache.
     */
    public static String getExampleCommandFromCache(List<String> exampleFiles) {
        String frequentFile = exampleFiles != null && !exampleFiles.isEmpty()
                ? exampleFiles.get(new Random().nextInt(exampleFiles.size()))
                : "<filepath>";

        return getRandomExampleCommand(frequentFile);
    }

    /**
     * Check if example files need refresh.
     */
    public static boolean needsRefresh(Long lastGenerated) {
        if (lastGenerated == null) return true;
        return System.currentTimeMillis() - lastGenerated > ONE_WEEK_MS;
    }

    /**
     * Find frequently modified files using git log.
     */
    public static List<String> findFrequentlyModifiedFiles(Path cwd) {
        if (!Files.exists(cwd.resolve(".git"))) {
            return List.of();
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "git", "log", "-n", "1000",
                    "--pretty=format:", "--name-only", "--diff-filter=M"
            );
            pb.directory(cwd.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            Map<String, Integer> counts = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String f = line.trim();
                    if (!f.isEmpty()) {
                        counts.merge(f, 1, Integer::sum);
                    }
                }
            }

            process.waitFor(10, TimeUnit.SECONDS);

            List<String> sorted = counts.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .map(Map.Entry::getKey)
                    .toList();

            return pickDiverseCoreFiles(sorted, 5);
        } catch (Exception e) {
            return List.of();
        }
    }
}