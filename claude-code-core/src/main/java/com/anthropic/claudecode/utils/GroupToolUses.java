/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tool use grouping utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Groups tool uses by message.id if the tool supports grouped rendering.
 */
public final class GroupToolUses {
    private GroupToolUses() {}

    /**
     * Grouping result.
     */
    public record GroupingResult(List<RenderableMessage> messages) {}

    /**
     * Renderable message types.
     */
    public sealed interface RenderableMessage permits
            NormalizedAssistantMessage, NormalizedUserMessage, GroupedToolUseMessage {}

    /**
     * Normalized assistant message.
     */
    public record NormalizedAssistantMessage(
            String type,
            String messageId,
            String uuid,
            long timestamp,
            List<MessageContent> content
    ) implements RenderableMessage {}

    /**
     * Normalized user message.
     */
    public record NormalizedUserMessage(
            String type,
            String uuid,
            long timestamp,
            List<MessageContent> content
    ) implements RenderableMessage {}

    /**
     * Grouped tool use message.
     */
    public record GroupedToolUseMessage(
            String type,
            String toolName,
            List<NormalizedAssistantMessage> messages,
            List<NormalizedUserMessage> results,
            NormalizedAssistantMessage displayMessage,
            String uuid,
            long timestamp,
            String messageId
    ) implements RenderableMessage {}

    /**
     * Message content.
     */
    public sealed interface MessageContent permits ToolUseContent, ToolResultContent, TextContent {}

    /**
     * Tool use content.
     */
    public record ToolUseContent(String type, String id, String name, Object input) implements MessageContent {}

    /**
     * Tool result content.
     */
    public record ToolResultContent(String type, String tool_use_id, String content) implements MessageContent {}

    /**
     * Text content.
     */
    public record TextContent(String type, String text) implements MessageContent {}

    /**
     * Apply grouping to messages.
     */
    public static GroupingResult applyGrouping(
            List<? extends RenderableMessage> messages,
            Set<String> toolsWithGrouping,
            boolean verbose
    ) {
        // In verbose mode, don't group
        if (verbose) {
            return new GroupingResult(new ArrayList<>(messages));
        }

        // First pass: group tool uses by message.id + tool name
        Map<String, List<NormalizedAssistantMessage>> groups = new LinkedHashMap<>();

        for (RenderableMessage msg : messages) {
            ToolUseInfo info = getToolUseInfo(msg);
            if (info != null && toolsWithGrouping.contains(info.toolName)) {
                String key = info.messageId + ":" + info.toolName;
                groups.computeIfAbsent(key, k -> new ArrayList<>())
                        .add((NormalizedAssistantMessage) msg);
            }
        }

        // Identify valid groups (2+ items)
        Map<String, List<NormalizedAssistantMessage>> validGroups = new LinkedHashMap<>();
        Set<String> groupedToolUseIds = new HashSet<>();

        for (Map.Entry<String, List<NormalizedAssistantMessage>> entry : groups.entrySet()) {
            if (entry.getValue().size() >= 2) {
                validGroups.put(entry.getKey(), entry.getValue());
                for (NormalizedAssistantMessage msg : entry.getValue()) {
                    ToolUseInfo info = getToolUseInfo(msg);
                    if (info != null) {
                        groupedToolUseIds.add(info.toolUseId);
                    }
                }
            }
        }

        // Collect result messages for grouped tool_uses
        Map<String, NormalizedUserMessage> resultsByToolUseId = new LinkedHashMap<>();

        for (RenderableMessage msg : messages) {
            if (msg instanceof NormalizedUserMessage userMsg) {
                for (MessageContent content : userMsg.content()) {
                    if (content instanceof ToolResultContent trc) {
                        if (groupedToolUseIds.contains(trc.tool_use_id())) {
                            resultsByToolUseId.put(trc.tool_use_id(), userMsg);
                        }
                    }
                }
            }
        }

        // Second pass: build output, emitting each group only once
        List<RenderableMessage> result = new ArrayList<>();
        Set<String> emittedGroups = new HashSet<>();

        for (RenderableMessage msg : messages) {
            ToolUseInfo info = getToolUseInfo(msg);

            if (info != null) {
                String key = info.messageId + ":" + info.toolName;
                List<NormalizedAssistantMessage> group = validGroups.get(key);

                if (group != null) {
                    if (!emittedGroups.contains(key)) {
                        emittedGroups.add(key);
                        NormalizedAssistantMessage firstMsg = group.get(0);

                        // Collect results for this group
                        List<NormalizedUserMessage> results = new ArrayList<>();
                        for (NormalizedAssistantMessage assistantMsg : group) {
                            ToolUseContent toolUse = (ToolUseContent) assistantMsg.content().get(0);
                            NormalizedUserMessage resultMsg = resultsByToolUseId.get(toolUse.id());
                            if (resultMsg != null) {
                                results.add(resultMsg);
                            }
                        }

                        GroupedToolUseMessage groupedMessage = new GroupedToolUseMessage(
                                "grouped_tool_use",
                                info.toolName,
                                group,
                                results,
                                firstMsg,
                                "grouped-" + firstMsg.uuid(),
                                firstMsg.timestamp(),
                                info.messageId
                        );
                        result.add(groupedMessage);
                    }
                    continue;
                }
            }

            // Skip user messages whose tool_results are all grouped
            if (msg instanceof NormalizedUserMessage userMsg) {
                List<ToolResultContent> toolResults = userMsg.content().stream()
                        .filter(c -> c instanceof ToolResultContent)
                        .map(c -> (ToolResultContent) c)
                        .toList();

                if (!toolResults.isEmpty()) {
                    boolean allGrouped = toolResults.stream()
                            .allMatch(tr -> groupedToolUseIds.contains(tr.tool_use_id()));
                    if (allGrouped) {
                        continue;
                    }
                }
            }

            result.add(msg);
        }

        return new GroupingResult(result);
    }

    /**
     * Get tool use info from a message.
     */
    private static ToolUseInfo getToolUseInfo(RenderableMessage msg) {
        if (msg instanceof NormalizedAssistantMessage assistantMsg) {
            if (!assistantMsg.content().isEmpty() &&
                assistantMsg.content().get(0) instanceof ToolUseContent toolUse) {
                return new ToolUseInfo(
                        assistantMsg.messageId(),
                        toolUse.id(),
                        toolUse.name()
                );
            }
        }
        return null;
    }

    /**
     * Tool use info.
     */
    private record ToolUseInfo(String messageId, String toolUseId, String toolName) {}
}