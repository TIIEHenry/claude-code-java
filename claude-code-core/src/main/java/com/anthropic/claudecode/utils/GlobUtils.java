/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code glob utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import com.anthropic.claudecode.utils.search.Ripgrep;

/**
 * Glob pattern utilities for file matching.
 */
public final class GlobUtils {
    private GlobUtils() {}

    /**
     * Extracts the static base directory from a glob pattern.
     * The base directory is everything before the first glob special character (* ? [ {).
     */
    public static GlobBaseResult extractGlobBaseDirectory(String pattern) {
        // Find the first glob special character: *, ?, [, {
        Pattern globChars = Pattern.compile("[*?\\[\\{]");
        Matcher matcher = globChars.matcher(pattern);

        if (!matcher.find()) {
            // No glob characters - this is a literal path
            Path path = Paths.get(pattern);
            Path dir = path.getParent();
            String file = path.getFileName() != null ? path.getFileName().toString() : "";
            return new GlobBaseResult(
                    dir != null ? dir.toString() : "",
                    file
            );
        }

        // Get everything before the first glob character
        String staticPrefix = pattern.substring(0, matcher.start());

        // Find the last path separator in the static prefix
        int lastSepIndex = Math.max(
                staticPrefix.lastIndexOf('/'),
                staticPrefix.lastIndexOf(FileSystems.getDefault().getSeparator())
        );

        if (lastSepIndex == -1) {
            // No path separator before the glob - pattern is relative to cwd
            return new GlobBaseResult("", pattern);
        }

        String baseDir = staticPrefix.substring(0, lastSepIndex);
        String relativePattern = pattern.substring(lastSepIndex + 1);

        // Handle root directory patterns
        if (baseDir.isEmpty() && lastSepIndex == 0) {
            baseDir = "/";
        }

        // Handle Windows drive root paths
        if (Platform.getPlatform() == Platform.Type.WINDOWS &&
            Pattern.matches("[A-Za-z]:$", baseDir)) {
            baseDir = baseDir + FileSystems.getDefault().getSeparator();
        }

        return new GlobBaseResult(baseDir, relativePattern);
    }

    /**
     * Glob base directory extraction result.
     */
    public record GlobBaseResult(String baseDir, String relativePattern) {}

    /**
     * Execute a glob search using ripgrep.
     */
    public static CompletableFuture<GlobResult> glob(
            String filePattern,
            String cwd,
            int limit,
            int offset,
            ToolPermissionContext toolPermissionContext
    ) {
        String searchDirRaw = cwd;
        String searchPatternRaw = filePattern;

        // Handle absolute paths
        if (Paths.get(filePattern).isAbsolute()) {
            GlobBaseResult extraction = extractGlobBaseDirectory(filePattern);
            if (!extraction.baseDir().isEmpty()) {
                searchDirRaw = extraction.baseDir();
                searchPatternRaw = extraction.relativePattern();
            }
        }

        final String searchDir = searchDirRaw;
        final String searchPattern = searchPatternRaw;

        // Build ripgrep arguments
        List<String> args = new ArrayList<>();
        args.add("--files");
        args.add("--glob");
        args.add(searchPattern);
        args.add("--sort=modified");

        // Handle environment settings
        String noIgnoreEnv = System.getenv("CLAUDE_CODE_GLOB_NO_IGNORE");
        boolean noIgnore = EnvUtils.isEnvTruthy(noIgnoreEnv != null ? noIgnoreEnv : "true");

        String hiddenEnv = System.getenv("CLAUDE_CODE_GLOB_HIDDEN");
        boolean hidden = EnvUtils.isEnvTruthy(hiddenEnv != null ? hiddenEnv : "true");

        if (noIgnore) args.add("--no-ignore");
        if (hidden) args.add("--hidden");

        // Add ignore patterns
        List<String> ignorePatterns = Permissions.getFileReadIgnorePatterns(toolPermissionContext);
        for (String pattern : ignorePatterns) {
            args.add("--glob");
            args.add("!" + pattern);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return Ripgrep.ripGrep(args, searchDir);
            } catch (Exception e) {
                return List.of();
            }
        }).thenApply(paths -> {
            // Convert to absolute paths
            @SuppressWarnings("unchecked")
            List<String> pathList = (List<String>) paths;
            List<String> absolutePaths = pathList.stream()
                    .map(p -> Paths.get(p).isAbsolute() ? p : Paths.get(searchDir).resolve(p).toString())
                    .toList();

            boolean truncated = absolutePaths.size() > offset + limit;
            List<String> files = absolutePaths.subList(
                    Math.min(offset, absolutePaths.size()),
                    Math.min(offset + limit, absolutePaths.size())
            );

            return new GlobResult(new ArrayList<>(files), truncated);
        });
    }

    /**
     * Glob search result.
     */
    public record GlobResult(List<String> files, boolean truncated) {}

    /**
     * Tool permission context placeholder.
     */
    public record ToolPermissionContext(List<String> ignorePatterns) {}

    /**
     * Permissions utilities placeholder.
     */
    public static final class Permissions {
        public static List<String> getFileReadIgnorePatterns(ToolPermissionContext context) {
            return context != null ? context.ignorePatterns() : List.of();
        }
    }
}