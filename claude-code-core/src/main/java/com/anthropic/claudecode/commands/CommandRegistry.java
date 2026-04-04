/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/registry
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all available commands.
 */
public final class CommandRegistry {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    public CommandRegistry() {
        registerBuiltins();
    }

    private void registerBuiltins() {
        // Core commands
        register(new InitCommand());
        register(new VersionCommand());
        register(new ClearCommand());
        register(new HelpCommand(this));
        register(new AddDirCommand());
        register(new CopyCommand());

        // Git commands
        register(new CommitCommand());
        register(new BranchCommand());
        register(new DiffCommand());

        // Configuration commands
        register(new ConfigCommand());
        register(new ModelCommand());

        // Stats commands
        register(new CostCommand());
        register(new StatsCommand());
        register(new UsageCommand());

        // Utility commands
        register(new CompactCommand());
        register(new ReviewCommand());
        register(new ExitCommand());
        register(new FastCommand());
        register(new MemoryCommand());
        register(new ContextCommand());
        register(new RenameCommand());
        register(new TasksCommand());

        // Diagnostic commands
        register(new DoctorCommand());
        register(new StatusCommand());

        // Auth commands
        register(new LoginCommand());
        register(new LogoutCommand());
        register(new UpgradeCommand());

        // Session commands
        register(new ResumeCommand());
        register(new RewindCommand());
        register(new ExportCommand());
        register(new SessionCommand());

        // MCP commands
        register(new McpCommand());

        // Skills commands
        register(new SkillsCommand());

        // Feedback commands
        register(new FeedbackCommand());

        // Permission commands
        register(new PermissionsCommand());
        register(new HooksCommand());

        // Plan and theme commands
        register(new PlanCommand());
        register(new ThemeCommand());
        register(new TerminalSetupCommand());

        // Model configuration commands
        register(new AdvisorCommand());
        register(new EffortCommand());

        // UI/UX commands
        register(new ColorCommand());
        register(new VimCommand());
        register(new KeybindingsCommand());

        // Development commands
        register(new FilesCommand());
        register(new HeapdumpCommand());
        register(new IdeCommand());

        // User engagement commands
        register(new StickersCommand());
        register(new ReleaseNotesCommand());

        // Session management commands
        register(new TagCommand());
        register(new SandboxCommand());
        register(new DesktopCommand());

        // Privacy and access commands
        register(new PrivacySettingsCommand());
        register(new PassesCommand());

        // Insights command
        register(new InsightsCommand());

        // Agent commands
        register(new AgentsCommand());
        register(new BriefCommand());
        register(new BtwCommand());
    }

    /**
     * Register a command.
     */
    public void register(Command command) {
        commands.put(command.name().toLowerCase(), command);

        // Register aliases
        for (String alias : command.aliases()) {
            aliases.put(alias.toLowerCase(), command.name().toLowerCase());
        }
    }

    /**
     * Unregister a command.
     */
    public void unregister(String name) {
        String normalizedName = name.toLowerCase();
        Command removed = commands.remove(normalizedName);
        
        if (removed != null) {
            // Remove aliases
            aliases.entrySet().removeIf(e -> e.getValue().equals(normalizedName));
        }
    }

    /**
     * Get a command by name or alias.
     */
    public Command get(String name) {
        String normalizedName = name.toLowerCase();
        
        // Check direct name
        Command cmd = commands.get(normalizedName);
        if (cmd != null) {
            return cmd;
        }

        // Check alias
        String aliasedName = aliases.get(normalizedName);
        if (aliasedName != null) {
            return commands.get(aliasedName);
        }

        return null;
    }

    /**
     * Check if a command exists.
     */
    public boolean exists(String name) {
        return get(name) != null;
    }

    /**
     * Get all registered commands.
     */
    public Collection<Command> getAllCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    /**
     * Get all command names.
     */
    public Set<String> getCommandNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    /**
     * Get all aliases.
     */
    public Map<String, String> getAliases() {
        return Collections.unmodifiableMap(aliases);
    }

    /**
     * Execute a command by name.
     */
    public CompletableFuture<CommandResult> execute(String name, String args, CommandContext context) {
        Command command = get(name);

        if (command == null) {
            return CompletableFuture.completedFuture(
                CommandResult.failure("Unknown command: " + name)
            );
        }

        if (!command.isEnabled()) {
            return CompletableFuture.completedFuture(
                CommandResult.failure("Command is disabled: " + name)
            );
        }

        try {
            return command.execute(args, context);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                CommandResult.failure("Command failed: " + e.getMessage())
            );
        }
    }
}
