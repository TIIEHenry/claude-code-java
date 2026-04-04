/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code doctor context warnings
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Context warnings for the doctor command.
 */
public final class DoctorContextWarnings {
    private DoctorContextWarnings() {}

    // Thresholds
    private static final int MCP_TOOLS_THRESHOLD = 25_000;
    private static final int AGENT_DESCRIPTIONS_THRESHOLD = 15_000;
    private static final int MAX_MEMORY_CHARACTER_COUNT = 40_000;

    /**
     * Context warning types.
     */
    public enum WarningType {
        CLAUDEMD_FILES,
        AGENT_DESCRIPTIONS,
        MCP_TOOLS,
        UNREACHABLE_RULES
    }

    /**
     * Warning severity.
     */
    public enum Severity {
        WARNING,
        ERROR
    }

    /**
     * Context warning.
     */
    public record ContextWarning(
            WarningType type,
            Severity severity,
            String message,
            List<String> details,
            int currentValue,
            int threshold
    ) {}

    /**
     * All context warnings.
     */
    public record ContextWarnings(
            ContextWarning claudeMdWarning,
            ContextWarning agentWarning,
            ContextWarning mcpWarning,
            ContextWarning unreachableRulesWarning
    ) {}

    /**
     * Memory file info.
     */
    public record MemoryFile(String path, String content) {}

    /**
     * MCP tool info.
     */
    public record McpToolInfo(String name, int tokens) {}

    /**
     * Agent info.
     */
    public record AgentInfo(String agentType, String whenToUse, String source) {}

    /**
     * Agent definitions result.
     */
    public record AgentDefinitionsResult(List<AgentInfo> activeAgents) {}

    /**
     * Check Claude.md files for size warnings.
     */
    public static ContextWarning checkClaudeMdFiles(List<MemoryFile> memoryFiles) {
        List<MemoryFile> largeFiles = memoryFiles.stream()
                .filter(f -> f.content().length() > MAX_MEMORY_CHARACTER_COUNT)
                .sorted((a, b) -> b.content().length() - a.content().length())
                .toList();

        if (largeFiles.isEmpty()) {
            return null;
        }

        List<String> details = largeFiles.stream()
                .map(f -> f.path() + ": " + String.format("%,d", f.content().length()) + " chars")
                .toList();

        String message = largeFiles.size() == 1
                ? String.format("Large CLAUDE.md file detected (%,d chars > %,d)",
                        largeFiles.get(0).content().length(), MAX_MEMORY_CHARACTER_COUNT)
                : String.format("%d large CLAUDE.md files detected (each > %,d chars)",
                        largeFiles.size(), MAX_MEMORY_CHARACTER_COUNT);

        return new ContextWarning(
                WarningType.CLAUDEMD_FILES,
                Severity.WARNING,
                message,
                details,
                largeFiles.size(),
                MAX_MEMORY_CHARACTER_COUNT
        );
    }

    /**
     * Check agent descriptions token count.
     */
    public static ContextWarning checkAgentDescriptions(AgentDefinitionsResult agentInfo) {
        if (agentInfo == null || agentInfo.activeAgents() == null) {
            return null;
        }

        // Calculate total tokens
        int totalTokens = 0;
        List<McpToolInfo> agentTokens = new ArrayList<>();

        for (AgentInfo agent : agentInfo.activeAgents()) {
            if (!"built-in".equals(agent.source())) {
                String description = agent.agentType() + ": " + agent.whenToUse();
                int tokens = roughTokenCountEstimation(description);
                agentTokens.add(new McpToolInfo(agent.agentType(), tokens));
                totalTokens += tokens;
            }
        }

        if (totalTokens <= AGENT_DESCRIPTIONS_THRESHOLD) {
            return null;
        }

        // Sort by token count
        agentTokens.sort((a, b) -> b.tokens() - a.tokens());

        List<String> details = agentTokens.stream()
                .limit(5)
                .map(a -> a.name() + ": ~" + String.format("%,d", a.tokens()) + " tokens")
                .toList();

        if (agentTokens.size() > 5) {
            details = new ArrayList<>(details);
            details.add("(" + (agentTokens.size() - 5) + " more custom agents)");
        }

        return new ContextWarning(
                WarningType.AGENT_DESCRIPTIONS,
                Severity.WARNING,
                String.format("Large agent descriptions (~%,d tokens > %,d)",
                        totalTokens, AGENT_DESCRIPTIONS_THRESHOLD),
                details,
                totalTokens,
                AGENT_DESCRIPTIONS_THRESHOLD
        );
    }

    /**
     * Check MCP tools token count.
     */
    public static ContextWarning checkMcpTools(List<McpToolInfo> mcpTools) {
        if (mcpTools == null || mcpTools.isEmpty()) {
            return null;
        }

        int totalTokens = mcpTools.stream().mapToInt(McpToolInfo::tokens).sum();

        if (totalTokens <= MCP_TOOLS_THRESHOLD) {
            return null;
        }

        // Group by server
        Map<String, int[]> toolsByServer = new LinkedHashMap<>();
        for (McpToolInfo tool : mcpTools) {
            String[] parts = tool.name().split("__");
            String serverName = parts.length > 1 ? parts[1] : "unknown";

            int[] current = toolsByServer.getOrDefault(serverName, new int[]{0, 0});
            current[0]++; // count
            current[1] += tool.tokens(); // tokens
            toolsByServer.put(serverName, current);
        }

        // Sort by token count
        List<Map.Entry<String, int[]>> sorted = toolsByServer.entrySet().stream()
                .sorted((a, b) -> b.getValue()[1] - a.getValue()[1])
                .limit(5)
                .toList();

        List<String> details = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : sorted) {
            details.add(entry.getKey() + ": " + entry.getValue()[0] + " tools (~" +
                    String.format("%,d", entry.getValue()[1]) + " tokens)");
        }

        if (toolsByServer.size() > 5) {
            details.add("(" + (toolsByServer.size() - 5) + " more servers)");
        }

        return new ContextWarning(
                WarningType.MCP_TOOLS,
                Severity.WARNING,
                String.format("Large MCP tools context (~%,d tokens > %,d)",
                        totalTokens, MCP_TOOLS_THRESHOLD),
                details,
                totalTokens,
                MCP_TOOLS_THRESHOLD
        );
    }

    /**
     * Check for unreachable permission rules.
     */
    public static ContextWarning checkUnreachableRules(List<String> unreachableRules) {
        if (unreachableRules == null || unreachableRules.isEmpty()) {
            return null;
        }

        return new ContextWarning(
                WarningType.UNREACHABLE_RULES,
                Severity.WARNING,
                unreachableRules.size() + " unreachable permission rule" +
                        (unreachableRules.size() > 1 ? "s" : "") + " detected",
                unreachableRules,
                unreachableRules.size(),
                0
        );
    }

    /**
     * Check all context warnings.
     */
    public static CompletableFuture<ContextWarnings> checkContextWarnings(
            List<MemoryFile> memoryFiles,
            AgentDefinitionsResult agentInfo,
            List<McpToolInfo> mcpTools,
            List<String> unreachableRules
    ) {
        return CompletableFuture.supplyAsync(() -> new ContextWarnings(
                checkClaudeMdFiles(memoryFiles),
                checkAgentDescriptions(agentInfo),
                checkMcpTools(mcpTools),
                checkUnreachableRules(unreachableRules)
        ));
    }

    /**
     * Rough token count estimation (4 chars per token).
     */
    private static int roughTokenCountEstimation(String text) {
        return text != null ? text.length() / 4 : 0;
    }
}