/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code example commands utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

/**
 * Example command generation utilities.
 */
public final class ExampleCommandsUtils {
    private ExampleCommandsUtils() {}

    // Patterns that mark a file as non-core (auto-generated, dependency, or config)
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
     * Check if a file path is a core file (not generated/config/dependency).
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
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN)
                .map(e -> String.format("%6d %s", e.getValue(), e.getKey()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    /**
     * Pick diverse core files from sorted paths.
     */
    public static List<String> pickDiverseCoreFiles(List<String> sortedPaths, int want) {
        List<String> picked = new ArrayList<>();
        Set<String> seenBasenames = new HashSet<>();
        Map<String, Integer> dirTally = new HashMap<>();

        // Greedy: on each pass allow +1 file per directory
        for (int cap = 1; picked.size() < want && cap <= want; cap++) {
            for (String p : sortedPaths) {
                if (picked.size() >= want) break;
                if (!isCoreFile(p)) continue;

                int lastSep = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
                String base = lastSep >= 0 ? p.substring(lastSep + 1) : p;
                if (base.isEmpty() || seenBasenames.contains(base)) continue;

                String dir = lastSep >= 0 ? p.substring(0, lastSep) : ".";
                if (dirTally.getOrDefault(dir, 0) >= cap) continue;

                picked.add(base);
                seenBasenames.add(base);
                dirTally.merge(dir, 1, Integer::sum);
            }
        }

        return picked.size() >= want ? picked : List.of();
    }

    /**
     * Get example command suggestions.
     */
    public static List<String> getExampleCommands(List<String> exampleFiles) {
        String frequentFile = exampleFiles != null && !exampleFiles.isEmpty()
                ? exampleFiles.get(0)
                : "<filepath>";

        return List.of(
                "fix lint errors",
                "fix typecheck errors",
                "how does " + frequentFile + " work?",
                "refactor " + frequentFile,
                "how do I log an error?",
                "edit " + frequentFile + " to...",
                "write a test for " + frequentFile,
                "create a util logging.py that..."
        );
    }

    /**
     * Get a random example command.
     */
    public static String getRandomExampleCommand(List<String> exampleFiles) {
        List<String> commands = getExampleCommands(exampleFiles);
        return commands.get((int) (Math.random() * commands.size()));
    }

    /**
     * Get example command with "Try" prefix.
     */
    public static String getTryExampleCommand(List<String> exampleFiles) {
        return "Try \"" + getRandomExampleCommand(exampleFiles) + "\"";
    }

    /**
     * Check if example files need refresh.
     */
    public static boolean needsRefresh(Long lastGenerated) {
        if (lastGenerated == null) return true;
        return System.currentTimeMillis() - lastGenerated > ONE_WEEK_MS;
    }

    /**
     * Get frequently modified files from git history.
     * This is a simplified implementation - full implementation would use git commands.
     */
    public static CompletableFuture<List<String>> getFrequentlyModifiedFiles(Path cwd) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> files = new ArrayList<>();

            try {
                // Use git log to find frequently modified files
                ProcessBuilder pb = new ProcessBuilder(
                        "git", "log", "-n", "1000",
                        "--pretty=format:", "--name-only", "--diff-filter=M"
                );
                pb.directory(cwd.toFile());
                pb.redirectErrorStream(true);

                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                Map<String, Integer> counts = new HashMap<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String f = line.trim();
                    if (!f.isEmpty()) {
                        counts.merge(f, 1, Integer::sum);
                    }
                }

                process.waitFor();

                // Sort by frequency
                List<String> sorted = counts.entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .map(Map.Entry::getKey)
                        .toList();

                return pickDiverseCoreFiles(sorted, 5);

            } catch (Exception e) {
                return files;
            }
        });
    }
}