/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/sessionMemoryCompact
 */
package com.anthropic.claudecode.services.compact;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Session memory compact - Session memory-based compaction.
 *
 * EXPERIMENT: Session memory compaction
 * Uses session memory for compaction instead of traditional compaction.
 */
public final class SessionMemoryCompact {
    private static final SessionMemoryCompactConfig DEFAULT_CONFIG = new SessionMemoryCompactConfig(
        10_000, 5, 40_000
    );

    private static volatile SessionMemoryCompactConfig config = DEFAULT_CONFIG;
    private static volatile boolean configInitialized = false;

    /**
     * Session memory compact config record.
     */
    public record SessionMemoryCompactConfig(
        int minTokens,
        int minTextBlockMessages,
        int maxTokens
    ) {
        public static SessionMemoryCompactConfig defaults() {
            return DEFAULT_CONFIG;
        }

        public boolean isValid() {
            return minTokens > 0 && minTextBlockMessages > 0 && maxTokens > minTokens;
        }
    }

    /**
     * Compaction result record.
     */
    public record CompactionResult(
        Object boundaryMarker,
        List<Object> summaryMessages,
        List<Object> attachments,
        List<Object> hookResults,
        List<Object> messagesToKeep,
        int preCompactTokenCount,
        int postCompactTokenCount,
        int truePostCompactTokenCount
    ) {
        public static CompactionResult empty() {
            return new CompactionResult(
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                0,
                0
            );
        }

        public int getTokensSaved() {
            return preCompactTokenCount - postCompactTokenCount;
        }

        public double getCompressionRatio() {
            if (preCompactTokenCount == 0) return 1.0;
            return (double) postCompactTokenCount / preCompactTokenCount;
        }
    }

    /**
     * Message record for compaction.
     */
    public record CompactMessage(
        String uuid,
        String type,
        Object content,
        String id,
        Map<String, Object> metadata
    ) {
        public boolean isAssistant() {
            return "assistant".equals(type);
        }

        public boolean isUser() {
            return "user".equals(type);
        }
    }

    /**
     * Set session memory compact config.
     */
    public static void setConfig(SessionMemoryCompactConfig newConfig) {
        config = newConfig;
    }

    /**
     * Get session memory compact config.
     */
    public static SessionMemoryCompactConfig getConfig() {
        return config;
    }

    /**
     * Reset config.
     */
    public static void resetConfig() {
        config = DEFAULT_CONFIG;
        configInitialized = false;
    }

    /**
     * Initialize config from remote.
     */
    public CompletableFuture<Void> initConfigFromRemote() {
        return CompletableFuture.runAsync(() -> {
            if (configInitialized) {
                return;
            }

            // Check environment variables for config overrides
            String minTokensEnv = System.getenv("CLAUDE_CODE_SM_COMPACT_MIN_TOKENS");
            String minTextBlockEnv = System.getenv("CLAUDE_CODE_SM_COMPACT_MIN_TEXT_BLOCK_MESSAGES");
            String maxTokensEnv = System.getenv("CLAUDE_CODE_SM_COMPACT_MAX_TOKENS");

            if (minTokensEnv != null || minTextBlockEnv != null || maxTokensEnv != null) {
                try {
                    int minTokens = minTokensEnv != null ? Integer.parseInt(minTokensEnv) : DEFAULT_CONFIG.minTokens();
                    int minTextBlocks = minTextBlockEnv != null ? Integer.parseInt(minTextBlockEnv) : DEFAULT_CONFIG.minTextBlockMessages();
                    int maxTokens = maxTokensEnv != null ? Integer.parseInt(maxTokensEnv) : DEFAULT_CONFIG.maxTokens();

                    SessionMemoryCompactConfig envConfig = new SessionMemoryCompactConfig(
                        minTokens, minTextBlocks, maxTokens
                    );

                    if (envConfig.isValid()) {
                        config = envConfig;
                    }
                } catch (NumberFormatException e) {
                    // Keep default config on parse error
                }
            }

            // Try to load config from file
            try {
                java.nio.file.Path configPath = java.nio.file.Paths.get(
                    System.getProperty("user.home"),
                    ".claude",
                    "sm-compact-config.json"
                );

                if (java.nio.file.Files.exists(configPath)) {
                    String content = java.nio.file.Files.readString(configPath);
                    SessionMemoryCompactConfig fileConfig = parseConfigJson(content);
                    if (fileConfig != null && fileConfig.isValid()) {
                        config = fileConfig;
                    }
                }
            } catch (Exception e) {
                // Keep current config on error
            }

            configInitialized = true;
        });
    }

    /**
     * Parse config JSON.
     */
    private SessionMemoryCompactConfig parseConfigJson(String json) {
        try {
            int minTokens = DEFAULT_CONFIG.minTokens();
            int minTextBlocks = DEFAULT_CONFIG.minTextBlockMessages();
            int maxTokens = DEFAULT_CONFIG.maxTokens();

            // Simple JSON field extraction
            int minTokensIdx = json.indexOf("\"minTokens\"");
            if (minTokensIdx >= 0) {
                int valStart = json.indexOf(":", minTokensIdx) + 1;
                while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
                int valEnd = valStart;
                while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
                minTokens = Integer.parseInt(json.substring(valStart, valEnd));
            }

            int minTextIdx = json.indexOf("\"minTextBlockMessages\"");
            if (minTextIdx >= 0) {
                int valStart = json.indexOf(":", minTextIdx) + 1;
                while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
                int valEnd = valStart;
                while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
                minTextBlocks = Integer.parseInt(json.substring(valStart, valEnd));
            }

            int maxTokensIdx = json.indexOf("\"maxTokens\"");
            if (maxTokensIdx >= 0) {
                int valStart = json.indexOf(":", maxTokensIdx) + 1;
                while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
                int valEnd = valStart;
                while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
                maxTokens = Integer.parseInt(json.substring(valStart, valEnd));
            }

            return new SessionMemoryCompactConfig(minTokens, minTextBlocks, maxTokens);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if message has text blocks.
     */
    public boolean hasTextBlocks(CompactMessage message) {
        if (message.isAssistant()) {
            Object content = message.content();
            if (content instanceof List<?> list) {
                return list.stream().anyMatch(block -> isTextBlock(block));
            }
        }
        if (message.isUser()) {
            Object content = message.content();
            if (content instanceof String s) {
                return s.length() > 0;
            }
            if (content instanceof List<?> list) {
                return list.stream().anyMatch(block -> isTextBlock(block));
            }
        }
        return false;
    }

    /**
     * Check if block is text.
     */
    private boolean isTextBlock(Object block) {
        if (block instanceof Map<?, ?> map) {
            return "text".equals(map.get("type"));
        }
        return false;
    }

    /**
     * Get tool result IDs from message.
     */
    public List<String> getToolResultIds(CompactMessage message) {
        if (!message.isUser()) {
            return Collections.emptyList();
        }

        Object content = message.content();
        if (!(content instanceof List<?> list)) {
            return Collections.emptyList();
        }

        List<String> ids = new ArrayList<>();
        for (Object block : list) {
            if (block instanceof Map<?, ?> map) {
                if ("tool_result".equals(map.get("type"))) {
                    Object id = map.get("tool_use_id");
                    if (id != null) {
                        ids.add(id.toString());
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Check if message has tool use with IDs.
     */
    public boolean hasToolUseWithIds(CompactMessage message, Set<String> toolUseIds) {
        if (!message.isAssistant()) {
            return false;
        }

        Object content = message.content();
        if (!(content instanceof List<?> list)) {
            return false;
        }

        for (Object block : list) {
            if (block instanceof Map<?, ?> map) {
                if ("tool_use".equals(map.get("type"))) {
                    Object id = map.get("id");
                    if (id != null && toolUseIds.contains(id.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Adjust index to preserve API invariants.
     */
    public int adjustIndexToPreserveAPIInvariants(List<CompactMessage> messages, int startIndex) {
        if (startIndex <= 0 || startIndex >= messages.size()) {
            return startIndex;
        }

        int adjustedIndex = startIndex;

        // Step 1: Handle tool_use/tool_result pairs
        List<String> allToolResultIds = new ArrayList<>();
        for (int i = startIndex; i < messages.size(); i++) {
            allToolResultIds.addAll(getToolResultIds(messages.get(i)));
        }

        if (!allToolResultIds.isEmpty()) {
            Set<String> toolUseIdsInKeptRange = new HashSet<>();
            for (int i = adjustedIndex; i < messages.size(); i++) {
                CompactMessage msg = messages.get(i);
                if (msg.isAssistant()) {
                    Object content = msg.content();
                    if (content instanceof List<?> list) {
                        for (Object block : list) {
                            if (block instanceof Map<?, ?> map) {
                                if ("tool_use".equals(map.get("type"))) {
                                    Object id = map.get("id");
                                    if (id != null) {
                                        toolUseIdsInKeptRange.add(id.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Set<String> neededToolUseIds = new HashSet<>();
            for (String id : allToolResultIds) {
                if (!toolUseIdsInKeptRange.contains(id)) {
                    neededToolUseIds.add(id);
                }
            }

            for (int i = adjustedIndex - 1; i >= 0 && !neededToolUseIds.isEmpty(); i--) {
                CompactMessage message = messages.get(i);
                if (hasToolUseWithIds(message, neededToolUseIds)) {
                    adjustedIndex = i;

                    // Remove found IDs
                    if (message.isAssistant()) {
                        Object content = message.content();
                        if (content instanceof List<?> list) {
                            for (Object block : list) {
                                if (block instanceof Map<?, ?> map) {
                                    if ("tool_use".equals(map.get("type"))) {
                                        Object id = map.get("id");
                                        if (id != null && neededToolUseIds.contains(id.toString())) {
                                            neededToolUseIds.remove(id.toString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 2: Handle thinking blocks
        Set<String> messageIdsInKeptRange = new HashSet<>();
        for (int i = adjustedIndex; i < messages.size(); i++) {
            CompactMessage msg = messages.get(i);
            if (msg.isAssistant() && msg.id() != null) {
                messageIdsInKeptRange.add(msg.id());
            }
        }

        for (int i = adjustedIndex - 1; i >= 0; i--) {
            CompactMessage message = messages.get(i);
            if (message.isAssistant() && message.id() != null &&
                messageIdsInKeptRange.contains(message.id())) {
                adjustedIndex = i;
            }
        }

        return adjustedIndex;
    }

    /**
     * Calculate messages to keep index.
     */
    public int calculateMessagesToKeepIndex(List<CompactMessage> messages, int lastSummarizedIndex) {
        if (messages.isEmpty()) {
            return 0;
        }

        SessionMemoryCompactConfig cfg = getConfig();

        int startIndex = lastSummarizedIndex >= 0
            ? lastSummarizedIndex + 1
            : messages.size();

        int totalTokens = 0;
        int textBlockMessageCount = 0;
        for (int i = startIndex; i < messages.size(); i++) {
            CompactMessage msg = messages.get(i);
            totalTokens += estimateMessageTokens(msg);
            if (hasTextBlocks(msg)) {
                textBlockMessageCount++;
            }
        }

        // Check max cap
        if (totalTokens >= cfg.maxTokens()) {
            return adjustIndexToPreserveAPIInvariants(messages, startIndex);
        }

        // Check minimums
        if (totalTokens >= cfg.minTokens() && textBlockMessageCount >= cfg.minTextBlockMessages()) {
            return adjustIndexToPreserveAPIInvariants(messages, startIndex);
        }

        // Expand backwards
        int floor = 0;
        for (int i = startIndex - 1; i >= floor; i--) {
            CompactMessage msg = messages.get(i);
            int msgTokens = estimateMessageTokens(msg);
            totalTokens += msgTokens;
            if (hasTextBlocks(msg)) {
                textBlockMessageCount++;
            }
            startIndex = i;

            if (totalTokens >= cfg.maxTokens()) {
                break;
            }
            if (totalTokens >= cfg.minTokens() && textBlockMessageCount >= cfg.minTextBlockMessages()) {
                break;
            }
        }

        return adjustIndexToPreserveAPIInvariants(messages, startIndex);
    }

    /**
     * Estimate message tokens.
     */
    private int estimateMessageTokens(CompactMessage message) {
        // Simple estimation
        Object content = message.content();
        if (content instanceof String s) {
            return s.length() / 4; // Approx 4 chars per token
        }
        if (content instanceof List<?> list) {
            int total = 0;
            for (Object block : list) {
                if (block instanceof Map<?, ?> map) {
                    Object text = map.get("text");
                    if (text instanceof String t) {
                        total += t.length() / 4;
                    }
                }
            }
            return total;
        }
        return 100; // Default estimate
    }

    /**
     * Should use session memory compaction.
     */
    public boolean shouldUseSessionMemoryCompaction() {
        // Check env var override
        String enableEnv = System.getenv("ENABLE_CLAUDE_CODE_SM_COMPACT");
        String disableEnv = System.getenv("DISABLE_CLAUDE_CODE_SM_COMPACT");

        if ("true".equalsIgnoreCase(enableEnv)) {
            return true;
        }
        if ("true".equalsIgnoreCase(disableEnv)) {
            return false;
        }

        // Check feature flags (would be from GrowthBook)
        return false;
    }

    /**
     * Try session memory compaction.
     */
    public CompletableFuture<CompactionResult> trySessionMemoryCompaction(
        List<CompactMessage> messages,
        String agentId,
        int autoCompactThreshold
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (!shouldUseSessionMemoryCompaction()) {
                return null;
            }

            // Initialize config
            initConfigFromRemote().join();

            // Wait for session memory extraction
            String sessionMemory = getSessionMemoryContent();
            if (sessionMemory == null || sessionMemory.isEmpty()) {
                return null;
            }

            String lastSummarizedMessageId = getLastSummarizedMessageId();
            int lastSummarizedIndex = -1;

            if (lastSummarizedMessageId != null) {
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).uuid().equals(lastSummarizedMessageId)) {
                        lastSummarizedIndex = i;
                        break;
                    }
                }

                if (lastSummarizedIndex == -1) {
                    return null;
                }
            } else {
                lastSummarizedIndex = messages.size() - 1;
            }

            int startIndex = calculateMessagesToKeepIndex(messages, lastSummarizedIndex);
            List<CompactMessage> messagesToKeep = messages.subList(startIndex, messages.size());

            // Filter out compact boundary messages
            messagesToKeep = messagesToKeep.stream()
                .filter(m -> !isCompactBoundaryMessage(m))
                .collect(Collectors.toList());

            int postCompactTokenCount = estimateTotalTokens(messagesToKeep);

            if (autoCompactThreshold > 0 && postCompactTokenCount >= autoCompactThreshold) {
                return null;
            }

            return new CompactionResult(
                createBoundaryMarker(messages),
                createSummaryMessages(sessionMemory),
                Collections.emptyList(),
                Collections.emptyList(),
                new ArrayList<>(messagesToKeep),
                estimateTotalTokens(messages),
                postCompactTokenCount,
                postCompactTokenCount
            );
        });
    }

    /**
     * Get session memory content.
     */
    private String getSessionMemoryContent() {
        // Implementation would read session memory file
        return null;
    }

    /**
     * Get last summarized message ID.
     */
    private String getLastSummarizedMessageId() {
        // Implementation would get from session memory utils
        return null;
    }

    /**
     * Check if compact boundary message.
     */
    private boolean isCompactBoundaryMessage(CompactMessage message) {
        Object content = message.content();
        if (content instanceof Map<?, ?> map) {
            return "compact_boundary".equals(map.get("type"));
        }
        return false;
    }

    /**
     * Create boundary marker.
     */
    private Object createBoundaryMarker(List<CompactMessage> messages) {
        return new CompactBoundaryMarker(
            "auto",
            estimateTotalTokens(messages),
            messages.isEmpty() ? null : messages.get(messages.size() - 1).uuid()
        );
    }

    /**
     * Create summary messages.
     */
    private List<Object> createSummaryMessages(String sessionMemory) {
        return List.of(new CompactSummaryMessage(sessionMemory, true));
    }

    /**
     * Estimate total tokens.
     */
    private int estimateTotalTokens(List<CompactMessage> messages) {
        return messages.stream()
            .mapToInt(this::estimateMessageTokens)
            .sum();
    }

    /**
     * Compact boundary marker record.
     */
    public record CompactBoundaryMarker(String type, int tokenCount, String lastUuid) {}

    /**
     * Compact summary message record.
     */
    public record CompactSummaryMessage(String content, boolean isCompactSummary) {}
}