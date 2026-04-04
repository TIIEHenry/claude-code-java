/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TomlUtils.
 */
class TomlUtilsTest {

    @Test
    @DisplayName("TomlUtils parse simple key-value")
    void parseSimple() {
        Map<String, Object> result = TomlUtils.parse("key = \"value\"");

        assertEquals("value", result.get("key"));
    }

    @Test
    @DisplayName("TomlUtils parse integer")
    void parseInteger() {
        Map<String, Object> result = TomlUtils.parse("count = 42");

        assertEquals(42L, result.get("count"));
    }

    @Test
    @DisplayName("TomlUtils parse boolean")
    void parseBoolean() {
        Map<String, Object> result = TomlUtils.parse("enabled = true\ndisabled = false");

        assertEquals(true, result.get("enabled"));
        assertEquals(false, result.get("disabled"));
    }

    @Test
    @DisplayName("TomlUtils parse float")
    void parseFloat() {
        Map<String, Object> result = TomlUtils.parse("value = 3.14");

        assertEquals(3.14, result.get("value"));
    }

    @Test
    @DisplayName("TomlUtils parse table")
    void parseTable() {
        Map<String, Object> result = TomlUtils.parse("[section]\nkey = \"value\"");

        assertTrue(result.containsKey("section"));
        Map<String, Object> section = (Map<String, Object>) result.get("section");
        assertEquals("value", section.get("key"));
    }

    @Test
    @DisplayName("TomlUtils parse nested table")
    void parseNestedTable() {
        Map<String, Object> result = TomlUtils.parse("[section.subsection]\nkey = \"value\"");

        assertTrue(result.containsKey("section"));
        Map<String, Object> section = (Map<String, Object>) result.get("section");
        assertTrue(section.containsKey("subsection"));
    }

    @Test
    @DisplayName("TomlUtils parse array of tables")
    void parseArrayOfTables() {
        Map<String, Object> result = TomlUtils.parse("[[items]]\nname = \"first\"\n[[items]]\nname = \"second\"");

        assertTrue(result.containsKey("items"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(2, items.size());
        assertEquals("first", items.get(0).get("name"));
        assertEquals("second", items.get(1).get("name"));
    }

    @Test
    @DisplayName("TomlUtils parse inline array")
    void parseInlineArray() {
        Map<String, Object> result = TomlUtils.parse("values = [1, 2, 3]");

        List<Object> values = (List<Object>) result.get("values");
        assertEquals(3, values.size());
        assertEquals(1L, values.get(0));
        assertEquals(2L, values.get(1));
        assertEquals(3L, values.get(2));
    }

    @Test
    @DisplayName("TomlUtils parse comment")
    void parseComment() {
        Map<String, Object> result = TomlUtils.parse("# comment\nkey = \"value\"");

        assertEquals(1, result.size());
        assertEquals("value", result.get("key"));
    }

    @Test
    @DisplayName("TomlUtils parse inline comment")
    void parseInlineComment() {
        // Inline comments are stripped for non-quoted values
        Map<String, Object> result = TomlUtils.parse("count = 42 # comment");

        assertEquals(42L, result.get("count"));
    }

    @Test
    @DisplayName("TomlUtils parse empty returns empty")
    void parseEmpty() {
        Map<String, Object> result = TomlUtils.parse("");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TomlUtils parse single quoted string")
    void parseSingleQuoted() {
        Map<String, Object> result = TomlUtils.parse("key = 'value'");

        assertEquals("value", result.get("key"));
    }

    @Test
    @DisplayName("TomlUtils parseFile reads file")
    void parseFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.toml").toFile();
        java.nio.file.Files.writeString(file.toPath(), "key = \"value\"");

        Map<String, Object> result = TomlUtils.parseFile(file);

        assertEquals("value", result.get("key"));
    }

    @Test
    @DisplayName("TomlUtils getString returns string")
    void getString() {
        Map<String, Object> toml = TomlUtils.parse("key = \"value\"");
        Optional<String> result = TomlUtils.getString(toml, "key");

        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

    @Test
    @DisplayName("TomlUtils getString nested path")
    void getStringNested() {
        Map<String, Object> toml = TomlUtils.parse("[section]\nkey = \"value\"");
        Optional<String> result = TomlUtils.getString(toml, "section", "key");

        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

    @Test
    @DisplayName("TomlUtils getString missing returns empty")
    void getStringMissing() {
        Map<String, Object> toml = TomlUtils.parse("key = \"value\"");
        Optional<String> result = TomlUtils.getString(toml, "missing");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("TomlUtils getInt returns integer")
    void getInt() {
        Map<String, Object> toml = TomlUtils.parse("count = 42");
        Optional<Integer> result = TomlUtils.getInt(toml, "count");

        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    @DisplayName("TomlUtils getLong returns long")
    void getLong() {
        Map<String, Object> toml = TomlUtils.parse("count = 42");
        Optional<Long> result = TomlUtils.getLong(toml, "count");

        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
    }

    @Test
    @DisplayName("TomlUtils getDouble returns double")
    void getDouble() {
        Map<String, Object> toml = TomlUtils.parse("value = 3.14");
        Optional<Double> result = TomlUtils.getDouble(toml, "value");

        assertTrue(result.isPresent());
        assertEquals(3.14, result.get());
    }

    @Test
    @DisplayName("TomlUtils getBoolean returns boolean")
    void getBoolean() {
        Map<String, Object> toml = TomlUtils.parse("enabled = true");
        Optional<Boolean> result = TomlUtils.getBoolean(toml, "enabled");

        assertTrue(result.isPresent());
        assertEquals(true, result.get());
    }

    @Test
    @DisplayName("TomlUtils getTable returns table")
    void getTable() {
        Map<String, Object> toml = TomlUtils.parse("[section]\nkey = \"value\"");
        Optional<Map<String, Object>> result = TomlUtils.getTable(toml, "section");

        assertTrue(result.isPresent());
        assertTrue(result.get().containsKey("key"));
    }

    @Test
    @DisplayName("TomlUtils getList returns list")
    void getList() {
        Map<String, Object> toml = TomlUtils.parse("values = [1, 2, 3]");
        Optional<List<Object>> result = TomlUtils.getList(toml, "values");

        assertTrue(result.isPresent());
        assertEquals(3, result.get().size());
    }

    @Test
    @DisplayName("TomlUtils multiline string")
    void multilineString() {
        Map<String, Object> result = TomlUtils.parse("text = \"\"\"multi\nline\"\"\"");

        assertTrue(result.get("text").toString().contains("multi"));
    }

    @Test
    @DisplayName("TomlUtils empty path returns empty")
    void emptyPath() {
        Map<String, Object> toml = TomlUtils.parse("key = \"value\"");
        Optional<String> result = TomlUtils.getString(toml);

        assertFalse(result.isPresent());
    }
}