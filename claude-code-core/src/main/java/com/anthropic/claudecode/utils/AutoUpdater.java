/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code auto-updater utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Auto-updater utilities for version management and installation.
 */
public final class AutoUpdater {
    private AutoUpdater() {}

    private static final String GCS_BUCKET_URL =
            "https://storage.googleapis.com/claude-code-dist-86c565f3-f756-42ad-8dfa-d59b1c096819/claude-code-releases";
    private static final long LOCK_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Install status enum.
     */
    public enum InstallStatus {
        SUCCESS, NO_PERMISSIONS, INSTALL_FAILED, IN_PROGRESS
    }

    /**
     * Auto updater result.
     */
    public record AutoUpdaterResult(
            String version,
            InstallStatus status,
            List<String> notifications
    ) {}

    /**
     * Max version config.
     */
    public record MaxVersionConfig(
            String external,
            String ant,
            String externalMessage,
            String antMessage
    ) {}

    /**
     * NPM dist tags.
     */
    public record NpmDistTags(String latest, String stable) {}

    /**
     * Get lock file path.
     */
    public static Path getLockFilePath() {
        return Path.of(EnvUtilsNew.getClaudeConfigHomeDir(), ".update.lock");
    }

    /**
     * Acquire update lock.
     */
    public static CompletableFuture<Boolean> acquireLock() {
        return CompletableFuture.supplyAsync(() -> {
            Path lockPath = getLockFilePath();

            try {
                if (Files.exists(lockPath)) {
                    long age = System.currentTimeMillis() -
                            Files.getLastModifiedTime(lockPath).toMillis();
                    if (age < LOCK_TIMEOUT_MS) {
                        return false;
                    }
                    // Stale lock - remove it
                    Files.delete(lockPath);
                }

                // Create lock file
                Files.writeString(lockPath, String.valueOf(ProcessHandle.current().pid()));
                return true;

            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Release update lock.
     */
    public static CompletableFuture<Void> releaseLock() {
        return CompletableFuture.runAsync(() -> {
            Path lockPath = getLockFilePath();
            try {
                if (Files.exists(lockPath)) {
                    String content = Files.readString(lockPath);
                    if (content.equals(String.valueOf(ProcessHandle.current().pid()))) {
                        Files.delete(lockPath);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    /**
     * Check global install permissions.
     */
    public static CompletableFuture<InstallPermissions> checkGlobalInstallPermissions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prefix = getInstallationPrefix();
                if (prefix == null) {
                    return new InstallPermissions(false, null);
                }

                Path prefixPath = Paths.get(prefix);
                if (!Files.isWritable(prefixPath)) {
                    return new InstallPermissions(false, prefix);
                }

                return new InstallPermissions(true, prefix);

            } catch (Exception e) {
                return new InstallPermissions(false, null);
            }
        });
    }

    /**
     * Get installation prefix (npm/bun global bin directory).
     */
    private static String getInstallationPrefix() {
        try {
            String home = System.getProperty("user.home");
            ProcessBuilder pb;

            if (isRunningWithBun()) {
                pb = new ProcessBuilder("bun", "pm", "bin", "-g");
            } else {
                pb = new ProcessBuilder("npm", "-g", "config", "get", "prefix");
            }

            pb.directory(new File(home));
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String output = reader.readLine();
            p.waitFor(5, TimeUnit.SECONDS);

            return output != null ? output.trim() : null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get latest version from GCS bucket.
     */
    public static CompletableFuture<String> getLatestVersionFromGcs(String channel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = GCS_BUCKET_URL + "/" + channel + "/version.txt";

                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();

                java.net.http.HttpResponse<String> response = client.send(
                    request,
                    java.net.http.HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {
                    return response.body().trim();
                }

                return null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Get version history (ant-only feature).
     */
    public static CompletableFuture<List<String>> getVersionHistory(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (!"ant".equals(System.getenv("USER_TYPE"))) {
                return List.of();
            }

            try {
                String home = System.getProperty("user.home");
                ProcessBuilder pb = new ProcessBuilder(
                        "npm", "view", "@anthropic-ai/claude-code",
                        "versions", "--json", "--prefer-online");
                pb.directory(new File(home));
                pb.redirectErrorStream(true);

                Process p = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                p.waitFor(30, TimeUnit.SECONDS);

                // Parse JSON array of versions
                // In real implementation, would parse properly
                return new ArrayList<String>();

            } catch (Exception e) {
                return List.of();
            }
        });
    }

    /**
     * Check if running with Bun.
     */
    private static boolean isRunningWithBun() {
        return System.getenv("BUN_INSTALL") != null;
    }

    /**
     * Install permissions record.
     */
    public record InstallPermissions(boolean hasPermissions, String npmPrefix) {}
}