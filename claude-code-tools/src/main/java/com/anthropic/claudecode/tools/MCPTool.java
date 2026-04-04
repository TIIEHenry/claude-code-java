/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/MCPTool/MCPTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;

/**
 * MCP Tool - execute MCP server tools.
 */
public final class MCPTool extends AbstractTool<MCPTool.Input, MCPTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "MCP";

    public MCPTool() {
        super(TOOL_NAME, "Execute MCP server tools");
    }

    /**
     * Input schema - allows any input object since MCP tools define their own schemas.
     */
    public record Input(
        Map<String, Object> fields
    ) {
        public Object get(String key) {
            return fields != null ? fields.get(key) : null;
        }
    }

    /**
     * Output schema.
     */
    public record Output(
        String result
    ) {}

    @Override
    public String description() {
        return "Execute tools from MCP (Model Context Protocol) servers";
    }

    @Override
    public String searchHint() {
        return "execute MCP server tools";
    }

    @Override
    public boolean isOpenWorld(Input input) {
        return false; // Overridden in mcpClient
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
    public CompletableFuture<PermissionResult> checkPermissions(Input input, ToolUseContext context) {
        return CompletableFuture.completedFuture(PermissionResult.ask("MCPTool requires permission.", input));
    }

    @Override
    public CompletableFuture<ToolResult<Output>> call(
        Input input,
        ToolUseContext context,
        CanUseToolFn canUseTool,
        AssistantMessage parent,
        Consumer<ToolProgress<ToolProgressData>> onProgress
    ) {
        // Overridden in mcpClient - this is a placeholder
        return CompletableFuture.completedFuture(ToolResult.of(new Output("")));
    }

    @Override
    public String formatResult(Output output) {
        return output.result() != null ? output.result() : "";
    }
}