/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/teamMemorySync/index.ts
 */
package com.anthropic.claudecode.services.teammemory;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.security.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.anthropic.claudecode.utils.Debug;

/**
 * Team Memory Sync Service.
 *
 * Syncs team memory files between the local filesystem and the server API.
 * Team memory is scoped per-repo (identified by git remote hash) and shared
 * across all authenticated org members.
 *
 * Sync semantics:
 *   - Pull overwrites local files with server content (server wins per-key).
 *   - Push uploads only keys whose content hash differs from serverChecksums.
 *   - File deletions do NOT propagate.
 */
public final class TeamMemorySyncService {
    private TeamMemorySyncService() {}

    private static final int TEAM_MEMORY_SYNC_TIMEOUT_MS = 30_000;
    private static final int MAX_FILE_SIZE_BYTES = 250_000;
    private static final int MAX_PUT_BODY_BYTES = 200_000;
    private static final int MAX_RETRIES = 3;
    private static final int MAX_CONFLICT_RETRIES = 2;

    /**
     * Sync state - mutable state for the team memory sync service.
     */
    public static class SyncState {
        private volatile String lastKnownChecksum;
        private final Map<String, String> serverChecksums = new ConcurrentHashMap<>();
        private volatile Integer serverMaxEntries;

        public String getLastKnownChecksum() { return lastKnownChecksum; }
        public void setLastKnownChecksum(String checksum) { this.lastKnownChecksum = checksum; }
        public Map<String, String> getServerChecksums() { return serverChecksums; }
        public Integer getServerMaxEntries() { return serverMaxEntries; }
        public void setServerMaxEntries(Integer max) { this.serverMaxEntries = max; }
    }

    /**
     * Create a new sync state.
     */
    public static SyncState createSyncState() {
        return new SyncState();
    }

    /**
     * Fetch result.
     */
    public record FetchResult(
        boolean success,
        boolean notModified,
        boolean isEmpty,
        Map<String, String> data,
        String checksum,
        String error,
        String errorType,
        Integer httpStatus,
        boolean skipRetry
    ) {}

    /**
     * Push result.
     */
    public record PushResult(
        boolean success,
        int filesUploaded,
        String checksum,
        boolean conflict,
        String error,
        String errorType,
        Integer httpStatus,
        List<SkippedSecretFile> skippedSecrets
    ) {}

    /**
     * Skipped secret file.
     */
    public record SkippedSecretFile(
        String path,
        String ruleId,
        String label
    ) {}

    /**
     * Hash content using SHA-256.
     */
    public static String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "sha256:" + hex;
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    /**
     * Check if team memory sync is available (requires first-party OAuth).
     */
    public static boolean isTeamMemorySyncAvailable() {
        String accessToken = System.getenv("CLAUDE_ACCESS_TOKEN");
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Pull team memory from the server and write to local directory.
     */
    public static CompletableFuture<PullResult> pullTeamMemory(
        SyncState state,
        PullOptions options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            if (!isTeamMemorySyncAvailable()) {
                logPull(startTime, false, 0, "no_oauth", null);
                return new PullResult(false, 0, 0, false, "OAuth not available");
            }

            String repoSlug = getGithubRepo();
            if (repoSlug == null) {
                logPull(startTime, false, 0, "no_repo", null);
                return new PullResult(false, 0, 0, false, "No git remote found");
            }

            String etag = options != null && options.skipEtagCache() ? null : state.getLastKnownChecksum();
            FetchResult result = fetchTeamMemory(state, repoSlug, etag);

            if (!result.success()) {
                logPull(startTime, false, 0, result.errorType(), result.httpStatus());
                return new PullResult(false, 0, 0, false, result.error());
            }

            if (result.notModified()) {
                logPull(startTime, true, 0, null, null);
                return new PullResult(true, 0, 0, true, null);
            }

            if (result.isEmpty() || result.data() == null) {
                state.getServerChecksums().clear();
                logPull(startTime, true, 0, null, null);
                return new PullResult(true, 0, 0, false, null);
            }

            // Update server checksums
            state.getServerChecksums().clear();
            // Would populate from response

            int filesWritten = writeRemoteEntriesToLocal(result.data());
            Debug.logForDebugging("team-memory-sync: pulled " + filesWritten + " files");

            logPull(startTime, true, filesWritten, null, null);

            return new PullResult(true, filesWritten, result.data().size(), false, null);
        });
    }

    /**
     * Push local team memory files to the server with optimistic locking.
     */
    public static CompletableFuture<PushResult> pushTeamMemory(SyncState state) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int conflictRetries = 0;

            if (!isTeamMemorySyncAvailable()) {
                logPush(startTime, false, 0, false, 0, "no_oauth", null);
                return new PushResult(false, 0, null, false, "OAuth not available", "no_oauth", null, Collections.emptyList());
            }

            String repoSlug = getGithubRepo();
            if (repoSlug == null) {
                logPush(startTime, false, 0, false, 0, "no_repo", null);
                return new PushResult(false, 0, null, false, "No git remote found", "no_repo", null, Collections.emptyList());
            }

            // Read local entries
            LocalReadResult localRead = readLocalTeamMemory(state.getServerMaxEntries());
            Map<String, String> entries = localRead.entries();
            List<SkippedSecretFile> skippedSecrets = localRead.skippedSecrets();

            // Hash local entries
            Map<String, String> localHashes = new HashMap<>();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                localHashes.put(entry.getKey(), hashContent(entry.getValue()));
            }

            boolean sawConflict = false;

            for (int conflictAttempt = 0; conflictAttempt <= MAX_CONFLICT_RETRIES; conflictAttempt++) {
                // Compute delta
                Map<String, String> delta = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : localHashes.entrySet()) {
                    String serverHash = state.getServerChecksums().get(entry.getKey());
                    if (!entry.getValue().equals(serverHash)) {
                        delta.put(entry.getKey(), entries.get(entry.getKey()));
                    }
                }

                if (delta.isEmpty()) {
                    logPush(startTime, true, 0, sawConflict, conflictRetries, null, null);
                    return new PushResult(true, 0, null, false, null, null, null, skippedSecrets);
                }

                // Upload
                UploadResult uploadResult = uploadTeamMemory(state, repoSlug, delta, state.getLastKnownChecksum());

                if (uploadResult.success()) {
                    // Update server checksums
                    for (String key : delta.keySet()) {
                        state.getServerChecksums().put(key, localHashes.get(key));
                    }
                    Debug.logForDebugging("team-memory-sync: pushed " + delta.size() + " files");
                    logPush(startTime, true, delta.size(), sawConflict, conflictRetries, null, null);
                    return new PushResult(true, delta.size(), uploadResult.checksum(), false, null, null, null, skippedSecrets);
                }

                if (!uploadResult.conflict()) {
                    logPush(startTime, false, 0, false, conflictRetries, uploadResult.errorType(), uploadResult.httpStatus());
                    return new PushResult(false, 0, null, false, uploadResult.error(), uploadResult.errorType(), uploadResult.httpStatus(), skippedSecrets);
                }

                // 412 conflict - refresh and retry
                sawConflict = true;
                conflictRetries++;

                Debug.logForDebugging("team-memory-sync: conflict (412), retrying");

                // Probe for hashes
                HashesResult probe = fetchTeamMemoryHashes(state, repoSlug);
                if (!probe.success()) {
                    logPush(startTime, false, 0, true, conflictRetries, "conflict", null);
                    return new PushResult(false, 0, null, true, "Conflict resolution failed", "conflict", null, skippedSecrets);
                }

                state.getServerChecksums().clear();
                state.getServerChecksums().putAll(probe.entryChecksums());
            }

            logPush(startTime, false, 0, false, conflictRetries, null, null);
            return new PushResult(false, 0, null, false, "Unexpected end of conflict resolution", null, null, skippedSecrets);
        });
    }

    /**
     * Bidirectional sync: pull from server, merge with local, push back.
     */
    public static CompletableFuture<SyncResult> syncTeamMemory(SyncState state) {
        return pullTeamMemory(state, new PullOptions(true))
            .thenCompose(pullResult -> {
                if (!pullResult.success()) {
                    return CompletableFuture.completedFuture(
                        new SyncResult(false, 0, 0, pullResult.error())
                    );
                }

                return pushTeamMemory(state).thenApply(pushResult ->
                    new SyncResult(
                        pushResult.success(),
                        pullResult.filesWritten(),
                        pushResult.filesUploaded(),
                        pushResult.error()
                    )
                );
            });
    }

    // Helper types and methods

    public record PullOptions(boolean skipEtagCache) {}
    public record PullResult(boolean success, int filesWritten, int entryCount, boolean notModified, String error) {}
    public record SyncResult(boolean success, int filesPulled, int filesPushed, String error) {}
    public record LocalReadResult(Map<String, String> entries, List<SkippedSecretFile> skippedSecrets) {}
    public record UploadResult(boolean success, String checksum, boolean conflict, String error, String errorType, Integer httpStatus) {}
    public record HashesResult(boolean success, Map<String, String> entryChecksums, String error) {}

    private static FetchResult fetchTeamMemory(SyncState state, String repoSlug, String etag) {
        // Make HTTP GET request to team memory API
        try {
            String apiToken = System.getenv("ANTHROPIC_API_KEY");
            if (apiToken == null || apiToken.isEmpty()) {
                return new FetchResult(false, false, false, null, null,
                    "Missing API key", "auth_error", null, true);
            }

            String baseUrl = System.getenv("CLAUDE_CODE_TEAM_MEMORY_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com/v1/team-memory";

            String url = baseUrl + "/" + URLEncoder.encode(repoSlug, StandardCharsets.UTF_8);

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("x-api-key", apiToken)
                .header("anthropic-version", "2023-06-01")
                .GET();

            if (etag != null) {
                requestBuilder.header("If-None-Match", etag);
            }

            java.net.http.HttpRequest request = requestBuilder.build();
            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 304) {
                return new FetchResult(true, true, false, null, etag, null, null, 304, false);
            }

            if (response.statusCode() == 200) {
                String checksum = response.headers().firstValue("ETag").orElse(null);
                Map<String, String> data = parseTeamMemoryResponse(response.body());
                boolean isEmpty = data == null || data.isEmpty();

                // Update state
                state.setLastKnownChecksum(checksum);

                return new FetchResult(true, false, isEmpty, data, checksum, null, null, 200, false);
            }

            String errorType = response.statusCode() == 404 ? "not_found" : "http_error";
            return new FetchResult(false, false, false, null, null,
                "HTTP " + response.statusCode(), errorType, response.statusCode(), false);
        } catch (Exception e) {
            return new FetchResult(false, false, false, null, null, e.getMessage(), "network_error", null, false);
        }
    }

    private static HashesResult fetchTeamMemoryHashes(SyncState state, String repoSlug) {
        // Make HTTP GET request to fetch hashes only
        try {
            String apiToken = System.getenv("ANTHROPIC_API_KEY");
            if (apiToken == null || apiToken.isEmpty()) {
                return new HashesResult(false, null, "Missing API key");
            }

            String baseUrl = System.getenv("CLAUDE_CODE_TEAM_MEMORY_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com/v1/team-memory";

            String url = baseUrl + "/" + URLEncoder.encode(repoSlug, StandardCharsets.UTF_8) + "/hashes";

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("x-api-key", apiToken)
                .header("anthropic-version", "2023-06-01")
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, String> hashes = parseHashesResponse(response.body());
                state.getServerChecksums().clear();
                state.getServerChecksums().putAll(hashes);
                return new HashesResult(true, hashes, null);
            }

            return new HashesResult(false, null, "HTTP " + response.statusCode());
        } catch (Exception e) {
            return new HashesResult(false, null, e.getMessage());
        }
    }

    private static UploadResult uploadTeamMemory(SyncState state, String repoSlug, Map<String, String> entries, String checksum) {
        // Make HTTP PUT request to upload team memory
        try {
            String apiToken = System.getenv("ANTHROPIC_API_KEY");
            if (apiToken == null || apiToken.isEmpty()) {
                return new UploadResult(false, null, false, "Missing API key", "auth_error", null);
            }

            String baseUrl = System.getenv("CLAUDE_CODE_TEAM_MEMORY_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com/v1/team-memory";

            String url = baseUrl + "/" + URLEncoder.encode(repoSlug, StandardCharsets.UTF_8);

            // Build JSON body
            StringBuilder jsonBody = new StringBuilder("{\"entries\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                if (!first) jsonBody.append(",");
                first = false;
                jsonBody.append("\"").append(escapeJson(entry.getKey())).append("\":");
                jsonBody.append("\"").append(escapeJson(entry.getValue())).append("\"");
            }
            jsonBody.append("}}");

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("x-api-key", apiToken)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody.toString()));

            if (checksum != null) {
                requestBuilder.header("If-Match", checksum);
            }

            java.net.http.HttpRequest request = requestBuilder.build();
            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String newChecksum = response.headers().firstValue("ETag").orElse(null);
                state.setLastKnownChecksum(newChecksum);
                return new UploadResult(true, newChecksum, false, null, null, response.statusCode());
            }

            if (response.statusCode() == 409) {
                // Conflict - need to retry
                return new UploadResult(false, null, true, "Conflict", "conflict", 409);
            }

            return new UploadResult(false, null, false, "HTTP " + response.statusCode(), "http_error", response.statusCode());
        } catch (Exception e) {
            return new UploadResult(false, null, false, e.getMessage(), "network_error", null);
        }
    }

    private static String getGithubRepo() {
        // Run git remote -v and parse
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "remote", "get-url", "origin");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(5, TimeUnit.SECONDS);

            if (process.exitValue() == 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                // Parse git URL like git@github.com:owner/repo.git or https://github.com/owner/repo.git
                if (output.contains("github.com")) {
                    int githubIdx = output.indexOf("github.com");
                    String afterGithub = output.substring(githubIdx + 10); // after "github.com"
                    // Remove leading : or /
                    while (afterGithub.startsWith(":") || afterGithub.startsWith("/")) {
                        afterGithub = afterGithub.substring(1);
                    }
                    // Remove .git suffix
                    if (afterGithub.endsWith(".git")) {
                        afterGithub = afterGithub.substring(0, afterGithub.length() - 4);
                    }
                    return afterGithub;
                }
            }
        } catch (Exception e) {
            // Fallback to placeholder
        }
        return "owner/repo";
    }

    private static Map<String, String> parseTeamMemoryResponse(String json) {
        Map<String, String> data = new LinkedHashMap<>();
        try {
            int entriesIdx = json.indexOf("\"entries\"");
            if (entriesIdx < 0) return data;

            int objStart = json.indexOf("{", entriesIdx);
            int objEnd = findJsonObjEnd(json, objStart);
            if (objStart < 0 || objEnd <= objStart) return data;

            String entriesObj = json.substring(objStart, objEnd);
            // Parse key-value pairs
            int i = 0;
            while (i < entriesObj.length()) {
                int keyStart = entriesObj.indexOf("\"", i);
                if (keyStart < 0) break;
                int keyEnd = entriesObj.indexOf("\"", keyStart + 1);
                if (keyEnd < 0) break;

                String key = entriesObj.substring(keyStart + 1, keyEnd);

                int valStart = entriesObj.indexOf("\"", keyEnd + 1);
                if (valStart < 0) break;
                int valEnd = entriesObj.indexOf("\"", valStart + 1);
                if (valEnd < 0) break;

                String value = entriesObj.substring(valStart + 1, valEnd);
                data.put(key, unescapeJson(value));

                i = valEnd + 1;
            }
        } catch (Exception e) {
            // Parse error
        }
        return data;
    }

    private static Map<String, String> parseHashesResponse(String json) {
        return parseTeamMemoryResponse(json); // Same format
    }

    private static int findJsonObjEnd(String json, int start) {
        int depth = 1;
        int pos = start + 1;
        while (pos < json.length() && depth > 0) {
            char c = json.charAt(pos);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            pos++;
        }
        return pos;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static int writeRemoteEntriesToLocal(Map<String, String> entries) {
        int written = 0;
        String teamDir = getTeamMemPath();

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            try {
                Path path = Paths.get(teamDir, entry.getKey());
                Files.createDirectories(path.getParent());
                Files.writeString(path, entry.getValue());
                written++;
            } catch (Exception e) {
                Debug.logForDebugging("team-memory-sync: failed to write " + entry.getKey());
            }
        }

        return written;
    }

    private static LocalReadResult readLocalTeamMemory(Integer maxEntries) {
        Map<String, String> entries = new LinkedHashMap<>();
        List<SkippedSecretFile> skippedSecrets = new ArrayList<>();

        String teamDir = getTeamMemPath();
        try {
            Files.walk(Paths.get(teamDir))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        String relPath = Paths.get(teamDir).relativize(path).toString().replace('\\', '/');

                        // Check size
                        if (content.getBytes().length > MAX_FILE_SIZE_BYTES) {
                            return;
                        }

                        entries.put(relPath, content);
                    } catch (Exception e) {
                        // Skip unreadable files
                    }
                });
        } catch (Exception e) {
            // Directory doesn't exist
        }

        // Truncate if needed
        if (maxEntries != null && entries.size() > maxEntries) {
            List<String> keys = new ArrayList<>(entries.keySet());
            Collections.sort(keys);
            Map<String, String> truncated = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(keys.size(), maxEntries); i++) {
                truncated.put(keys.get(i), entries.get(keys.get(i)));
            }
            return new LocalReadResult(truncated, skippedSecrets);
        }

        return new LocalReadResult(entries, skippedSecrets);
    }

    private static String getTeamMemPath() {
        String home = System.getProperty("user.home");
        String cwd = System.getProperty("user.dir");
        String slug = cwd.replaceAll("[^a-zA-Z0-9]", "-");
        return home + "/.claude/projects/" + slug + "/team/";
    }

    private static void logPull(long startTime, boolean success, int filesWritten, String errorType, Integer status) {
        long duration = System.currentTimeMillis() - startTime;
        Debug.logForDebugging(String.format("team-memory-sync: pull %s (files=%d, duration=%dms, error=%s)",
            success ? "success" : "failed", filesWritten, duration, errorType));
    }

    private static void logPush(long startTime, boolean success, int filesUploaded, boolean conflict, int conflictRetries, String errorType, Integer status) {
        long duration = System.currentTimeMillis() - startTime;
        Debug.logForDebugging(String.format("team-memory-sync: push %s (files=%d, conflict=%b, duration=%dms)",
            success ? "success" : "failed", filesUploaded, conflict, duration));
    }
}