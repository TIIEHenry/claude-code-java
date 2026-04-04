/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/config.ts types
 */
package com.anthropic.claudecode.utils.config;

import java.util.*;

/**
 * Project configuration.
 */
public record ProjectConfig(
    List<String> allowedTools,
    List<String> mcpContextUris,
    Map<String, McpServerConfig> mcpServers,
    Long lastAPIDuration,
    Long lastAPIDurationWithoutRetries,
    Long lastToolDuration,
    Double lastCost,
    Long lastDuration,
    Integer lastLinesAdded,
    Integer lastLinesRemoved,
    Long lastTotalInputTokens,
    Long lastTotalOutputTokens,
    Long lastTotalCacheCreationInputTokens,
    Long lastTotalCacheReadInputTokens,
    Integer lastTotalWebSearchRequests,
    String lastSessionId,
    Map<String, ModelUsageStats> lastModelUsage,
    Map<String, Number> lastSessionMetrics,
    List<String> exampleFiles,
    Long exampleFilesGeneratedAt,
    Boolean hasTrustDialogAccepted,
    Boolean hasCompletedProjectOnboarding,
    Integer projectOnboardingSeenCount,
    Boolean hasClaudeMdExternalIncludesApproved,
    Boolean hasClaudeMdExternalIncludesWarningShown,
    List<String> enabledMcpjsonServers,
    List<String> disabledMcpjsonServers,
    Boolean enableAllProjectMcpServers,
    List<String> disabledMcpServers,
    List<String> enabledMcpServers,
    ActiveWorktreeSession activeWorktreeSession,
    String remoteControlSpawnMode
) {
    /**
     * Create default project config.
     */
    public static ProjectConfig createDefault() {
        return new ProjectConfig(
            List.of(),
            List.of(),
            Map.of(),
            null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            0,
            false, false,
            List.of(), List.of(), false,
            List.of(), List.of(),
            null, null
        );
    }

    /**
     * MCP server configuration.
     */
    public record McpServerConfig(
        String command,
        List<String> args,
        Map<String, String> env,
        String type,
        String url,
        String transport,
        Map<String, Object> options
    ) {}

    /**
     * Model usage statistics.
     */
    public record ModelUsageStats(
        long inputTokens,
        long outputTokens,
        long cacheReadInputTokens,
        long cacheCreationInputTokens,
        int webSearchRequests,
        double costUSD
    ) {}

    /**
     * Active worktree session.
     */
    public record ActiveWorktreeSession(
        String originalCwd,
        String worktreePath,
        String worktreeName,
        String originalBranch,
        String sessionId,
        Boolean hookBased
    ) {}
}

/**
 * Release channel enum.
 */
enum ReleaseChannel {
    STABLE,
    LATEST
}

/**
 * Install method enum.
 */
enum InstallMethod {
    LOCAL,
    NATIVE,
    GLOBAL,
    UNKNOWN
}