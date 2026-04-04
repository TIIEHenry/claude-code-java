/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BuilderUtils.
 */
class BuilderUtilsTest {

    @Test
    @DisplayName("BuilderUtils mapBuilder builds map")
    void mapBuilderWorks() {
        BuilderUtils.MapBuilder<String, Integer> builder = BuilderUtils.mapBuilder();
        builder.put("a", 1);
        builder.put("b", 2);
        Map<String, Integer> map = builder.build();

        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    @DisplayName("BuilderUtils mapBuilder putAll works")
    void mapBuilderPutAll() {
        Map<String, Integer> other = Map.of("x", 10, "y", 20);

        BuilderUtils.MapBuilder<String, Integer> builder = BuilderUtils.mapBuilder();
        builder.putAll(other);
        Map<String, Integer> map = builder.build();

        assertEquals(2, map.size());
    }

    @Test
    @DisplayName("BuilderUtils mapBuilder putIfPresent works")
    void mapBuilderPutIfPresent() {
        BuilderUtils.MapBuilder<String, Integer> builder = BuilderUtils.mapBuilder();
        builder.putIfPresent("present", 1);
        builder.putIfPresent("absent", null);
        Map<String, Integer> map = builder.build();

        assertEquals(1, map.size());
        assertTrue(map.containsKey("present"));
        assertFalse(map.containsKey("absent"));
    }

    @Test
    @DisplayName("BuilderUtils mapBuilder putIf works")
    void mapBuilderPutIf() {
        BuilderUtils.MapBuilder<String, Integer> builder = BuilderUtils.mapBuilder();
        builder.putIf(true, "included", 1);
        builder.putIf(false, "excluded", 2);
        Map<String, Integer> map = builder.build();

        assertEquals(1, map.size());
        assertTrue(map.containsKey("included"));
    }

    @Test
    @DisplayName("BuilderUtils mapBuilder buildUnmodifiable returns unmodifiable")
    void mapBuilderUnmodifiable() {
        BuilderUtils.MapBuilder<String, Integer> builder = BuilderUtils.mapBuilder();
        builder.put("a", 1);
        Map<String, Integer> map = builder.buildUnmodifiable();

        assertThrows(UnsupportedOperationException.class, () -> map.put("b", 2));
    }

    @Test
    @DisplayName("BuilderUtils listBuilder builds list")
    void listBuilderWorks() {
        BuilderUtils.ListBuilder<String> builder = BuilderUtils.listBuilder();
        builder.add("a");
        builder.add("b");
        List<String> list = builder.build();

        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
    }

    @Test
    @DisplayName("BuilderUtils listBuilder addAll works")
    void listBuilderAddAll() {
        List<String> other = List.of("x", "y");

        BuilderUtils.ListBuilder<String> builder = BuilderUtils.listBuilder();
        builder.addAll(other);
        List<String> list = builder.build();

        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("BuilderUtils listBuilder addIf works")
    void listBuilderAddIf() {
        BuilderUtils.ListBuilder<String> builder = BuilderUtils.listBuilder();
        builder.addIf(true, "included");
        builder.addIf(false, "excluded");
        List<String> list = builder.build();

        assertEquals(1, list.size());
        assertEquals("included", list.get(0));
    }

    @Test
    @DisplayName("BuilderUtils listBuilder addIfPresent works")
    void listBuilderAddIfPresent() {
        BuilderUtils.ListBuilder<String> builder = BuilderUtils.listBuilder();
        builder.addIfPresent("present");
        builder.addIfPresent(null);
        List<String> list = builder.build();

        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("BuilderUtils listBuilder buildUnmodifiable returns unmodifiable")
    void listBuilderUnmodifiable() {
        BuilderUtils.ListBuilder<String> builder = BuilderUtils.listBuilder();
        builder.add("a");
        List<String> list = builder.buildUnmodifiable();

        assertThrows(UnsupportedOperationException.class, () -> list.add("b"));
    }

    @Test
    @DisplayName("BuilderUtils stringBuilder builds string")
    void stringBuilderWorks() {
        String result = BuilderUtils.stringBuilder()
            .append("Hello")
            .append(" ")
            .append("World")
            .toString();

        assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("BuilderUtils stringBuilder append with format")
    void stringBuilderFormat() {
        String result = BuilderUtils.stringBuilder()
            .append("Value: %d", 42)
            .toString();

        assertEquals("Value: 42", result);
    }

    @Test
    @DisplayName("BuilderUtils stringBuilder appendIf works")
    void stringBuilderAppendIf() {
        String result = BuilderUtils.stringBuilder()
            .appendIf(true, "included")
            .appendIf(false, "excluded")
            .toString();

        assertEquals("included", result);
    }

    @Test
    @DisplayName("BuilderUtils stringBuilder appendLine works")
    void stringBuilderAppendLine() {
        String result = BuilderUtils.stringBuilder()
            .appendLine("line1")
            .appendLine("line2")
            .toString();

        assertEquals("line1\nline2\n", result);
    }

    @Test
    @DisplayName("BuilderUtils stringBuilder prepend works")
    void stringBuilderPrepend() {
        String result = BuilderUtils.stringBuilder()
            .append("World")
            .prepend("Hello ")
            .toString();

        assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("BuilderUtils stringBuilder trimTrailingNewline works")
    void stringBuilderTrimTrailingNewline() {
        String result = BuilderUtils.stringBuilder()
            .appendLine("line")
            .trimTrailingNewline()
            .toString();

        assertEquals("line", result);
    }

    @Test
    @DisplayName("BuilderUtils stringBuilder isEmpty works")
    void stringBuilderIsEmpty() {
        BuilderUtils.StringBuilder sb = BuilderUtils.stringBuilder();

        assertTrue(sb.isEmpty());
        sb.append("x");
        assertFalse(sb.isEmpty());
    }

    @Test
    @DisplayName("BuilderUtils stringBuilder length works")
    void stringBuilderLength() {
        BuilderUtils.StringBuilder sb = BuilderUtils.stringBuilder()
            .append("Hello");

        assertEquals(5, sb.length());
    }

    @Test
    @DisplayName("BuilderUtils build with consumer works")
    void buildWithConsumer() {
        List<String> list = BuilderUtils.build(
            ArrayList::new,
            l -> { l.add("a"); l.add("b"); }
        );

        assertEquals(List.of("a", "b"), list);
    }

    @Test
    @DisplayName("BuilderUtils configurableBuilder works")
    void configurableBuilderWorks() {
        BuilderUtils.ConfigurableBuilder<List<String>> builder = BuilderUtils.configurableBuilder(ArrayList::new);

        List<String> list = builder
            .configure(l -> l.add("first"))
            .configure(l -> l.add("second"))
            .build();

        assertEquals(List.of("first", "second"), list);
    }

    @Test
    @DisplayName("BuilderUtils configurableBuilder configureIf works")
    void configurableBuilderConfigureIf() {
        BuilderUtils.ConfigurableBuilder<List<String>> builder = BuilderUtils.configurableBuilder(ArrayList::new);

        List<String> list = builder
            .configureIf(true, l -> l.add("included"))
            .configureIf(false, l -> l.add("excluded"))
            .build();

        assertEquals(List.of("included"), list);
    }

    @Test
    @DisplayName("BuilderUtils Builder interface works")
    void builderInterface() {
        BuilderUtils.Builder<String> builder = () -> "built";

        assertEquals("built", builder.build());
    }
}