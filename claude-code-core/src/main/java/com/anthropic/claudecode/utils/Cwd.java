/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/cwd.ts
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;

/**
 * Current working directory utilities.
 */
public class Cwd {
    private static volatile Path currentDir = Paths.get(System.getProperty("user.dir"));

    /**
     * Get the current working directory.
     */
    public static Path getCwd() {
        return currentDir;
    }

    /**
     * Set the current working directory.
     */
    public static void setCwd(Path path) {
        currentDir = path.toAbsolutePath();
    }

    /**
     * Set the current working directory from string.
     */
    public static void setCwd(String path) {
        setCwd(Paths.get(path));
    }

    /**
     * Get absolute path.
     */
    public static Path toAbsolute(Path path) {
        if (path.isAbsolute()) {
            return path;
        }
        return currentDir.resolve(path);
    }

    /**
     * Get absolute path from string.
     */
    public static Path toAbsolute(String path) {
        return toAbsolute(Paths.get(path));
    }

    /**
     * Check if path is within cwd.
     */
    public static boolean isWithinCwd(Path path) {
        return path.toAbsolutePath().startsWith(currentDir);
    }
}