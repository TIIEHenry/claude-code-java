/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code dynamic environment utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Dynamic environment detection that requires async operations.
 */
public final class EnvDynamic {
    private EnvDynamic() {}

    private static volatile Boolean muslRuntimeCache = null;
    private static volatile String jetBrainsIDECache = null;

    static {
        // Initialize musl detection on Linux
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) {
            String arch = System.getProperty("os.arch", "");
            String muslArch = arch.contains("64") ? "x86_64" : "aarch64";
            Path muslPath = Paths.get("/lib/libc.musl-" + muslArch + ".so.1");
            muslRuntimeCache = Files.exists(muslPath);
        }
    }

    /**
     * Check if running in Docker.
     */
    public static CompletableFuture<Boolean> getIsDocker() {
        return CompletableFuture.supplyAsync(() -> {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("linux")) return false;

            return Files.exists(Paths.get("/.dockerenv"));
        });
    }

    /**
     * Check if running in Bubblewrap sandbox.
     */
    public static boolean getIsBubblewrapSandbox() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux") &&
                EnvUtils.isTruthy(System.getenv("CLAUDE_CODE_BUBBLEWRAP"));
    }

    /**
     * Check if system is using MUSL libc.
     */
    public static boolean isMuslEnvironment() {
        // Check for compile-time flags (would be set in native builds)
        String isMusl = System.getenv("CLAUDE_CODE_IS_LIBC_MUSL");
        String isGlibc = System.getenv("CLAUDE_CODE_IS_LIBC_GLIBC");

        if (EnvUtils.isTruthy(isMusl)) return true;
        if (EnvUtils.isTruthy(isGlibc)) return false;

        // Fallback: runtime detection
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) return false;

        return Boolean.TRUE.equals(muslRuntimeCache);
    }

    /**
     * Get terminal with JetBrains detection.
     */
    public static String getTerminalWithJetBrainsDetection() {
        String terminalEmulator = System.getenv("TERMINAL_EMULATOR");
        if ("JetBrains-JediTerm".equals(terminalEmulator)) {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("mac")) {
                // Return cached value or fallback
                if (jetBrainsIDECache != null) {
                    return jetBrainsIDECache.isEmpty() ? "pycharm" : jetBrainsIDECache;
                }
                return "pycharm";
            }
        }

        return System.getenv("TERM");
    }

    /**
     * Get terminal with JetBrains detection (async version).
     */
    public static CompletableFuture<String> getTerminalWithJetBrainsDetectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            String terminalEmulator = System.getenv("TERMINAL_EMULATOR");
            if ("JetBrains-JediTerm".equals(terminalEmulator)) {
                String os = System.getProperty("os.name", "").toLowerCase();
                if (!os.contains("mac")) {
                    return detectJetBrainsIDEFromParentProcess()
                            .thenApply(ide -> ide != null ? ide : "pycharm")
                            .join();
                }
            }
            return System.getenv("TERM");
        });
    }

    /**
     * Detect JetBrains IDE from parent process.
     */
    private static CompletableFuture<String> detectJetBrainsIDEFromParentProcess() {
        return CompletableFuture.supplyAsync(() -> {
            if (jetBrainsIDECache != null) {
                return jetBrainsIDECache.isEmpty() ? null : jetBrainsIDECache;
            }

            String[] jetBrainsIDEs = {
                    "idea", "pycharm", "webstorm", "clion", "goland",
                    "rider", "datagrip", "phpstorm", "rubymine"
            };

            try {
                // Check parent process via /proc on Linux
                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("linux")) {
                    try {
                        // Read parent process cmdline
                        Path parentCmdline = Paths.get("/proc", "..", "cmdline");
                        if (Files.exists(parentCmdline)) {
                            String cmdline = Files.readString(parentCmdline);
                            for (String ide : jetBrainsIDEs) {
                                if (cmdline.contains(ide)) {
                                    jetBrainsIDECache = ide;
                                    return ide;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // /proc not available, check environment instead
                    }
                }

                // Check environment variables for JetBrains IDE home paths
                for (String ide : jetBrainsIDEs) {
                    String home = System.getenv(ide.toUpperCase() + "_HOME");
                    if (home != null && !home.isEmpty()) {
                        jetBrainsIDECache = ide;
                        return ide;
                    }
                    // Also check for JetBrains-specific environment variables
                    String toolbox = System.getenv("JETBRAINS_TOOLBOX_" + ide.toUpperCase());
                    if (toolbox != null && !toolbox.isEmpty()) {
                        jetBrainsIDECache = ide;
                        return ide;
                    }
                }

                // Check for common JetBrains directories
                String home = System.getProperty("user.home");
                Path toolboxPath = Paths.get(home, ".local", "share", "JetBrains", "Toolbox", "apps");
                if (Files.exists(toolboxPath)) {
                    try {
                        // List installed IDEs
                        for (String ide : jetBrainsIDEs) {
                            Path idePath = toolboxPath.resolve(ide);
                            if (Files.exists(idePath)) {
                                jetBrainsIDECache = ide;
                                return ide;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            } catch (Exception e) {
                // Ignore all errors
            }

            jetBrainsIDECache = "";
            return null;
        });
    }

    /**
     * Initialize JetBrains IDE detection.
     */
    public static CompletableFuture<Void> initJetBrainsDetection() {
        String terminalEmulator = System.getenv("TERMINAL_EMULATOR");
        if ("JetBrains-JediTerm".equals(terminalEmulator)) {
            return detectJetBrainsIDEFromParentProcess().thenAccept(ide -> {});
        }
        return CompletableFuture.completedFuture(null);
    }
}