/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Treeify.
 */
class TreeifyTest {

    @Test
    @DisplayName("Treeify Options defaults")
    void optionsDefaults() {
        Treeify.Options opts = Treeify.Options.defaults();

        assertTrue(opts.showValues());
        assertFalse(opts.hideFunctions());
        assertNull(opts.treeCharColor());
        assertNull(opts.keyColor());
        assertNull(opts.valueColor());
    }

    @Test
    @DisplayName("Treeify Options record construction")
    void optionsRecord() {
        Treeify.Options opts = new Treeify.Options(
            false, true, "blue", "red", "green"
        );

        assertFalse(opts.showValues());
        assertTrue(opts.hideFunctions());
        assertEquals("blue", opts.treeCharColor());
        assertEquals("red", opts.keyColor());
        assertEquals("green", opts.valueColor());
    }

    @Test
    @DisplayName("Treeify treeify null map")
    void treeifyNullMap() {
        String result = Treeify.treeify((Map<String, Object>) null, Treeify.Options.defaults());
        assertEquals("(empty)", result);
    }

    @Test
    @DisplayName("Treeify treeify empty map")
    void treeifyEmptyMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        String result = Treeify.treeify(map, Treeify.Options.defaults());
        assertEquals("(empty)", result);
    }

    @Test
    @DisplayName("Treeify treeify simple map")
    void treeifySimpleMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("key1"));
        assertTrue(result.contains("value1"));
        assertTrue(result.contains("key2"));
        assertTrue(result.contains("value2"));
    }

    @Test
    @DisplayName("Treeify treeify nested map")
    void treeifyNestedMap() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("innerKey", "innerValue");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("outerKey", nested);

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("outerKey"));
        assertTrue(result.contains("innerKey"));
        assertTrue(result.contains("innerValue"));
    }

    @Test
    @DisplayName("Treeify treeify with null options uses defaults")
    void treeifyNullOptions() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");

        String result = Treeify.treeify(map, null);

        assertTrue(result.contains("key"));
        assertTrue(result.contains("value"));
    }

    @Test
    @DisplayName("Treeify treeify hides values when showValues false")
    void treeifyHideValues() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key1", "value1");

        Treeify.Options opts = new Treeify.Options(false, false, null, null, null);
        String result = Treeify.treeify(map, opts);

        assertTrue(result.contains("key1"));
        assertFalse(result.contains("value1"));
    }

    @Test
    @DisplayName("Treeify treeify Object with null")
    void treeifyObjectNull() {
        String result = Treeify.treeify((Object) null);
        assertEquals("(empty)", result);
    }

    @Test
    @DisplayName("Treeify treeify Object with non-map")
    void treeifyObjectNonMap() {
        String result = Treeify.treeify("simple string");
        assertEquals("simple string", result);
    }

    @Test
    @DisplayName("Treeify treeify Object with number")
    void treeifyObjectNumber() {
        String result = Treeify.treeify(42);
        assertEquals("42", result);
    }

    @Test
    @DisplayName("Treeify treeify Object with map")
    void treeifyObjectMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");

        String result = Treeify.treeify((Object) map);

        assertTrue(result.contains("key"));
        assertTrue(result.contains("value"));
    }

    @Test
    @DisplayName("Treeify treeify with list shows array count")
    void treeifyWithList() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("items", List.of("a", "b", "c"));

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("items"));
        assertTrue(result.contains("[Array(3)]"));
    }

    @Test
    @DisplayName("Treeify treeify circular reference")
    void treeifyCircularRef() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("self", map);

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("[Circular]"));
    }

    @Test
    @DisplayName("Treeify treeify deeply nested")
    void treeifyDeeplyNested() {
        Map<String, Object> level3 = new LinkedHashMap<>();
        level3.put("deepKey", "deepValue");

        Map<String, Object> level2 = new LinkedHashMap<>();
        level2.put("level3", level3);

        Map<String, Object> level1 = new LinkedHashMap<>();
        level1.put("level2", level2);

        String result = Treeify.treeify(level1, Treeify.Options.defaults());

        assertTrue(result.contains("level2"));
        assertTrue(result.contains("level3"));
        assertTrue(result.contains("deepKey"));
        assertTrue(result.contains("deepValue"));
    }

    @Test
    @DisplayName("Treeify treeify with empty key")
    void treeifyEmptyKey() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("", "value");

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("value"));
    }

    @Test
    @DisplayName("Treeify treeify integer value")
    void treeifyIntegerValue() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("number", 123);

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("number"));
        assertTrue(result.contains("123"));
    }

    @Test
    @DisplayName("Treeify treeify boolean value")
    void treeifyBooleanValue() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bool", true);

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("bool"));
        assertTrue(result.contains("true"));
    }

    @Test
    @DisplayName("Treeify treeify null value")
    void treeifyNullValue() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nullKey", null);

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("nullKey"));
        assertTrue(result.contains("null"));
    }

    @Test
    @DisplayName("Treeify fromMap simple")
    void fromMapSimple() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");

        String result = Treeify.fromMap(map);

        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    @DisplayName("Treeify fromMap empty")
    void fromMapEmpty() {
        Map<String, Object> map = new LinkedHashMap<>();
        String result = Treeify.fromMap(map);
        assertEquals("(empty)", result);
    }

    @Test
    @DisplayName("Treeify tree characters present")
    void treeCharactersPresent() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("first", "1");
        map.put("last", "2");

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        // Tree chars: ├ (branch), └ (last branch), │ (line)
        assertTrue(result.contains("└") || result.contains("├"));
    }

    @Test
    @DisplayName("Treeify treeify with string value directly")
    void treeifyStringValue() {
        // String values are rendered with colorize
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("greeting", "Hello World");

        String result = Treeify.treeify(map, Treeify.Options.defaults());

        assertTrue(result.contains("greeting"));
        assertTrue(result.contains("Hello World"));
    }

    @Test
    @DisplayName("Treeify TreeNode interface exists")
    void treeNodeInterface() {
        // Verify the interface exists and has getChildren method
        Treeify.TreeNode node = new Treeify.TreeNode() {
            @Override
            public Map<String, Object> getChildren() {
                return new LinkedHashMap<>();
            }
        };

        assertNotNull(node.getChildren());
    }

    @Test
    @DisplayName("Treeify multiple branches")
    void multipleBranches() {
        Map<String, Object> child1 = new LinkedHashMap<>();
        child1.put("child1Key", "child1Value");

        Map<String, Object> child2 = new LinkedHashMap<>();
        child2.put("child2Key", "child2Value");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("branch1", child1);
        root.put("branch2", child2);

        String result = Treeify.treeify(root, Treeify.Options.defaults());

        assertTrue(result.contains("branch1"));
        assertTrue(result.contains("branch2"));
        assertTrue(result.contains("child1Key"));
        assertTrue(result.contains("child2Key"));
    }
}