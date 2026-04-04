/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/agents/validateAgent
 */
package com.anthropic.claudecode.components.agents;

import java.util.*;
import java.util.regex.*;

/**
 * Agent validator - Validates agent definitions.
 */
public final class AgentValidator {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_DESCRIPTION_LENGTH = 256;
    private static final Set<String> VALID_MODELS = Set.of(
        "claude-opus-4-6",
        "claude-sonnet-4-6",
        "claude-sonnet-4-5",
        "claude-haiku-4-5",
        "auto"
    );

    /**
     * Validate an agent definition.
     */
    public static AgentTypes.AgentValidationResult validate(AgentTypes.AgentDefinition agent) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (agent == null) {
            return AgentTypes.AgentValidationResult.withErrors(List.of("Agent definition is null"));
        }

        // Validate name
        validateName(agent.name(), errors, warnings);

        // Validate description
        validateDescription(agent.description(), errors, warnings);

        // Validate model
        validateModel(agent.model(), warnings);

        // Validate tools
        validateTools(agent.tools(), warnings);

        // Validate config
        validateConfig(agent.config(), warnings);

        return new AgentTypes.AgentValidationResult(errors.isEmpty(), warnings, errors);
    }

    /**
     * Validate name only.
     */
    public static AgentTypes.AgentValidationResult validateName(String name) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        validateName(name, errors, warnings);
        return new AgentTypes.AgentValidationResult(errors.isEmpty(), warnings, errors);
    }

    private static void validateName(String name, List<String> errors, List<String> warnings) {
        if (name == null || name.isEmpty()) {
            errors.add("Agent name is required");
            return;
        }

        if (name.length() > MAX_NAME_LENGTH) {
            errors.add("Agent name must be " + MAX_NAME_LENGTH + " characters or less");
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            errors.add("Agent name must contain only letters, numbers, hyphens, and underscores");
        }

        if (name.startsWith("-") || name.startsWith("_")) {
            warnings.add("Agent name should not start with a hyphen or underscore");
        }

        // Check for reserved names
        Set<String> reserved = Set.of("default", "system", "claude", "assistant", "user");
        if (reserved.contains(name.toLowerCase())) {
            errors.add("Agent name '" + name + "' is reserved");
        }
    }

    private static void validateDescription(String description, List<String> errors, List<String> warnings) {
        if (description == null || description.isEmpty()) {
            warnings.add("Agent description is recommended");
            return;
        }

        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            warnings.add("Description is long and may be truncated");
        }
    }

    private static void validateModel(String model, List<String> warnings) {
        if (model == null || model.isEmpty()) {
            return; // Model is optional, defaults will be used
        }

        if (!VALID_MODELS.contains(model) && !model.startsWith("claude-")) {
            warnings.add("Model '" + model + "' may not be a valid Claude model");
        }
    }

    private static void validateTools(List<String> tools, List<String> warnings) {
        if (tools == null || tools.isEmpty()) {
            return; // Empty tools list is valid
        }

        Set<String> validTools = Set.of(
            "bash", "read", "write", "edit", "glob", "grep",
            "webfetch", "websearch", "ask", "agent", "task"
        );

        for (String tool : tools) {
            if (!validTools.contains(tool.toLowerCase())) {
                warnings.add("Unknown tool: " + tool);
            }
        }
    }

    private static void validateConfig(Map<String, Object> config, List<String> warnings) {
        if (config == null || config.isEmpty()) {
            return;
        }

        // Check for known config keys
        Set<String> knownKeys = Set.of(
            "max_tokens", "temperature", "system_prompt",
            "max_turns", "timeout", "auto_mode"
        );

        for (String key : config.keySet()) {
            if (!knownKeys.contains(key)) {
                warnings.add("Unknown config key: " + key);
            }
        }

        // Validate specific config values
        if (config.containsKey("temperature")) {
            Object temp = config.get("temperature");
            if (temp instanceof Number) {
                double t = ((Number) temp).doubleValue();
                if (t < 0 || t > 1) {
                    warnings.add("Temperature should be between 0 and 1");
                }
            }
        }

        if (config.containsKey("max_tokens")) {
            Object maxTokens = config.get("max_tokens");
            if (maxTokens instanceof Number) {
                int mt = ((Number) maxTokens).intValue();
                if (mt < 1 || mt > 128000) {
                    warnings.add("max_tokens should be between 1 and 128000");
                }
            }
        }
    }

    /**
     * Quick validation check.
     */
    public static boolean isValid(AgentTypes.AgentDefinition agent) {
        return validate(agent).isValid();
    }
}