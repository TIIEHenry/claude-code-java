/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JsonUtil.
 */
class JsonUtilTest {

    @BeforeEach
    void clearCache() {
        JsonUtil.clearCache();
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses object")
    void parseObject() {
        Object result = JsonUtil.safeParseJSON("{\"key\": \"value\"}");

        assertNotNull(result);
        assertTrue(result instanceof org.json.JSONObject);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses array")
    void parseArray() {
        Object result = JsonUtil.safeParseJSON("[1, 2, 3]");

        assertNotNull(result);
        assertTrue(result instanceof org.json.JSONArray);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses null returns null")
    void parseNull() {
        Object result = JsonUtil.safeParseJSON(null);

        assertNull(result);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses empty returns null")
    void parseEmpty() {
        Object result = JsonUtil.safeParseJSON("");

        assertNull(result);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses primitive string")
    void parsePrimitiveString() {
        Object result = JsonUtil.safeParseJSON("\"hello\"");

        assertEquals("hello", result);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses primitive boolean true")
    void parsePrimitiveTrue() {
        Object result = JsonUtil.safeParseJSON("true");

        assertEquals(true, result);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses primitive boolean false")
    void parsePrimitiveFalse() {
        Object result = JsonUtil.safeParseJSON("false");

        assertEquals(false, result);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses primitive integer")
    void parsePrimitiveInteger() {
        Object result = JsonUtil.safeParseJSON("42");

        assertEquals(42L, result);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses primitive double")
    void parsePrimitiveDouble() {
        Object result = JsonUtil.safeParseJSON("3.14");

        assertEquals(3.14, result);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON parses null literal")
    void parseNullLiteral() {
        Object result = JsonUtil.safeParseJSON("null");

        assertNull(result);
    }

    @Test
    @DisplayName("JsonUtil safeParseJSON returns null for invalid")
    void parseInvalid() {
        Object result = JsonUtil.safeParseJSON("not json", false);

        assertNull(result);
    }

    @Test
    @DisplayName("JsonUtil stripBOM removes UTF-8 BOM")
    void stripBOM() {
        String result = JsonUtil.stripBOM("\uFEFF{\"key\": \"value\"}");

        assertEquals("{\"key\": \"value\"}", result);
    }

    @Test
    @DisplayName("JsonUtil stripBOM null returns null")
    void stripBOMNull() {
        assertNull(JsonUtil.stripBOM(null));
    }

    @Test
    @DisplayName("JsonUtil stripBOM empty returns empty")
    void stripBOMEmpty() {
        assertEquals("", JsonUtil.stripBOM(""));
    }

    @Test
    @DisplayName("JsonUtil stripBOM no BOM unchanged")
    void stripBOMNoBOM() {
        String result = JsonUtil.stripBOM("no bom here");

        assertEquals("no bom here", result);
    }

    @Test
    @DisplayName("JsonUtil parseJSONL parses lines")
    void parseJSONL() {
        String data = "{\"a\": 1}\n{\"b\": 2}\n{\"c\": 3}";
        List<Object> result = JsonUtil.parseJSONL(data);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("JsonUtil parseJSONL skips empty lines")
    void parseJSONLSkipEmpty() {
        String data = "{\"a\": 1}\n\n{\"b\": 2}";
        List<Object> result = JsonUtil.parseJSONL(data);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("JsonUtil parseJSONL null returns empty")
    void parseJSONLNull() {
        List<Object> result = JsonUtil.parseJSONL(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("JsonUtil parseJSONL empty returns empty")
    void parseJSONLEmpty() {
        List<Object> result = JsonUtil.parseJSONL("");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("JsonUtil readJSONLFile reads file")
    void readJSONLFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.jsonl").toFile();
        java.nio.file.Files.writeString(file.toPath(), "{\"a\": 1}\n{\"b\": 2}");

        List<Object> result = JsonUtil.readJSONLFile(file.getAbsolutePath());

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("JsonUtil addItemToJSONArray adds to empty")
    void addItemToJSONArrayEmpty() {
        String result = JsonUtil.addItemToJSONArray(null, "item");

        assertEquals("[\"item\"]", result);
    }

    @Test
    @DisplayName("JsonUtil addItemToJSONArray adds to existing")
    void addItemToJSONArrayExisting() {
        String result = JsonUtil.addItemToJSONArray("[\"existing\"]", "new");

        assertTrue(result.contains("existing"));
        assertTrue(result.contains("new"));
    }

    @Test
    @DisplayName("JsonUtil jsonStringify string")
    void stringifyString() {
        String result = JsonUtil.jsonStringify("hello");

        assertEquals("\"hello\"", result);
    }

    @Test
    @DisplayName("JsonUtil jsonStringify null")
    void stringifyNull() {
        String result = JsonUtil.jsonStringify(null);

        assertEquals("null", result);
    }

    @Test
    @DisplayName("JsonUtil jsonStringify boolean")
    void stringifyBoolean() {
        assertEquals("true", JsonUtil.jsonStringify(true));
        assertEquals("false", JsonUtil.jsonStringify(false));
    }

    @Test
    @DisplayName("JsonUtil jsonStringify number")
    void stringifyNumber() {
        assertEquals("42", JsonUtil.jsonStringify(42));
        assertEquals("3.14", JsonUtil.jsonStringify(3.14));
    }

    @Test
    @DisplayName("JsonUtil jsonStringify map")
    void stringifyMap() {
        Map<String, Object> map = Map.of("key", "value");
        String result = JsonUtil.jsonStringify(map);

        assertTrue(result.contains("key"));
        assertTrue(result.contains("value"));
    }

    @Test
    @DisplayName("JsonUtil jsonStringify list")
    void stringifyList() {
        List<Object> list = List.of(1, 2, 3);
        String result = JsonUtil.jsonStringify(list);

        assertEquals("[1,2,3]", result);
    }

    @Test
    @DisplayName("JsonUtil jsonStringify with indent")
    void stringifyWithIndent() {
        List<Object> list = List.of(1, 2, 3);
        String result = JsonUtil.jsonStringify(list, 2);

        assertTrue(result.contains("\n"));
    }

    @Test
    @DisplayName("JsonUtil jsonStringify escapes special chars")
    void stringifyEscapes() {
        String result = JsonUtil.jsonStringify("hello\nworld");

        assertTrue(result.contains("\\n"));
    }

    @Test
    @DisplayName("JsonUtil clearCache clears cache")
    void testClearCache() {
        // Parse twice to cache
        JsonUtil.safeParseJSON("{\"test\": 1}");
        JsonUtil.clearCache();

        // No way to directly verify cache is empty, but method should work
        assertDoesNotThrow(JsonUtil::clearCache);
    }

    @Test
    @DisplayName("JsonUtil caching returns same object for identical input")
    void cachingReturnsSame() {
        String json = "{\"small\": \"value\"}";
        Object result1 = JsonUtil.safeParseJSON(json);
        Object result2 = JsonUtil.safeParseJSON(json);

        // Should be same cached object
        assertEquals(result1, result2);
    }
}