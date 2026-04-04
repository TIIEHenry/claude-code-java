/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConsoleDisplay.
 */
class ConsoleDisplayTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Reset any state if needed
    }

    @Test
    @DisplayName("ConsoleDisplay progressBar basic")
    void progressBarBasic() {
        String bar = ConsoleDisplay.progressBar(0.5, 10);
        assertTrue(bar.contains("["));
        assertTrue(bar.contains("]"));
        assertTrue(bar.contains("="));
        assertTrue(bar.contains(" "));
    }

    @Test
    @DisplayName("ConsoleDisplay progressBar zero")
    void progressBarZero() {
        String bar = ConsoleDisplay.progressBar(0, 10);
        assertEquals("[          ]", bar);
    }

    @Test
    @DisplayName("ConsoleDisplay progressBar full")
    void progressBarFull() {
        String bar = ConsoleDisplay.progressBar(1.0, 10);
        assertEquals("[==========]", bar);
    }

    @Test
    @DisplayName("ConsoleDisplay progressBar with label")
    void progressBarWithLabel() {
        String bar = ConsoleDisplay.progressBar(0.5, 10, "Progress");
        assertTrue(bar.contains("Progress"));
        assertTrue(bar.contains("50.0%"));
    }

    @Test
    @DisplayName("ConsoleDisplay SPINNER_FRAMES constant")
    void spinnerFramesConstant() {
        assertEquals(10, ConsoleDisplay.SPINNER_FRAMES.length);
    }

    @Test
    @DisplayName("ConsoleDisplay spinner returns frame")
    void spinnerReturnsFrame() {
        String frame = ConsoleDisplay.spinner(0);
        assertNotNull(frame);
        assertTrue(ConsoleDisplay.SPINNER_FRAMES[0].equals(frame));
    }

    @Test
    @DisplayName("ConsoleDisplay spinner wraps around")
    void spinnerWrapsAround() {
        String frame10 = ConsoleDisplay.spinner(10);
        String frame0 = ConsoleDisplay.spinner(0);
        assertEquals(frame0, frame10);
    }

    @Test
    @DisplayName("ConsoleDisplay table empty")
    void tableEmpty() {
        String table = ConsoleDisplay.table().build();
        assertEquals("", table);
    }

    @Test
    @DisplayName("ConsoleDisplay table with headers")
    void tableWithHeaders() {
        String table = ConsoleDisplay.table()
            .headers("Name", "Value")
            .build();

        assertTrue(table.contains("Name"));
        assertTrue(table.contains("Value"));
        assertTrue(table.contains("┌"));
        assertTrue(table.contains("┐"));
    }

    @Test
    @DisplayName("ConsoleDisplay table with rows")
    void tableWithRows() {
        String table = ConsoleDisplay.table()
            .headers("A", "B")
            .row("1", "2")
            .row("3", "4")
            .build();

        assertTrue(table.contains("1"));
        assertTrue(table.contains("2"));
        assertTrue(table.contains("3"));
        assertTrue(table.contains("4"));
    }

    @Test
    @DisplayName("ConsoleDisplay table padding")
    void tablePadding() {
        String table = ConsoleDisplay.table()
            .headers("A")
            .padding(2)
            .build();

        assertNotNull(table);
    }

    @Test
    @DisplayName("ConsoleDisplay tree basic")
    void treeBasic() {
        String tree = ConsoleDisplay.tree("root", List.of("child1", "child2"));
        assertTrue(tree.contains("root"));
        assertTrue(tree.contains("child1"));
        assertTrue(tree.contains("child2"));
        assertTrue(tree.contains("├──"));
        assertTrue(tree.contains("└──"));
    }

    @Test
    @DisplayName("ConsoleDisplay tree single child")
    void treeSingleChild() {
        String tree = ConsoleDisplay.tree("root", List.of("only"));
        assertTrue(tree.contains("└──"));
        assertFalse(tree.contains("├──"));
    }

    @Test
    @DisplayName("ConsoleDisplay histogram")
    void histogram() {
        Map<String, Integer> data = Map.of("A", 10, "B", 5);
        String hist = ConsoleDisplay.histogram(data, 20);

        assertTrue(hist.contains("A"));
        assertTrue(hist.contains("B"));
        assertTrue(hist.contains("█"));
        assertTrue(hist.contains("10"));
        assertTrue(hist.contains("5"));
    }

    @Test
    @DisplayName("ConsoleDisplay keyValue")
    void keyValue() {
        Map<String, Object> data = Map.of("key1", "value1", "key2", 123);
        String kv = ConsoleDisplay.keyValue(data);

        assertTrue(kv.contains("key1"));
        assertTrue(kv.contains("value1"));
        assertTrue(kv.contains("key2"));
        assertTrue(kv.contains("123"));
    }

    @Test
    @DisplayName("ConsoleDisplay list")
    void list() {
        String list = ConsoleDisplay.list(List.of("a", "b", "c"));
        assertTrue(list.contains("•"));
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains("c"));
    }

    @Test
    @DisplayName("ConsoleDisplay numberedList")
    void numberedList() {
        String list = ConsoleDisplay.numberedList(List.of("first", "second", "third"));
        assertTrue(list.contains("1."));
        assertTrue(list.contains("2."));
        assertTrue(list.contains("3."));
        assertTrue(list.contains("first"));
        assertTrue(list.contains("second"));
        assertTrue(list.contains("third"));
    }

    @Test
    @DisplayName("ConsoleDisplay columns")
    void columns() {
        String cols = ConsoleDisplay.columns(List.of("a", "b", "c", "d"), 10, 20);
        assertNotNull(cols);
        assertTrue(cols.contains("a"));
        assertTrue(cols.contains("b"));
    }

    @Test
    @DisplayName("ConsoleDisplay box")
    void box() {
        String box = ConsoleDisplay.box("Hello");
        assertTrue(box.contains("┌"));
        assertTrue(box.contains("┐"));
        assertTrue(box.contains("└"));
        assertTrue(box.contains("┘"));
        assertTrue(box.contains("Hello"));
    }

    @Test
    @DisplayName("ConsoleDisplay box multiline")
    void boxMultiline() {
        String box = ConsoleDisplay.box("Line1\nLine2");
        assertTrue(box.contains("Line1"));
        assertTrue(box.contains("Line2"));
    }

    @Test
    @DisplayName("ConsoleDisplay divider")
    void divider() {
        String div = ConsoleDisplay.divider('-', 10);
        assertEquals("----------", div);
    }

    @Test
    @DisplayName("ConsoleDisplay center")
    void center() {
        String centered = ConsoleDisplay.center("test", 10);
        assertTrue(centered.contains("test"));
        assertEquals(10, centered.length());
    }

    @Test
    @DisplayName("ConsoleDisplay center longer text")
    void centerLongerText() {
        String centered = ConsoleDisplay.center("verylongtext", 5);
        assertEquals("verylongtext", centered);
    }
}