/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/ListMcpResourcesTool/ListMcpResourcesTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import com.anthropic.claudecode.*;

/**
 * ListMcpResources Tool - list resources from connected MCP servers.
 */
public final class ListMcpResourcesTool extends AbstractTool<ListMcpResourcesTool.Input, ListMcpResourcesTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "ListMcpResources";

    public ListMcpResourcesTool() {
        super(TOOL_NAME, "List resources from connected MCP servers");
    }

    /**
     * Input schema.
     */
    public record Input(
        String server
    ) {}

    /**
     * Resource info.
     */
    public record ResourceInfo(
        String uri,
        String name,
        String mimeType,
        String description,
        String server
    ) {}

    /**
     * Output schema.
     */
    public record Output(
        List<ResourceInfo> resources
    ) {}

    @Override
    public String description() {
        return "List resources from connected MCP servers";
    }

    @Override
    public String searchHint() {
        return "list resources from connected MCP servers";
    }

    @Override
    public boolean shouldDefer() {
        return true;
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
            List<ResourceInfo> results = new ArrayList<>();

            try {
                // Get MCP clients from context
                var mcpClients = context.options().mcpClients();
                if (mcpClients == null || mcpClients.isEmpty()) {
                    return ToolResult.of(new Output(results));
                }

                // List resources from each MCP client
                for (var client : mcpClients) {
                    String serverName = getServerName(client);

                    // Filter by server if specified
                    if (input != null && input.server() != null && !input.server().isEmpty()) {
                        if (!serverName.equals(input.server())) {
                            continue;
                        }
                    }

                    // Get resources from this client
                    List<ResourceInfo> serverResources = listServerResources(client, serverName);
                    results.addAll(serverResources);
                }
            } catch (Exception e) {
                // Return empty results on error
            }

            return ToolResult.of(new Output(results));
        });
    }

    private String getServerName(Object client) {
        // Get server name from MCP client
        try {
            var method = client.getClass().getMethod("getServerName");
            return (String) method.invoke(client);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private List<ResourceInfo> listServerResources(Object client, String serverName) {
        List<ResourceInfo> resources = new ArrayList<>();
        try {
            // Try to call listResources on the client
            var method = client.getClass().getMethod("listResources");
            @SuppressWarnings("unchecked")
            List<?> result = (List<?>) method.invoke(client);

            for (Object res : result) {
                String uri = getResourceUri(res);
                String name = getResourceName(res);
                String mimeType = getResourceMimeType(res);
                String description = getResourceDescription(res);

                resources.add(new ResourceInfo(uri, name, mimeType, description, serverName));
            }
        } catch (Exception e) {
            // Client doesn't support listResources or error occurred
        }
        return resources;
    }

    private String getResourceUri(Object res) {
        try {
            var method = res.getClass().getMethod("getUri");
            return (String) method.invoke(res);
        } catch (Exception e) {
            return null;
        }
    }

    private String getResourceName(Object res) {
        try {
            var method = res.getClass().getMethod("getName");
            return (String) method.invoke(res);
        } catch (Exception e) {
            return null;
        }
    }

    private String getResourceMimeType(Object res) {
        try {
            var method = res.getClass().getMethod("getMimeType");
            return (String) method.invoke(res);
        } catch (Exception e) {
            return null;
        }
    }

    private String getResourceDescription(Object res) {
        try {
            var method = res.getClass().getMethod("getDescription");
            return (String) method.invoke(res);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String formatResult(Output output) {
        if (output.resources() == null || output.resources().isEmpty()) {
            return "No resources found. MCP servers may still provide tools even if they have no resources.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (ResourceInfo r : output.resources()) {
            if (!first) sb.append(",");
            sb.append("\n  {\"uri\":\"").append(r.uri()).append("\",");
            sb.append("\"name\":\"").append(r.name()).append("\",");
            if (r.mimeType() != null) {
                sb.append("\"mimeType\":\"").append(r.mimeType()).append("\",");
            }
            sb.append("\"server\":\"").append(r.server()).append("\"}");
            first = false;
        }
        sb.append("\n]");
        return sb.toString();
    }
}