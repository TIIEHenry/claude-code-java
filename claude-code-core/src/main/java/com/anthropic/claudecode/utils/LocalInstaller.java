/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code local installer utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Local installation utilities.
 */
public final class LocalInstaller {
    private LocalInstaller() {}

    /**
     * Install result.
     */
    public enum InstallResult {
        IN_PROGRESS,
        SUCCESS,
        INSTALL_FAILED
    }

    /**
     * Release channel.
     */
    public enum ReleaseChannel {
        LATEST,
        STABLE
    }

    /**
     * Get local install directory.
     */
    public static Path getLocalInstallDir() {
        return Paths.get(EnvUtils.getClaudeConfigHomeDir(), "local");
    }

    /**
     * Get local Claude path.
     */
    public static Path getLocalClaudePath() {
        return getLocalInstallDir().resolve("claude");
    }

    /**
     * Check if running from local installation.
     */
    public static boolean isRunningFromLocalInstallation() {
        String execPath = System.getProperty("java.class.path", "");
        return execPath.contains("/.claude/local/");
    }

    /**
     * Ensure local package environment exists.
     */
    public static boolean ensureLocalPackageEnvironment() {
        try {
            Path localInstallDir = getLocalInstallDir();
            Files.createDirectories(localInstallDir);

            // Create package.json
            Path packageJson = localInstallDir.resolve("package.json");
            if (!Files.exists(packageJson)) {
                String content = "{\n" +
                        "  \"name\": \"claude-local\",\n" +
                        "  \"version\": \"0.0.1\",\n" +
                        "  \"private\": true\n" +
                        "}";
                Files.writeString(packageJson, content);
            }

            // Create wrapper script
            Path wrapper = localInstallDir.resolve("claude");
            if (!Files.exists(wrapper)) {
                String script = "#!/bin/sh\nexec \"" + localInstallDir + "/node_modules/.bin/claude\" \"$@\"";
                Files.writeString(wrapper, script);
                wrapper.toFile().setExecutable(true);
            }

            return true;
        } catch (Exception e) {
            Debug.log("Failed to ensure local package environment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install or update Claude package.
     */
    public static CompletableFuture<InstallResult> installOrUpdateClaudePackage(
            ReleaseChannel channel,
            String specificVersion
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!ensureLocalPackageEnvironment()) {
                    return InstallResult.INSTALL_FAILED;
                }

                String versionSpec = specificVersion != null
                        ? specificVersion
                        : (channel == ReleaseChannel.STABLE ? "stable" : "latest");

                ProcessBuilder pb = new ProcessBuilder(
                        "npm", "install",
                        "@anthropic-ai/claude-code@" + versionSpec
                );
                pb.directory(getLocalInstallDir().toFile());
                pb.redirectErrorStream(true);

                Process process = pb.start();
                boolean finished = process.waitFor(120, TimeUnit.SECONDS);

                if (!finished) {
                    process.destroyForcibly();
                    return InstallResult.IN_PROGRESS;
                }

                if (process.exitValue() != 0) {
                    Debug.log("npm install failed with exit code " + process.exitValue());
                    return InstallResult.INSTALL_FAILED;
                }

                return InstallResult.SUCCESS;
            } catch (Exception e) {
                Debug.log("Failed to install Claude package: " + e.getMessage());
                return InstallResult.INSTALL_FAILED;
            }
        });
    }

    /**
     * Check if local installation exists.
     */
    public static boolean localInstallationExists() {
        Path claudeBin = getLocalInstallDir().resolve("node_modules").resolve(".bin").resolve("claude");
        return Files.exists(claudeBin);
    }

    /**
     * Get shell type.
     */
    public static String getShellType() {
        String shellPath = System.getenv("SHELL");
        if (shellPath == null) return "unknown";

        if (shellPath.contains("zsh")) return "zsh";
        if (shellPath.contains("bash")) return "bash";
        if (shellPath.contains("fish")) return "fish";

        return "unknown";
    }
}