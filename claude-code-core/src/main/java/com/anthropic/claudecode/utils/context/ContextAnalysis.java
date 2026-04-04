/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code context analysis utilities
 */
package com.anthropic.claudecode.utils.context;

import java.util.*;

/**
 * Context analysis utilities for token tracking.
 */
public final class ContextAnalysis {
    private ContextAnalysis() {}

    /**
     * Token statistics for context analysis.
     */
    public record TokenStats(
            Map<String, Integer> toolRequests,
            Map<String, Integer> toolResults,
            int humanMessages,
            int assistantMessages,
            int localCommandOutputs,
            int other,
            Map<String, Integer> attachments,
            Map<String, DuplicateReadInfo> duplicateFileReads,
            int total
    ) {
        public static TokenStats empty() {
            return new TokenStats(
                    new HashMap<>(),
                    new HashMap<>(),
                    0, 0, 0, 0,
                    new HashMap<>(),
                    new HashMap<>(),
                    0
            );
        }
    }

    /**
     * Duplicate read information.
     */
    public record DuplicateReadInfo(int count, int tokens) {}

    /**
     * Content block type enum.
     */
    public enum BlockType {
        TEXT,
        TOOL_USE,
        TOOL_RESULT,
        IMAGE,
        THINKING,
        OTHER
    }

    /**
     * Content block for analysis.
     */
    public record ContentBlock(
            BlockType type,
            String text,
            String toolName,
            String toolId,
            Map<String, Object> input,
            String toolUseId
    ) {}

    /**
     * Message type for analysis.
     */
    public enum MessageType {
        USER,
        ASSISTANT
    }

    /**
     * Message for analysis.
     */
    public record Message(
            MessageType type,
            List<ContentBlock> content
    ) {}

    /**
     * Analyze messages for token statistics.
     */
    public static TokenStats analyzeContext(List<Message> messages) {
        Map<String, Integer> toolRequests = new HashMap<>();
        Map<String, Integer> toolResults = new HashMap<>();
        Map<String, Integer> attachments = new HashMap<>();
        Map<String, DuplicateReadInfo> duplicateFileReads = new HashMap<>();

        int humanMessages = 0;
        int assistantMessages = 0;
        int localCommandOutputs = 0;
        int other = 0;
        int total = 0;

        Map<String, String> toolIdsToToolNames = new HashMap<>();
        Map<String, String> readToolIdToFilePath = new HashMap<>();
        Map<String, FileReadStats> fileReadStats = new HashMap<>();

        for (Message msg : messages) {
            for (ContentBlock block : msg.content()) {
                int tokens = estimateTokens(block);
                total += tokens;

                switch (block.type()) {
                    case TEXT -> {
                        if (msg.type() == MessageType.USER &&
                            block.text() != null &&
                            block.text().contains("local-command-stdout")) {
                            localCommandOutputs += tokens;
                        } else {
                            if (msg.type() == MessageType.USER) {
                                humanMessages += tokens;
                            } else {
                                assistantMessages += tokens;
                            }
                        }
                    }

                    case TOOL_USE -> {
                        String toolName = block.toolName() != null ? block.toolName() : "unknown";
                        increment(toolRequests, toolName, tokens);

                        if (block.toolId() != null) {
                            toolIdsToToolNames.put(block.toolId(), toolName);
                        }

                        // Track Read tool file paths
                        if ("Read".equals(toolName) && block.input() != null) {
                            Object filePath = block.input().get("file_path");
                            if (filePath != null && block.toolId() != null) {
                                readToolIdToFilePath.put(block.toolId(), filePath.toString());
                            }
                        }
                    }

                    case TOOL_RESULT -> {
                        String toolId = block.toolUseId();
                        String toolName = toolId != null ?
                                toolIdsToToolNames.getOrDefault(toolId, "unknown") : "unknown";
                        increment(toolResults, toolName, tokens);

                        // Track file read tokens
                        if ("Read".equals(toolName) && toolId != null) {
                            String path = readToolIdToFilePath.get(toolId);
                            if (path != null) {
                                FileReadStats stats = fileReadStats.getOrDefault(
                                        path, new FileReadStats(0, 0));
                                fileReadStats.put(path, new FileReadStats(
                                        stats.count + 1,
                                        stats.totalTokens + tokens
                                ));
                            }
                        }
                    }

                    case IMAGE, THINKING, OTHER -> other += tokens;
                }
            }
        }

        // Calculate duplicate file reads
        for (Map.Entry<String, FileReadStats> entry : fileReadStats.entrySet()) {
            FileReadStats stats = entry.getValue();
            if (stats.count > 1) {
                int averageTokensPerRead = stats.totalTokens / stats.count;
                int duplicateTokens = averageTokensPerRead * (stats.count - 1);
                duplicateFileReads.put(entry.getKey(),
                        new DuplicateReadInfo(stats.count, duplicateTokens));
            }
        }

        return new TokenStats(
                toolRequests,
                toolResults,
                humanMessages,
                assistantMessages,
                localCommandOutputs,
                other,
                attachments,
                duplicateFileReads,
                total
        );
    }

    /**
     * Convert token stats to metrics map.
     */
    public static Map<String, Number> tokenStatsToMetrics(TokenStats stats) {
        Map<String, Number> metrics = new LinkedHashMap<>();

        metrics.put("total_tokens", stats.total());
        metrics.put("human_message_tokens", stats.humanMessages());
        metrics.put("assistant_message_tokens", stats.assistantMessages());
        metrics.put("local_command_output_tokens", stats.localCommandOutputs());
        metrics.put("other_tokens", stats.other());

        // Tool request tokens
        stats.toolRequests().forEach((tool, tokens) ->
                metrics.put("tool_request_" + tool + "_tokens", tokens));

        // Tool result tokens
        stats.toolResults().forEach((tool, tokens) ->
                metrics.put("tool_result_" + tool + "_tokens", tokens));

        // Duplicate reads
        int duplicateTotal = stats.duplicateFileReads().values().stream()
                .mapToInt(DuplicateReadInfo::tokens)
                .sum();

        metrics.put("duplicate_read_tokens", duplicateTotal);
        metrics.put("duplicate_read_file_count", stats.duplicateFileReads().size());

        // Percentages
        if (stats.total() > 0) {
            metrics.put("human_message_percent",
                    Math.round((double) stats.humanMessages() / stats.total() * 100));
            metrics.put("assistant_message_percent",
                    Math.round((double) stats.assistantMessages() / stats.total() * 100));
            metrics.put("local_command_output_percent",
                    Math.round((double) stats.localCommandOutputs() / stats.total() * 100));
            metrics.put("duplicate_read_percent",
                    Math.round((double) duplicateTotal / stats.total() * 100));

            int toolRequestTotal = stats.toolRequests().values().stream()
                    .mapToInt(Integer::intValue).sum();
            int toolResultTotal = stats.toolResults().values().stream()
                    .mapToInt(Integer::intValue).sum();

            metrics.put("tool_request_percent",
                    Math.round((double) toolRequestTotal / stats.total() * 100));
            metrics.put("tool_result_percent",
                    Math.round((double) toolResultTotal / stats.total() * 100));

            // Individual tool percentages
            stats.toolRequests().forEach((tool, tokens) ->
                    metrics.put("tool_request_" + tool + "_percent",
                            Math.round((double) tokens / stats.total() * 100)));

            stats.toolResults().forEach((tool, tokens) ->
                    metrics.put("tool_result_" + tool + "_percent",
                            Math.round((double) tokens / stats.total() * 100)));
        }

        return metrics;
    }

    /**
     * Estimate tokens for a content block.
     */
    private static int estimateTokens(ContentBlock block) {
        // Simple estimation: ~4 chars per token
        String content = block.text();
        if (content == null) {
            content = block.toString();
        }
        return content.length() / 4 + 1;
    }

    /**
     * Increment a map value.
     */
    private static void increment(Map<String, Integer> map, String key, int value) {
        map.merge(key, value, Integer::sum);
    }

    /**
     * File read statistics helper.
     */
    private record FileReadStats(int count, int totalTokens) {}
}