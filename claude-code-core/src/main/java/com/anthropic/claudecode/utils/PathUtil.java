/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/path.ts
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.regex.*;

/**
 * Path utilities.
 */
public final class PathUtil {
    private PathUtil() {}

    /**
     * Expands a path that may contain tilde notation (~) to an absolute path.
     */
    public static String expandPath(String path) {
        return expandPath(path, null);
    }

    public static String expandPath(String path, String baseDir) {
        // Set default baseDir
        String actualBaseDir = baseDir != null ? baseDir : System.getProperty("user.dir");

        // Input validation
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }

        // Security: Check for null bytes
        if (path.contains("\0") || actualBaseDir.contains("\0")) {
            throw new IllegalArgumentException("Path contains null bytes");
        }

        // Handle empty or whitespace-only paths
        String trimmedPath = path.trim();
        if (trimmedPath.isEmpty()) {
            return Paths.get(actualBaseDir).normalize().toString();
        }

        // Handle home directory notation
        String homeDir = System.getProperty("user.home");
        if (trimmedPath.equals("~")) {
            return homeDir;
        }

        if (trimmedPath.startsWith("~/")) {
            return Paths.get(homeDir, trimmedPath.substring(2)).normalize().toString();
        }

        // Handle absolute paths
        Path p = Paths.get(trimmedPath);
        if (p.isAbsolute()) {
            return p.normalize().toString();
        }

        // Handle relative paths
        return Paths.get(actualBaseDir, trimmedPath).normalize().toString();
    }

    /**
     * Converts an absolute path to a relative path from cwd.
     */
    public static String toRelativePath(String absolutePath) {
        return toRelativePath(absolutePath, Cwd.getCwd().toString());
    }

    public static String toRelativePath(String absolutePath, String cwd) {
        Path absPath = Paths.get(absolutePath);
        Path cwdPath = Paths.get(cwd);

        try {
            Path relative = cwdPath.relativize(absPath);
            String relativeStr = relative.toString();

            // If the relative path would go outside cwd, keep absolute
            if (relativeStr.startsWith("..")) {
                return absolutePath;
            }
            return relativeStr;
        } catch (Exception e) {
            return absolutePath;
        }
    }

    /**
     * Gets the directory path for a given file or directory path.
     */
    public static String getDirectoryForPath(String path) {
        String absolutePath = expandPath(path);

        // SECURITY: Skip filesystem operations for UNC paths
        if (absolutePath.startsWith("\\\\") || absolutePath.startsWith("//")) {
            return Paths.get(absolutePath).getParent().toString();
        }

        try {
            Path p = Paths.get(absolutePath);
            if (Files.isDirectory(p)) {
                return absolutePath;
            }
        } catch (Exception e) {
            // Path doesn't exist or can't be accessed
        }

        // If it's not a directory or doesn't exist, return the parent directory
        Path parent = Paths.get(absolutePath).getParent();
        return parent != null ? parent.toString() : absolutePath;
    }

    /**
     * Checks if a path contains directory traversal patterns.
     */
    public static boolean containsPathTraversal(String path) {
        if (path == null) return false;
        Pattern pattern = Pattern.compile("(?:^|[\\\\/])\\.\\.(?:[\\\\/]|$)");
        return pattern.matcher(path).find();
    }

    /**
     * Sanitizes a path by removing dangerous elements.
     */
    public static String sanitizePath(String path) {
        if (path == null) return null;

        // Remove null bytes
        String sanitized = path.replace("\0", "");

        // Normalize the path
        try {
            return Paths.get(sanitized).normalize().toString();
        } catch (Exception e) {
            return sanitized;
        }
    }

    /**
     * Normalizes a path for use as a JSON config key.
     */
    public static String normalizePathForConfigKey(String path) {
        if (path == null) return null;

        // First normalize to resolve . and .. segments
        String normalized = Paths.get(path).normalize().toString();

        // Then convert all backslashes to forward slashes
        return normalized.replace('\\', '/');
    }

    /**
     * Check if path is absolute.
     */
    public static boolean isAbsolute(String path) {
        if (path == null || path.isEmpty()) return false;
        return Paths.get(path).isAbsolute();
    }

    /**
     * Get file name from path.
     */
    public static String getFileName(String path) {
        if (path == null || path.isEmpty()) return null;
        Path p = Paths.get(path);
        Path fileName = p.getFileName();
        return fileName != null ? fileName.toString() : null;
    }

    /**
     * Get file extension from path.
     */
    public static String getExtension(String path) {
        if (path == null || path.isEmpty()) return null;
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1);
        }
        return null;
    }

    /**
     * Join path components.
     */
    public static String join(String... parts) {
        if (parts == null || parts.length == 0) return null;

        Path result = Paths.get(parts[0] != null ? parts[0] : "");
        for (int i = 1; i < parts.length; i++) {
            if (parts[i] != null) {
                result = result.resolve(parts[i]);
            }
        }
        return result.normalize().toString();
    }

    /**
     * Resolve path against base.
     */
    public static String resolve(String base, String path) {
        if (path == null || path.isEmpty()) return base;
        Path basePath = Paths.get(base != null ? base : "");
        return basePath.resolve(path).normalize().toString();
    }

    /**
     * Check if a path is within another path.
     */
    public static boolean isWithin(String path, String parent) {
        if (path == null || parent == null) return false;

        try {
            Path p = Paths.get(path).normalize();
            Path parentPath = Paths.get(parent).normalize();

            return p.startsWith(parentPath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if path is a UNC path (Windows network path).
     */
    public static boolean isUncPath(String path) {
        if (path == null) return false;
        return path.startsWith("\\\\") || path.startsWith("//");
    }

    /**
     * Check if path is a home-relative path.
     */
    public static boolean isHomeRelative(String path) {
        if (path == null) return false;
        return path.equals("~") || path.startsWith("~/");
    }

    /**
     * Expand home in path.
     */
    public static String expandHome(String path) {
        if (path == null) return null;
        if (path.equals("~")) {
            return System.getProperty("user.home");
        }
        if (path.startsWith("~/")) {
            return Paths.get(System.getProperty("user.home"), path.substring(2)).toString();
        }
        return path;
    }
}