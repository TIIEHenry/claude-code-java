/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code GitHub PR status utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * GitHub PR status utilities for fetching PR information.
 */
public final class GhPrStatus {
    private GhPrStatus() {}

    private static final int GH_TIMEOUT_MS = 5000;

    /**
     * PR review state enum.
     */
    public enum PrReviewState {
        APPROVED,
        PENDING,
        CHANGES_REQUESTED,
        DRAFT,
        MERGED,
        CLOSED
    }

    /**
     * PR status record.
     */
    public record PrStatus(int number, String url, PrReviewState reviewState) {}

    /**
     * Derive review state from GitHub API values.
     */
    public static PrReviewState deriveReviewState(boolean isDraft, String reviewDecision) {
        if (isDraft) return PrReviewState.DRAFT;
        switch (reviewDecision) {
            case "APPROVED":
                return PrReviewState.APPROVED;
            case "CHANGES_REQUESTED":
                return PrReviewState.CHANGES_REQUESTED;
            default:
                return PrReviewState.PENDING;
        }
    }

    /**
     * Fetch PR status for the current branch using `gh pr view`.
     */
    public static CompletableFuture<PrStatus> fetchPrStatus() {
        return Git.getIsGit().thenCompose(isGit -> {
            if (!isGit) return CompletableFuture.completedFuture(null);

            // Skip on the default branch
            return Git.getBranch().thenCompose(branch ->
                    Git.getDefaultBranch().thenCompose(defaultBranch -> {
                        if (branch.equals(defaultBranch)) {
                            return CompletableFuture.completedFuture(null);
                        }

                        return ExecFileNoThrow.execFileNoThrow(
                                "gh",
                                new String[]{"pr", "view", "--json",
                                        "number,url,reviewDecision,isDraft,headRefName,state"},
                                new ExecFileNoThrow.ExecOptions(GH_TIMEOUT_MS, true, null, null, false, null)
                        ).thenApply(result -> {
                            if (result.code() != 0 || result.stdout().trim().isEmpty()) {
                                return null;
                            }

                            try {
                                GhPrData data = SlowOperations.jsonParse(result.stdout(), GhPrData.class);

                                // Don't show PR status for PRs from the default branch
                                if (data.headRefName.equals(defaultBranch) ||
                                    data.headRefName.equals("main") ||
                                    data.headRefName.equals("master")) {
                                    return null;
                                }

                                // Don't show merged or closed PRs
                                if (data.state.equals("MERGED") || data.state.equals("CLOSED")) {
                                    return null;
                                }

                                return new PrStatus(
                                        data.number,
                                        data.url,
                                        deriveReviewState(data.isDraft, data.reviewDecision)
                                );
                            } catch (Exception e) {
                                return null;
                            }
                        });
                    })
            );
        });
    }

    /**
     * GitHub PR data from API.
     */
    private record GhPrData(
            int number,
            String url,
            String reviewDecision,
            boolean isDraft,
            String headRefName,
            String state
    ) {}
}