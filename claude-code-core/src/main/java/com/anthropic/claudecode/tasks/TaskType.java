/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Task types
 */
package com.anthropic.claudecode.tasks;

/**
 * Task type enum.
 */
public enum TaskType {
    LOCAL_BASH,
    LOCAL_AGENT,
    REMOTE_AGENT,
    IN_PROCESS_TEAMMATE,
    LOCAL_WORKFLOW,
    MONITOR_MCP,
    DREAM
}