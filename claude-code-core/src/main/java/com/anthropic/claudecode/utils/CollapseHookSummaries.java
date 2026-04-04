/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hook summary collapsing
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Collapses consecutive hook summary messages with the same hookLabel
 * into a single summary. This happens when parallel tool calls each
 * emit their own hook summary.
 */
public final class CollapseHookSummaries {
    private CollapseHookSummaries() {}

    /**
     * Collapse hook summaries with the same label.
     *
     * @param messages The messages to collapse
     * @return Collapsed messages
     */
    public static List<HookSummaryMessage> collapseHookSummaries(
            List<HookSummaryMessage> messages) {

        List<HookSummaryMessage> result = new ArrayList<>();
        int i = 0;

        while (i < messages.size()) {
            HookSummaryMessage msg = messages.get(i);
            if (isLabeledHookSummary(msg)) {
                String label = msg.hookLabel();
                List<HookSummaryMessage> group = new ArrayList<>();

                while (i < messages.size()) {
                    HookSummaryMessage next = messages.get(i);
                    if (!isLabeledHookSummary(next) ||
                            !label.equals(next.hookLabel())) {
                        break;
                    }
                    group.add(next);
                    i++;
                }

                if (group.size() == 1) {
                    result.add(msg);
                } else {
                    result.add(mergeGroup(group));
                }
            } else {
                result.add(msg);
                i++;
            }
        }

        return result;
    }

    /**
     * Check if message is a labeled hook summary.
     */
    private static boolean isLabeledHookSummary(HookSummaryMessage msg) {
        return msg.type() == HookSummaryMessage.MessageType.SYSTEM &&
                msg.subtype() == HookSummaryMessage.SystemSubtype.STOP_HOOK_SUMMARY &&
                msg.hookLabel() != null;
    }

    /**
     * Merge a group of hook summaries.
     */
    private static HookSummaryMessage mergeGroup(List<HookSummaryMessage> group) {
        int totalHookCount = 0;
        List<HookInfo> allHookInfos = new ArrayList<>();
        List<HookError> allHookErrors = new ArrayList<>();
        boolean preventedContinuation = false;
        boolean hasOutput = false;
        long maxDurationMs = 0;

        for (HookSummaryMessage msg : group) {
            totalHookCount += msg.hookCount();
            if (msg.hookInfos() != null) allHookInfos.addAll(msg.hookInfos());
            if (msg.hookErrors() != null) allHookErrors.addAll(msg.hookErrors());
            preventedContinuation = preventedContinuation || msg.preventedContinuation();
            hasOutput = hasOutput || msg.hasOutput();
            if (msg.totalDurationMs() != null) {
                maxDurationMs = Math.max(maxDurationMs, msg.totalDurationMs());
            }
        }

        // Use first message as template
        HookSummaryMessage first = group.get(0);
        return new HookSummaryMessage(
                first.type(),
                first.subtype(),
                first.hookLabel(),
                totalHookCount,
                allHookInfos,
                allHookErrors,
                preventedContinuation,
                hasOutput,
                maxDurationMs
        );
    }

    /**
     * Hook summary message representation.
     */
    public record HookSummaryMessage(
            MessageType type,
            SystemSubtype subtype,
            String hookLabel,
            int hookCount,
            List<HookInfo> hookInfos,
            List<HookError> hookErrors,
            boolean preventedContinuation,
            boolean hasOutput,
            Long totalDurationMs
    ) {
        public enum MessageType { USER, ASSISTANT, SYSTEM, PROGRESS }
        public enum SystemSubtype { STOP_HOOK_SUMMARY, START_HOOK, OTHER }
    }

    /**
     * Hook info record.
     */
    public record HookInfo(
            String hookName,
            String hookPath,
            long durationMs,
            boolean success
    ) {}

    /**
     * Hook error record.
     */
    public record HookError(
            String hookName,
            String error,
            int exitCode
    ) {}
}