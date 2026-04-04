/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code types/tools.ts ToolProgressData
 */
package com.anthropic.claudecode;

/**
 * Base interface for tool progress data.
 *
 * <p>Corresponds to ToolProgressData in types/tools.ts.
 */
public interface ToolProgressData {

    /**
     * Get the progress type name.
     */
    default String type() {
        return "progress";
    }

    // ==================== Default Implementations ====================

    record AgentToolProgress(String agentId, String status, String description) implements ToolProgressData {
        @Override
        public String type() { return "agent_tool"; }
    }

    record BashProgress(String command, String output, int exitCode) implements ToolProgressData {
        @Override
        public String type() { return "bash"; }
    }

    record MCPProgress(String serverName, String toolName, String status) implements ToolProgressData {
        @Override
        public String type() { return "mcp"; }
    }

    record REPLToolProgress(String status, String output) implements ToolProgressData {
        @Override
        public String type() { return "repl"; }
    }

    record SkillToolProgress(String skillName, String status) implements ToolProgressData {
        @Override
        public String type() { return "skill"; }
    }

    record TaskOutputProgress(String taskId, String status, String output) implements ToolProgressData {
        @Override
        public String type() { return "task_output"; }
    }

    record WebSearchProgress(String query, int results) implements ToolProgressData {
        @Override
        public String type() { return "web_search"; }
    }

    record Default(String message) implements ToolProgressData {
        @Override
        public String type() { return "default"; }
    }
}