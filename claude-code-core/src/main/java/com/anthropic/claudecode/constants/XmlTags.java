/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/xml.ts
 */
package com.anthropic.claudecode.constants;

import java.util.List;

/**
 * XML tag names used in messages.
 */
public final class XmlTags {
    private XmlTags() {}

    // Command metadata tags
    public static final String COMMAND_NAME_TAG = "command-name";
    public static final String COMMAND_MESSAGE_TAG = "command-message";
    public static final String COMMAND_ARGS_TAG = "command-args";

    // Terminal/bash command tags
    public static final String BASH_INPUT_TAG = "bash-input";
    public static final String BASH_STDOUT_TAG = "bash-stdout";
    public static final String BASH_STDERR_TAG = "bash-stderr";
    public static final String LOCAL_COMMAND_STDOUT_TAG = "local-command-stdout";
    public static final String LOCAL_COMMAND_STDERR_TAG = "local-command-stderr";
    public static final String LOCAL_COMMAND_CAVEAT_TAG = "local-command-caveat";

    // All terminal-related tags
    public static final List<String> TERMINAL_OUTPUT_TAGS = List.of(
        BASH_INPUT_TAG,
        BASH_STDOUT_TAG,
        BASH_STDERR_TAG,
        LOCAL_COMMAND_STDOUT_TAG,
        LOCAL_COMMAND_STDERR_TAG,
        LOCAL_COMMAND_CAVEAT_TAG
    );

    // Tick tag
    public static final String TICK_TAG = "tick";

    // Task notification tags
    public static final String TASK_NOTIFICATION_TAG = "task-notification";
    public static final String TASK_ID_TAG = "task-id";
    public static final String TOOL_USE_ID_TAG = "tool-use-id";
    public static final String TASK_TYPE_TAG = "task-type";
    public static final String OUTPUT_FILE_TAG = "output-file";
    public static final String STATUS_TAG = "status";
    public static final String SUMMARY_TAG = "summary";
    public static final String REASON_TAG = "reason";
    public static final String WORKTREE_TAG = "worktree";
    public static final String WORKTREE_PATH_TAG = "worktreePath";
    public static final String WORKTREE_BRANCH_TAG = "worktreeBranch";

    // Ultraplan mode tag
    public static final String ULTRAPLAN_TAG = "ultraplan";

    // Remote review tags
    public static final String REMOTE_REVIEW_TAG = "remote-review";
    public static final String REMOTE_REVIEW_PROGRESS_TAG = "remote-review-progress";

    // Teammate message tag
    public static final String TEAMMATE_MESSAGE_TAG = "teammate-message";

    // Channel message tags
    public static final String CHANNEL_MESSAGE_TAG = "channel-message";
    public static final String CHANNEL_TAG = "channel";

    // Cross-session message tag
    public static final String CROSS_SESSION_MESSAGE_TAG = "cross-session-message";

    // Fork boilerplate tags
    public static final String FORK_BOILERPLATE_TAG = "fork-boilerplate";
    public static final String FORK_DIRECTIVE_PREFIX = "Your directive: ";

    // Common argument patterns for slash commands
    public static final List<String> COMMON_HELP_ARGS = List.of("help", "-h", "--help");

    public static final List<String> COMMON_INFO_ARGS = List.of(
        "list", "show", "display", "current", "view",
        "get", "check", "describe", "print", "version",
        "about", "status", "?"
    );
}