/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code ToolUseContext type
 */
package com.anthropic.claudecode;

import com.anthropic.claudecode.message.Message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Context passed to tool implementations.
 *
 * <p>Corresponds to ToolUseContext in Tool.ts.
 */
public record ToolUseContext(
        // ==================== Options ====================
        ToolUseOptions options,

        // ==================== Core State ====================
        AbortController abortController,
        FileStateCache readFileState,
        Function<Void, Map<String, Object>> getAppState,
        Function<Map<String, Object>, Void> setAppState,

        // ==================== Tool Management ====================
        Consumer<Set<String>> setInProgressToolUseIDs,
        Consumer<Integer> setResponseLength,
        Consumer<String> setStreamMode,

        // ==================== State Updates ====================
        Function<FileHistoryState, FileHistoryState> updateFileHistoryState,
        Function<AttributionState, AttributionState> updateAttributionState,

        // ==================== Messages ====================
        List<Message> messages,

        // ==================== Limits ====================
        FileReadingLimits fileReadingLimits,
        GlobLimits globLimits,

        // ==================== Optional Fields ====================
        Map<String, ToolDecision> toolDecisions,
        QueryChainTracking queryTracking,
        BiConsumer<PromptRequest, PromptResponse> requestPrompt,
        String toolUseId,
        String criticalSystemReminderExperimental,
        boolean preserveToolUseResults,
        DenialTrackingState localDenialTracking,
        ContentReplacementState contentReplacementState,
        String renderedSystemPrompt
) {
    /**
     * Create an empty context with defaults.
     */
    public static ToolUseContext empty() {
        return new ToolUseContext(
                ToolUseOptions.empty(),
                new AbortController(),
                FileStateCache.empty(),
                v -> Map.of(),
                state -> null,
                ids -> {},
                len -> {},
                mode -> {},
                state -> state,
                state -> state,
                List.of(),
                null,
                null,
                Map.of(),
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null
        );
    }

    /**
     * Create a new context with a specific tool use ID.
     */
    public ToolUseContext withToolUseId(String newToolUseId) {
        return new ToolUseContext(
                options,
                abortController,
                readFileState,
                getAppState,
                setAppState,
                setInProgressToolUseIDs,
                setResponseLength,
                setStreamMode,
                updateFileHistoryState,
                updateAttributionState,
                messages,
                fileReadingLimits,
                globLimits,
                toolDecisions,
                queryTracking,
                requestPrompt,
                newToolUseId,
                criticalSystemReminderExperimental,
                preserveToolUseResults,
                localDenialTracking,
                contentReplacementState,
                renderedSystemPrompt
        );
    }

    /**
     * Add a tool use ID to the in-progress set.
     * Uses the setInProgressToolUseIDs consumer to update the state.
     */
    public void addInProgressToolUseId(String id) {
        if (setInProgressToolUseIDs != null && id != null) {
            // Get current set from app state, add the ID, and update
            Map<String, Object> state = getAppState != null ? getAppState.apply(null) : Map.of();
            Object currentSet = state.get("inProgressToolUseIDs");
            java.util.HashSet<String> ids = new java.util.HashSet<>();
            if (currentSet instanceof Set) {
                for (Object item : (Set<?>) currentSet) {
                    if (item instanceof String) {
                        ids.add((String) item);
                    }
                }
            }
            ids.add(id);
            setInProgressToolUseIDs.accept(ids);
        }
    }

    /**
     * Remove a tool use ID from the in-progress set.
     * Uses the setInProgressToolUseIDs consumer to update the state.
     */
    public void removeInProgressToolUseId(String id) {
        if (setInProgressToolUseIDs != null && id != null) {
            // Get current set from app state, remove the ID, and update
            Map<String, Object> state = getAppState != null ? getAppState.apply(null) : Map.of();
            Object currentSet = state.get("inProgressToolUseIDs");
            java.util.HashSet<String> ids = new java.util.HashSet<>();
            if (currentSet instanceof Set) {
                for (Object item : (Set<?>) currentSet) {
                    if (item instanceof String) {
                        ids.add((String) item);
                    }
                }
            }
            ids.remove(id);
            setInProgressToolUseIDs.accept(ids);
        }
    }

    // ==================== Nested Records ====================

    public record ToolUseOptions(
            List<Command> commands,
            boolean debug,
            String mainLoopModel,
            List<Tool<?, ?, ?>> tools,
            boolean verbose,
            ThinkingConfig thinkingConfig,
            List<McpServerConnection> mcpClients,
            Map<String, List<ServerResource>> mcpResources,
            boolean isNonInteractiveSession,
            AgentDefinitionsResult agentDefinitions,
            Double maxBudgetUsd,
            String customSystemPrompt,
            String appendSystemPrompt,
            String querySource,
            Function<Void, List<Tool<?, ?, ?>>> refreshTools
    ) {
        public static ToolUseOptions empty() {
            return new ToolUseOptions(
                    List.of(), false, "", List.of(), false,
                    null, List.of(), Map.of(), false,
                    null, null, null, null, null, null
            );
        }
    }

    public record Command(String name, String description, Runnable handler) {}

    public record ThinkingConfig(String type, int maxThinkingLength) {}

    public record McpServerConnection(String name, String status) {}

    public record ServerResource(String uri, String name, String description) {}

    public record AgentDefinitionsResult(
            List<AgentDefinition> activeAgents,
            List<AgentDefinition> allAgents
    ) {}

    public record AgentDefinition(
            String agentType,
            String whenToUse,
            String description,
            String model,
            List<String> tools,
            List<String> disallowedTools
    ) {}

    public record FileStateCache(Map<String, FileState> cache) {
        public static FileStateCache empty() { return new FileStateCache(Map.of()); }
    }

    public record FileState(String path, String content, long lastModified) {}

    public record AbortController(boolean aborted, Runnable abort) {
        public AbortController() {
            this(false, () -> {});
        }
    }

    public record FileReadingLimits(Integer maxTokens, Integer maxSizeBytes) {}

    public record GlobLimits(Integer maxResults) {}

    public record ToolDecision(String source, String decision, long timestamp) {}

    public record QueryChainTracking(String chainId, int depth) {}

    public record PromptRequest(String sourceName, String toolInputSummary) {}

    public record PromptResponse(String decision, String feedback) {}

    public record FileHistoryState(Map<String, FileSnapshot> snapshots) {}

    public record FileSnapshot(String uuid, String content, long timestamp) {}

    public record AttributionState(String author, String coAuthor) {}

    public record DenialTrackingState(int count, long lastDenialTime) {}

    public record ContentReplacementState(Map<String, ReplacementEntry> entries) {}

    public record ReplacementEntry(String key, String value, long timestamp) {}
}