/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/agents/generateAgent
 */
package com.anthropic.claudecode.components.agents;

import java.util.*;
import java.nio.file.*;

/**
 * Agent generator - Generates agent definitions.
 */
public final class AgentGenerator {

    /**
     * Generate a new agent definition.
     */
    public static AgentTypes.AgentDefinition generate(String name, String description) {
        return new AgentTypes.AgentDefinition(
            AgentUtils.normalizeAgentName(name),
            description != null ? description : "Agent: " + name,
            "auto",
            Collections.emptyList(),
            new HashMap<>(),
            AgentTypes.SettingSource.USER,
            null
        );
    }

    /**
     * Generate agent from template.
     */
    public static AgentTypes.AgentDefinition fromTemplate(String templateName) {
        return switch (templateName.toLowerCase()) {
            case "code-reviewer" -> generateCodeReviewer();
            case "test-writer" -> generateTestWriter();
            case "doc-writer" -> generateDocWriter();
            case "refactor" -> generateRefactorer();
            default -> generate(templateName, "Custom agent: " + templateName);
        };
    }

    /**
     * Generate code reviewer agent.
     */
    public static AgentTypes.AgentDefinition generateCodeReviewer() {
        Map<String, Object> config = new HashMap<>();
        config.put("system_prompt", "You are a code reviewer. Analyze code for quality, bugs, and improvements.");

        return new AgentTypes.AgentDefinition(
            "code-reviewer",
            "Reviews code for quality, bugs, and improvements",
            "claude-sonnet-4-6",
            List.of("read", "glob", "grep"),
            config,
            AgentTypes.SettingSource.USER,
            null
        );
    }

    /**
     * Generate test writer agent.
     */
    public static AgentTypes.AgentDefinition generateTestWriter() {
        Map<String, Object> config = new HashMap<>();
        config.put("system_prompt", "You are a test writer. Generate comprehensive tests for the provided code.");

        return new AgentTypes.AgentDefinition(
            "test-writer",
            "Generates comprehensive tests for code",
            "claude-sonnet-4-6",
            List.of("read", "write", "glob", "grep"),
            config,
            AgentTypes.SettingSource.USER,
            null
        );
    }

    /**
     * Generate documentation writer agent.
     */
    public static AgentTypes.AgentDefinition generateDocWriter() {
        Map<String, Object> config = new HashMap<>();
        config.put("system_prompt", "You are a documentation writer. Create clear and comprehensive documentation.");

        return new AgentTypes.AgentDefinition(
            "doc-writer",
            "Creates clear and comprehensive documentation",
            "claude-sonnet-4-6",
            List.of("read", "write", "glob"),
            config,
            AgentTypes.SettingSource.USER,
            null
        );
    }

    /**
     * Generate refactorer agent.
     */
    public static AgentTypes.AgentDefinition generateRefactorer() {
        Map<String, Object> config = new HashMap<>();
        config.put("system_prompt", "You are a code refactorer. Improve code structure while maintaining functionality.");

        return new AgentTypes.AgentDefinition(
            "refactor",
            "Refactors code to improve structure and maintainability",
            "claude-sonnet-4-6",
            List.of("read", "write", "edit", "glob", "grep"),
            config,
            AgentTypes.SettingSource.USER,
            null
        );
    }

    /**
     * Generate agent from file.
     */
    public static AgentTypes.AgentDefinition fromFile(Path path) {
        try {
            String content = Files.readString(path);
            return parseAgentMarkdown(content, path);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse agent from markdown content.
     */
    public static AgentTypes.AgentDefinition parseAgentMarkdown(String content, Path path) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String name = null;
        String description = null;
        String model = "auto";
        List<String> tools = new ArrayList<>();
        Map<String, Object> config = new HashMap<>();

        // Parse frontmatter
        String[] lines = content.split("\n");
        boolean inFrontmatter = false;

        for (String line : lines) {
            if (line.equals("---")) {
                inFrontmatter = !inFrontmatter;
                continue;
            }

            if (inFrontmatter) {
                if (line.contains(":")) {
                    int colonIndex = line.indexOf(':');
                    String key = line.substring(0, colonIndex).trim().toLowerCase();
                    String value = line.substring(colonIndex + 1).trim();

                    switch (key) {
                        case "name" -> name = value;
                        case "description" -> description = value;
                        case "model" -> model = value;
                        case "tools" -> {
                            String[] toolList = value.split(",");
                            for (String t : toolList) {
                                tools.add(t.trim());
                            }
                        }
                        default -> config.put(key, value);
                    }
                }
            }
        }

        if (name == null) {
            name = path != null ? path.getFileName().toString().replace(".md", "") : "unnamed-agent";
        }

        return new AgentTypes.AgentDefinition(
            name,
            description != null ? description : "",
            model,
            tools,
            config,
            AgentTypes.SettingSource.USER,
            path != null ? path.toString() : null
        );
    }

    /**
     * Serialize agent to markdown.
     */
    public static String toMarkdown(AgentTypes.AgentDefinition agent) {
        StringBuilder sb = new StringBuilder();

        sb.append("---\n");
        sb.append("name: ").append(agent.name()).append("\n");

        if (agent.description() != null && !agent.description().isEmpty()) {
            sb.append("description: ").append(agent.description()).append("\n");
        }

        if (agent.model() != null && !"auto".equals(agent.model())) {
            sb.append("model: ").append(agent.model()).append("\n");
        }

        if (!agent.tools().isEmpty()) {
            sb.append("tools: ").append(String.join(", ", agent.tools())).append("\n");
        }

        for (Map.Entry<String, Object> entry : agent.config().entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        sb.append("---\n");

        return sb.toString();
    }
}