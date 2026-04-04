/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandTypes.
 */
class CommandTypesTest {

    @Test
    @DisplayName("CommandTypes CommandAvailability enum values")
    void commandAvailabilityEnum() {
        CommandTypes.CommandAvailability[] values = CommandTypes.CommandAvailability.values();
        assertEquals(2, values.length);
        assertEquals(CommandTypes.CommandAvailability.CLAUDE_AI, CommandTypes.CommandAvailability.valueOf("CLAUDE_AI"));
        assertEquals(CommandTypes.CommandAvailability.CONSOLE, CommandTypes.CommandAvailability.valueOf("CONSOLE"));
    }

    @Test
    @DisplayName("CommandTypes CommandResultDisplay enum values")
    void commandResultDisplayEnum() {
        CommandTypes.CommandResultDisplay[] values = CommandTypes.CommandResultDisplay.values();
        assertEquals(3, values.length);
        assertEquals(CommandTypes.CommandResultDisplay.SKIP, CommandTypes.CommandResultDisplay.valueOf("SKIP"));
        assertEquals(CommandTypes.CommandResultDisplay.SYSTEM, CommandTypes.CommandResultDisplay.valueOf("SYSTEM"));
        assertEquals(CommandTypes.CommandResultDisplay.USER, CommandTypes.CommandResultDisplay.valueOf("USER"));
    }

    @Test
    @DisplayName("CommandTypes ResumeEntrypoint enum values")
    void resumeEntrypointEnum() {
        CommandTypes.ResumeEntrypoint[] values = CommandTypes.ResumeEntrypoint.values();
        assertEquals(5, values.length);
        assertEquals(CommandTypes.ResumeEntrypoint.CLI_FLAG, CommandTypes.ResumeEntrypoint.valueOf("CLI_FLAG"));
        assertEquals(CommandTypes.ResumeEntrypoint.SLASH_COMMAND_PICKER, CommandTypes.ResumeEntrypoint.valueOf("SLASH_COMMAND_PICKER"));
        assertEquals(CommandTypes.ResumeEntrypoint.FORK, CommandTypes.ResumeEntrypoint.valueOf("FORK"));
    }

    @Test
    @DisplayName("CommandTypes CommandType enum values")
    void commandTypeEnum() {
        CommandTypes.CommandType[] values = CommandTypes.CommandType.values();
        assertEquals(3, values.length);
        assertEquals(CommandTypes.CommandType.PROMPT, CommandTypes.CommandType.valueOf("PROMPT"));
        assertEquals(CommandTypes.CommandType.LOCAL, CommandTypes.CommandType.valueOf("LOCAL"));
        assertEquals(CommandTypes.CommandType.LOCAL_JSX, CommandTypes.CommandType.valueOf("LOCAL_JSX"));
    }

    @Test
    @DisplayName("CommandTypes LocalCommandResult.Text record")
    void localCommandResultText() {
        CommandTypes.LocalCommandResult.Text text = new CommandTypes.LocalCommandResult.Text("Hello");
        assertEquals("Hello", text.value());
        assertTrue(text instanceof CommandTypes.LocalCommandResult);
    }

    @Test
    @DisplayName("CommandTypes LocalCommandResult.Compact record")
    void localCommandResultCompact() {
        CommandTypes.CompactResult compactResult = new CommandTypes.CompactResult(
            true, 1000, 500, "Summary"
        );
        CommandTypes.LocalCommandResult.Compact compact = new CommandTypes.LocalCommandResult.Compact(
            compactResult, "Display text"
        );

        assertTrue(compact.compactionResult().success());
        assertEquals(1000, compact.compactionResult().tokensBefore());
        assertEquals(500, compact.compactionResult().tokensAfter());
        assertEquals("Summary", compact.compactionResult().summary());
        assertEquals("Display text", compact.displayText());
    }

    @Test
    @DisplayName("CommandTypes LocalCommandResult.Skip record")
    void localCommandResultSkip() {
        CommandTypes.LocalCommandResult.Skip skip = new CommandTypes.LocalCommandResult.Skip();
        assertTrue(skip instanceof CommandTypes.LocalCommandResult);
    }

    @Test
    @DisplayName("CommandTypes CompactResult record")
    void compactResultRecord() {
        CommandTypes.CompactResult result = new CommandTypes.CompactResult(
            false, 2000, 1800, "Test summary"
        );

        assertFalse(result.success());
        assertEquals(2000, result.tokensBefore());
        assertEquals(1800, result.tokensAfter());
        assertEquals("Test summary", result.summary());
    }

    @Test
    @DisplayName("CommandTypes PromptCommand record")
    void promptCommandRecord() {
        CommandTypes.PromptCommand cmd = new CommandTypes.PromptCommand(
            "test", "Test command", "Processing...", 100,
            List.of("arg1"), List.of("Bash"), "sonnet",
            "built-in", List.of("t"), false
        );

        assertEquals("test", cmd.name());
        assertEquals("Test command", cmd.description());
        assertEquals("Processing...", cmd.progressMessage());
        assertEquals(100, cmd.contentLength());
        assertEquals(1, cmd.argNames().size());
        assertEquals(1, cmd.allowedTools().size());
        assertEquals("sonnet", cmd.model());
        assertEquals("built-in", cmd.source());
        assertEquals(1, cmd.aliases().size());
        assertFalse(cmd.isHidden());
        assertTrue(cmd.isEnabled());
        assertTrue(cmd.isUserInvocable());
        assertFalse(cmd.isMcp());
    }

    @Test
    @DisplayName("CommandTypes LocalCommand record")
    void localCommandRecord() {
        CommandTypes.LocalCommand cmd = new CommandTypes.LocalCommand(
            "local", "Local command", true, List.of("l")
        );

        assertEquals("local", cmd.name());
        assertEquals("Local command", cmd.description());
        assertTrue(cmd.supportsNonInteractive());
        assertEquals(1, cmd.aliases().size());
        assertTrue(cmd.isEnabled());
        assertFalse(cmd.isHidden());
        assertFalse(cmd.isMcp());
    }

    @Test
    @DisplayName("CommandTypes getCommandName returns name")
    void getCommandName() {
        CommandTypes.PromptCommand cmd = new CommandTypes.PromptCommand(
            "mycommand", "desc", null, 0,
            List.of(), List.of(), null, null, List.of(), false
        );

        assertEquals("mycommand", CommandTypes.getCommandName(cmd));
    }

    @Test
    @DisplayName("CommandTypes isCommandEnabled returns true for enabled command")
    void isCommandEnabled() {
        CommandTypes.LocalCommand cmd = new CommandTypes.LocalCommand(
            "enabled", "desc", false, List.of()
        );

        assertTrue(CommandTypes.isCommandEnabled(cmd));
    }
}