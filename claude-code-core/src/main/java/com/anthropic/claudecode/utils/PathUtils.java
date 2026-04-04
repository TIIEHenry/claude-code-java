/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;

/**
 * Path utilities.
 */
public final class PathUtils {
    private PathUtils() {}

    /**
     * Normalize path.
     */
    public static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    /**
     * Normalize path string.
     */
    public static String normalize(String path) {
        return normalize(Paths.get(path)).toString();
    }

    /**
     * Check if path is absolute.
     */
    public static boolean isAbsolute(String path) {
        return Paths.get(path).isAbsolute();
    }

    /**
     * Get relative path.
     */
    public static String relativize(Path base, Path target) {
        return base.relativize(target).toString();
    }

    /**
     * Get file extension.
     */
    public static Optional<String> getExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            return Optional.of(name.substring(dot + 1).toLowerCase());
        }
        return Optional.empty();
    }

    /**
     * Check if path is hidden.
     */
    public static boolean isHidden(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(".");
    }

    /**
     * Join paths.
     */
    public static Path join(String first, String... more) {
        return Paths.get(first, more);
    }

    /**
     * Get parent directory.
     */
    public static Optional<Path> getParent(Path path) {
        Path parent = path.getParent();
        return Optional.ofNullable(parent);
    }

    /**
     * Get filename without extension.
     */
    public static String getBaseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * Sanitize path for use in filenames.
     */
    public static String sanitizePath(String path) {
        if (path == null) return "";
        return path.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}