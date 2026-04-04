/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commit attribution utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Attribution tracking for Claude's contributions to files.
 * Tracks file modifications and calculates contribution percentages.
 */
public final class CommitAttribution {
    private CommitAttribution() {}

    // Internal model repos allowlist
    private static final Set<String> INTERNAL_MODEL_REPOS = Set.of(
        "github.com:anthropics/claude-cli-internal",
        "github.com/anthropics/claude-cli-internal"
        // Additional repos would be added here
    );

    private static volatile String repoClassCache = null;

    /**
     * File attribution state record.
     */
    public record FileAttributionState(
        String contentHash,
        int claudeContribution,
        long mtime
    ) {}

    /**
     * Attribution state for tracking Claude's contributions.
     */
    public record AttributionState(
        Map<String, FileAttributionState> fileStates,
        Map<String, BaselineState> sessionBaselines,
        String surface,
        String startingHeadSha,
        int promptCount,
        int promptCountAtLastCommit,
        int permissionPromptCount,
        int permissionPromptCountAtLastCommit,
        int escapeCount,
        int escapeCountAtLastCommit
    ) {
        public static AttributionState createEmpty() {
            return new AttributionState(
                    new HashMap<>(),
                    new HashMap<>(),
                    getClientSurface(),
                    null,
                    0, 0, 0, 0, 0, 0
            );
        }
    }

    /**
     * Baseline state for net change calculation.
     */
    public record BaselineState(String contentHash, long mtime) {}

    /**
     * Attribution summary for a commit.
     */
    public record AttributionSummary(
        int claudePercent,
        int claudeChars,
        int humanChars,
        List<String> surfaces
    ) {}

    /**
     * Per-file attribution details.
     */
    public record FileAttribution(
        int claudeChars,
        int humanChars,
        int percent,
        String surface
    ) {}

    /**
     * Full attribution data for git notes.
     */
    public record AttributionData(
            int version,
            AttributionSummary summary,
            Map<String, FileAttribution> files,
            Map<String, SurfaceBreakdown> surfaceBreakdown,
            List<String> excludedGenerated,
            List<String> sessions
    ) {}

    /**
     * Surface breakdown record.
     */
    public record SurfaceBreakdown(int claudeChars, int percent) {}

    /**
     * Get the current client surface from environment.
     */
    public static String getClientSurface() {
        String entrypoint = System.getenv("CLAUDE_CODE_ENTRYPOINT");
        return entrypoint != null ? entrypoint : "cli";
    }

    /**
     * Build a surface key with model name.
     */
    public static String buildSurfaceKey(String surface, String modelName) {
        return surface + "/" + modelName;
    }

    /**
     * Compute SHA-256 hash of content.
     */
    public static String computeContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    /**
     * Normalize file path to relative path from cwd.
     */
    public static String normalizeFilePath(String filePath, String cwd) {
        if (!Paths.get(filePath).isAbsolute()) {
            return filePath;
        }

        Path path = Paths.get(filePath);
        Path cwdPath = Paths.get(cwd);

        try {
            path = path.toRealPath();
            cwdPath = cwdPath.toRealPath();
        } catch (IOException e) {
            // Use original paths if realpath fails
        }

        if (path.startsWith(cwdPath) || path.equals(cwdPath)) {
            return cwdPath.relativize(path).toString().replace('\\', '/');
        }

        return filePath;
    }

    /**
     * Track a file modification by Claude.
     */
    public static AttributionState trackFileModification(
            AttributionState state,
            String filePath,
            String oldContent,
            String newContent,
            long mtime) {

        String normalizedPath = normalizeFilePath(filePath, System.getProperty("user.dir"));

        int claudeContribution = computeContribution(oldContent, newContent);

        FileAttributionState existingState = state.fileStates().get(normalizedPath);
        int existingContribution = existingState != null ? existingState.claudeContribution() : 0;

        FileAttributionState newState = new FileAttributionState(
                computeContentHash(newContent),
                existingContribution + claudeContribution,
                mtime
        );

        Map<String, FileAttributionState> newFileStates = new HashMap<>(state.fileStates());
        newFileStates.put(normalizedPath, newState);

        return new AttributionState(
                newFileStates,
                state.sessionBaselines(),
                state.surface(),
                state.startingHeadSha(),
                state.promptCount(),
                state.promptCountAtLastCommit(),
                state.permissionPromptCount(),
                state.permissionPromptCountAtLastCommit(),
                state.escapeCount(),
                state.escapeCountAtLastCommit()
        );
    }

    /**
     * Compute character contribution for a modification.
     */
    private static int computeContribution(String oldContent, String newContent) {
        if (oldContent.isEmpty() || newContent.isEmpty()) {
            return oldContent.isEmpty() ? newContent.length() : oldContent.length();
        }

        int minLen = Math.min(oldContent.length(), newContent.length());
        int prefixEnd = 0;
        while (prefixEnd < minLen && oldContent.charAt(prefixEnd) == newContent.charAt(prefixEnd)) {
            prefixEnd++;
        }

        int suffixLen = 0;
        while (suffixLen < minLen - prefixEnd &&
                oldContent.charAt(oldContent.length() - 1 - suffixLen) ==
                        newContent.charAt(newContent.length() - 1 - suffixLen)) {
            suffixLen++;
        }

        int oldChangedLen = oldContent.length() - prefixEnd - suffixLen;
        int newChangedLen = newContent.length() - prefixEnd - suffixLen;

        return Math.max(oldChangedLen, newChangedLen);
    }

    /**
     * Track a file creation by Claude.
     */
    public static AttributionState trackFileCreation(
            AttributionState state,
            String filePath,
            String content,
            long mtime) {
        return trackFileModification(state, filePath, "", content, mtime);
    }

    /**
     * Track a file deletion by Claude.
     */
    public static AttributionState trackFileDeletion(
            AttributionState state,
            String filePath,
            String oldContent) {
        String normalizedPath = normalizeFilePath(filePath, System.getProperty("user.dir"));
        FileAttributionState existingState = state.fileStates().get(normalizedPath);
        int existingContribution = existingState != null ? existingState.claudeContribution() : 0;

        FileAttributionState newState = new FileAttributionState(
                "",
                existingContribution + oldContent.length(),
                System.currentTimeMillis()
        );

        Map<String, FileAttributionState> newFileStates = new HashMap<>(state.fileStates());
        newFileStates.put(normalizedPath, newState);

        return new AttributionState(
                newFileStates,
                state.sessionBaselines(),
                state.surface(),
                state.startingHeadSha(),
                state.promptCount(),
                state.promptCountAtLastCommit(),
                state.permissionPromptCount(),
                state.permissionPromptCountAtLastCommit(),
                state.escapeCount(),
                state.escapeCountAtLastCommit()
        );
    }

    /**
     * Sanitize model name to public equivalent.
     */
    public static String sanitizeModelName(String shortName) {
        if (shortName.contains("opus-4-6")) return "claude-opus-4-6";
        if (shortName.contains("opus-4-5")) return "claude-opus-4-5";
        if (shortName.contains("opus-4-1")) return "claude-opus-4-1";
        if (shortName.contains("opus-4")) return "claude-opus-4";
        if (shortName.contains("sonnet-4-6")) return "claude-sonnet-4-6";
        if (shortName.contains("sonnet-4-5")) return "claude-sonnet-4-5";
        if (shortName.contains("sonnet-4")) return "claude-sonnet-4";
        if (shortName.contains("sonnet-3-7")) return "claude-sonnet-3-7";
        if (shortName.contains("haiku-4-5")) return "claude-haiku-4-5";
        if (shortName.contains("haiku-3-5")) return "claude-haiku-3-5";
        return "claude";
    }

    /**
     * Sanitize surface key to use public model names.
     */
    public static String sanitizeSurfaceKey(String surfaceKey) {
        int slashIndex = surfaceKey.lastIndexOf('/');
        if (slashIndex == -1) {
            return surfaceKey;
        }

        String surface = surfaceKey.substring(0, slashIndex);
        String model = surfaceKey.substring(slashIndex + 1);
        String sanitizedModel = sanitizeModelName(model);

        return surface + "/" + sanitizedModel;
    }

    /**
     * Check if repo is internal model repo (cached).
     */
    public static boolean isInternalModelRepoCached() {
        return "internal".equals(repoClassCache);
    }

    /**
     * Get cached repo classification.
     */
    public static String getRepoClassCached() {
        return repoClassCache;
    }

    /**
     * Check if repo is internal model repo.
     */
    public static CompletableFuture<Boolean> isInternalModelRepo() {
        return CompletableFuture.supplyAsync(() -> {
            if (repoClassCache != null) {
                return "internal".equals(repoClassCache);
            }

            // In real implementation, would check git remote URL
            repoClassCache = "external";
            return false;
        });
    }

    /**
     * Increment prompt count.
     */
    public static AttributionState incrementPromptCount(AttributionState state) {
        return new AttributionState(
                state.fileStates(),
                state.sessionBaselines(),
                state.surface(),
                state.startingHeadSha(),
                state.promptCount() + 1,
                state.promptCountAtLastCommit(),
                state.permissionPromptCount(),
                state.permissionPromptCountAtLastCommit(),
                state.escapeCount(),
                state.escapeCountAtLastCommit()
        );
    }
}