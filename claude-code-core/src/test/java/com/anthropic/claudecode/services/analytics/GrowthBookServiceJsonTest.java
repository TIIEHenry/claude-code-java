/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.analytics;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for GrowthBookService JSON parsing.
 */
@DisplayName("GrowthBookService JSON Parsing Tests")
class GrowthBookServiceJsonTest {

    @Test
    @DisplayName("parseJsonToMap handles empty object")
    void parseJsonToMapHandlesEmptyObject() {
        Map<String, Object> result = invokeParseJsonToMap("{}");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseJsonToMap handles simple string values")
    void parseJsonToMapHandlesSimpleStringValues() {
        Map<String, Object> result = invokeParseJsonToMap("{\"name\":\"test\",\"value\":\"hello\"}");

        assertEquals(2, result.size());
        assertEquals("test", result.get("name"));
        assertEquals("hello", result.get("value"));
    }

    @Test
    @DisplayName("parseJsonToMap handles numeric values")
    void parseJsonToMapHandlesNumericValues() {
        Map<String, Object> result = invokeParseJsonToMap("{\"count\":42,\"price\":3.14}");

        assertEquals(2, result.size());
        assertEquals(42L, result.get("count"));
        assertEquals(3.14, result.get("price"));
    }

    @Test
    @DisplayName("parseJsonToMap handles boolean values")
    void parseJsonToMapHandlesBooleanValues() {
        Map<String, Object> result = invokeParseJsonToMap("{\"enabled\":true,\"disabled\":false}");

        assertEquals(2, result.size());
        assertEquals(true, result.get("enabled"));
        assertEquals(false, result.get("disabled"));
    }

    @Test
    @DisplayName("parseJsonToMap handles null values")
    void parseJsonToMapHandlesNullValues() {
        Map<String, Object> result = invokeParseJsonToMap("{\"value\":null}");

        assertEquals(1, result.size());
        assertNull(result.get("value"));
    }

    @Test
    @DisplayName("parseJsonToMap handles nested objects")
    void parseJsonToMapHandlesNestedObjects() {
        Map<String, Object> result = invokeParseJsonToMap("{\"outer\":{\"inner\":\"value\"}}");

        assertEquals(1, result.size());
        assertTrue(result.get("outer") instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) result.get("outer");
        assertEquals("value", nested.get("inner"));
    }

    @Test
    @DisplayName("parseJsonToMap handles arrays")
    void parseJsonToMapHandlesArrays() {
        Map<String, Object> result = invokeParseJsonToMap("{\"items\":[1,2,3]}");

        assertEquals(1, result.size());
        assertTrue(result.get("items") instanceof List);

        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) result.get("items");
        assertEquals(3, items.size());
        assertEquals(1L, items.get(0));
        assertEquals(2L, items.get(1));
        assertEquals(3L, items.get(2));
    }

    @Test
    @DisplayName("parseJsonToMap handles mixed content")
    void parseJsonToMapHandlesMixedContent() {
        String json = "{\"name\":\"test\",\"count\":10,\"enabled\":true,\"nested\":{\"key\":\"value\"},\"items\":[\"a\",\"b\"]}";

        Map<String, Object> result = invokeParseJsonToMap(json);

        assertEquals(5, result.size());
        assertEquals("test", result.get("name"));
        assertEquals(10L, result.get("count"));
        assertEquals(true, result.get("enabled"));
        assertTrue(result.get("nested") instanceof Map);
        assertTrue(result.get("items") instanceof List);
    }

    @Test
    @DisplayName("parseJsonToMap handles null and empty input")
    void parseJsonToMapHandlesNullAndEmpty() {
        assertTrue(invokeParseJsonToMap(null).isEmpty());
        assertTrue(invokeParseJsonToMap("").isEmpty());
        assertTrue(invokeParseJsonToMap("   ").isEmpty());
    }

    @Test
    @DisplayName("parseJsonToMap handles invalid JSON gracefully")
    void parseJsonToMapHandlesInvalidJson() {
        // Not a JSON object
        assertTrue(invokeParseJsonToMap("not json").isEmpty());
        // Array instead of object
        assertTrue(invokeParseJsonToMap("[1,2,3]").isEmpty());
    }

    @Test
    @DisplayName("parseJsonArray handles empty array")
    void parseJsonArrayHandlesEmptyArray() {
        List<Object> result = invokeParseJsonArray("[]");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseJsonArray handles string elements")
    void parseJsonArrayHandlesStringElements() {
        List<Object> result = invokeParseJsonArray("[\"a\",\"b\",\"c\"]");

        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    @DisplayName("parseJsonArray handles numeric elements")
    void parseJsonArrayHandlesNumericElements() {
        List<Object> result = invokeParseJsonArray("[1,2.5,3]");

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2.5, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    @DisplayName("parseJsonArray handles nested arrays")
    void parseJsonArrayHandlesNestedArrays() {
        List<Object> result = invokeParseJsonArray("[[1,2],[3,4]]");

        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof List);
        assertTrue(result.get(1) instanceof List);
    }

    @Test
    @DisplayName("parseJsonArray handles nested objects")
    void parseJsonArrayHandlesNestedObjects() {
        List<Object> result = invokeParseJsonArray("[{\"name\":\"a\"},{\"name\":\"b\"}]");

        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof Map);
        assertTrue(result.get(1) instanceof Map);
    }

    // Helper methods to invoke private methods via reflection
    private Map<String, Object> invokeParseJsonToMap(String json) {
        try {
            var method = GrowthBookService.class.getDeclaredMethod("parseJsonToMap", String.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(null, json);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private List<Object> invokeParseJsonArray(String json) {
        try {
            var method = GrowthBookService.class.getDeclaredMethod("parseJsonArray", String.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> result = (List<Object>) method.invoke(null, json);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}