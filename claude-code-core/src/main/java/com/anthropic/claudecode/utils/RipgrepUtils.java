/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/ripgrep.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;

/**
 * Ripgrep utilities for file searching and counting.
 */
public final class RipgrepUtils {
    private RipgrepUtils() {}

    private static final int MAX_BUFFER_SIZE = 20_000_000;
    private static final int DEFAULT_TIMEOUT_MS = 20_000;
    private static final AtomicBoolean ripgrepTested = new AtomicBoolean(false);

    /**
     * Custom error class for ripgrep timeouts.
     */
    public static class RipgrepTimeoutException extends Exception {
        private final List<String> partialResults;

        public RipgrepTimeoutException(String message, List<String> partialResults) {
            super(message);
            this.partialResults = partialResults;
        }

        public List<String> getPartialResults() {
            return partialResults;
        }
    }

    /**
     * Get the ripgrep command path.
     */
    public static String getRipgrepPath() {
        // Try system ripgrep first
        String systemRg = findExecutable("rg");
        if (systemRg != null) {
            return systemRg;
        }
        return "rg"; // Fallback
    }

    /**
     * Find an executable in PATH.
     */
    private static String findExecutable(String name) {
        String path = System.getenv("PATH");
        if (path == null) return null;

        String[] paths = path.split(File.pathSeparator);
        for (String dir : paths) {
            File exe = new File(dir, name);
            if (exe.canExecute()) {
                return exe.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Run ripgrep and return matching lines.
     */
    public static CompletableFuture<List<String>> ripGrep(
            List<String> args,
            String target,
            long timeoutMs) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> command = new ArrayList<>();
            command.add(getRipgrepPath());
            command.addAll(args);
            command.add(target);

            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(false);
                Process process = pb.start();

                List<String> results = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        results.add(line);
                    }
                }

                boolean completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    throw new RuntimeException("Ripgrep timed out");
                }

                return results;
            } catch (Exception e) {
                throw new RuntimeException("Ripgrep failed", e);
            }
        });
    }

    /**
     * Check if ripgrep is available.
     */
    public static boolean isRipgrepAvailable() {
        return getRipgrepPath() != null;
    }
}