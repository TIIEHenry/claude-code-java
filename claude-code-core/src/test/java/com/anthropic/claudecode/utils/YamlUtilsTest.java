/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for YamlUtils.
 */
class YamlUtilsTest {

    @Test
    @DisplayName("YamlUtils parse simple object")
    void parseSimple() {
        Object result = YamlUtils.parse("key: value");

        assertNotNull(result);
        assertTrue(result instanceof Map);
    }

    @Test
    @DisplayName("YamlUtils parse null returns null")
    void parseNull() {
        assertNull(YamlUtils.parse(null));
    }

    @Test
    @DisplayName("YamlUtils parse empty returns null")
    void parseEmpty() {
        assertNull(YamlUtils.parse(""));
    }

    @Test
    @DisplayName("YamlUtils parseMap returns map")
    void parseMap() {
        Map<String, Object> result = YamlUtils.parseMap("name: test\nvalue: 123");

        assertEquals("test", result.get("name"));
        assertEquals(123, result.get("value"));
    }

    @Test
    @DisplayName("YamlUtils parseMap null returns empty")
    void parseMapNull() {
        Map<String, Object> result = YamlUtils.parseMap(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("YamlUtils parseList returns list")
    void parseList() {
        List<Object> result = YamlUtils.parseList("- item1\n- item2\n- item3");

        assertEquals(3, result.size());
        assertEquals("item1", result.get(0));
    }

    @Test
    @DisplayName("YamlUtils parseList null returns empty")
    void parseListNull() {
        List<Object> result = YamlUtils.parseList(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("YamlUtils dump object")
    void dumpObject() {
        Map<String, Object> map = Map.of("key", "value");
        String result = YamlUtils.dump(map);

        assertTrue(result.contains("key"));
        assertTrue(result.contains("value"));
    }

    @Test
    @DisplayName("YamlUtils dump null returns empty")
    void dumpNull() {
        assertEquals("", YamlUtils.dump(null));
    }

    @Test
    @DisplayName("YamlUtils dump with custom options")
    void dumpWithOptions() {
        Map<String, Object> map = Map.of("key", "value");
        DumperOptions options = YamlUtils.prettyOptions();
        String result = YamlUtils.dump(map, options);

        assertNotNull(result);
        assertTrue(result.contains("key"));
    }

    @Test
    @DisplayName("YamlUtils dumpAll multiple documents")
    void dumpAll() {
        List<Object> docs = List.of(Map.of("a", 1), Map.of("b", 2));
        String result = YamlUtils.dumpAll(docs);

        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    @DisplayName("YamlUtils dumpAll null returns empty")
    void dumpAllNull() {
        assertEquals("", YamlUtils.dumpAll(null));
    }

    @Test
    @DisplayName("YamlUtils loadAll multiple documents")
    void loadAll() {
        String yaml = "---\nkey1: value1\n---\nkey2: value2";
        Iterable<Object> result = YamlUtils.loadAll(yaml);

        int count = 0;
        for (Object obj : result) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    @DisplayName("YamlUtils loadAll null returns empty")
    void loadAllNull() {
        Iterable<Object> result = YamlUtils.loadAll(null);

        assertFalse(result.iterator().hasNext());
    }

    @Test
    @DisplayName("YamlUtils prettyOptions creates options")
    void prettyOptions() {
        DumperOptions options = YamlUtils.prettyOptions();

        assertEquals(DumperOptions.FlowStyle.BLOCK, options.getDefaultFlowStyle());
        assertEquals(2, options.getIndent());
    }

    @Test
    @DisplayName("YamlUtils compactOptions creates options")
    void compactOptions() {
        DumperOptions options = YamlUtils.compactOptions();

        assertEquals(DumperOptions.FlowStyle.FLOW, options.getDefaultFlowStyle());
    }

    @Test
    @DisplayName("YamlUtils parseFile reads file")
    void parseFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.yaml").toFile();
        java.nio.file.Files.writeString(file.toPath(), "key: value");

        Object result = YamlUtils.parseFile(file.getAbsolutePath());

        assertTrue(result instanceof Map);
        assertEquals("value", ((Map<?, ?>) result).get("key"));
    }

    @Test
    @DisplayName("YamlUtils parseFileToMap reads file")
    void parseFileToMap(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.yaml").toFile();
        java.nio.file.Files.writeString(file.toPath(), "name: test");

        Map<String, Object> result = YamlUtils.parseFileToMap(file.getAbsolutePath());

        assertEquals("test", result.get("name"));
    }

    @Test
    @DisplayName("YamlUtils writeToFile writes file")
    void writeToFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.yaml").toFile();
        Map<String, Object> data = Map.of("key", "value");

        YamlUtils.writeToFile(data, file.getAbsolutePath());

        String content = java.nio.file.Files.readString(file.toPath());
        assertTrue(content.contains("key"));
    }

    @Test
    @DisplayName("YamlUtils isValidYaml true for valid")
    void isValidYamlTrue() {
        assertTrue(YamlUtils.isValidYaml("key: value"));
    }

    @Test
    @DisplayName("YamlUtils isValidYaml false for invalid")
    void isValidYamlFalse() {
        // SnakeYAML may be lenient with some inputs
        // Test with truly invalid YAML that causes parse error
        assertFalse(YamlUtils.isValidYaml("key: !!python/object/apply:os.system [rm -rf /]"));
    }

    @Test
    @DisplayName("YamlUtils isValidYaml null returns true")
    void isValidYamlNull() {
        assertTrue(YamlUtils.isValidYaml(null));
    }

    @Test
    @DisplayName("YamlUtils isValidYaml empty returns true")
    void isValidYamlEmpty() {
        assertTrue(YamlUtils.isValidYaml(""));
    }

    @Test
    @DisplayName("YamlUtils nested structure")
    void nestedStructure() {
        String yaml = "outer:\n  inner:\n    key: value";
        Map<String, Object> result = YamlUtils.parseMap(yaml);

        assertTrue(result.containsKey("outer"));
        Map<String, Object> outer = (Map<String, Object>) result.get("outer");
        assertTrue(outer.containsKey("inner"));
    }

    @Test
    @DisplayName("YamlUtils list in map")
    void listInMap() {
        String yaml = "items:\n  - one\n  - two\n  - three";
        Map<String, Object> result = YamlUtils.parseMap(yaml);

        assertTrue(result.get("items") instanceof List);
        List<?> items = (List<?>) result.get("items");
        assertEquals(3, items.size());
    }

    @Test
    @DisplayName("YamlUtils parseMap non-map returns empty")
    void parseMapNonMap() {
        Map<String, Object> result = YamlUtils.parseMap("- item1\n- item2");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("YamlUtils parseList non-list returns empty")
    void parseListNonList() {
        List<Object> result = YamlUtils.parseList("key: value");

        assertTrue(result.isEmpty());
    }
}