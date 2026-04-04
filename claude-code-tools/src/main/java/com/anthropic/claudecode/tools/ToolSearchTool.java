/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/ToolSearchTool/ToolSearchTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.*;
import com.anthropic.claudecode.*;

/**
 * ToolSearch Tool - Search for deferred tools.
 */
public final class ToolSearchTool extends AbstractTool<ToolSearchTool.Input, ToolSearchTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "ToolSearch";

    // Cache for tool descriptions
    private static final Map<String, String> descriptionCache = new ConcurrentHashMap<>();

    public ToolSearchTool() {
        super(TOOL_NAME, "Search for deferred tools");
    }

    /**
     * Input schema.
     */
    public record Input(
        String query,
        Integer max_results
    ) {}

    /**
     * Output schema.
     */
    public record Output(
        List<String> matches,
        String query,
        int total_deferred_tools,
        List<String> pending_mcp_servers
    ) {}

    @Override
    public String description() {
        return """
            Search for deferred tools by name or keyword.

            Use "select:<tool_name>" for direct selection, or keywords to search.
            Supports comma-separated multi-select: "select:A,B,C".

            For MCP tools, search by server name (e.g., "slack", "github") or action (e.g., "read", "list").""";
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return true;
    }

    @Override
    public boolean isReadOnly(Input input) {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Always enabled
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
            String query = input.query().toLowerCase().trim();
            int maxResults = input.max_results() != null ? input.max_results() : 5;

            List<Tool<?, ?, ?>> deferredTools = getDeferredTools(context);
            List<Tool<?, ?, ?>> allTools = context != null ? context.options().tools() : Collections.emptyList();

            // Check for select: prefix
            if (query.startsWith("select:")) {
                return handleSelect(query.substring(7), deferredTools, allTools, maxResults);
            }

            // Keyword search
            List<String> matches = searchWithKeywords(query, deferredTools, allTools, maxResults);

            return ToolResult.of(new Output(
                matches,
                input.query(),
                deferredTools.size(),
                matches.isEmpty() ? getPendingServerNames(context) : null
            ));
        });
    }

    private ToolResult<Output> handleSelect(
        String toolNames,
        List<Tool<?, ?, ?>> deferredTools,
        List<Tool<?, ?, ?>> allTools,
        int maxResults
    ) {
        String[] requested = toolNames.split(",");
        List<String> found = new ArrayList<>();

        for (String name : requested) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;

            Tool<?, ?, ?> tool = findToolByName(deferredTools, trimmed);
            if (tool == null) {
                tool = findToolByName(allTools, trimmed);
            }
            if (tool != null && !found.contains(tool.name())) {
                found.add(tool.name());
            }
        }

        return ToolResult.of(new Output(
            found,
            "select:" + toolNames,
            deferredTools.size(),
            found.isEmpty() ? Collections.emptyList() : null
        ));
    }

    private List<String> searchWithKeywords(
        String query,
        List<Tool<?, ?, ?>> deferredTools,
        List<Tool<?, ?, ?>> allTools,
        int maxResults
    ) {
        // Check for exact match
        for (Tool<?, ?, ?> tool : deferredTools) {
            if (tool.name().toLowerCase().equals(query)) {
                return List.of(tool.name());
            }
        }

        // Check for MCP prefix match
        if (query.startsWith("mcp__") && query.length() > 5) {
            List<String> matches = new ArrayList<>();
            for (Tool<?, ?, ?> tool : deferredTools) {
                if (tool.name().toLowerCase().startsWith(query)) {
                    matches.add(tool.name());
                    if (matches.size() >= maxResults) break;
                }
            }
            if (!matches.isEmpty()) return matches;
        }

        // Parse query terms
        List<String> queryTerms = Arrays.asList(query.split("\\s+"));
        List<ScoredTool> scored = new ArrayList<>();

        for (Tool<?, ?, ?> tool : deferredTools) {
            int score = scoreTool(tool, queryTerms);
            if (score > 0) {
                scored.add(new ScoredTool(tool.name(), score));
            }
        }

        // Sort by score and return top results
        return scored.stream()
            .sorted((a, b) -> Integer.compare(b.score, a.score))
            .limit(maxResults)
            .map(ScoredTool::name)
            .toList();
    }

    private int scoreTool(Tool<?, ?, ?> tool, List<String> queryTerms) {
        ParsedName parsed = parseToolName(tool.name());
        int score = 0;

        for (String term : queryTerms) {
            // Exact part match
            if (parsed.parts().contains(term)) {
                score += parsed.isMcp() ? 12 : 10;
            } else if (parsed.parts().stream().anyMatch(p -> p.contains(term))) {
                score += parsed.isMcp() ? 6 : 5;
            }

            // Full name match
            if (parsed.full().contains(term) && score == 0) {
                score += 3;
            }

            // Search hint match
            if (tool.searchHint() != null) {
                String hint = tool.searchHint().toLowerCase();
                if (hint.contains(term)) {
                    score += 4;
                }
            }
        }

        return score;
    }

    private ParsedName parseToolName(String name) {
        if (name.startsWith("mcp__")) {
            String withoutPrefix = name.substring(5).toLowerCase();
            List<String> parts = Arrays.stream(withoutPrefix.split("__"))
                .flatMap(p -> Arrays.stream(p.split("_")))
                .filter(p -> !p.isEmpty())
                .toList();
            return new ParsedName(parts, withoutPrefix.replace("__", " ").replace("_", " "), true);
        }

        // Regular tool - split by CamelCase and underscores
        String[] parts = name.replaceAll("([a-z])([A-Z])", "$1 $2")
            .replace("_", " ")
            .toLowerCase()
            .split("\\s+");

        return new ParsedName(
            Arrays.stream(parts).filter(p -> !p.isEmpty()).toList(),
            String.join(" ", parts),
            false
        );
    }

    private record ParsedName(List<String> parts, String full, boolean isMcp) {}
    private record ScoredTool(String name, int score) {}

    private List<Tool<?, ?, ?>> getDeferredTools(ToolUseContext context) {
        if (context == null) return Collections.emptyList();
        return context.options().tools().stream()
            .filter(t -> t.shouldDefer())
            .toList();
    }

    private List<String> getPendingServerNames(ToolUseContext context) {
        // Check MCP client status for pending servers
        List<String> pending = new ArrayList<>();

        try {
            var mcpClients = context.options().mcpClients();
            if (mcpClients == null) return pending;

            for (var client : mcpClients) {
                // Check if client is still connecting
                try {
                    var isConnectedMethod = client.getClass().getMethod("isConnected");
                    Boolean connected = (Boolean) isConnectedMethod.invoke(client);
                    if (!connected) {
                        var getServerNameMethod = client.getClass().getMethod("getServerName");
                        String serverName = (String) getServerNameMethod.invoke(client);
                        if (serverName != null) {
                            pending.add(serverName);
                        }
                    }
                } catch (Exception e) {
                    // Client doesn't have the expected methods
                }
            }
        } catch (Exception e) {
            // Return empty list on error
        }

        return pending;
    }

    @SuppressWarnings("unchecked")
    private Tool<?, ?, ?> findToolByName(List<Tool<?, ?, ?>> tools, String name) {
        for (Tool<?, ?, ?> tool : tools) {
            if (tool.name().equalsIgnoreCase(name)) {
                return tool;
            }
        }
        return null;
    }

    @Override
    public String formatResult(Output output) {
        if (output.matches().isEmpty()) {
            String msg = "No matching deferred tools found";
            if (output.pending_mcp_servers() != null && !output.pending_mcp_servers().isEmpty()) {
                msg += ". Some MCP servers are still connecting: " + String.join(", ", output.pending_mcp_servers());
            }
            return msg;
        }
        return "Found tools: " + String.join(", ", output.matches());
    }
}