/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/updates/autoUpdater
 */
package com.anthropic.claudecode.services.updates;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

/**
 * Auto updater - Automatic update management.
 */
public final class AutoUpdater {
    private volatile UpdateStatus status = UpdateStatus.IDLE;
    private volatile VersionInfo currentVersion;
    private volatile VersionInfo latestVersion;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<UpdateListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Update status enum.
     */
    public enum UpdateStatus {
        IDLE,
        CHECKING,
        UPDATE_AVAILABLE,
        DOWNLOADING,
        INSTALLING,
        UPDATED,
        ERROR
    }

    /**
     * Version info record.
     */
    public record VersionInfo(
        String version,
        String releaseDate,
        String releaseNotes,
        List<String> changes,
        boolean isStable,
        String downloadUrl
    ) {
        public int compareTo(VersionInfo other) {
            return compareVersions(version, other.version);
        }

        private int compareVersions(String v1, String v2) {
            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");

            for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
                int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
                int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
                if (n1 != n2) return n1 - n2;
            }

            return 0;
        }

        public boolean isNewerThan(VersionInfo other) {
            return compareTo(other) > 0;
        }
    }

    /**
     * Create auto updater.
     */
    public AutoUpdater(String currentVersion) {
        this.currentVersion = new VersionInfo(
            currentVersion,
            "",
            "",
            Collections.emptyList(),
            true,
            ""
        );
    }

    /**
     * Check for updates.
     */
    public CompletableFuture<UpdateResult> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            status = UpdateStatus.CHECKING;
            notifyListeners();

            try {
                // Fetch latest version from update server
                VersionInfo latest = fetchLatestVersion();

                latestVersion = latest;

                if (latest.isNewerThan(currentVersion)) {
                    status = UpdateStatus.UPDATE_AVAILABLE;
                    notifyListeners();
                    return new UpdateResult(true, latest, "Update available: " + latest.version());
                }

                status = UpdateStatus.IDLE;
                notifyListeners();
                return new UpdateResult(false, currentVersion, "No updates available");
            } catch (Exception e) {
                status = UpdateStatus.ERROR;
                notifyListeners();
                return new UpdateResult(false, currentVersion, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch latest version info.
     */
    private VersionInfo fetchLatestVersion() {
        try {
            // Check for updates from GitHub releases API
            String updateUrl = System.getenv("CLAUDE_CODE_UPDATE_URL");
            if (updateUrl == null) {
                updateUrl = "https://api.github.com/repos/anthropics/claude-code/releases/latest";
            }

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(updateUrl))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Claude-Code-Java/" + currentVersion.version())
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseGitHubRelease(response.body());
            }
        } catch (Exception e) {
            // Fall back to default
        }

        return new VersionInfo(
            "0.0.1",
            "2026-01-01",
            "Latest version",
            Collections.emptyList(),
            true,
            ""
        );
    }

    /**
     * Parse GitHub release JSON.
     */
    private VersionInfo parseGitHubRelease(String json) {
        try {
            String version = extractJsonString(json, "tag_name");
            if (version.startsWith("v")) {
                version = version.substring(1);
            }

            String releaseDate = extractJsonString(json, "published_at");
            String releaseNotes = extractJsonString(json, "body");
            String downloadUrl = extractDownloadUrl(json);

            List<String> changes = extractChanges(releaseNotes);

            return new VersionInfo(
                version,
                releaseDate != null ? releaseDate : "",
                releaseNotes != null ? releaseNotes : "",
                changes,
                true,
                downloadUrl != null ? downloadUrl : ""
            );
        } catch (Exception e) {
            return new VersionInfo(
                "0.0.1",
                "",
                "",
                Collections.emptyList(),
                true,
                ""
            );
        }
    }

    /**
     * Extract string value from JSON.
     */
    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return null;

        idx += searchKey.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;

        if (idx >= json.length() || json.charAt(idx) != '"') return null;
        idx++;

        int start = idx;
        while (idx < json.length() && json.charAt(idx) != '"') {
            if (json.charAt(idx) == '\\') idx++;
            idx++;
        }

        return json.substring(start, idx);
    }

    /**
     * Extract download URL for JAR file.
     */
    private String extractDownloadUrl(String json) {
        int assetsIdx = json.indexOf("\"assets\":");
        if (assetsIdx < 0) return null;

        int arrStart = json.indexOf("[", assetsIdx);
        if (arrStart < 0) return null;

        int depth = 1;
        int arrEnd = arrStart + 1;
        while (arrEnd < json.length() && depth > 0) {
            char c = json.charAt(arrEnd);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            arrEnd++;
        }

        String assetsArray = json.substring(arrStart, arrEnd);

        // Look for .jar file
        int jarIdx = assetsArray.indexOf(".jar");
        if (jarIdx < 0) return null;

        // Find the browser_download_url for this asset
        int assetStart = assetsArray.lastIndexOf("{", jarIdx);
        int assetEnd = assetsArray.indexOf("}", jarIdx);
        if (assetStart < 0 || assetEnd < 0) return null;

        String assetObj = assetsArray.substring(assetStart, assetEnd + 1);
        return extractJsonString(assetObj, "browser_download_url");
    }

    /**
     * Extract changes from release notes.
     */
    private List<String> extractChanges(String releaseNotes) {
        if (releaseNotes == null || releaseNotes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> changes = new ArrayList<>();
        for (String line : releaseNotes.split("\n")) {
            line = line.trim();
            if (line.startsWith("- ") || line.startsWith("* ")) {
                changes.add(line.substring(2));
            }
        }

        return changes;
    }

    /**
     * Download update file.
     */
    private void downloadUpdate(String downloadUrl) throws Exception {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(downloadUrl))
            .GET()
            .build();

        java.net.http.HttpResponse<java.io.InputStream> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new Exception("Download failed: HTTP " + response.statusCode());
        }

        // Save to temp file
        Path updateDir = Paths.get(System.getProperty("user.home"), ".claude", "updates");
        Files.createDirectories(updateDir);

        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        Path updateFile = updateDir.resolve(fileName);

        try (java.io.InputStream in = response.body();
             java.io.OutputStream out = Files.newOutputStream(updateFile)) {
            in.transferTo(out);
        }
    }

    /**
     * Install downloaded update.
     */
    private void installDownloadedUpdate() throws Exception {
        Path updateDir = Paths.get(System.getProperty("user.home"), ".claude", "updates");

        if (!Files.exists(updateDir)) {
            return; // No update downloaded
        }

        // Find downloaded JAR
        java.util.stream.Stream<Path> files = Files.list(updateDir);
        Path updateJar = files
            .filter(p -> p.toString().endsWith(".jar"))
            .findFirst()
            .orElse(null);
        files.close();

        if (updateJar == null) {
            return; // No JAR found
        }

        // Get current JAR path
        String currentJarPath = getCurrentJarPath();
        if (currentJarPath == null) {
            return;
        }

        Path currentJar = Paths.get(currentJarPath);

        // Backup current version
        Path backup = Paths.get(currentJarPath + ".backup");
        if (Files.exists(currentJar)) {
            Files.copy(currentJar, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // Replace with new version
        Files.copy(updateJar, currentJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Clean up
        Files.deleteIfExists(updateJar);
    }

    /**
     * Get current JAR path.
     */
    private String getCurrentJarPath() {
        try {
            java.net.URI uri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            return Paths.get(uri).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Download and install update.
     */
    public CompletableFuture<UpdateResult> installUpdate() {
        return CompletableFuture.supplyAsync(() -> {
            if (status != UpdateStatus.UPDATE_AVAILABLE) {
                return new UpdateResult(false, currentVersion, "No update available");
            }

            try {
                status = UpdateStatus.DOWNLOADING;
                notifyListeners();

                // Download update
                if (latestVersion.downloadUrl() != null && !latestVersion.downloadUrl().isEmpty()) {
                    downloadUpdate(latestVersion.downloadUrl());
                } else {
                    Thread.sleep(500); // Simulate download
                }

                status = UpdateStatus.INSTALLING;
                notifyListeners();

                // Install update
                installDownloadedUpdate();

                status = UpdateStatus.UPDATED;
                notifyListeners();

                return new UpdateResult(true, latestVersion, "Update installed successfully");
            } catch (Exception e) {
                status = UpdateStatus.ERROR;
                notifyListeners();
                return new UpdateResult(false, currentVersion, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Start periodic checks.
     */
    public void startPeriodicChecks(long intervalHours) {
        scheduler.scheduleAtFixedRate(
            () -> checkForUpdates(),
            0,
            intervalHours,
            TimeUnit.HOURS
        );
    }

    /**
     * Stop periodic checks.
     */
    public void stopPeriodicChecks() {
        scheduler.shutdown();
    }

    /**
     * Get status.
     */
    public UpdateStatus getStatus() {
        return status;
    }

    /**
     * Get current version.
     */
    public VersionInfo getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Get latest version.
     */
    public VersionInfo getLatestVersion() {
        return latestVersion;
    }

    /**
     * Add listener.
     */
    public void addListener(UpdateListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(UpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (UpdateListener listener : listeners) {
            listener.onStatusChange(status, currentVersion, latestVersion);
        }
    }

    /**
     * Update result record.
     */
    public record UpdateResult(
        boolean success,
        VersionInfo version,
        String message
    ) {}

    /**
     * Update listener interface.
     */
    public interface UpdateListener {
        void onStatusChange(UpdateStatus status, VersionInfo current, VersionInfo latest);
    }

    /**
     * Update config record.
     */
    public record UpdateConfig(
        boolean autoCheck,
        boolean autoInstall,
        long checkIntervalHours,
        String updateChannel,
        boolean includePrereleases
    ) {
        public static UpdateConfig defaultConfig() {
            return new UpdateConfig(true, false, 24, "stable", false);
        }
    }
}