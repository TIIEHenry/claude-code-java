/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code find executable utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Find an executable by searching PATH, similar to `which`.
 */
public final class FindExecutable {
    private FindExecutable() {}

    /**
     * Result of finding an executable.
     */
    public record ExecutableResult(String cmd, List<String> args) {}

    /**
     * Find an executable by searching PATH.
     *
     * @param exe  The executable name
     * @param args The arguments to pass
     * @return The result with resolved path and args
     */
    public static ExecutableResult findExecutable(String exe, List<String> args) {
        String resolved = which(exe);
        return new ExecutableResult(resolved != null ? resolved : exe, args);
    }

    /**
     * Find an executable by searching PATH.
     */
    public static ExecutableResult findExecutable(String exe, String... args) {
        return findExecutable(exe, Arrays.asList(args));
    }

    /**
     * Find an executable in PATH.
     *
     * @param exe The executable name
     * @return The resolved path, or null if not found
     */
    public static String which(String exe) {
        if (exe == null || exe.isEmpty()) {
            return null;
        }

        // If it's already an absolute or relative path, check if it exists
        if (exe.contains("/") || exe.contains("\\")) {
            Path path = Paths.get(exe);
            if (Files.isExecutable(path)) {
                return path.toAbsolutePath().toString();
            }
            // On Windows, try with extensions
            if (Platform.isWindows()) {
                for (String ext : new String[]{".exe", ".cmd", ".bat"}) {
                    Path withExt = Paths.get(exe + ext);
                    if (Files.isExecutable(withExt)) {
                        return withExt.toAbsolutePath().toString();
                    }
                }
            }
            return null;
        }

        // Search in PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return null;
        }

        String[] pathDirs = pathEnv.split(File.pathSeparator);
        for (String dir : pathDirs) {
            Path candidate = Paths.get(dir, exe);
            if (Files.isExecutable(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
            // On Windows, try with extensions
            if (Platform.isWindows()) {
                for (String ext : new String[]{".exe", ".cmd", ".bat"}) {
                    Path withExt = Paths.get(dir, exe + ext);
                    if (Files.isExecutable(withExt)) {
                        return withExt.toAbsolutePath().toString();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if an executable exists in PATH.
     */
    public static boolean exists(String exe) {
        return which(exe) != null;
    }

    /**
     * Find all matching executables in PATH.
     */
    public static List<String> whichAll(String exe) {
        List<String> results = new ArrayList<>();

        if (exe == null || exe.isEmpty()) {
            return results;
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return results;
        }

        String[] pathDirs = pathEnv.split(File.pathSeparator);
        for (String dir : pathDirs) {
            Path candidate = Paths.get(dir, exe);
            if (Files.isExecutable(candidate)) {
                results.add(candidate.toAbsolutePath().toString());
            }
            // On Windows, try with extensions
            if (Platform.isWindows()) {
                for (String ext : new String[]{".exe", ".cmd", ".bat"}) {
                    Path withExt = Paths.get(dir, exe + ext);
                    if (Files.isExecutable(withExt)) {
                        results.add(withExt.toAbsolutePath().toString());
                    }
                }
            }
        }

        return results;
    }
}