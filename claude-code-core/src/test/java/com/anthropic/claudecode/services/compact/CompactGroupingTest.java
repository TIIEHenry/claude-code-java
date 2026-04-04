/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.compact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CompactGrouping.
 */
class CompactGroupingTest {

    @Test
    @DisplayName("CompactGrouping GroupType enum values")
    void groupTypeEnum() {
        CompactGrouping.GroupType[] types = CompactGrouping.GroupType.values();
        assertEquals(6, types.length);
        assertEquals(CompactGrouping.GroupType.TOPIC, CompactGrouping.GroupType.valueOf("TOPIC"));
        assertEquals(CompactGrouping.GroupType.TOOL_CHAIN, CompactGrouping.GroupType.valueOf("TOOL_CHAIN"));
        assertEquals(CompactGrouping.GroupType.CONVERSATION_TURN, CompactGrouping.GroupType.valueOf("CONVERSATION_TURN"));
        assertEquals(CompactGrouping.GroupType.CODE_BLOCK, CompactGrouping.GroupType.valueOf("CODE_BLOCK"));
        assertEquals(CompactGrouping.GroupType.ERROR_RESOLUTION, CompactGrouping.GroupType.valueOf("ERROR_RESOLUTION"));
        assertEquals(CompactGrouping.GroupType.QUESTION_ANSWER, CompactGrouping.GroupType.valueOf("QUESTION_ANSWER"));
    }

    @Test
    @DisplayName("CompactGrouping GroupedMessage record")
    void groupedMessageRecord() {
        CompactGrouping.GroupedMessage msg = new CompactGrouping.GroupedMessage(
            "msg-1", "Hello world", "user", 10, 0, Map.of("key", "value")
        );

        assertEquals("msg-1", msg.id());
        assertEquals("Hello world", msg.content());
        assertEquals("user", msg.role());
        assertEquals(10, msg.tokenCount());
        assertEquals(0, msg.originalIndex());
        assertEquals("value", msg.metadata().get("key"));
    }

    @Test
    @DisplayName("CompactGrouping MessageGroup record")
    void messageGroupRecord() {
        CompactGrouping.GroupedMessage msg1 = new CompactGrouping.GroupedMessage("1", "content1", "user", 5, 0, Map.of());
        CompactGrouping.GroupedMessage msg2 = new CompactGrouping.GroupedMessage("2", "content2", "assistant", 5, 1, Map.of());

        CompactGrouping.MessageGroup group = new CompactGrouping.MessageGroup(
            "group-1",
            CompactGrouping.GroupType.TOPIC,
            "code",
            List.of(msg1, msg2),
            10,
            0,
            1,
            0.8
        );

        assertEquals("group-1", group.id());
        assertEquals(CompactGrouping.GroupType.TOPIC, group.type());
        assertEquals("code", group.topic());
        assertEquals(2, group.messages().size());
        assertEquals(10, group.totalTokens());
        assertEquals(0, group.startIndex());
        assertEquals(1, group.endIndex());
        assertEquals(0.8, group.cohesion());
        assertEquals(2, group.getMessageCount());
    }

    @Test
    @DisplayName("CompactGrouping MessageGroup canCompact")
    void messageGroupCanCompact() {
        CompactGrouping.GroupedMessage msg = new CompactGrouping.GroupedMessage("1", "c", "user", 1, 0, Map.of());

        // Single message - cannot compact
        CompactGrouping.MessageGroup single = new CompactGrouping.MessageGroup(
            "g1", CompactGrouping.GroupType.TOPIC, "t", List.of(msg), 1, 0, 0, 0.8
        );
        assertFalse(single.canCompact());

        // Multiple messages, low cohesion
        CompactGrouping.GroupedMessage msg2 = new CompactGrouping.GroupedMessage("2", "d", "user", 1, 1, Map.of());
        CompactGrouping.MessageGroup lowCohesion = new CompactGrouping.MessageGroup(
            "g2", CompactGrouping.GroupType.TOPIC, "t", List.of(msg, msg2), 2, 0, 1, 0.4
        );
        assertFalse(lowCohesion.canCompact());

        // Multiple messages, high cohesion
        CompactGrouping.MessageGroup highCohesion = new CompactGrouping.MessageGroup(
            "g3", CompactGrouping.GroupType.TOPIC, "t", List.of(msg, msg2), 2, 0, 1, 0.8
        );
        assertTrue(highCohesion.canCompact());
    }

    @Test
    @DisplayName("CompactGrouping groupByTopic groups messages")
    void groupByTopic() {
        CompactGrouping grouping = new CompactGrouping();
        CompactGrouping.GroupedMessage msg1 = new CompactGrouping.GroupedMessage(
            "1", "This is a function for the class", "user", 10, 0, Map.of()
        );
        CompactGrouping.GroupedMessage msg2 = new CompactGrouping.GroupedMessage(
            "2", "Here's a method implementation", "assistant", 10, 1, Map.of()
        );
        CompactGrouping.GroupedMessage msg3 = new CompactGrouping.GroupedMessage(
            "3", "There was an error in the code", "user", 10, 2, Map.of()
        );

        List<CompactGrouping.MessageGroup> groups = grouping.groupByTopic(List.of(msg1, msg2, msg3));

        assertNotNull(groups);
        assertFalse(groups.isEmpty());
    }

    @Test
    @DisplayName("CompactGrouping groupByToolChain groups tool messages")
    void groupByToolChain() {
        CompactGrouping grouping = new CompactGrouping();
        CompactGrouping.GroupedMessage msg1 = new CompactGrouping.GroupedMessage(
            "1", "Tool output 1", "tool", 5, 0, Map.of("toolName", "Read")
        );
        CompactGrouping.GroupedMessage msg2 = new CompactGrouping.GroupedMessage(
            "2", "Tool output 2", "tool", 5, 1, Map.of("toolName", "Edit")
        );
        CompactGrouping.GroupedMessage msg3 = new CompactGrouping.GroupedMessage(
            "3", "Regular message", "user", 5, 2, Map.of()
        );

        List<CompactGrouping.MessageGroup> groups = grouping.groupByToolChain(List.of(msg1, msg2, msg3));

        assertNotNull(groups);
    }

    @Test
    @DisplayName("CompactGrouping GroupingOptions defaults")
    void groupingOptionsDefaults() {
        CompactGrouping.GroupingOptions options = CompactGrouping.GroupingOptions.defaults();

        assertEquals(2, options.minGroupSize());
        assertEquals(20, options.maxGroupSize());
        assertEquals(0.3, options.minCohesion());
        assertEquals(6, options.enabledTypes().size());
        assertTrue(options.mergeAdjacent());
    }

    @Test
    @DisplayName("CompactGrouping GroupingOptions record")
    void groupingOptionsRecord() {
        CompactGrouping.GroupingOptions options = new CompactGrouping.GroupingOptions(
            3, 15, 0.5, List.of(CompactGrouping.GroupType.TOPIC), false
        );

        assertEquals(3, options.minGroupSize());
        assertEquals(15, options.maxGroupSize());
        assertEquals(0.5, options.minCohesion());
        assertEquals(1, options.enabledTypes().size());
        assertFalse(options.mergeAdjacent());
    }

    @Test
    @DisplayName("CompactGrouping GroupingResult record")
    void groupingResultRecord() {
        CompactGrouping.GroupingResult result = new CompactGrouping.GroupingResult(
            List.of(), 10, 2, 3, 0.75
        );

        assertEquals(10, result.totalMessages());
        assertEquals(2, result.totalGroups());
        assertEquals(3, result.ungroupedMessages());
        assertEquals(0.75, result.avgCohesion());
    }

    @Test
    @DisplayName("CompactGrouping GroupingResult format")
    void groupingResultFormat() {
        CompactGrouping.GroupingResult result = new CompactGrouping.GroupingResult(
            List.of(), 10, 2, 3, 0.75
        );

        String formatted = result.format();

        assertTrue(formatted.contains("10 messages"));
        assertTrue(formatted.contains("2 groups"));
        assertTrue(formatted.contains("0.75"));
    }
}