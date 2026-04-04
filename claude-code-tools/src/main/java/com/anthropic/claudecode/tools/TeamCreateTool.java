/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/TeamCreateTool/TeamCreateTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.nio.file.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;

/**
 * TeamCreate Tool - create a multi-agent swarm team.
 */
public final class TeamCreateTool extends AbstractTool<TeamCreateTool.Input, TeamCreateTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "TeamCreate";

    public TeamCreateTool() {
        super(TOOL_NAME, "Create a new team for coordinating multiple agents");
    }

    /**
     * Input schema.
     */
    public record Input(
        String team_name,
        String description,
        String agent_type
    ) {}

    /**
     * Output schema.
     */
    public record Output(
        String team_name,
        String team_file_path,
        String lead_agent_id
    ) {}

    private static final String TEAM_LEAD_NAME = "team-lead";

    @Override
    public String description() {
        return "Create a new team for coordinating multiple agents";
    }

    @Override
    public String searchHint() {
        return "create a multi-agent swarm team";
    }

    @Override
    public boolean shouldDefer() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        String env = System.getenv("CLAUDE_CODE_SWARM_ENABLED");
        return "true".equalsIgnoreCase(env) || "1".equals(env);
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public Class<Output> outputType() {
        return Output.class;
    }

    @Override
    public CompletableFuture<ValidationResult> validateInput(Input input, ToolUseContext context) {
        if (input.team_name() == null || input.team_name().trim().isEmpty()) {
            return CompletableFuture.completedFuture(ValidationResult.failure("team_name is required for TeamCreate", 9));
        }
        return CompletableFuture.completedFuture(ValidationResult.success());
    }

    @Override
    public CompletableFuture<ToolResult<Output>> call(
        Input input,
        ToolUseContext context,
        CanUseToolFn canUseTool,
        AssistantMessage parent,
        Consumer<ToolProgress<ToolProgressData>> onProgress
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Get app state
            Map<String, Object> appState = context != null && context.getAppState() != null
                ? context.getAppState().apply(null)
                : Map.of();

            // Check if already in a team
            String existingTeam = (String) appState.get("teamName");
            if (existingTeam != null) {
                throw new RuntimeException(
                    "Already leading team \"" + existingTeam + "\". " +
                    "A leader can only manage one team at a time. " +
                    "Use TeamDelete to end the current team before creating a new one."
                );
            }

            // Generate unique team name
            String finalTeamName = generateUniqueTeamName(input.team_name());

            // Generate agent ID for the team lead
            String leadAgentId = formatAgentId(TEAM_LEAD_NAME, finalTeamName);
            String leadAgentType = input.agent_type() != null ? input.agent_type() : TEAM_LEAD_NAME;

            // Create team file path
            String teamFilePath = getTeamFilePath(finalTeamName);

            // Write team file
            writeTeamFile(finalTeamName, leadAgentId);

            // Ensure tasks dir exists
            ensureTasksDir(sanitizeName(finalTeamName));

            // Update app state with team info
            if (context != null && context.setAppState() != null) {
                Map<String, Object> newState = new HashMap<>(appState);
                newState.put("teamName", finalTeamName);
                newState.put("teamFilePath", teamFilePath);
                newState.put("leadAgentId", leadAgentId);
                context.setAppState().apply(newState);
            }

            // Log analytics
            AnalyticsMetadata.logEvent("tengu_team_created", Map.of(
                "team_name", finalTeamName,
                "teammate_count", "1",
                "lead_agent_type", leadAgentType
            ), true);

            return ToolResult.of(new Output(
                finalTeamName,
                teamFilePath,
                leadAgentId
            ));
        });
    }

    @Override
    public String formatResult(Output output) {
        return "Created team \"" + output.team_name() + "\" with lead agent " + output.lead_agent_id();
    }

    // Helper methods

    private String generateUniqueTeamName(String providedName) {
        if (!teamExists(providedName)) {
            return providedName;
        }
        return generateWordSlug();
    }

    private boolean teamExists(String name) {
        String path = getTeamFilePath(name);
        return Files.exists(Paths.get(path));
    }

    private String generateWordSlug() {
        return "team-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String formatAgentId(String name, String teamName) {
        return name + "@" + teamName;
    }

    private String getTeamFilePath(String teamName) {
        String home = System.getProperty("user.home");
        return home + "/.claude/teams/" + sanitizeName(teamName) + ".json";
    }

    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    private void writeTeamFile(String teamName, String leadAgentId) {
        try {
            Path path = Paths.get(getTeamFilePath(teamName));
            Files.createDirectories(path.getParent());
            String json = String.format(
                "{\"name\":\"%s\",\"leadAgentId\":\"%s\",\"createdAt\":%d}",
                teamName, leadAgentId, System.currentTimeMillis()
            );
            Files.writeString(path, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write team file: " + e.getMessage());
        }
    }

    private void ensureTasksDir(String taskListId) {
        try {
            String home = System.getProperty("user.home");
            Path tasksDir = Paths.get(home, ".claude", "tasks", taskListId);
            Files.createDirectories(tasksDir);
        } catch (Exception e) {
            // Ignore
        }
    }
}