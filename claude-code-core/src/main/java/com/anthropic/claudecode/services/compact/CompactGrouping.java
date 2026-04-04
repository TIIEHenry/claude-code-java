/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/compact/grouping
 */
package com.anthropic.claudecode.services.compact;

import java.util.*;
import java.util.regex.*;

/**
 * Compact grouping - Message grouping for compaction.
 */
public final class CompactGrouping {

    /**
     * Message group record.
     */
    public record MessageGroup(
        String id,
        GroupType type,
        String topic,
        List<GroupedMessage> messages,
        int totalTokens,
        int startIndex,
        int endIndex,
        double cohesion
    ) {
        public int getMessageCount() {
            return messages.size();
        }

        public boolean canCompact() {
            return messages.size() > 1 && cohesion > 0.5;
        }
    }

    /**
     * Group type enum.
     */
    public enum GroupType {
        TOPIC,
        TOOL_CHAIN,
        CONVERSATION_TURN,
        CODE_BLOCK,
        ERROR_RESOLUTION,
        QUESTION_ANSWER
    }

    /**
     * Grouped message record.
     */
    public record GroupedMessage(
        String id,
        String content,
        String role,
        int tokenCount,
        int originalIndex,
        Map<String, Object> metadata
    ) {}

    /**
     * Group messages by topic.
     */
    public List<MessageGroup> groupByTopic(List<GroupedMessage> messages) {
        List<MessageGroup> groups = new ArrayList<>();
        List<GroupedMessage> currentGroup = new ArrayList<>();
        String currentTopic = null;
        int groupStart = 0;

        for (int i = 0; i < messages.size(); i++) {
            GroupedMessage msg = messages.get(i);
            String topic = detectTopic(msg.content());

            if (currentTopic == null) {
                currentTopic = topic;
            }

            if (isSameTopic(currentTopic, topic)) {
                currentGroup.add(msg);
            } else {
                // Save current group
                if (!currentGroup.isEmpty()) {
                    groups.add(createGroup(currentGroup, GroupType.TOPIC, currentTopic, groupStart, i - 1));
                }

                // Start new group
                currentGroup = new ArrayList<>();
                currentGroup.add(msg);
                currentTopic = topic;
                groupStart = i;
            }
        }

        // Save final group
        if (!currentGroup.isEmpty()) {
            groups.add(createGroup(currentGroup, GroupType.TOPIC, currentTopic, groupStart, messages.size() - 1));
        }

        return groups;
    }

    /**
     * Group messages by tool chain.
     */
    public List<MessageGroup> groupByToolChain(List<GroupedMessage> messages) {
        List<MessageGroup> groups = new ArrayList<>();
        List<GroupedMessage> currentChain = new ArrayList<>();
        boolean inToolChain = false;
        int chainStart = 0;

        for (int i = 0; i < messages.size(); i++) {
            GroupedMessage msg = messages.get(i);
            boolean isToolMessage = isToolRelated(msg);

            if (isToolMessage && !inToolChain) {
                // Start new chain
                inToolChain = true;
                currentChain = new ArrayList<>();
                chainStart = i;
            }

            if (inToolChain) {
                if (isToolMessage) {
                    currentChain.add(msg);
                } else {
                    // End chain
                    if (currentChain.size() > 1) {
                        groups.add(createGroup(currentChain, GroupType.TOOL_CHAIN, "Tool Chain", chainStart, i - 1));
                    }
                    inToolChain = false;
                    currentChain = new ArrayList<>();
                }
            }
        }

        // Save final chain
        if (currentChain.size() > 1) {
            groups.add(createGroup(currentChain, GroupType.TOOL_CHAIN, "Tool Chain", chainStart, messages.size() - 1));
        }

        return groups;
    }

    /**
     * Detect topic from content.
     */
    private String detectTopic(String content) {
        if (content == null || content.isEmpty()) return "unknown";

        String lower = content.toLowerCase();

        // Check for code topics
        if (lower.contains("function") || lower.contains("class") || lower.contains("method")) {
            return "code";
        }
        if (lower.contains("error") || lower.contains("exception") || lower.contains("bug")) {
            return "error";
        }
        if (lower.contains("test") || lower.contains("spec")) {
            return "testing";
        }
        if (lower.contains("api") || lower.contains("endpoint") || lower.contains("request")) {
            return "api";
        }
        if (lower.contains("git") || lower.contains("commit") || lower.contains("branch")) {
            return "git";
        }
        if (lower.contains("file") || lower.contains("directory") || lower.contains("path")) {
            return "files";
        }

        return "general";
    }

    /**
     * Check if same topic.
     */
    private boolean isSameTopic(String topic1, String topic2) {
        if (topic1 == null || topic2 == null) return false;
        return topic1.equals(topic2);
    }

    /**
     * Check if tool related.
     */
    private boolean isToolRelated(GroupedMessage msg) {
        if (msg.metadata() == null) return false;

        return msg.metadata().containsKey("toolName") ||
               msg.metadata().containsKey("toolCall") ||
               msg.role().equals("tool");
    }

    /**
     * Create group from messages.
     */
    private MessageGroup createGroup(
        List<GroupedMessage> messages,
        GroupType type,
        String topic,
        int start,
        int end
    ) {
        int tokens = messages.stream().mapToInt(GroupedMessage::tokenCount).sum();
        double cohesion = calculateCohesion(messages);

        return new MessageGroup(
            UUID.randomUUID().toString(),
            type,
            topic,
            messages,
            tokens,
            start,
            end,
            cohesion
        );
    }

    /**
     * Calculate group cohesion.
     */
    private double calculateCohesion(List<GroupedMessage> messages) {
        if (messages.size() < 2) return 1.0;

        // Calculate based on content similarity
        double totalSimilarity = 0;
        int comparisons = 0;

        for (int i = 0; i < messages.size() - 1; i++) {
            for (int j = i + 1; j < messages.size(); j++) {
                totalSimilarity += calculateSimilarity(
                    messages.get(i).content(),
                    messages.get(j).content()
                );
                comparisons++;
            }
        }

        return comparisons > 0 ? totalSimilarity / comparisons : 0;
    }

    /**
     * Calculate similarity between texts.
     */
    private double calculateSimilarity(String a, String b) {
        if (a == null || b == null) return 0;

        Set<String> wordsA = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\s+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);

        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    /**
     * Grouping options record.
     */
    public record GroupingOptions(
        int minGroupSize,
        int maxGroupSize,
        double minCohesion,
        List<GroupType> enabledTypes,
        boolean mergeAdjacent
    ) {
        public static GroupingOptions defaults() {
            return new GroupingOptions(
                2,
                20,
                0.3,
                Arrays.asList(GroupType.values()),
                true
            );
        }
    }

    /**
     * Grouping result record.
     */
    public record GroupingResult(
        List<MessageGroup> groups,
        int totalMessages,
        int totalGroups,
        int ungroupedMessages,
        double avgCohesion
    ) {
        public String format() {
            return String.format(
                "%d messages in %d groups (avg cohesion: %.2f), %d ungrouped",
                totalMessages, totalGroups, avgCohesion, ungroupedMessages
            );
        }
    }
}