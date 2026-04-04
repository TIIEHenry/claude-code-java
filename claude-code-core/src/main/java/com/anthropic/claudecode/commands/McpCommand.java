/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/mcp
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MCP command - Manage MCP (Model Context Protocol) servers.
 */
public final class McpCommand implements Command {
    @Override
    public String name() {
        return "mcp";
    }

    @Override
    public String description() {
        return "Manage MCP servers";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();

        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(showMcpStatus(context, sb));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        switch (action) {
            case "enable":
                return CompletableFuture.completedFuture(enableMcpServer(context, parts, sb));
            case "disable":
                return CompletableFuture.completedFuture(disableMcpServer(context, parts, sb));
            case "list":
                return CompletableFuture.completedFuture(listMcpServers(context, sb));
            case "status":
                return CompletableFuture.completedFuture(showMcpStatus(context, sb));
            case "reconnect":
                return CompletableFuture.completedFuture(reconnectMcpServer(context, parts, sb));
            default:
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Available actions:\n");
                sb.append("  enable [server]  - Enable an MCP server\n");
                sb.append("  disable [server] - Disable an MCP server\n");
                sb.append("  list             - List all MCP servers\n");
                sb.append("  status           - Show MCP status\n");
                sb.append("  reconnect <name> - Reconnect to a server\n");
                return CompletableFuture.completedFuture(CommandResult.failure(sb.toString()));
        }
    }

    private CommandResult showMcpStatus(CommandContext context, StringBuilder sb) {
        sb.append("MCP Server Status\n");
        sb.append("=================\n\n");

        Map<String, Object> servers = context.getMcpServers();

        if (servers.isEmpty()) {
            sb.append("No MCP servers configured.\n\n");
            sb.append("To add an MCP server, edit your .claude/settings.json or CLAUDE.md:\n");
            sb.append("  mcpServers:\n");
            sb.append("    my-server:\n");
            sb.append("      command: node\n");
            sb.append("      args: [\"server.js\"]\n");
            return CommandResult.success(sb.toString());
        }

        int enabled = 0;
        int disabled = 0;

        for (Map.Entry<String, Object> entry : servers.entrySet()) {
            sb.append("• ").append(entry.getKey()).append("\n");
            sb.append("  Status: configured\n\n");
            enabled++;
        }

        sb.append("Summary: ").append(enabled).append(" configured\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult listMcpServers(CommandContext context, StringBuilder sb) {
        sb.append("MCP Servers\n");
        sb.append("===========\n\n");

        Map<String, Object> servers = context.getMcpServers();

        if (servers.isEmpty()) {
            sb.append("No MCP servers configured.\n");
            return CommandResult.success(sb.toString());
        }

        for (String name : servers.keySet()) {
            sb.append("• ").append(name).append("\n");
        }

        return CommandResult.success(sb.toString());
    }

    private CommandResult enableMcpServer(CommandContext context, String[] args, StringBuilder sb) {
        if (args.length < 2) {
            return CommandResult.failure("Usage: mcp enable <server-name>");
        }

        String target = args[1];
        context.toggleMcpServer(target, true);

        sb.append("Enabled MCP server: ").append(target).append("\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult disableMcpServer(CommandContext context, String[] args, StringBuilder sb) {
        if (args.length < 2) {
            return CommandResult.failure("Usage: mcp disable <server-name>");
        }

        String target = args[1];
        context.toggleMcpServer(target, false);

        sb.append("Disabled MCP server: ").append(target).append("\n");
        return CommandResult.success(sb.toString());
    }

    private CommandResult reconnectMcpServer(CommandContext context, String[] args, StringBuilder sb) {
        if (args.length < 2) {
            return CommandResult.failure("Usage: mcp reconnect <server-name>");
        }

        String serverName = args[1];
        context.reconnectMcpServer(serverName);

        sb.append("Reconnected to MCP server: ").append(serverName).append("\n");
        return CommandResult.success(sb.toString());
    }
}