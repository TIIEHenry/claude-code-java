/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Factory for creating all built-in tools.
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.Tool;

import java.util.ArrayList;
import java.util.List;

/**
 * ToolFactory - Creates all built-in Claude Code tools.
 *
 * <p>Use this factory to get the standard tool set for a Claude Code instance.
 */
public final class ToolFactory {

    private ToolFactory() {} // Private constructor

    /**
     * Create all built-in tools.
     *
     * @return List of all standard Claude Code tools
     */
    public static List<Tool<?, ?, ?>> createAllTools() {
        List<Tool<?, ?, ?>> tools = new ArrayList<>();

        // Core execution tools
        tools.add(new BashTool());
        tools.add(new BashOutputTool());
        tools.add(new AgentTool());

        // File tools
        tools.add(new FileReadTool());
        tools.add(new FileWriteTool());
        tools.add(new FileEditTool());
        tools.add(new GlobTool());
        tools.add(new GrepTool());
        tools.add(new ReadImageTool());

        // Notebook tools
        tools.add(new NotebookEditTool());

        // Web tools
        tools.add(new WebFetchTool());
        tools.add(new WebSearchTool());

        // Interaction tools
        tools.add(new AskUserQuestionTool());
        tools.add(new TaskTool());
        tools.add(new SkillTool());
        tools.add(new EnterPlanModeTool());

        // Scheduling tools
        tools.add(new CronTool());

        return tools;
    }

    /**
     * Create only read-only tools (safe mode).
     *
     * @return List of read-only tools
     */
    public static List<Tool<?, ?, ?>> createReadOnlyTools() {
        List<Tool<?, ?, ?>> tools = new ArrayList<>();

        tools.add(new FileReadTool());
        tools.add(new GlobTool());
        tools.add(new GrepTool());
        tools.add(new ReadImageTool());
        tools.add(new WebFetchTool());
        tools.add(new WebSearchTool());
        tools.add(new BashOutputTool());
        tools.add(new AskUserQuestionTool());
        tools.add(new TaskTool());
        tools.add(new SkillTool());

        return tools;
    }

    /**
     * Create tools by category.
     */
    public static List<Tool<?, ?, ?>> createFileTools() {
        List<Tool<?, ?, ?>> tools = new ArrayList<>();
        tools.add(new FileReadTool());
        tools.add(new FileWriteTool());
        tools.add(new FileEditTool());
        tools.add(new GlobTool());
        tools.add(new GrepTool());
        return tools;
    }

    public static List<Tool<?, ?, ?>> createWebTools() {
        List<Tool<?, ?, ?>> tools = new ArrayList<>();
        tools.add(new WebFetchTool());
        tools.add(new WebSearchTool());
        return tools;
    }

    public static List<Tool<?, ?, ?>> createExecutionTools() {
        List<Tool<?, ?, ?>> tools = new ArrayList<>();
        tools.add(new BashTool());
        tools.add(new AgentTool());
        return tools;
    }
}