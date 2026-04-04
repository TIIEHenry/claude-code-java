/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiUtils.
 */
class ApiUtilsTest {

    @Test
    @DisplayName("ApiUtils CacheScope enum values")
    void cacheScopeEnum() {
        ApiUtils.CacheScope[] values = ApiUtils.CacheScope.values();
        assertEquals(3, values.length);
        assertEquals(ApiUtils.CacheScope.GLOBAL, ApiUtils.CacheScope.valueOf("GLOBAL"));
        assertEquals(ApiUtils.CacheScope.ORG, ApiUtils.CacheScope.valueOf("ORG"));
        assertEquals(ApiUtils.CacheScope.NONE, ApiUtils.CacheScope.valueOf("NONE"));
    }

    @Test
    @DisplayName("ApiUtils SystemPromptBlock record")
    void systemPromptBlock() {
        ApiUtils.SystemPromptBlock block = new ApiUtils.SystemPromptBlock("test text", ApiUtils.CacheScope.GLOBAL);
        assertEquals("test text", block.text());
        assertEquals(ApiUtils.CacheScope.GLOBAL, block.cacheScope());
    }

    @Test
    @DisplayName("ApiUtils splitSysPromptPrefix empty list")
    void splitSysPromptPrefixEmpty() {
        List<ApiUtils.SystemPromptBlock> result = ApiUtils.splitSysPromptPrefix(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("ApiUtils splitSysPromptPrefix single block")
    void splitSysPromptPrefixSingle() {
        List<ApiUtils.SystemPromptBlock> result = ApiUtils.splitSysPromptPrefix(List.of("test prompt"));
        assertEquals(1, result.size());
        assertEquals("test prompt", result.get(0).text());
        assertEquals(ApiUtils.CacheScope.ORG, result.get(0).cacheScope());
    }

    @Test
    @DisplayName("ApiUtils splitSysPromptPrefix multiple blocks")
    void splitSysPromptPrefixMultiple() {
        List<ApiUtils.SystemPromptBlock> result = ApiUtils.splitSysPromptPrefix(
            List.of("prompt 1", "prompt 2", "prompt 3")
        );
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("ApiUtils splitSysPromptPrefix with skipGlobalCache")
    void splitSysPromptPrefixWithSkipGlobalCache() {
        List<ApiUtils.SystemPromptBlock> result = ApiUtils.splitSysPromptPrefix(
            List.of("test prompt"),
            true
        );
        assertEquals(1, result.size());
        // Should still have ORG scope
        assertEquals(ApiUtils.CacheScope.ORG, result.get(0).cacheScope());
    }

    @Test
    @DisplayName("ApiUtils splitSysPromptPrefix filters null")
    void splitSysPromptPrefixFiltersNull() {
        List<String> prompts = new ArrayList<>();
        prompts.add("prompt 1");
        prompts.add(null);
        prompts.add("prompt 2");
        
        List<ApiUtils.SystemPromptBlock> result = ApiUtils.splitSysPromptPrefix(prompts);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("ApiUtils splitSysPromptPrefix filters empty")
    void splitSysPromptPrefixFiltersEmpty() {
        List<String> prompts = Arrays.asList("prompt 1", "", "prompt 2");
        
        List<ApiUtils.SystemPromptBlock> result = ApiUtils.splitSysPromptPrefix(prompts);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("ApiUtils appendSystemContext empty context")
    void appendSystemContextEmpty() {
        List<String> result = ApiUtils.appendSystemContext(
            List.of("prompt 1"),
            Map.of()
        );
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("ApiUtils appendSystemContext adds context")
    void appendSystemContextAddsContext() {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("key1", "value1");
        context.put("key2", "value2");
        
        List<String> result = ApiUtils.appendSystemContext(
            List.of("prompt 1"),
            context
        );
        assertEquals(2, result.size());
        assertTrue(result.get(1).contains("key1"));
        assertTrue(result.get(1).contains("value1"));
        assertTrue(result.get(1).contains("key2"));
        assertTrue(result.get(1).contains("value2"));
    }

    @Test
    @DisplayName("ApiUtils appendSystemContext preserves original")
    void appendSystemContextPreservesOriginal() {
        List<String> original = List.of("prompt 1", "prompt 2");
        List<String> result = ApiUtils.appendSystemContext(original, Map.of("key", "value"));
        assertEquals(3, result.size());
        assertEquals("prompt 1", result.get(0));
        assertEquals("prompt 2", result.get(1));
    }

    @Test
    @DisplayName("ApiUtils logAPIPrefix with null does nothing")
    void logApiPrefixNull() {
        // Should not throw
        ApiUtils.logAPIPrefix(null);
    }

    @Test
    @DisplayName("ApiUtils logAPIPrefix with empty does nothing")
    void logApiPrefixEmpty() {
        // Should not throw
        ApiUtils.logAPIPrefix(List.of());
    }

    @Test
    @DisplayName("ApiUtils logAPIPrefix with prompts")
    void logApiPrefixWithPrompts() {
        // Should not throw
        ApiUtils.logAPIPrefix(List.of("prompt 1", "prompt 2"));
    }
}
