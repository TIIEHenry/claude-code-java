/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import java.time.Instant;

import com.anthropic.claudecode.utils.permissions.ToolPermissionContext;

import java.util.List;
import java.util.function.Supplier;

/**
 * Options for generating tool prompt.
 */
public record ToolPromptOptions(
        Supplier<ToolPermissionContext> getToolPermissionContext,
        List<Tool<?, ?, ?>> tools,
        List<AgentDefinition> agents,
        List<String> allowedAgentTypes
) {
    public record AgentDefinition(
            String agentType,
            String whenToUse,
            String description,
            String model,
            List<String> tools
    ) {}

    public static ToolPromptOptions empty() {
        return new ToolPromptOptions(
                () -> ToolPermissionContext.createDefault(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}