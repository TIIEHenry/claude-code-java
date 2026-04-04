/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/TeamDeleteTool/TeamDeleteTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.nio.file.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;

/**
 * TeamDelete Tool - disband a swarm team and clean up.
 */
public final class TeamDeleteTool extends AbstractTool<TeamDeleteTool.Input, TeamDeleteTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "TeamDelete";

    public TeamDeleteTool() {
        super(TOOL_NAME, "Disband a swarm team and clean up");
    }

    /**
     * Input schema.
     */
    public record Input() {}

    /**
     * Output schema.
     */
    public record Output(
        boolean success,
        String message,
        String team_name
    ) {}

    private static final String TEAM_LEAD_NAME = "team-lead";

    @Override
    public String description() {
        return "Clean up team and task directories when the swarm is complete";
    }

    @Override
    public String searchHint() {
        return "disband a swarm team and clean up";
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
    public CompletableFuture<ToolResult<Output>> call(
        Input input,
        ToolUseContext context,
        CanUseToolFn canUseTool,
        AssistantMessage parent,
        Consumer<ToolProgress<ToolProgressData>> onProgress
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Get team name from app state
            Map<String, Object> appState = context != null && context.getAppState() != null
                ? context.getAppState().apply(null)
                : Map.of();

            String teamName = (String) appState.get("teamName");

            if (teamName != null) {
                // Cleanup team directories
                cleanupTeamDirectories(teamName);

                // Log analytics
                AnalyticsMetadata.logEvent("tengu_team_deleted", Map.of(
                    "team_name", teamName
                ), true);

                // Update app state to clear team info
                if (context != null && context.setAppState() != null) {
                    Map<String, Object> newState = new HashMap<>(appState);
                    newState.remove("teamName");
                    newState.remove("teamContext");
                    newState.put("inboxMessages", Collections.emptyList());
                    context.setAppState().apply(newState);
                }
            }

            return ToolResult.of(new Output(
                true,
                teamName != null
                    ? "Cleaned up directories and worktrees for team \"" + teamName + "\""
                    : "No team name found, nothing to clean up",
                teamName
            ));
        });
    }

    @Override
    public String formatResult(Output output) {
        return output.message();
    }

    // Helper methods

    private void cleanupTeamDirectories(String teamName) {
        try {
            String home = System.getProperty("user.home");

            // Clean up team directory
            Path teamDir = Paths.get(home, ".claude", "teams", teamName);
            deleteRecursively(teamDir);

            // Clean up worktrees for this team
            Path worktreesDir = Paths.get(home, ".claude", "worktrees");
            if (Files.exists(worktreesDir)) {
                Files.list(worktreesDir)
                    .filter(p -> p.getFileName().toString().startsWith(teamName))
                    .forEach(this::deleteRecursively);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception e) {}
                    });
            } else {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}