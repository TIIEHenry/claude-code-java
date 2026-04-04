/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/agents/types
 */
package com.anthropic.claudecode.components.agents;

import java.util.*;

/**
 * Agent types - Type definitions for agent system.
 */
public final class AgentTypes {

    /**
     * Agent paths constants.
     */
    public static final class AgentPaths {
        public static final String FOLDER_NAME = ".claude";
        public static final String AGENTS_DIR = "agents";

        private AgentPaths() {}
    }

    /**
     * Setting source enum.
     */
    public enum SettingSource {
        USER,
        PROJECT,
        ENTERPRISE,
        MANAGED,
        PLUGIN
    }

    /**
     * Agent mode state (sealed interface pattern).
     */
    public sealed interface ModeState permits
        MainMenuMode,
        ListAgentsMode,
        AgentMenuMode,
        ViewAgentMode,
        CreateAgentMode,
        EditAgentMode,
        DeleteConfirmMode {}

    /**
     * Main menu mode.
     */
    public record MainMenuMode() implements ModeState {}

    /**
     * List agents mode.
     */
    public record ListAgentsMode(SettingSource source) implements ModeState {}

    /**
     * Agent menu mode.
     */
    public record AgentMenuMode(
        AgentDefinition agent,
        ModeState previousMode
    ) implements ModeState {}

    /**
     * View agent mode.
     */
    public record ViewAgentMode(
        AgentDefinition agent,
        ModeState previousMode
    ) implements ModeState {}

    /**
     * Create agent mode.
     */
    public record CreateAgentMode() implements ModeState {}

    /**
     * Edit agent mode.
     */
    public record EditAgentMode(
        AgentDefinition agent,
        ModeState previousMode
    ) implements ModeState {}

    /**
     * Delete confirm mode.
     */
    public record DeleteConfirmMode(
        AgentDefinition agent,
        ModeState previousMode
    ) implements ModeState {}

    /**
     * Agent definition record.
     */
    public record AgentDefinition(
        String name,
        String description,
        String model,
        List<String> tools,
        Map<String, Object> config,
        SettingSource source,
        String path
    ) {}

    /**
     * Agent validation result record.
     */
    public record AgentValidationResult(
        boolean isValid,
        List<String> warnings,
        List<String> errors
    ) {
        public static AgentValidationResult valid() {
            return new AgentValidationResult(true, Collections.emptyList(), Collections.emptyList());
        }

        public static AgentValidationResult withWarnings(List<String> warnings) {
            return new AgentValidationResult(true, warnings, Collections.emptyList());
        }

        public static AgentValidationResult withErrors(List<String> errors) {
            return new AgentValidationResult(false, Collections.emptyList(), errors);
        }
    }
}