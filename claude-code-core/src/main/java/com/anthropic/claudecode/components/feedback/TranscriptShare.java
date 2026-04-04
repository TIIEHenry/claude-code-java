/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/FeedbackSurvey/submitTranscriptShare
 */
package com.anthropic.claudecode.components.feedback;

import java.time.Instant;

import java.util.*;
import java.time.Instant;

import java.util.concurrent.*;
import java.time.Instant;

import java.nio.file.*;

/**
 * Submit transcript share - Transcript sharing utilities.
 */
public final class TranscriptShare {
    private final String apiEndpoint;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Create transcript share.
     */
    public TranscriptShare(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    /**
     * Share result record.
     */
    public record ShareResult(
        boolean success,
        String shareId,
        String shareUrl,
        String error,
        Instant expiresAt
    ) {
        public static ShareResult success(String shareId, String shareUrl) {
            return new ShareResult(true, shareId, shareUrl, null,
                Instant.now().plusSeconds(7 * 24 * 60 * 60)); // 7 days
        }

        public static ShareResult failure(String error) {
            return new ShareResult(false, null, null, error, null);
        }

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Share options record.
     */
    public record ShareOptions(
        boolean includeCode,
        boolean includeFileContents,
        boolean anonymize,
        String title,
        String description,
        int expiresInDays
    ) {
        public static ShareOptions defaultOptions() {
            return new ShareOptions(true, false, true, "", "", 7);
        }
    }

    /**
     * Submit transcript share.
     */
    public CompletableFuture<ShareResult> submitShare(
        String transcript,
        ShareOptions options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Process transcript
                String processed = processTranscript(transcript, options);

                // Would submit to API
                String shareId = UUID.randomUUID().toString();
                String shareUrl = "https://share.claude.ai/transcript/" + shareId;

                return ShareResult.success(shareId, shareUrl);
            } catch (Exception e) {
                return ShareResult.failure(e.getMessage());
            }
        }, executor);
    }

    /**
     * Process transcript for sharing.
     */
    private String processTranscript(String transcript, ShareOptions options) {
        String result = transcript;

        if (options.anonymize()) {
            result = anonymizeTranscript(result);
        }

        if (!options.includeCode()) {
            result = removeCodeBlocks(result);
        }

        return result;
    }

    /**
     * Anonymize transcript.
     */
    private String anonymizeTranscript(String transcript) {
        // Replace file paths
        String result = transcript.replaceAll(
            "/Users/[^/\\s]+",
            "/Users/[user]"
        );

        // Replace email addresses
        result = result.replaceAll(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            "[email]"
        );

        // Replace API keys
        result = result.replaceAll(
            "sk-[a-zA-Z0-9]{20,}",
            "[api-key]"
        );

        return result;
    }

    /**
     * Remove code blocks.
     */
    private String removeCodeBlocks(String transcript) {
        return transcript.replaceAll("```[\\s\\S]*?```", "[code removed]");
    }

    /**
     * Share config record.
     */
    public record ShareConfig(
        int maxTranscriptSize,
        List<String> allowedDomains,
        boolean requireAuth,
        int defaultExpiryDays
    ) {
        public static ShareConfig defaultConfig() {
            return new ShareConfig(
                1_000_000,
                Collections.emptyList(),
                true,
                7
            );
        }
    }

    /**
     * Get share info.
     */
    public CompletableFuture<ShareInfo> getShareInfo(String shareId) {
        return CompletableFuture.supplyAsync(() -> {
            // Would fetch from API
            return new ShareInfo(
                shareId,
                "Shared Transcript",
                Instant.now(),
                Instant.now().plusSeconds(7 * 24 * 60 * 60),
                100
            );
        }, executor);
    }

    /**
     * Delete share.
     */
    public CompletableFuture<Boolean> deleteShare(String shareId) {
        return CompletableFuture.supplyAsync(() -> {
            // Would delete from API
            return true;
        }, executor);
    }

    /**
     * Share info record.
     */
    public record ShareInfo(
        String shareId,
        String title,
        Instant createdAt,
        Instant expiresAt,
        int viewCount
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public long getRemainingSeconds() {
            return Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        }
    }

    /**
     * Shutdown executor.
     */
    public void shutdown() {
        executor.shutdown();
    }
}