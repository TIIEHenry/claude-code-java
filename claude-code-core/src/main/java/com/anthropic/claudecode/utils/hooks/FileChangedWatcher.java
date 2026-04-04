/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/fileChangedWatcher.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * File changed watcher for hook events.
 */
public final class FileChangedWatcher {
    private FileChangedWatcher() {}

    private static volatile WatchService watcher = null;
    private static volatile String currentCwd = null;
    private static volatile List<String> dynamicWatchPaths = new ArrayList<>();
    private static volatile List<String> dynamicWatchPathsSorted = new ArrayList<>();
    private static volatile boolean initialized = false;
    private static volatile boolean hasEnvHooks = false;
    private static volatile BiConsumer<String, Boolean> notifyCallback = null;
    private static volatile ExecutorService executor = null;
    private static volatile Map<Path, WatchKey> watchKeys = new ConcurrentHashMap<>();

    /**
     * Set the notification callback for hook events.
     */
    public static void setEnvHookNotifier(BiConsumer<String, Boolean> callback) {
        notifyCallback = callback;
    }

    /**
     * Initialize the file changed watcher.
     */
    public static void initializeFileChangedWatcher(String cwd) {
        if (initialized) return;
        initialized = true;
        currentCwd = cwd;

        Map<String, List<HooksConfigSnapshot.HookMatcherConfig>> config =
            HooksConfigSnapshot.getHooksConfigFromSnapshot();

        hasEnvHooks = config.containsKey("CwdChanged") || config.containsKey("FileChanged");

        if (hasEnvHooks) {
            executor = Executors.newSingleThreadExecutor();
        }

        List<String> paths = resolveWatchPaths(config);
        if (paths.isEmpty()) return;

        startWatching(paths);
    }

    private static List<String> resolveWatchPaths(
            Map<String, List<HooksConfigSnapshot.HookMatcherConfig>> config) {

        List<HooksConfigSnapshot.HookMatcherConfig> matchers =
            config.getOrDefault("FileChanged", new ArrayList<>());

        List<String> staticPaths = new ArrayList<>();
        for (HooksConfigSnapshot.HookMatcherConfig m : matchers) {
            if (m.matcher() == null || m.matcher().isEmpty()) continue;

            for (String name : m.matcher().split("\\|")) {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) continue;

                Path path = Paths.get(trimmed);
                if (!path.isAbsolute()) {
                    path = Paths.get(currentCwd, trimmed);
                }
                staticPaths.add(path.toString());
            }
        }

        // Combine static matcher paths with dynamic paths from hook output
        Set<String> allPaths = new HashSet<>();
        allPaths.addAll(staticPaths);
        allPaths.addAll(dynamicWatchPaths);

        return new ArrayList<>(allPaths);
    }

    private static void startWatching(List<String> paths) {
        logForDebugging("FileChanged: watching " + paths.size() + " paths");

        try {
            watcher = FileSystems.getDefault().newWatchService();

            for (String pathStr : paths) {
                Path path = Paths.get(pathStr);
                if (!Files.exists(path)) continue;

                WatchKey key = path.register(watcher,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);

                watchKeys.put(path, key);
            }

            // Start watching thread
            if (executor != null) {
                executor.submit(() -> watchLoop());
            }
        } catch (Exception e) {
            logForDebugging("FileChanged: failed to start watching: " + e.getMessage());
        }
    }

    private static void watchLoop() {
        while (initialized && watcher != null) {
            try {
                WatchKey key = watcher.poll(1, TimeUnit.SECONDS);
                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path dir = (Path) key.watchable();
                    Path fullPath = dir.resolve((Path) event.context());

                    String eventType = event.kind().name();
                    logForDebugging("FileChanged: " + eventType + " " + fullPath);

                    handleFileEvent(fullPath.toString(), eventType);
                }

                key.reset();
            } catch (Exception e) {
                if (initialized) {
                    logForDebugging("FileChanged: watch error: " + e.getMessage());
                }
            }
        }
    }

    private static void handleFileEvent(String path, String event) {
        if (executor != null) {
            executor.submit(() -> {
                try {
                    // Execute file changed hooks (placeholder)
                    HookResult result = executeFileChangedHooks(path, event);

                    if (!result.watchPaths().isEmpty()) {
                        updateWatchPaths(result.watchPaths());
                    }

                    for (String msg : result.systemMessages()) {
                        if (notifyCallback != null) {
                            notifyCallback.accept(msg, false);
                        }
                    }

                    for (HookExecutionResult r : result.results()) {
                        if (!r.succeeded() && r.output() != null) {
                            if (notifyCallback != null) {
                                notifyCallback.accept(r.output(), true);
                            }
                        }
                    }
                } catch (Exception e) {
                    logForDebugging("FileChanged hook failed: " + e.getMessage());
                    if (notifyCallback != null) {
                        notifyCallback.accept(e.getMessage(), true);
                    }
                }
            });
        }
    }

    /**
     * Hook execution result.
     */
    public record HookExecutionResult(
        boolean succeeded,
        String output
    ) {}

    /**
     * Hook result.
     */
    public record HookResult(
        List<HookExecutionResult> results,
        List<String> watchPaths,
        List<String> systemMessages
    ) {}

    /**
     * Execute file changed hooks.
     */
    private static HookResult executeFileChangedHooks(String path, String event) {
        List<HookExecutionResult> results = new ArrayList<>();
        List<String> watchPaths = new ArrayList<>();
        List<String> systemMessages = new ArrayList<>();

        try {
            // Load hooks from settings
            String home = System.getProperty("user.home");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(home, ".claude", "settings.json");

            if (!java.nio.file.Files.exists(settingsPath)) {
                return new HookResult(results, watchPaths, systemMessages);
            }

            String content = java.nio.file.Files.readString(settingsPath);

            // Find fileChanged hooks
            int hooksIdx = content.indexOf("\"hooks\"");
            if (hooksIdx < 0) return new HookResult(results, watchPaths, systemMessages);

            int fileChangedIdx = content.indexOf("\"fileChanged\"", hooksIdx);
            if (fileChangedIdx < 0) return new HookResult(results, watchPaths, systemMessages);

            // Find the hooks array
            int arrStart = content.indexOf("[", fileChangedIdx);
            if (arrStart < 0) return new HookResult(results, watchPaths, systemMessages);

            int depth = 1;
            int arrEnd = arrStart + 1;
            while (arrEnd < content.length() && depth > 0) {
                char c = content.charAt(arrEnd);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                arrEnd++;
            }

            String hooksArray = content.substring(arrStart, arrEnd);

            // Execute each hook command
            int i = 0;
            while (i < hooksArray.length()) {
                int strStart = hooksArray.indexOf("\"", i);
                if (strStart < 0) break;
                int strEnd = hooksArray.indexOf("\"", strStart + 1);
                if (strEnd < 0) break;

                String hookCmd = hooksArray.substring(strStart + 1, strEnd);
                if (!hookCmd.isEmpty()) {
                    // Replace variables
                    hookCmd = hookCmd.replace("$FILE", path);
                    hookCmd = hookCmd.replace("$EVENT", event);

                    // Execute hook
                    HookExecutionResult result = executeHookCommand(hookCmd);
                    results.add(result);
                }

                i = strEnd + 1;
            }
        } catch (Exception e) {
            systemMessages.add("Error executing file changed hooks: " + e.getMessage());
        }

        return new HookResult(results, watchPaths, systemMessages);
    }

    /**
     * Execute cwd changed hooks.
     */
    public static HookResult executeCwdChangedHooks(String oldCwd, String newCwd) {
        List<HookExecutionResult> results = new ArrayList<>();
        List<String> watchPaths = new ArrayList<>();
        List<String> systemMessages = new ArrayList<>();

        try {
            // Load hooks from settings
            String home = System.getProperty("user.home");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(home, ".claude", "settings.json");

            if (!java.nio.file.Files.exists(settingsPath)) {
                return new HookResult(results, watchPaths, systemMessages);
            }

            String content = java.nio.file.Files.readString(settingsPath);

            // Find cwdChanged hooks
            int hooksIdx = content.indexOf("\"hooks\"");
            if (hooksIdx < 0) return new HookResult(results, watchPaths, systemMessages);

            int cwdChangedIdx = content.indexOf("\"cwdChanged\"", hooksIdx);
            if (cwdChangedIdx < 0) return new HookResult(results, watchPaths, systemMessages);

            // Find the hooks array
            int arrStart = content.indexOf("[", cwdChangedIdx);
            if (arrStart < 0) return new HookResult(results, watchPaths, systemMessages);

            int depth = 1;
            int arrEnd = arrStart + 1;
            while (arrEnd < content.length() && depth > 0) {
                char c = content.charAt(arrEnd);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                arrEnd++;
            }

            String hooksArray = content.substring(arrStart, arrEnd);

            // Execute each hook command
            int i = 0;
            while (i < hooksArray.length()) {
                int strStart = hooksArray.indexOf("\"", i);
                if (strStart < 0) break;
                int strEnd = hooksArray.indexOf("\"", strStart + 1);
                if (strEnd < 0) break;

                String hookCmd = hooksArray.substring(strStart + 1, strEnd);
                if (!hookCmd.isEmpty()) {
                    // Replace variables
                    hookCmd = hookCmd.replace("$OLD_CWD", oldCwd != null ? oldCwd : "");
                    hookCmd = hookCmd.replace("$NEW_CWD", newCwd != null ? newCwd : "");

                    // Execute hook
                    HookExecutionResult result = executeHookCommand(hookCmd);
                    results.add(result);
                }

                i = strEnd + 1;
            }
        } catch (Exception e) {
            systemMessages.add("Error executing cwd changed hooks: " + e.getMessage());
        }

        return new HookResult(results, watchPaths, systemMessages);
    }

    private static HookExecutionResult executeHookCommand(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

            String output = new String(process.getInputStream().readAllBytes());

            return new HookExecutionResult(
                process.exitValue() == 0,
                output
            );
        } catch (Exception e) {
            return new HookExecutionResult(
                false,
                "Error: " + e.getMessage()
            );
        }
    }

    /**
     * Update watch paths from hook output.
     */
    public static void updateWatchPaths(List<String> paths) {
        if (!initialized) return;

        List<String> sorted = new ArrayList<>(paths);
        Collections.sort(sorted);

        if (sorted.equals(dynamicWatchPathsSorted)) return;

        dynamicWatchPaths = paths;
        dynamicWatchPathsSorted = sorted;
        restartWatching();
    }

    private static void restartWatching() {
        // Close existing watcher
        if (watcher != null) {
            try {
                watcher.close();
            } catch (Exception e) {
                // Ignore
            }
            watcher = null;
        }
        watchKeys.clear();

        // Start new watcher
        List<String> paths = resolveWatchPaths(HooksConfigSnapshot.getHooksConfigFromSnapshot());
        if (!paths.isEmpty()) {
            startWatching(paths);
        }
    }

    /**
     * Handle cwd changed event.
     */
    public static CompletableFuture<Void> onCwdChangedForHooks(String oldCwd, String newCwd) {
        return CompletableFuture.runAsync(() -> {
            if (Objects.equals(oldCwd, newCwd)) return;

            Map<String, List<HooksConfigSnapshot.HookMatcherConfig>> config =
                HooksConfigSnapshot.getHooksConfigFromSnapshot();

            boolean currentHasEnvHooks =
                config.containsKey("CwdChanged") || config.containsKey("FileChanged");

            if (!currentHasEnvHooks) return;

            currentCwd = newCwd;

            HookResult hookResult = executeCwdChangedHooks(oldCwd, newCwd);

            dynamicWatchPaths = hookResult.watchPaths();
            dynamicWatchPathsSorted = new ArrayList<>(hookResult.watchPaths());
            Collections.sort(dynamicWatchPathsSorted);

            for (String msg : hookResult.systemMessages()) {
                if (notifyCallback != null) {
                    notifyCallback.accept(msg, false);
                }
            }

            for (HookExecutionResult r : hookResult.results()) {
                if (!r.succeeded() && r.output() != null) {
                    if (notifyCallback != null) {
                        notifyCallback.accept(r.output(), true);
                    }
                }
            }

            if (initialized) {
                restartWatching();
            }
        });
    }

    /**
     * Dispose the watcher.
     */
    public static void dispose() {
        initialized = false;

        if (watcher != null) {
            try {
                watcher.close();
            } catch (Exception e) {
                // Ignore
            }
            watcher = null;
        }

        watchKeys.clear();
        dynamicWatchPaths = new ArrayList<>();
        dynamicWatchPathsSorted = new ArrayList<>();
        hasEnvHooks = false;
        notifyCallback = null;

        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                executor.shutdownNow();
            }
            executor = null;
        }
    }

    /**
     * Reset for testing.
     */
    public static void resetFileChangedWatcherForTesting() {
        dispose();
    }

    private static void logForDebugging(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[file-changed-watcher] " + message);
        }
    }
}