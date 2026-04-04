/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CollapseHookSummaries.
 */
class CollapseHookSummariesTest {

    @Test
    @DisplayName("CollapseHookSummaries HookSummaryMessage MessageType enum")
    void messageTypeEnum() {
        assertEquals(4, CollapseHookSummaries.HookSummaryMessage.MessageType.values().length);
    }

    @Test
    @DisplayName("CollapseHookSummaries HookSummaryMessage SystemSubtype enum")
    void systemSubtypeEnum() {
        assertEquals(3, CollapseHookSummaries.HookSummaryMessage.SystemSubtype.values().length);
    }

    @Test
    @DisplayName("CollapseHookSummaries HookSummaryMessage record")
    void hookSummaryMessageRecord() {
        CollapseHookSummaries.HookInfo info = new CollapseHookSummaries.HookInfo(
            "hook1", "/path/to/hook", 100L, true
        );

        CollapseHookSummaries.HookSummaryMessage msg = new CollapseHookSummaries.HookSummaryMessage(
            CollapseHookSummaries.HookSummaryMessage.MessageType.SYSTEM,
            CollapseHookSummaries.HookSummaryMessage.SystemSubtype.STOP_HOOK_SUMMARY,
            "test-label",
            1,
            List.of(info),
            null,
            false,
            true,
            100L
        );

        assertEquals(CollapseHookSummaries.HookSummaryMessage.MessageType.SYSTEM, msg.type());
        assertEquals(CollapseHookSummaries.HookSummaryMessage.SystemSubtype.STOP_HOOK_SUMMARY, msg.subtype());
        assertEquals("test-label", msg.hookLabel());
        assertEquals(1, msg.hookCount());
        assertEquals(1, msg.hookInfos().size());
    }

    @Test
    @DisplayName("CollapseHookSummaries HookInfo record")
    void hookInfoRecord() {
        CollapseHookSummaries.HookInfo info = new CollapseHookSummaries.HookInfo(
            "pre-commit", "/hooks/pre-commit", 50L, true
        );

        assertEquals("pre-commit", info.hookName());
        assertEquals("/hooks/pre-commit", info.hookPath());
        assertEquals(50L, info.durationMs());
        assertTrue(info.success());
    }

    @Test
    @DisplayName("CollapseHookSummaries HookError record")
    void hookErrorRecord() {
        CollapseHookSummaries.HookError error = new CollapseHookSummaries.HookError(
            "test-hook", "Hook failed", 1
        );

        assertEquals("test-hook", error.hookName());
        assertEquals("Hook failed", error.error());
        assertEquals(1, error.exitCode());
    }

    @Test
    @DisplayName("CollapseHookSummaries collapseHookSummaries empty list")
    void collapseEmptyList() {
        List<CollapseHookSummaries.HookSummaryMessage> messages = new ArrayList<>();
        List<CollapseHookSummaries.HookSummaryMessage> result =
            CollapseHookSummaries.collapseHookSummaries(messages);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("CollapseHookSummaries collapseHookSummaries single message")
    void collapseSingleMessage() {
        CollapseHookSummaries.HookSummaryMessage msg = new CollapseHookSummaries.HookSummaryMessage(
            CollapseHookSummaries.HookSummaryMessage.MessageType.USER,
            CollapseHookSummaries.HookSummaryMessage.SystemSubtype.OTHER,
            null, 0, null, null, false, false, null
        );

        List<CollapseHookSummaries.HookSummaryMessage> messages = List.of(msg);
        List<CollapseHookSummaries.HookSummaryMessage> result =
            CollapseHookSummaries.collapseHookSummaries(messages);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("CollapseHookSummaries collapseHookSummaries non-system message")
    void collapseNonSystemMessage() {
        CollapseHookSummaries.HookSummaryMessage msg = new CollapseHookSummaries.HookSummaryMessage(
            CollapseHookSummaries.HookSummaryMessage.MessageType.ASSISTANT,
            CollapseHookSummaries.HookSummaryMessage.SystemSubtype.OTHER,
            null, 0, null, null, false, false, null
        );

        List<CollapseHookSummaries.HookSummaryMessage> messages = List.of(msg);
        List<CollapseHookSummaries.HookSummaryMessage> result =
            CollapseHookSummaries.collapseHookSummaries(messages);

        assertEquals(1, result.size());
    }
}