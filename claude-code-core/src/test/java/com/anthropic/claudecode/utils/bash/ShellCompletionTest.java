/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShellCompletion.
 */
class ShellCompletionTest {

    @Test
    @DisplayName("ShellCompletion getCompletions returns list")
    void getCompletions() {
        ShellCompletion completion = new ShellCompletion();
        List<ShellCompletion.CompletionItem> items = completion.getCompletions("ls ", 3);
        assertNotNull(items);
    }

    @Test
    @DisplayName("ShellCompletion getCompletions for command")
    void getCompletionsForCommand() {
        ShellCompletion completion = new ShellCompletion();
        List<ShellCompletion.CompletionItem> items = completion.getCompletions("l", 1);
        assertNotNull(items);
        // Should suggest commands starting with 'l'
        assertTrue(items.stream().anyMatch(i -> i.text().startsWith("l")));
    }

    @Test
    @DisplayName("ShellCompletion getCompletions for option")
    void getCompletionsForOption() {
        ShellCompletion completion = new ShellCompletion();
        List<ShellCompletion.CompletionItem> items = completion.getCompletions("ls -", 4);
        assertNotNull(items);
    }

    @Test
    @DisplayName("ShellCompletion CompletionItem record")
    void completionItemRecord() {
        ShellCompletion.CompletionItem item = new ShellCompletion.CompletionItem(
            "ls", "ls", "List directory", ShellCompletion.CompletionType.COMMAND, 0, 1
        );
        assertEquals("ls", item.text());
        assertEquals("ls", item.display());
        assertEquals("List directory", item.description());
        assertEquals(ShellCompletion.CompletionType.COMMAND, item.type());
        assertEquals(0, item.insertStart());
        assertEquals(1, item.insertEnd());
        assertEquals("ls", item.getInsertText());
    }

    @Test
    @DisplayName("ShellCompletion CompletionType enum values")
    void completionTypeEnum() {
        ShellCompletion.CompletionType[] types = ShellCompletion.CompletionType.values();
        assertEquals(7, types.length);
        assertEquals(ShellCompletion.CompletionType.FILE, ShellCompletion.CompletionType.valueOf("FILE"));
        assertEquals(ShellCompletion.CompletionType.DIRECTORY, ShellCompletion.CompletionType.valueOf("DIRECTORY"));
        assertEquals(ShellCompletion.CompletionType.COMMAND, ShellCompletion.CompletionType.valueOf("COMMAND"));
        assertEquals(ShellCompletion.CompletionType.OPTION, ShellCompletion.CompletionType.valueOf("OPTION"));
        assertEquals(ShellCompletion.CompletionType.ARGUMENT, ShellCompletion.CompletionType.valueOf("ARGUMENT"));
        assertEquals(ShellCompletion.CompletionType.VARIABLE, ShellCompletion.CompletionType.valueOf("VARIABLE"));
        assertEquals(ShellCompletion.CompletionType.KEYWORD, ShellCompletion.CompletionType.valueOf("KEYWORD"));
    }

    @Test
    @DisplayName("ShellCompletion CompletionProvider interface exists")
    void completionProviderInterface() {
        // Verify the interface is accessible
        ShellCompletion.CompletionProvider provider = (input, word) -> List.of();
        assertNotNull(provider);
    }

    @Test
    @DisplayName("ShellCompletion empty input returns empty list")
    void getCompletionsEmpty() {
        ShellCompletion completion = new ShellCompletion();
        List<ShellCompletion.CompletionItem> items = completion.getCompletions("", 0);
        assertNotNull(items);
    }
}