/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/classifierDecision.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Auto mode allowlisted tools.
 *
 * Tools that are safe and don't need any classifier checking.
 * Used by the auto mode classifier to skip unnecessary API calls.
 */
public final class ClassifierDecision {
    private ClassifierDecision() {}

    /**
     * Tools that are safe and don't need classifier checking.
     * Does NOT include write/edit tools — those are handled by the
     * acceptEdits fast path (allowed in CWD, classified outside CWD).
     */
    private static final Set<String> SAFE_YOLO_ALLOWLISTED_TOOLS = Set.of(
        // Read-only file operations
        "Read",
        // Search / read-only
        "Grep",
        "Glob",
        "LSP",
        "ToolSearch",
        "ListMcpResources",
        "ReadMcpResource",
        // Task management (metadata only)
        "TodoWrite",
        "TaskCreate",
        "TaskGet",
        "TaskUpdate",
        "TaskList",
        "TaskStop",
        "TaskOutput",
        // Plan mode / UI
        "AskUserQuestion",
        "EnterPlanMode",
        "ExitPlanMode",
        // Swarm coordination
        "TeamCreate",
        "TeamDelete",
        "SendMessage",
        // Misc safe
        "Sleep",
        // Internal classifier tool
        "classify_result"
    );

    /**
     * Check if a tool is allowlisted for auto mode.
     */
    public static boolean isAutoModeAllowlistedTool(String toolName) {
        return SAFE_YOLO_ALLOWLISTED_TOOLS.contains(toolName);
    }

    /**
     * Classifier result for auto mode decisions.
     */
    public record ClassifierResult(
        boolean shouldBlock,
        String reason,
        String model,
        boolean unavailable,
        String thinking,
        ClassifierUsage usage,
        long durationMs,
        String stage,
        boolean transcriptTooLong,
        String errorDumpPath
    ) {
        public static ClassifierResult allow(String reason, String model) {
            return new ClassifierResult(false, reason, model, false, null, null, 0, null, false, null);
        }

        public static ClassifierResult block(String reason, String model) {
            return new ClassifierResult(true, reason, model, false, null, null, 0, null, false, null);
        }

        public static ClassifierResult unavailable(String reason, String model) {
            return new ClassifierResult(true, reason, model, true, null, null, 0, null, false, null);
        }
    }

    /**
     * Classifier usage statistics.
     */
    public record ClassifierUsage(
        int inputTokens,
        int outputTokens,
        int cacheReadInputTokens,
        int cacheCreationInputTokens
    ) {
        public int totalInputTokens() {
            return inputTokens + cacheReadInputTokens + cacheCreationInputTokens;
        }
    }

    /**
     * Transcript entry for classifier input.
     */
    public record TranscriptEntry(
        String role,  // "user" or "assistant"
        List<TranscriptBlock> content
    ) {}

    /**
     * Transcript block for classifier input.
     */
    public sealed interface TranscriptBlock permits
        TranscriptBlock.TextBlock,
        TranscriptBlock.ToolUseBlock {

        record TextBlock(String text) implements TranscriptBlock {}
        record ToolUseBlock(String name, Object input) implements TranscriptBlock {}
    }

    /**
     * Auto mode rules configuration.
     */
    public record AutoModeRules(
        List<String> allow,
        List<String> softDeny,
        List<String> environment
    ) {
        public static AutoModeRules empty() {
            return new AutoModeRules(List.of(), List.of(), List.of());
        }

        public static AutoModeRules of(List<String> allow, List<String> softDeny, List<String> environment) {
            return new AutoModeRules(
                allow != null ? allow : List.of(),
                softDeny != null ? softDeny : List.of(),
                environment != null ? environment : List.of()
            );
        }
    }

    /**
     * Format an action for the classifier from tool name and input.
     */
    public static TranscriptEntry formatActionForClassifier(String toolName, Object toolInput) {
        return new TranscriptEntry(
            "assistant",
            List.of(new TranscriptBlock.ToolUseBlock(toolName, toolInput))
        );
    }

    /**
     * Check if tool declares no classifier-relevant input.
     */
    public static boolean isNoClassifierRelevance(String encodedAction) {
        return encodedAction == null || encodedAction.isEmpty();
    }
}