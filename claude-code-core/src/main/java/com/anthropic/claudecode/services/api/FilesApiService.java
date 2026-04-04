/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/filesApi.ts
 */
package com.anthropic.claudecode.services.api;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Files API client for managing file uploads/downloads.
 */
public final class FilesApiService {
    private static final String FILES_API_BETA_HEADER = "files-api-2025-04-14,oauth-2025-04-20";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500;
    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    private static final int DEFAULT_CONCURRENCY = 5;

    private FilesApiService() {}

    /**
     * File specification parsed from CLI args.
     */
    public record FileSpec(String fileId, String relativePath) {}

    /**
     * Files API configuration.
     */
    public record FilesApiConfig(
        String oauthToken,
        String baseUrl,
        String sessionId
    ) {
        public FilesApiConfig(String oauthToken, String sessionId) {
            this(oauthToken, null, sessionId);
        }
    }

    /**
     * Download result.
     */
    public record DownloadResult(
        String fileId,
        String path,
        boolean success,
        String error,
        Long bytesWritten
    ) {}

    /**
     * Upload result.
     */
    public record UploadResult(
        String path,
        String fileId,
        long size,
        boolean success,
        String error
    ) {}

    /**
     * File metadata.
     */
    public record FileMetadata(
        String filename,
        String fileId,
        long size
    ) {}

    /**
     * Get default API base URL.
     */
    public static String getDefaultApiBaseUrl() {
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }
        baseUrl = System.getenv("CLAUDE_CODE_API_BASE_URL");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }
        return "https://api.anthropic.com";
    }

    /**
     * Download a file from the Files API.
     */
    public static CompletableFuture<byte[]> downloadFile(String fileId, FilesApiConfig config) {
        String baseUrl = config.baseUrl() != null ? config.baseUrl() : getDefaultApiBaseUrl();
        String url = baseUrl + "/v1/files/" + fileId + "/content";

        return retryWithBackoff("Download file " + fileId, attempt -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(60))
                    .build();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.oauthToken())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("anthropic-beta", FILES_API_BETA_HEADER)
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    return new RetryResult<>(true, response.body(), null);
                }

                if (response.statusCode() == 404) {
                    throw new CompletionException(new FileNotFoundException("File not found: " + fileId));
                }
                if (response.statusCode() == 401) {
                    throw new CompletionException(new SecurityException("Authentication failed"));
                }
                if (response.statusCode() == 403) {
                    throw new CompletionException(new SecurityException("Access denied to file: " + fileId));
                }

                return new RetryResult<>(false, null, "status " + response.statusCode());
            } catch (IOException | InterruptedException e) {
                return new RetryResult<>(false, null, e.getMessage());
            }
        });
    }

    /**
     * Build download path for a file.
     */
    public static String buildDownloadPath(String basePath, String sessionId, String relativePath) {
        String normalized = Paths.get(relativePath).normalize().toString();
        if (normalized.startsWith("..")) {
            return null; // Path traversal attempt
        }

        Path uploadsBase = Paths.get(basePath, sessionId, "uploads");
        return uploadsBase.resolve(normalized).toString();
    }

    /**
     * Download and save a file.
     */
    public static CompletableFuture<DownloadResult> downloadAndSaveFile(
            FileSpec attachment, FilesApiConfig config, String cwd) {
        String fullPath = buildDownloadPath(cwd, config.sessionId(), attachment.relativePath());

        if (fullPath == null) {
            return CompletableFuture.completedFuture(new DownloadResult(
                attachment.fileId(),
                "",
                false,
                "Invalid file path: " + attachment.relativePath(),
                null
            ));
        }

        return downloadFile(attachment.fileId(), config).thenApply(content -> {
            try {
                // Create parent directory
                Path parentDir = Paths.get(fullPath).getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                }

                // Write file
                Files.write(Paths.get(fullPath), content);

                return new DownloadResult(
                    attachment.fileId(),
                    fullPath,
                    true,
                    null,
                    (long) content.length
                );
            } catch (IOException e) {
                return new DownloadResult(
                    attachment.fileId(),
                    fullPath,
                    false,
                    e.getMessage(),
                    null
                );
            }
        }).exceptionally(e -> new DownloadResult(
            attachment.fileId(),
            fullPath,
            false,
            e.getCause() != null ? e.getCause().getMessage() : e.getMessage(),
            null
        ));
    }

    /**
     * Download all session files in parallel.
     */
    public static CompletableFuture<List<DownloadResult>> downloadSessionFiles(
            List<FileSpec> files, FilesApiConfig config, String cwd) {
        return downloadSessionFiles(files, config, cwd, DEFAULT_CONCURRENCY);
    }

    /**
     * Download all session files in parallel with concurrency limit.
     */
    public static CompletableFuture<List<DownloadResult>> downloadSessionFiles(
            List<FileSpec> files, FilesApiConfig config, String cwd, int concurrency) {
        if (files.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(concurrency, files.size()));
        List<CompletableFuture<DownloadResult>> futures = new ArrayList<>();

        for (FileSpec file : files) {
            futures.add(downloadAndSaveFile(file, config, cwd));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<DownloadResult> results = new ArrayList<>();
                for (CompletableFuture<DownloadResult> future : futures) {
                    results.add(future.join());
                }
                executor.shutdown();
                return results;
            });
    }

    /**
     * Upload a file to the Files API.
     */
    public static CompletableFuture<UploadResult> uploadFile(
            String filePath, String relativePath, FilesApiConfig config) {
        return uploadFile(filePath, relativePath, config, null);
    }

    /**
     * Upload a file to the Files API with cancellation support.
     */
    public static CompletableFuture<UploadResult> uploadFile(
            String filePath, String relativePath, FilesApiConfig config,
            CancellationToken cancellationToken) {
        String baseUrl = config.baseUrl() != null ? config.baseUrl() : getDefaultApiBaseUrl();
        String url = baseUrl + "/v1/files";

        // Read file content
        byte[] content;
        try {
            content = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            return CompletableFuture.completedFuture(new UploadResult(
                relativePath, null, 0, false, "Failed to read file: " + e.getMessage()
            ));
        }

        long fileSize = content.length;
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            return CompletableFuture.completedFuture(new UploadResult(
                relativePath, null, 0, false,
                "File exceeds maximum size of " + MAX_FILE_SIZE_BYTES + " bytes"
            ));
        }

        return retryWithBackoff("Upload file " + relativePath, attempt -> {
            try {
                // Build multipart body
                String boundary = "----FormBoundary" + UUID.randomUUID().toString().replace("-", "");
                String filename = Paths.get(relativePath).getFileName().toString();

                ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

                // File part
                bodyStream.write(("--" + boundary + "\r\n").getBytes());
                bodyStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes());
                bodyStream.write("Content-Type: application/octet-stream\r\n\r\n".getBytes());
                bodyStream.write(content);
                bodyStream.write("\r\n".getBytes());

                // Purpose part
                bodyStream.write(("--" + boundary + "\r\n").getBytes());
                bodyStream.write("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n".getBytes());
                bodyStream.write("user_data\r\n".getBytes());

                // End boundary
                bodyStream.write(("--" + boundary + "--\r\n").getBytes());

                byte[] body = bodyStream.toByteArray();

                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(120))
                    .build();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.oauthToken())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("anthropic-beta", FILES_API_BETA_HEADER)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Content-Length", String.valueOf(body.length))
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

                if (cancellationToken != null && cancellationToken.isCancelled()) {
                    throw new CompletionException(new InterruptedException("Upload cancelled"));
                }

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    // Parse file ID from response (simple JSON parsing)
                    String responseBody = response.body();
                    String fileId = extractFileId(responseBody);
                    if (fileId == null) {
                        return new RetryResult<>(false, null, "No file ID in response");
                    }
                    return new RetryResult<>(true, new UploadResult(relativePath, fileId, fileSize, true, null), null);
                }

                if (response.statusCode() == 401) {
                    throw new CompletionException(new SecurityException("Authentication failed"));
                }
                if (response.statusCode() == 403) {
                    throw new CompletionException(new SecurityException("Access denied"));
                }
                if (response.statusCode() == 413) {
                    throw new CompletionException(new IllegalArgumentException("File too large"));
                }

                return new RetryResult<>(false, null, "status " + response.statusCode());
            } catch (IOException | InterruptedException e) {
                return new RetryResult<>(false, null, e.getMessage());
            }
        });
    }

    /**
     * Upload multiple files in parallel.
     */
    public static CompletableFuture<List<UploadResult>> uploadSessionFiles(
            List<FileSpec> files, FilesApiConfig config) {
        return uploadSessionFiles(files, config, DEFAULT_CONCURRENCY);
    }

    /**
     * Upload multiple files in parallel with concurrency limit.
     */
    public static CompletableFuture<List<UploadResult>> uploadSessionFiles(
            List<FileSpec> files, FilesApiConfig config, int concurrency) {
        if (files.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(concurrency, files.size()));
        List<CompletableFuture<UploadResult>> futures = new ArrayList<>();

        for (FileSpec file : files) {
            futures.add(uploadFile(file.relativePath(), file.relativePath(), config));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<UploadResult> results = new ArrayList<>();
                for (CompletableFuture<UploadResult> future : futures) {
                    results.add(future.join());
                }
                executor.shutdown();
                return results;
            });
    }

    /**
     * Parse file specs from CLI arguments.
     */
    public static List<FileSpec> parseFileSpecs(List<String> fileSpecs) {
        List<FileSpec> files = new ArrayList<>();

        for (String spec : fileSpecs) {
            // Handle space-separated specs
            for (String s : spec.split(" ")) {
                if (s.isEmpty()) continue;

                int colonIndex = s.indexOf(':');
                if (colonIndex == -1) continue;

                String fileId = s.substring(0, colonIndex);
                String relativePath = s.substring(colonIndex + 1);

                if (!fileId.isEmpty() && !relativePath.isEmpty()) {
                    files.add(new FileSpec(fileId, relativePath));
                }
            }
        }

        return files;
    }

    // Retry helper
    private static <T> CompletableFuture<T> retryWithBackoff(
            String operation, RetryFunction<T> attemptFn) {
        CompletableFuture<T> result = new CompletableFuture<>();

        retryWithBackoffInternal(operation, attemptFn, 1, result, "");

        return result;
    }

    private static <T> void retryWithBackoffInternal(
            String operation, RetryFunction<T> attemptFn, int attempt,
            CompletableFuture<T> result, String lastError) {
        if (attempt > MAX_RETRIES) {
            result.completeExceptionally(new RuntimeException(lastError + " after " + MAX_RETRIES + " attempts"));
            return;
        }

        try {
            RetryResult<T> retryResult = attemptFn.apply(attempt);
            if (retryResult.done()) {
                result.complete(retryResult.value());
            } else {
                long delayMs = BASE_DELAY_MS * (long) Math.pow(2, attempt - 1);
                CompletableFuture.runAsync(() -> retryWithBackoffInternal(
                    operation, attemptFn, attempt + 1, result, retryResult.error()),
                    CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS));
            }
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
    }

    private static String extractFileId(String json) {
        // Simple JSON extraction - look for "id":"..."
        int start = json.indexOf("\"id\":\"");
        if (start == -1) return null;
        start += 6;
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    // Helper types
    private record RetryResult<T>(boolean done, T value, String error) {}
    @FunctionalInterface
    private interface RetryFunction<T> {
        RetryResult<T> apply(int attempt) throws Exception;
    }

    /**
     * Cancellation token for upload operations.
     */
    public static class CancellationToken {
        private volatile boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}