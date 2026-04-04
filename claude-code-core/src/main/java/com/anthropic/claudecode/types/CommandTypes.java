/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code types/command.ts
 */
package com.anthropic.claudecode.types;

import java.util.*;
import java.util.function.Supplier;

/**
 * Command types for slash commands and skills.
 */
public final class CommandTypes {
    private CommandTypes() {}

    /**
     * Command availability types.
     */
    public enum CommandAvailability {
        CLAUDE_AI,  // claude.ai OAuth subscriber
        CONSOLE     // Console API key user
    }

    /**
     * Command result display type.
     */
    public enum CommandResultDisplay {
        SKIP,
        SYSTEM,
        USER
    }

    /**
     * Resume entrypoint type.
     */
    public enum ResumeEntrypoint {
        CLI_FLAG,
        SLASH_COMMAND_PICKER,
        SLASH_COMMAND_SESSION_ID,
        SLASH_COMMAND_TITLE,
        FORK
    }

    /**
     * Local command result.
     */
    public sealed interface LocalCommandResult permits
        LocalCommandResult.Text,
        LocalCommandResult.Compact,
        LocalCommandResult.Skip {

        record Text(String value) implements LocalCommandResult {}
        record Compact(CompactResult compactionResult, String displayText) implements LocalCommandResult {}
        record Skip() implements LocalCommandResult {}
    }

    /**
     * Compact result placeholder.
     */
    public record CompactResult(
        boolean success,
        int tokensBefore,
        int tokensAfter,
        String summary
    ) {}

    /**
     * Command type enum.
     */
    public enum CommandType {
        PROMPT,
        LOCAL,
        LOCAL_JSX
    }

    /**
     * Command base interface.
     */
    public interface CommandBase {
        String name();
        String description();
        List<String> aliases();
        List<CommandAvailability> availability();
        boolean isEnabled();
        boolean isHidden();
        boolean isMcp();
        String argumentHint();
        String whenToUse();
        String version();
        boolean isUserInvocable();
    }

    /**
     * Prompt command.
     */
    public record PromptCommand(
        String name,
        String description,
        String progressMessage,
        int contentLength,
        List<String> argNames,
        List<String> allowedTools,
        String model,
        String source,
        List<String> aliases,
        boolean isHidden
    ) implements CommandBase {
        @Override
        public List<CommandAvailability> availability() { return List.of(); }

        @Override
        public boolean isEnabled() { return true; }

        @Override
        public boolean isMcp() { return false; }

        @Override
        public String argumentHint() { return null; }

        @Override
        public String whenToUse() { return null; }

        @Override
        public String version() { return null; }

        @Override
        public boolean isUserInvocable() { return true; }
    }

    /**
     * Local command.
     */
    public record LocalCommand(
        String name,
        String description,
        boolean supportsNonInteractive,
        List<String> aliases
    ) implements CommandBase {
        @Override
        public List<CommandAvailability> availability() { return List.of(); }

        @Override
        public boolean isEnabled() { return true; }

        @Override
        public boolean isHidden() { return false; }

        @Override
        public boolean isMcp() { return false; }

        @Override
        public String argumentHint() { return null; }

        @Override
        public String whenToUse() { return null; }

        @Override
        public String version() { return null; }

        @Override
        public boolean isUserInvocable() { return true; }
    }

    /**
     * Get command name from base.
     */
    public static String getCommandName(CommandBase cmd) {
        return cmd.name();
    }

    /**
     * Check if command is enabled.
     */
    public static boolean isCommandEnabled(CommandBase cmd) {
        return cmd.isEnabled();
    }
}