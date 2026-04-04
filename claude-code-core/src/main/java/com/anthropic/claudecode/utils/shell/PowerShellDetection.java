/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/shell/powershellDetection.ts
 */
package com.anthropic.claudecode.utils.shell;

import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/**
 * PowerShell detection utilities.
 * Attempts to find PowerShell on the system via PATH.
 * Prefers pwsh (PowerShell Core 7+), falls back to powershell (5.1).
 */
public final class PowerShellDetection {
    private PowerShellDetection() {}

    private static volatile CompletableFuture<String> cachedPowerShellPath = null;

    /**
     * PowerShell edition.
     */
    public enum Edition {
        CORE,    // PowerShell 7+
        DESKTOP  // Windows PowerShell 5.1
    }

    /**
     * Attempts to find PowerShell on the system via PATH.
     * Prefers pwsh (PowerShell Core 7+), falls back to powershell (5.1).
     */
    public static CompletableFuture<String> findPowerShell() {
        return CompletableFuture.supplyAsync(() -> {
            // Try pwsh first (PowerShell Core 7+)
            String pwshPath = findExecutable("pwsh");
            if (pwshPath != null) {
                // On Linux, check for snap launcher which can hang
                if (isLinux()) {
                    try {
                        String resolved = Files.readSymbolicLink(Paths.get(pwshPath)).toString();
                        if (pwshPath.startsWith("/snap/") || resolved.startsWith("/snap/")) {
                            // Try direct binary paths
                            String direct = probePath("/opt/microsoft/powershell/7/pwsh");
                            if (direct != null) return direct;
                            direct = probePath("/usr/bin/pwsh");
                            if (direct != null) return direct;
                        }
                    } catch (Exception e) {
                        // Not a symlink
                    }
                }
                return pwshPath;
            }

            // Fall back to powershell (Windows PowerShell 5.1)
            String powershellPath = findExecutable("powershell");
            if (powershellPath != null) {
                return powershellPath;
            }

            return null;
        });
    }

    /**
     * Gets the cached PowerShell path.
     */
    public static CompletableFuture<String> getCachedPowerShellPath() {
        if (cachedPowerShellPath == null) {
            synchronized (PowerShellDetection.class) {
                if (cachedPowerShellPath == null) {
                    cachedPowerShellPath = findPowerShell();
                }
            }
        }
        return cachedPowerShellPath;
    }

    /**
     * Infers the PowerShell edition from the binary name.
     * - pwsh → core (PowerShell 7+)
     * - powershell → desktop (Windows PowerShell 5.1)
     */
    public static CompletableFuture<Edition> getPowerShellEdition() {
        return getCachedPowerShellPath().thenApply(path -> {
            if (path == null) return null;

            // Get basename without extension
            String filename = Paths.get(path).getFileName().toString();
            String base = filename.toLowerCase().replace(".exe", "");

            return "pwsh".equals(base) ? Edition.CORE : Edition.DESKTOP;
        });
    }

    /**
     * Resets the cached PowerShell path. Only for testing.
     */
    public static void resetPowerShellCache() {
        cachedPowerShellPath = null;
    }

    // Helpers

    private static String findExecutable(String name) {
        String path = System.getenv("PATH");
        if (path == null) return null;

        String pathSeparator = isWindows() ? ";" : ":";
        String[] dirs = path.split(pathSeparator);

        for (String dir : dirs) {
            String fullPath = dir + "/" + name;
            if (isWindows()) {
                fullPath = dir + "\\" + name + ".exe";
            }
            if (probePath(fullPath) != null) {
                return fullPath;
            }
        }

        return null;
    }

    private static String probePath(String path) {
        try {
            Path p = Paths.get(path);
            if (Files.exists(p) && Files.isRegularFile(p)) {
                return path;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }
}