/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/which.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.concurrent.*;

/**
 * Utility to find the full path to a command executable.
 */
public final class Which {
    private Which() {}

    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Find the full path to a command executable (async).
     */
    public static CompletableFuture<String> whichAsync(String command) {
        return CompletableFuture.supplyAsync(() -> whichSync(command));
    }

    /**
     * Find the full path to a command executable (sync).
     */
    public static String whichSync(String command) {
        if (command == null || command.isEmpty()) {
            return null;
        }

        // Check cache first
        String cached = cache.get(command);
        if (cached != null) {
            return cached;
        }

        String result = findCommand(command);
        if (result != null) {
            cache.put(command, result);
        }
        return result;
    }

    /**
     * Find the full path to a command executable.
     */
    public static String which(String command) {
        return whichSync(command);
    }

    /**
     * Find the full path to a command executable.
     */
    public static String find(String command) {
        return whichSync(command);
    }

    /**
     * Check if a command is available.
     */
    public static boolean isAvailable(String command) {
        return whichSync(command) != null;
    }

    /**
     * Clear the cache.
     */
    public static void clearCache() {
        cache.clear();
    }

    private static String findCommand(String command) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("where.exe", command);
            } else {
                pb = new ProcessBuilder("which", command);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.waitFor();

                if (process.exitValue() == 0 && line != null && !line.isEmpty()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            // Command not found or error
        }

        // Fallback: check common paths
        return checkCommonPaths(command, os);
    }

    private static String checkCommonPaths(String command, String os) {
        String[] paths;

        if (os.contains("win")) {
            paths = new String[] {
                System.getenv("SystemRoot") + "\\System32\\" + command + ".exe",
                System.getenv("ProgramFiles") + "\\" + command + "\\" + command + ".exe",
                System.getenv("ProgramFiles(x86)") + "\\" + command + "\\" + command + ".exe"
            };
        } else {
            String pathEnv = System.getenv("PATH");
            String[] pathDirs = pathEnv != null ? pathEnv.split(":") : new String[0];

            paths = new String[pathDirs.length + 4];
            int i = 0;
            for (String dir : pathDirs) {
                paths[i++] = dir + "/" + command;
            }
            paths[i++] = "/usr/bin/" + command;
            paths[i++] = "/usr/local/bin/" + command;
            paths[i++] = "/opt/homebrew/bin/" + command;
            paths[i++] = "/bin/" + command;
        }

        for (String path : paths) {
            if (path != null) {
                File file = new File(path);
                if (file.exists() && file.canExecute()) {
                    return path;
                }
            }
        }

        return null;
    }
}