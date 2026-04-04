/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code binary check utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Binary check utilities for detecting installed commands.
 */
public final class BinaryCheck {
    private BinaryCheck() {}

    // Session cache to avoid repeated checks
    private static final Map<String, Boolean> binaryCache = new ConcurrentHashMap<>();

    /**
     * Check if a binary/command is installed and available on the system.
     * Uses 'which' on Unix systems and 'where' on Windows.
     *
     * @param command The command name to check
     * @return true if the command exists, false otherwise
     */
    public static boolean isBinaryInstalled(String command) {
        // Edge case: empty or whitespace-only command
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        // Trim the command to handle whitespace
        String trimmedCommand = command.trim();

        // Check cache first
        Boolean cached = binaryCache.get(trimmedCommand);
        if (cached != null) {
            return cached;
        }

        boolean exists = checkBinaryExists(trimmedCommand);

        // Cache the result
        binaryCache.put(trimmedCommand, exists);

        return exists;
    }

    /**
     * Check if binary exists asynchronously.
     */
    public static CompletableFuture<Boolean> isBinaryInstalledAsync(String command) {
        return CompletableFuture.supplyAsync(() -> isBinaryInstalled(command));
    }

    private static boolean checkBinaryExists(String command) {
        String osName = System.getProperty("os.name").toLowerCase();
        String checkCommand = osName.contains("win") ? "where" : "which";

        try {
            ProcessBuilder pb = new ProcessBuilder(checkCommand, command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clear the binary check cache.
     */
    public static void clearBinaryCache() {
        binaryCache.clear();
    }

    /**
     * Get cache size for testing.
     */
    public static int getCacheSize() {
        return binaryCache.size();
    }

    /**
     * Find the full path to a binary.
     */
    public static Optional<String> findBinaryPath(String command) {
        if (command == null || command.trim().isEmpty()) {
            return Optional.empty();
        }

        String osName = System.getProperty("os.name").toLowerCase();
        String checkCommand = osName.contains("win") ? "where" : "which";

        try {
            ProcessBuilder pb = new ProcessBuilder(checkCommand, command.trim());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String path = reader.readLine();
            process.waitFor();

            if (path != null && !path.isEmpty()) {
                return Optional.of(path.trim());
            }
        } catch (Exception e) {
            // Ignore
        }

        return Optional.empty();
    }

    /**
     * Check if multiple binaries are installed.
     */
    public static Map<String, Boolean> checkBinaries(String... commands) {
        Map<String, Boolean> results = new HashMap<>();
        for (String cmd : commands) {
            results.put(cmd, isBinaryInstalled(cmd));
        }
        return results;
    }
}