/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XmlTags.
 */
class XmlTagsTest {

    @Test
    @DisplayName("XmlTags COMMAND_NAME_TAG is defined")
    void commandNameTag() {
        assertEquals("command-name", XmlTags.COMMAND_NAME_TAG);
    }

    @Test
    @DisplayName("XmlTags COMMAND_MESSAGE_TAG is defined")
    void commandMessageTag() {
        assertEquals("command-message", XmlTags.COMMAND_MESSAGE_TAG);
    }

    @Test
    @DisplayName("XmlTags COMMAND_ARGS_TAG is defined")
    void commandArgsTag() {
        assertEquals("command-args", XmlTags.COMMAND_ARGS_TAG);
    }

    @Test
    @DisplayName("XmlTags BASH tags are defined")
    void bashTags() {
        assertEquals("bash-input", XmlTags.BASH_INPUT_TAG);
        assertEquals("bash-stdout", XmlTags.BASH_STDOUT_TAG);
        assertEquals("bash-stderr", XmlTags.BASH_STDERR_TAG);
    }

    @Test
    @DisplayName("XmlTags LOCAL_COMMAND tags are defined")
    void localCommandTags() {
        assertEquals("local-command-stdout", XmlTags.LOCAL_COMMAND_STDOUT_TAG);
        assertEquals("local-command-stderr", XmlTags.LOCAL_COMMAND_STDERR_TAG);
        assertEquals("local-command-caveat", XmlTags.LOCAL_COMMAND_CAVEAT_TAG);
    }

    @Test
    @DisplayName("XmlTags TERMINAL_OUTPUT_TAGS contains all terminal tags")
    void terminalOutputTags() {
        List<String> tags = XmlTags.TERMINAL_OUTPUT_TAGS;
        assertEquals(6, tags.size());
        assertTrue(tags.contains(XmlTags.BASH_INPUT_TAG));
        assertTrue(tags.contains(XmlTags.BASH_STDOUT_TAG));
        assertTrue(tags.contains(XmlTags.BASH_STDERR_TAG));
        assertTrue(tags.contains(XmlTags.LOCAL_COMMAND_STDOUT_TAG));
        assertTrue(tags.contains(XmlTags.LOCAL_COMMAND_STDERR_TAG));
        assertTrue(tags.contains(XmlTags.LOCAL_COMMAND_CAVEAT_TAG));
    }

    @Test
    @DisplayName("XmlTags TICK_TAG is defined")
    void tickTag() {
        assertEquals("tick", XmlTags.TICK_TAG);
    }

    @Test
    @DisplayName("XmlTags task notification tags are defined")
    void taskNotificationTags() {
        assertEquals("task-notification", XmlTags.TASK_NOTIFICATION_TAG);
        assertEquals("task-id", XmlTags.TASK_ID_TAG);
        assertEquals("tool-use-id", XmlTags.TOOL_USE_ID_TAG);
        assertEquals("task-type", XmlTags.TASK_TYPE_TAG);
        assertEquals("output-file", XmlTags.OUTPUT_FILE_TAG);
        assertEquals("status", XmlTags.STATUS_TAG);
        assertEquals("summary", XmlTags.SUMMARY_TAG);
        assertEquals("reason", XmlTags.REASON_TAG);
    }

    @Test
    @DisplayName("XmlTags worktree tags are defined")
    void worktreeTags() {
        assertEquals("worktree", XmlTags.WORKTREE_TAG);
        assertEquals("worktreePath", XmlTags.WORKTREE_PATH_TAG);
        assertEquals("worktreeBranch", XmlTags.WORKTREE_BRANCH_TAG);
    }

    @Test
    @DisplayName("XmlTags ULTRAPLAN_TAG is defined")
    void ultraplanTag() {
        assertEquals("ultraplan", XmlTags.ULTRAPLAN_TAG);
    }

    @Test
    @DisplayName("XmlTags remote review tags are defined")
    void remoteReviewTags() {
        assertEquals("remote-review", XmlTags.REMOTE_REVIEW_TAG);
        assertEquals("remote-review-progress", XmlTags.REMOTE_REVIEW_PROGRESS_TAG);
    }

    @Test
    @DisplayName("XmlTags TEAMMATE_MESSAGE_TAG is defined")
    void teammateMessageTag() {
        assertEquals("teammate-message", XmlTags.TEAMMATE_MESSAGE_TAG);
    }

    @Test
    @DisplayName("XmlTags channel message tags are defined")
    void channelMessageTags() {
        assertEquals("channel-message", XmlTags.CHANNEL_MESSAGE_TAG);
        assertEquals("channel", XmlTags.CHANNEL_TAG);
    }

    @Test
    @DisplayName("XmlTags CROSS_SESSION_MESSAGE_TAG is defined")
    void crossSessionMessageTag() {
        assertEquals("cross-session-message", XmlTags.CROSS_SESSION_MESSAGE_TAG);
    }

    @Test
    @DisplayName("XmlTags fork boilerplate tags are defined")
    void forkBoilerplateTags() {
        assertEquals("fork-boilerplate", XmlTags.FORK_BOILERPLATE_TAG);
        assertEquals("Your directive: ", XmlTags.FORK_DIRECTIVE_PREFIX);
    }

    @Test
    @DisplayName("XmlTags COMMON_HELP_ARGS contains expected values")
    void commonHelpArgs() {
        List<String> args = XmlTags.COMMON_HELP_ARGS;
        assertEquals(3, args.size());
        assertTrue(args.contains("help"));
        assertTrue(args.contains("-h"));
        assertTrue(args.contains("--help"));
    }

    @Test
    @DisplayName("XmlTags COMMON_INFO_ARGS contains expected values")
    void commonInfoArgs() {
        List<String> args = XmlTags.COMMON_INFO_ARGS;
        assertTrue(args.size() > 5);
        assertTrue(args.contains("list"));
        assertTrue(args.contains("show"));
        assertTrue(args.contains("version"));
        assertTrue(args.contains("status"));
    }

    @Test
    @DisplayName("XmlTags lists are immutable")
    void listsAreImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> {
            XmlTags.TERMINAL_OUTPUT_TAGS.add("test");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            XmlTags.COMMON_HELP_ARGS.add("test");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            XmlTags.COMMON_INFO_ARGS.add("test");
        });
    }
}