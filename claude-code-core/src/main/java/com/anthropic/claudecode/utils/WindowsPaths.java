/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Windows path utilities
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Windows path conversion utilities.
 *
 * Handles conversion between Windows and POSIX path formats.
 */
public final class WindowsPaths {
    private WindowsPaths() {}

    // Cache for memoization
    private static final ConcurrentHashMap<String, String> windowsToPosixCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> posixToWindowsCache = new ConcurrentHashMap<>();

    // Patterns for path matching
    private static final Pattern DRIVE_PATTERN = Pattern.compile("^([A-Za-z]):[/\\\\]");
    private static final Pattern CYGDRIVE_PATTERN = Pattern.compile("^/cygdrive/([A-Za-z])(/|$)");
    private static final Pattern POSIX_DRIVE_PATTERN = Pattern.compile("^/([A-Za-z])(/|$)");

    /**
     * Convert a Windows path to a POSIX path.
     * Handles UNC paths (\\server\share -> //server/share)
     * Handles drive letter paths (C:\Users\foo -> /c/Users/foo)
     */
    public static String windowsPathToPosixPath(String windowsPath) {
        if (windowsPath == null || windowsPath.isEmpty()) {
            return windowsPath;
        }

        // Check cache first
        String cached = windowsToPosixCache.get(windowsPath);
        if (cached != null) {
            return cached;
        }

        String result;
        // Handle UNC paths: \\server\share -> //server/share
        if (windowsPath.startsWith("\\\\")) {
            result = windowsPath.replace('\\', '/');
        } else {
            // Handle drive letter paths: C:\Users\foo -> /c/Users/foo
            Matcher matcher = DRIVE_PATTERN.matcher(windowsPath);
            if (matcher.find()) {
                String driveLetter = matcher.group(1).toLowerCase();
                result = '/' + driveLetter + windowsPath.substring(2).replace('\\', '/');
            } else {
                // Already POSIX or relative — just flip slashes
                result = windowsPath.replace('\\', '/');
            }
        }

        windowsToPosixCache.put(windowsPath, result);
        return result;
    }

    /**
     * Convert a POSIX path to a Windows path.
     * Handles UNC paths (//server/share -> \\server\share)
     * Handles /c/... format (C:\...)
     */
    public static String posixPathToWindowsPath(String posixPath) {
        if (posixPath == null || posixPath.isEmpty()) {
            return posixPath;
        }

        // Check cache first
        String cached = posixToWindowsCache.get(posixPath);
        if (cached != null) {
            return cached;
        }

        String result;
        // Handle UNC paths: //server/share -> \\server\share
        if (posixPath.startsWith("//")) {
            result = posixPath.replace('/', '\\');
        } else {
            // Handle /cygdrive/c/... format
            Matcher cygMatcher = CYGDRIVE_PATTERN.matcher(posixPath);
            if (cygMatcher.find()) {
                String driveLetter = cygMatcher.group(1).toUpperCase();
                int prefixLen = "/cygdrive/".length() + cygMatcher.group(1).length();
                String rest = posixPath.length() > prefixLen ? posixPath.substring(prefixLen) : "";
                result = driveLetter + ":" + (rest.isEmpty() ? "\\" : rest.replace('/', '\\'));
            } else {
                // Handle /c/... format (MSYS2/Git Bash)
                Matcher driveMatcher = POSIX_DRIVE_PATTERN.matcher(posixPath);
                if (driveMatcher.find()) {
                    String driveLetter = driveMatcher.group(1).toUpperCase();
                    String rest = posixPath.length() > 2 ? posixPath.substring(2) : "";
                    result = driveLetter + ":" + (rest.isEmpty() ? "\\" : rest.replace('/', '\\'));
                } else {
                    // Already Windows or relative — just flip slashes
                    result = posixPath.replace('/', '\\');
                }
            }
        }

        posixToWindowsCache.put(posixPath, result);
        return result;
    }

    /**
     Check if a path is a Windows path.
     */
    public static boolean isWindowsPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.contains("\\") || DRIVE_PATTERN.matcher(path).find() || path.startsWith("\\\\");
    }

    /**
     * Check if a path is a POSIX path.
     */
    public static boolean isPosixPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.contains("/") && !isWindowsPath(path);
    }

    /**
     * Normalize a path to the current platform format.
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        if (Platform.isWindows()) {
            if (isPosixPath(path)) {
                return posixPathToWindowsPath(path);
            }
        } else {
            if (isWindowsPath(path)) {
                return windowsPathToPosixPath(path);
            }
        }

        return path;
    }

    /**
     * Get the drive letter from a Windows path.
     */
    public static String getDriveLetter(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        Matcher matcher = DRIVE_PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        // Check POSIX /c/... format
        Matcher posixMatcher = POSIX_DRIVE_PATTERN.matcher(path);
        if (posixMatcher.find()) {
            return posixMatcher.group(1).toUpperCase();
        }

        return null;
    }

    /**
     * Check if path is a UNC path.
     */
    public static boolean isUncPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.startsWith("\\\\") || path.startsWith("//");
    }

    /**
     * Clear the path conversion caches.
     */
    public static void clearCaches() {
        windowsToPosixCache.clear();
        posixToWindowsCache.clear();
    }
}