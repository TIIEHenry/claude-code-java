/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JsonUtils.
 */
class JsonUtilsTest {

    @Test
    @DisplayName("JsonUtils parse handles null")
    void parseNull() {
        Map<String, Object> result = JsonUtils.parse(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("JsonUtils parse handles empty string")
    void parseEmpty() {
        Map<String, Object> result = JsonUtils.parse("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("JsonUtils parse handles empty object")
    void parseEmptyObject() {
        Map<String, Object> result = JsonUtils.parse("{}");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("JsonUtils parse simple object")
    void parseSimpleObject() {
        Map<String, Object> result = JsonUtils.parse("{\"key\": \"value\"}");

        assertEquals(1, result.size());
        assertEquals("value", result.get("key"));
    }

    @Test
    @DisplayName("JsonUtils parse multiple keys")
    void parseMultipleKeys() {
        Map<String, Object> result = JsonUtils.parse("{\"a\": 1, \"b\": 2}");

        assertEquals(2, result.size());
        assertEquals(1L, result.get("a"));
        assertEquals(2L, result.get("b"));
    }

    @Test
    @DisplayName("JsonUtils parse nested object")
    void parseNestedObject() {
        Map<String, Object> result = JsonUtils.parse("{\"outer\": {\"inner\": \"value\"}}");

        assertEquals(1, result.size());
        assertTrue(result.get("outer") instanceof Map);

        Map<?, ?> inner = (Map<?, ?>) result.get("outer");
        assertEquals("value", inner.get("inner"));
    }

    @Test
    @DisplayName("JsonUtils parse array in object")
    void parseArrayInObject() {
        Map<String, Object> result = JsonUtils.parse("{\"items\": [1, 2, 3]}");

        assertEquals(1, result.size());
        assertTrue(result.get("items") instanceof List);

        List<?> items = (List<?>) result.get("items");
        assertEquals(3, items.size());
    }

    @Test
    @DisplayName("JsonUtils parseArray handles null")
    void parseArrayNull() {
        List<Object> result = JsonUtils.parseArray(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("JsonUtils parseArray handles empty array")
    void parseArrayEmpty() {
        List<Object> result = JsonUtils.parseArray("[]");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("JsonUtils parseArray simple values")
    void parseArraySimple() {
        List<Object> result = JsonUtils.parseArray("[1, \"two\", true]");

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals("two", result.get(1));
        assertEquals(true, result.get(2));
    }

    @Test
    @DisplayName("JsonUtils stringify null")
    void stringifyNull() {
        assertEquals("null", JsonUtils.stringify(null));
    }

    @Test
    @DisplayName("JsonUtils stringify boolean")
    void stringifyBoolean() {
        assertEquals("true", JsonUtils.stringify(true));
        assertEquals("false", JsonUtils.stringify(false));
    }

    @Test
    @DisplayName("JsonUtils stringify number")
    void stringifyNumber() {
        assertEquals("42", JsonUtils.stringify(42));
        assertEquals("3.14", JsonUtils.stringify(3.14));
    }

    @Test
    @DisplayName("JsonUtils stringify string")
    void stringifyString() {
        assertEquals("\"hello\"", JsonUtils.stringify("hello"));
    }

    @Test
    @DisplayName("JsonUtils stringify escapes special chars")
    void stringifyEscapes() {
        String result = JsonUtils.stringify("line1\nline2");
        assertTrue(result.contains("\\n"));
    }

    @Test
    @DisplayName("JsonUtils stringify map")
    void stringifyMap() {
        Map<String, Object> map = Map.of("key", "value");
        String result = JsonUtils.stringify(map);

        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("key"));
        assertTrue(result.contains("value"));
    }

    @Test
    @DisplayName("JsonUtils stringify list")
    void stringifyList() {
        List<Object> list = List.of(1, 2, 3);
        String result = JsonUtils.stringify(list);

        assertEquals("[1,2,3]", result);
    }

    @Test
    @DisplayName("JsonUtils toJson is alias for stringify")
    void toJsonAlias() {
        assertEquals(JsonUtils.stringify("test"), JsonUtils.toJson("test"));
    }

    @Test
    @DisplayName("JsonUtils tryParse returns empty map on invalid")
    void tryParseInvalid() {
        Map<String, Object> result = JsonUtils.tryParse("invalid json");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("JsonUtils tryParse works on valid")
    void tryParseValid() {
        Map<String, Object> result = JsonUtils.tryParse("{\"key\": \"value\"}");
        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }

    @Test
    @DisplayName("JsonUtils isValidJson rejects null")
    void isValidJsonNull() {
        assertFalse(JsonUtils.isValidJson(null));
    }

    @Test
    @DisplayName("JsonUtils isValidJson accepts valid object")
    void isValidJsonObject() {
        assertTrue(JsonUtils.isValidJson("{\"key\": \"value\"}"));
    }

    @Test
    @DisplayName("JsonUtils isValidJson accepts valid array")
    void isValidJsonArray() {
        assertTrue(JsonUtils.isValidJson("[1, 2, 3]"));
    }

    @Test
    @DisplayName("JsonUtils isValidJson rejects invalid")
    void isValidJsonInvalid() {
        assertFalse(JsonUtils.isValidJson("not json"));
    }

    @Test
    @DisplayName("JsonUtils roundtrip works")
    void roundtripWorks() {
        Map<String, Object> original = Map.of(
            "name", "test",
            "count", 42L,
            "active", true
        );

        String json = JsonUtils.stringify(original);
        Map<String, Object> parsed = JsonUtils.parse(json);

        assertEquals("test", parsed.get("name"));
        assertEquals(42L, parsed.get("count"));
        assertEquals(true, parsed.get("active"));
    }
}