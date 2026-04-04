/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code AgentTool
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AgentTool - Launch a specialized agent for complex tasks.
 *
 * <p>Corresponds to AgentTool in tools/AgentTool/.
 *
 * <p>Usage notes from system prompt:
 * - Launch a new agent to handle complex, multi-step tasks autonomously
 * - Specialized agents handle specific tasks and have access to different tools
 * - Available agent types: general-purpose, Explore, Plan, claude-code-guide
 * - Use general-purpose for complex questions and multi-step tasks
 * - Use Explore for fast codebase exploration
 * - Use Plan for implementation planning
 * - Specify thoroughness level for Explore: "quick", "medium", "very thorough"
 * - Launch multiple agents concurrently for independent work
 * - Agents work autonomously and return results when complete
 */
public class AgentTool extends AbstractTool<AgentTool.Input, AgentTool.Output, AgentTool.Progress> {

    public static final String NAME = "Agent";

    // Available agent types
    private static final Set<String> AGENT_TYPES = Set.of(
            "general-purpose",
            "Explore",
            "Plan",
            "claude-code-guide"
    );

    public AgentTool() {
        super(NAME, List.of("agent", "spawn"), createSchema());
    }

    private static Map<String, Object> createSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> subagentTypeProp = new LinkedHashMap<>();
        subagentTypeProp.put("type", "string");
        subagentTypeProp.put("enum", List.of("general-purpose", "Explore", "Plan", "claude-code-guide"));
        subagentTypeProp.put("description", "The type of specialized agent to use");
        subagentTypeProp.put("default", "general-purpose");
        properties.put("subagent_type", subagentTypeProp);

        Map<String, Object> promptProp = new LinkedHashMap<>();
        promptProp.put("type", "string");
        promptProp.put("description", "The task for the agent to perform");
        properties.put("prompt", promptProp);

        Map<String, Object> descriptionProp = new LinkedHashMap<>();
        descriptionProp.put("type", "string");
        descriptionProp.put("description", "A short (3-5 word) description of the task");
        properties.put("description", descriptionProp);

        Map<String, Object> modelProp = new LinkedHashMap<>();
        modelProp.put("type", "string");
        modelProp.put("enum", List.of("sonnet", "opus", "haiku"));
        modelProp.put("description", "Optional model override");
        properties.put("model", modelProp);

        Map<String, Object> backgroundProp = new LinkedHashMap<>();
        backgroundProp.put("type", "boolean");
        backgroundProp.put("description", "Run in background and be notified when complete");
        properties.put("run_in_background", backgroundProp);

        Map<String, Object> isolationProp = new LinkedHashMap<>();
        isolationProp.put("type", "string");
        isolationProp.put("enum", List.of("worktree"));
        isolationProp.put("description", "Run in isolated git worktree");
        properties.put("isolation", isolationProp);

        schema.put("properties", properties);
        schema.put("required", List.of("prompt"));
        return schema;
    }

    @Override
    public CompletableFuture<ToolResult<Output>> call(
            Input input,
            ToolUseContext context,
            CanUseToolFn canUseTool,
            AssistantMessage parentMessage,
            Consumer<ToolProgress<Progress>> onProgress) {

        return CompletableFuture.supplyAsync(() -> {
            String agentType = input.subagentType() != null ? input.subagentType() : "general-purpose";

            // Validate agent type
            if (!AGENT_TYPES.contains(agentType)) {
                return ToolResult.of(new Output(
                        "",
                        "Unknown agent type: " + agentType,
                        "",
                        null,
                        true
                ));
            }

            String agentId = "agent_" + UUID.randomUUID().toString().substring(0, 8);

            // Report progress
            if (onProgress != null) {
                onProgress.accept(ToolProgress.of(agentId, new Progress("starting", "Initializing " + agentType + " agent")));
            }

            // Execute agent task based on type
            String result;
            try {
                result = executeAgentTask(agentType, input, context, onProgress, agentId);
            } catch (Exception e) {
                return ToolResult.of(new Output(
                        "",
                        "Agent execution failed: " + e.getMessage(),
                        agentType,
                        agentId,
                        true
                ));
            }

            return ToolResult.of(new Output(
                    result,
                    "",
                    agentType,
                    agentId,
                    false
            ));
        });
    }

    /**
     * Execute the agent task based on type.
     */
    private String executeAgentTask(
            String agentType,
            Input input,
            ToolUseContext context,
            Consumer<ToolProgress<Progress>> onProgress,
            String agentId) {

        String prompt = input.prompt();

        return switch (agentType) {
            case "Explore" -> executeExploreAgent(prompt, input, onProgress, agentId);
            case "Plan" -> executePlanAgent(prompt, input, onProgress, agentId);
            case "claude-code-guide" -> executeClaudeCodeGuideAgent(prompt, input, onProgress, agentId);
            default -> executeGeneralPurposeAgent(prompt, input, onProgress, agentId);
        };
    }

    /**
     * Execute general-purpose agent.
     */
    private String executeGeneralPurposeAgent(
            String prompt,
            Input input,
            Consumer<ToolProgress<Progress>> onProgress,
            String agentId) {

        if (onProgress != null) {
            onProgress.accept(ToolProgress.of(agentId, new Progress("processing", "Analyzing task requirements")));
        }

        StringBuilder result = new StringBuilder();
        result.append("Agent Task Completed\n\n");
        result.append("Prompt: ").append(prompt).append("\n\n");

        if (input.description() != null) {
            result.append("Description: ").append(input.description()).append("\n\n");
        }

        // Simulate agent processing
        result.append("Analysis:\n");
        result.append("- Task type identified\n");
        result.append("- Resources gathered\n");
        result.append("- Execution completed\n\n");

        result.append("Result: Task has been processed. ");
        result.append("For full functionality, connect to Claude API.");

        return result.toString();
    }

    /**
     * Execute Explore agent for codebase exploration.
     */
    private String executeExploreAgent(
            String prompt,
            Input input,
            Consumer<ToolProgress<Progress>> onProgress,
            String agentId) {

        if (onProgress != null) {
            onProgress.accept(ToolProgress.of(agentId, new Progress("exploring", "Searching codebase")));
        }

        StringBuilder result = new StringBuilder();
        result.append("Codebase Exploration Results\n\n");
        result.append("Query: ").append(prompt).append("\n\n");

        String thoroughness = "medium"; // default
        result.append("Thoroughness: ").append(thoroughness).append("\n\n");

        result.append("Findings:\n");
        result.append("- Searched project structure\n");
        result.append("- Analyzed relevant files\n");
        result.append("- Identified key patterns\n\n");

        result.append("Note: Full exploration requires Claude API connection.");

        return result.toString();
    }

    /**
     * Execute Plan agent for implementation planning.
     */
    private String executePlanAgent(
            String prompt,
            Input input,
            Consumer<ToolProgress<Progress>> onProgress,
            String agentId) {

        if (onProgress != null) {
            onProgress.accept(ToolProgress.of(agentId, new Progress("planning", "Creating implementation plan")));
        }

        StringBuilder result = new StringBuilder();
        result.append("Implementation Plan\n\n");
        result.append("Task: ").append(prompt).append("\n\n");

        result.append("Proposed Steps:\n");
        result.append("1. Analyze requirements\n");
        result.append("2. Design solution architecture\n");
        result.append("3. Implement core functionality\n");
        result.append("4. Add tests\n");
        result.append("5. Review and refine\n\n");

        result.append("Note: Full planning requires Claude API connection.");

        return result.toString();
    }

    /**
     * Execute claude-code-guide agent.
     */
    private String executeClaudeCodeGuideAgent(
            String prompt,
            Input input,
            Consumer<ToolProgress<Progress>> onProgress,
            String agentId) {

        if (onProgress != null) {
            onProgress.accept(ToolProgress.of(agentId, new Progress("researching", "Looking up Claude Code documentation")));
        }

        StringBuilder result = new StringBuilder();
        result.append("Claude Code Guide\n\n");
        result.append("Question: ").append(prompt).append("\n\n");

        result.append("Claude Code CLI Features:\n");
        result.append("- Interactive agent for software engineering tasks\n");
        result.append("- Available as CLI, desktop app, and IDE extensions\n");
        result.append("- Supports multiple tools: FileRead, FileWrite, Bash, etc.\n");
        result.append("- Can spawn specialized agents for complex tasks\n\n");

        result.append("For detailed help, use: claude-code --help");

        return result.toString();
    }

    @Override
    public CompletableFuture<String> describe(Input input, ToolDescribeOptions options) {
        String desc = input.description() != null ? input.description() : "Agent task";
        String type = input.subagentType() != null ? input.subagentType() : "general-purpose";
        return CompletableFuture.completedFuture("Launch " + type + " agent: " + desc);
    }

    @Override
    public boolean isReadOnly(Input input) {
        // Agents can do both read and write operations
        return false;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        // Agents can run concurrently if background mode
        return input.runInBackground();
    }

    @Override
    public String getActivityDescription(Input input) {
        String desc = input.description();
        if (desc != null) {
            return desc;
        }
        return "Running agent task";
    }

    @Override
    public String getToolUseSummary(Input input) {
        String type = input.subagentType() != null ? input.subagentType() : "agent";
        String desc = input.description();
        return type + (desc != null ? ": " + desc : "");
    }

    // ==================== Input/Output/Progress ====================

    public record Input(
            String subagentType,
            String prompt,
            String description,
            String model,
            boolean runInBackground,
            String isolation
    ) {
        // Convenience constructor
        public Input(String prompt, String description) {
            this(null, prompt, description, null, false, null);
        }
    }

    public record Output(
            String result,
            String error,
            String agentType,
            String agentId,
            boolean isError
    ) {
        public String toResultString() {
            if (isError) {
                return error;
            }
            return result;
        }
    }

    public record Progress(
            String status,
            String partialResult
    ) implements ToolProgressData {}
}