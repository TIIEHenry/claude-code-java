/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/api.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;

/**
 * API utilities for tool schema generation and context management.
 */
public final class ApiUtils {
    private ApiUtils() {}

    /**
     * Cache scope for system prompt blocks.
     */
    public enum CacheScope {
        GLOBAL,
        ORG,
        NONE
    }

    /**
     * System prompt block with cache scope.
     */
    public record SystemPromptBlock(String text, CacheScope cacheScope) {}

    /**
     * Split system prompt blocks by content type.
     */
    public static List<SystemPromptBlock> splitSysPromptPrefix(List<String> systemPrompt) {
        return splitSysPromptPrefix(systemPrompt, false);
    }

    /**
     * Split system prompt blocks by content type for API matching and cache control.
     */
    public static List<SystemPromptBlock> splitSysPromptPrefix(
            List<String> systemPrompt,
            boolean skipGlobalCacheForSystemPrompt) {
        List<SystemPromptBlock> result = new ArrayList<>();
        for (String block : systemPrompt) {
            if (block == null || block.isEmpty()) continue;
            result.add(new SystemPromptBlock(block, CacheScope.ORG));
        }
        return result;
    }

    /**
     * Append system context to system prompt.
     */
    public static List<String> appendSystemContext(
            List<String> systemPrompt,
            Map<String, String> context) {
        List<String> result = new ArrayList<>(systemPrompt);
        if (!context.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : context.entrySet()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            result.add(sb.toString());
        }
        return result;
    }

    /**
     * Log API prefix metrics.
     */
    public static void logAPIPrefix(List<String> systemPrompt) {
        if (systemPrompt == null || systemPrompt.isEmpty()) return;
        AnalyticsMetadata.logEvent("tengu_sysprompt_block", Map.of(
            "block_count", String.valueOf(systemPrompt.size())
        ));
    }
}