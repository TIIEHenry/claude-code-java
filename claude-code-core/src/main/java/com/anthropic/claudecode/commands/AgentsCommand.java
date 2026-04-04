/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/agents
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Agents command - Manage agent configurations.
 */
public final class AgentsCommand implements Command {
    @Override
    public String name() {
        return "agents";
    }

    @Override
    public String description() {
        return "Manage agent configurations";
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
        if (args == null || args.isEmpty()) {
            return CompletableFuture.completedFuture(listAgents(context));
        }

        String[] parts = args.trim().split("\\s+");
        String action = parts[0].toLowerCase();

        return CompletableFuture.completedFuture(switch (action) {
            case "list", "ls" -> listAgents(context);
            case "create", "new" -> createAgent(context, parts);
            case "configure", "config" -> configureAgent(context, parts);
            case "delete", "remove" -> deleteAgent(context, parts);
            case "info", "show" -> showAgentInfo(context, parts);
            case "run", "execute" -> runAgent(context, parts);
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown action: ").append(action).append("\n\n");
                sb.append("Usage: agents [list|create|configure|delete|info|run]\n");
                yield CommandResult.failure(sb.toString());
            }
        });
    }

    private CommandResult listAgents(CommandContext context) {
        List<CommandContext.AgentConfig> agents = context.getAllAgents();

        StringBuilder sb = new StringBuilder();
        sb.append("Agent Configurations\n");
        sb.append("====================\n\n");

        if (agents.isEmpty()) {
            sb.append("No agents configured.\n\n");
            sb.append("Create an agent with: agents create <name>\n");
        } else {
            String activeAgent = context.getActiveAgent();

            sb.append("Active agent: ").append(activeAgent != null ? activeAgent : "none").append("\n\n");

            sb.append("Available agents:\n");
            for (CommandContext.AgentConfig agent : agents) {
                String marker = agent.name().equals(activeAgent) ? "* " : "  ";
                sb.append(marker).append(agent.name()).append("\n");
                sb.append("    Model: ").append(agent.model()).append("\n");
                sb.append("    Type: ").append(agent.type()).append("\n");
                sb.append("    Tools: ").append(agent.enabledTools().size()).append(" enabled\n");
            }

            sb.append("\nUsage:\n");
            sb.append("  agents create <name>  - Create a new agent\n");
            sb.append("  agents configure <name> - Configure an agent\n");
            sb.append("  agents run <name>     - Run an agent\n");
        }

        return CommandResult.success(sb.toString());
    }

    private CommandResult createAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("Please provide an agent name.\n\n");
            sb.append("Usage: agents create <name> [type]\n");
            sb.append("Types: general-purpose, statusline-setup, explore, plan\n");
            return CommandResult.failure(sb.toString());
        }

        String name = args[1];
        String type = args.length > 2 ? args[2].toLowerCase() : "general-purpose";

        if (!isValidAgentType(type)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid agent type: ").append(type).append("\n\n");
            sb.append("Valid types: general-purpose, statusline-setup, explore, plan\n");
            return CommandResult.failure(sb.toString());
        }

        CommandContext.AgentConfig agent = context.createAgent(name, type);

        StringBuilder sb = new StringBuilder();
        sb.append("Agent created.\n\n");
        sb.append("Name: ").append(agent.name()).append("\n");
        sb.append("Type: ").append(agent.type()).append("\n");
        sb.append("Model: ").append(agent.model()).append("\n");

        sb.append("\nConfigure with: agents configure ").append(name).append("\n");
        sb.append("Run with: agents run ").append(name).append("\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult configureAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("Please provide an agent name.\nUsage: agents configure <name> [setting] [value]\n");
        }

        String name = args[1];
        CommandContext.AgentConfig agent = context.getAgentConfig(name);

        if (agent == null) {
            return CommandResult.failure("Agent not found: " + name + "\n");
        }

        if (args.length < 3) {
            return showAgentConfig(context, agent);
        }

        String setting = args[2].toLowerCase();
        String value = args.length > 3 ? args[3] : "";

        context.configureAgent(name, setting, value);

        StringBuilder sb = new StringBuilder();
        sb.append("Agent configured.\n");
        sb.append(name).append(": ").append(setting).append(" = ").append(value).append("\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult showAgentConfig(CommandContext context, CommandContext.AgentConfig agent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Agent Configuration: ").append(agent.name()).append("\n");
        sb.append("========================================\n\n");

        sb.append("Name: ").append(agent.name()).append("\n");
        sb.append("Type: ").append(agent.type()).append("\n");
        sb.append("Model: ").append(agent.model()).append("\n");

        sb.append("\nEnabled Tools:\n");
        for (String tool : agent.enabledTools()) {
            sb.append("  ✓ ").append(tool).append("\n");
        }

        sb.append("\nSettings:\n");
        sb.append("  Max turns: ").append(agent.maxTurns()).append("\n");
        sb.append("  Timeout: ").append(agent.timeoutSeconds()).append(" seconds\n");
        sb.append("  Auto mode: ").append(agent.autoMode() ? "enabled" : "disabled").append("\n");

        sb.append("\nUsage:\n");
        sb.append("  agents configure ").append(agent.name()).append(" model <model>\n");
        sb.append("  agents configure ").append(agent.name()).append(" tools <tool-list>\n");
        sb.append("  agents configure ").append(agent.name()).append(" timeout <seconds>\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult deleteAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("Please provide an agent name.\nUsage: agents delete <name>\n");
        }

        String name = args[1];

        if (!context.deleteAgent(name)) {
            return CommandResult.failure("Agent not found: " + name + "\n");
        }

        return CommandResult.success("Agent deleted: " + name + "\n");
    }

    private CommandResult showAgentInfo(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("Please provide an agent name.\nUsage: agents info <name>\n");
        }

        String name = args[1];
        CommandContext.AgentConfig agent = context.getAgentConfig(name);

        if (agent == null) {
            return CommandResult.failure("Agent not found: " + name + "\n");
        }

        return showAgentConfig(context, agent);
    }

    private CommandResult runAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("Please provide an agent name.\nUsage: agents run <name> [prompt]\n");
        }

        String name = args[1];
        String prompt = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";

        CommandContext.AgentConfig agent = context.getAgentConfig(name);

        if (agent == null) {
            return CommandResult.failure("Agent not found: " + name + "\n");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Starting agent: ").append(name).append("\n\n");
        sb.append("Type: ").append(agent.type()).append("\n");
        sb.append("Model: ").append(agent.model()).append("\n");

        if (!prompt.isEmpty()) {
            sb.append("\nPrompt: ").append(prompt).append("\n");
        }

        context.startAgent(name, prompt);

        sb.append("\nAgent started. Check progress with: tasks list\n");

        return CommandResult.success(sb.toString());
    }

    private boolean isValidAgentType(String type) {
        return Set.of("general-purpose", "statusline-setup", "explore", "plan").contains(type);
    }
}