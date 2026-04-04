/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code ripgrep utilities
 */
package com.anthropic.claudecode.utils.search;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Ripgrep-style file search utilities.
 * Uses system commands when available, falls back to Java implementation.
 */
public final class Ripgrep {
    private Ripgrep() {}

    private static final int MAX_BUFFER_SIZE = 20_000_000; // 20MB
    private static final int DEFAULT_TIMEOUT_MS = 20_000;
    private static final int WSL_TIMEOUT_MS = 60_000;

    private static volatile RipgrepConfig cachedConfig = null;
    private static volatile Boolean working = null;

    /**
     * Ripgrep configuration.
     */
    public record RipgrepConfig(
            String mode,  // "system", "builtin", or "embedded"
            String command,
            List<String> args,
            String argv0
    ) {}

    /**
     * Custom error for ripgrep timeouts.
     */
    public static class RipgrepTimeoutError extends Exception {
        private final List<String> partialResults;

        public RipgrepTimeoutError(String message, List<String> partialResults) {
            super(message);
            this.partialResults = partialResults;
        }

        public List<String> getPartialResults() {
            return partialResults;
        }
    }

    /**
     * Get ripgrep configuration.
     */
    public static RipgrepConfig getRipgrepConfig() {
        if (cachedConfig != null) {
            return cachedConfig;
        }

        // Check if user wants system ripgrep
        String useBuiltin = System.getenv("USE_BUILTIN_RIPGREP");
        boolean userWantsSystem = useBuiltin == null || useBuiltin.isEmpty() ||
                                   "false".equalsIgnoreCase(useBuiltin);

        if (userWantsSystem) {
            String systemPath = findExecutable("rg");
            if (systemPath != null) {
                cachedConfig = new RipgrepConfig("system", "rg", List.of(), null);
                return cachedConfig;
            }
        }

        // Fallback to Java-based search
        cachedConfig = new RipgrepConfig("builtin", "java", List.of(), null);
        return cachedConfig;
    }

    /**
     * Find executable in PATH.
     */
    private static String findExecutable(String name) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;

        String[] paths = pathEnv.split(File.pathSeparator);
        for (String path : paths) {
            File file = new File(path, name);
            if (file.canExecute()) {
                return file.getAbsolutePath();
            }
            // On Windows, try with .exe extension
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                file = new File(path, name + ".exe");
                if (file.canExecute()) {
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /**
     * Execute ripgrep search.
     */
    public static List<String> ripGrep(List<String> args, String target) throws Exception {
        return ripGrep(args, target, null);
    }

    /**
     * Execute ripgrep search with abort signal.
     */
    public static List<String> ripGrep(List<String> args, String target,
                                        java.util.function.Consumer<Process> abortSignal) throws Exception {
        RipgrepConfig config = getRipgrepConfig();

        if ("system".equals(config.mode())) {
            return executeSystemRipgrep(config, args, target, abortSignal);
        } else {
            return executeJavaSearch(args, target);
        }
    }

    /**
     * Execute system ripgrep command.
     */
    private static List<String> executeSystemRipgrep(RipgrepConfig config, List<String> args,
                                                      String target, Consumer<Process> abortSignal) throws Exception {
        List<String> fullArgs = new ArrayList<>();
        fullArgs.addAll(config.args());
        fullArgs.addAll(args);
        fullArgs.add(target);

        ProcessBuilder pb = new ProcessBuilder(config.command());
        pb.command().addAll(fullArgs);
        pb.redirectErrorStream(false);

        int timeout = isWSL() ? WSL_TIMEOUT_MS : DEFAULT_TIMEOUT_MS;
        String envTimeout = System.getenv("CLAUDE_CODE_GLOB_TIMEOUT_SECONDS");
        if (envTimeout != null) {
            try {
                int seconds = Integer.parseInt(envTimeout);
                if (seconds > 0) timeout = seconds * 1000;
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        Process process = pb.start();

        // Set up timeout
        CompletableFuture<Process> future = process.onExit();

        try {
            future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            process.destroyForcibly();
            throw new RipgrepTimeoutError("Ripgrep search timed out", List.of());
        }

        // Read output
        List<String> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    // Remove trailing \r for Windows
                    if (line.endsWith("\r")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    results.add(line);
                }
            }
        }

        int exitCode = process.exitValue();

        // Exit code 1 means no matches
        if (exitCode == 1) {
            return List.of();
        }

        // Exit code 0 means matches found
        if (exitCode == 0) {
            return results;
        }

        // Other exit codes indicate errors
        throw new RuntimeException("ripgrep exited with code " + exitCode);
    }

    /**
     * Execute Java-based file search.
     */
    private static List<String> executeJavaSearch(List<String> args, String target) {
        List<String> results = new ArrayList<>();
        Path targetPath = Paths.get(target);

        if (!Files.exists(targetPath)) {
            return results;
        }

        boolean showHidden = args.contains("--hidden");
        boolean followLinks = args.contains("--follow");
        boolean filesMode = args.contains("--files");
        String globPattern = extractGlobPattern(args);

        try {
            if (filesMode) {
                // List files
                Files.walk(targetPath)
                        .filter(Files::isRegularFile)
                        .filter(p -> showHidden || !isHidden(p))
                        .filter(p -> globPattern == null || matchesGlob(p, globPattern))
                        .forEach(p -> results.add(p.toString()));
            } else {
                // Search content
                String pattern = extractSearchPattern(args);
                if (pattern != null) {
                    Files.walk(targetPath)
                            .filter(Files::isRegularFile)
                            .filter(p -> showHidden || !isHidden(p))
                            .filter(p -> globPattern == null || matchesGlob(p, globPattern))
                            .forEach(p -> searchInFile(p, pattern, results));
                }
            }
        } catch (IOException e) {
            // Return partial results
        }

        return results;
    }

    /**
     * Extract glob pattern from args.
     */
    private static String extractGlobPattern(List<String> args) {
        for (int i = 0; i < args.size() - 1; i++) {
            if ("--glob".equals(args.get(i))) {
                return args.get(i + 1);
            }
        }
        return null;
    }

    /**
     * Extract search pattern from args.
     */
    private static String extractSearchPattern(List<String> args) {
        // Pattern is usually the first non-flag argument
        for (String arg : args) {
            if (!arg.startsWith("-") && !arg.startsWith("--")) {
                return arg;
            }
        }
        return null;
    }

    /**
     * Check if path is hidden.
     */
    private static boolean isHidden(Path path) {
        try {
            return Files.isHidden(path) ||
                   path.getFileName().toString().startsWith(".");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if path matches glob pattern.
     */
    private static boolean matchesGlob(Path path, String pattern) {
        if (pattern.startsWith("!")) {
            // Negated pattern
            return !matchesGlob(path, pattern.substring(1));
        }

        String fileName = path.getFileName().toString();
        String pathStr = path.toString();

        // Simple glob matching
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");

        return fileName.matches(regex) || pathStr.matches(".*" + regex + ".*");
    }

    /**
     * Search for pattern in file.
     */
    private static void searchInFile(Path file, String pattern, List<String> results) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains(pattern)) {
                    results.add(file + ":" + (i + 1) + ":" + lines.get(i));
                }
            }
        } catch (IOException e) {
            // Skip files that can't be read
        }
    }

    /**
     * Check if running on WSL.
     */
    private static boolean isWSL() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("linux")) {
            return false;
        }

        try {
            String release = Files.readString(Paths.get("/proc/version"));
            return release.toLowerCase().contains("microsoft") ||
                   release.toLowerCase().contains("wsl");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get ripgrep status.
     */
    public static Map<String, Object> getRipgrepStatus() {
        RipgrepConfig config = getRipgrepConfig();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("mode", config.mode());
        status.put("path", config.command());
        status.put("working", working);
        return status;
    }

    /**
     * Count files in directory using ripgrep.
     */
    public static Integer countFilesRounded(Path dirPath) {
        // Skip home directory to avoid permission dialogs
        if (dirPath.equals(Paths.get(System.getProperty("user.home")))) {
            return null;
        }

        try {
            long count = Files.walk(dirPath)
                    .filter(Files::isRegularFile)
                    .count();

            if (count == 0) return 0;

            // Round to nearest power of 10
            int magnitude = (int) Math.floor(Math.log10(count));
            long power = (long) Math.pow(10, magnitude);
            return (int) (Math.round((double) count / power) * power);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Stream search results.
     */
    public static void ripGrepStream(List<String> args, String target,
                                      Consumer<List<String>> onLines) throws Exception {
        List<String> results = ripGrep(args, target);
        // Batch results for streaming
        int batchSize = 100;
        for (int i = 0; i < results.size(); i += batchSize) {
            int end = Math.min(i + batchSize, results.size());
            onLines.accept(results.subList(i, end));
        }
    }

    /**
     * Search for files matching pattern.
     */
    public static List<String> findFiles(String target, String pattern) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("--files");
        if (pattern != null && !pattern.isEmpty()) {
            args.add("--glob");
            args.add(pattern);
        }
        return ripGrep(args, target);
    }

    /**
     * Search for content in files.
     */
    public static List<String> grep(String target, String pattern) throws Exception {
        return ripGrep(List.of(pattern), target);
    }
}