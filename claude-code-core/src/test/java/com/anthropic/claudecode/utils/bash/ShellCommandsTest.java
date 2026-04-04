/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShellCommands.
 */
class ShellCommandsTest {

    @Test
    @DisplayName("ShellCommands isRegistered true for known commands")
    void isRegisteredTrue() {
        assertTrue(ShellCommands.isRegistered("ls"));
        assertTrue(ShellCommands.isRegistered("cat"));
        assertTrue(ShellCommands.isRegistered("grep"));
        assertTrue(ShellCommands.isRegistered("git"));
    }

    @Test
    @DisplayName("ShellCommands isRegistered false for unknown commands")
    void isRegisteredFalse() {
        assertFalse(ShellCommands.isRegistered("unknowncmd123"));
        assertFalse(ShellCommands.isRegistered("foobar"));
    }

    @Test
    @DisplayName("ShellCommands getCommandInfo returns info")
    void getCommandInfo() {
        ShellCommands.CommandInfo ls = ShellCommands.getCommandInfo("ls");
        assertNotNull(ls);
        assertEquals("ls", ls.name());
        assertEquals("List directory contents", ls.description());
        assertTrue(ls.options().contains("-l"));
        assertEquals(ShellCommands.CommandCategory.FILE_OPERATION, ls.category());
    }

    @Test
    @DisplayName("ShellCommands getCommandInfo null for unknown")
    void getCommandInfoUnknown() {
        assertNull(ShellCommands.getCommandInfo("unknowncmd"));
    }

    @Test
    @DisplayName("ShellCommands isSafe true for safe commands")
    void isSafeTrue() {
        assertTrue(ShellCommands.isSafe("ls"));
        assertTrue(ShellCommands.isSafe("cat"));
        assertTrue(ShellCommands.isSafe("grep"));
        assertTrue(ShellCommands.isSafe("pwd"));
    }

    @Test
    @DisplayName("ShellCommands isSafe false for unsafe commands")
    void isSafeFalse() {
        assertFalse(ShellCommands.isSafe("rm"));
        assertFalse(ShellCommands.isSafe("mv"));
        assertFalse(ShellCommands.isSafe("cp"));
    }

    @Test
    @DisplayName("ShellCommands isDestructive true for destructive commands")
    void isDestructiveTrue() {
        assertTrue(ShellCommands.isDestructive("rm"));
        assertTrue(ShellCommands.isDestructive("mv"));
        assertTrue(ShellCommands.isDestructive("cp"));
        assertTrue(ShellCommands.isDestructive("mkdir"));
    }

    @Test
    @DisplayName("ShellCommands isDestructive false for non-destructive commands")
    void isDestructiveFalse() {
        assertFalse(ShellCommands.isDestructive("ls"));
        assertFalse(ShellCommands.isDestructive("cat"));
        assertFalse(ShellCommands.isDestructive("grep"));
    }

    @Test
    @DisplayName("ShellCommands getAllCommands returns map")
    void getAllCommands() {
        Map<String, ShellCommands.CommandInfo> commands = ShellCommands.getAllCommands();
        assertFalse(commands.isEmpty());
        assertTrue(commands.containsKey("ls"));
        assertTrue(commands.containsKey("git"));
    }

    @Test
    @DisplayName("ShellCommands getCommandsByCategory returns filtered list")
    void getCommandsByCategory() {
        List<ShellCommands.CommandInfo> fileOps = ShellCommands.getCommandsByCategory(
            ShellCommands.CommandCategory.FILE_OPERATION
        );
        assertFalse(fileOps.isEmpty());
        assertTrue(fileOps.stream().allMatch(c -> c.category() == ShellCommands.CommandCategory.FILE_OPERATION));
    }

    @Test
    @DisplayName("ShellCommands getDestructiveCommands returns list")
    void getDestructiveCommands() {
        List<String> destructive = ShellCommands.getDestructiveCommands();
        assertFalse(destructive.isEmpty());
        assertTrue(destructive.contains("rm"));
        assertTrue(destructive.contains("mv"));
    }

    @Test
    @DisplayName("ShellCommands getSafeCommands returns list")
    void getSafeCommands() {
        List<String> safe = ShellCommands.getSafeCommands();
        assertFalse(safe.isEmpty());
        assertTrue(safe.contains("ls"));
        assertTrue(safe.contains("cat"));
    }

    @Test
    @DisplayName("ShellCommands register adds new command")
    void registerNewCommand() {
        ShellCommands.CommandInfo newCmd = new ShellCommands.CommandInfo(
            "mycmd",
            "My custom command",
            List.of("-a", "-b"),
            ShellCommands.CommandCategory.OTHER,
            true,
            false
        );
        ShellCommands.register(newCmd);

        assertTrue(ShellCommands.isRegistered("mycmd"));
        assertEquals("My custom command", ShellCommands.getCommandInfo("mycmd").description());
    }

    @Test
    @DisplayName("ShellCommands CommandInfo isReadOnly")
    void commandInfoIsReadOnly() {
        ShellCommands.CommandInfo ls = ShellCommands.getCommandInfo("ls");
        assertTrue(ls.isReadOnly());

        ShellCommands.CommandInfo rm = ShellCommands.getCommandInfo("rm");
        assertFalse(rm.isReadOnly());
    }

    @Test
    @DisplayName("ShellCommands CommandCategory enum values")
    void commandCategoryEnum() {
        ShellCommands.CommandCategory[] categories = ShellCommands.CommandCategory.values();
        assertEquals(9, categories.length);
        assertEquals(ShellCommands.CommandCategory.FILE_OPERATION, ShellCommands.CommandCategory.valueOf("FILE_OPERATION"));
        assertEquals(ShellCommands.CommandCategory.PROCESS_MANAGEMENT, ShellCommands.CommandCategory.valueOf("PROCESS_MANAGEMENT"));
        assertEquals(ShellCommands.CommandCategory.NETWORK, ShellCommands.CommandCategory.valueOf("NETWORK"));
        assertEquals(ShellCommands.CommandCategory.SYSTEM_INFO, ShellCommands.CommandCategory.valueOf("SYSTEM_INFO"));
        assertEquals(ShellCommands.CommandCategory.TEXT_PROCESSING, ShellCommands.CommandCategory.valueOf("TEXT_PROCESSING"));
        assertEquals(ShellCommands.CommandCategory.PACKAGE_MANAGEMENT, ShellCommands.CommandCategory.valueOf("PACKAGE_MANAGEMENT"));
        assertEquals(ShellCommands.CommandCategory.VERSION_CONTROL, ShellCommands.CommandCategory.valueOf("VERSION_CONTROL"));
        assertEquals(ShellCommands.CommandCategory.DEVELOPMENT, ShellCommands.CommandCategory.valueOf("DEVELOPMENT"));
        assertEquals(ShellCommands.CommandCategory.OTHER, ShellCommands.CommandCategory.valueOf("OTHER"));
    }

    @Test
    @DisplayName("ShellCommands CommandInfo record")
    void commandInfoRecord() {
        ShellCommands.CommandInfo info = new ShellCommands.CommandInfo(
            "test",
            "Test command",
            List.of("-a", "-b"),
            ShellCommands.CommandCategory.OTHER,
            false,
            true
        );
        assertEquals("test", info.name());
        assertEquals("Test command", info.description());
        assertEquals(2, info.options().size());
        assertEquals(ShellCommands.CommandCategory.OTHER, info.category());
        assertFalse(info.isSafe());
        assertTrue(info.isDestructive());
    }
}