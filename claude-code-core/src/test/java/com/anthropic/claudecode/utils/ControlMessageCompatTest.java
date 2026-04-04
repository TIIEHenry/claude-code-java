/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ControlMessageCompat.
 */
class ControlMessageCompatTest {

    @Test
    @DisplayName("ControlMessageCompat normalizeControlMessageKeys null")
    void normalizeNull() {
        assertNull(ControlMessageCompat.normalizeControlMessageKeys(null));
    }

    @Test
    @DisplayName("ControlMessageCompat normalizeControlMessageKeys non-map")
    void normalizeNonMap() {
        String input = "not a map";
        assertEquals(input, ControlMessageCompat.normalizeControlMessageKeys(input));
    }

    @Test
    @DisplayName("ControlMessageCompat normalizeControlMessageKeys requestId")
    void normalizeRequestId() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", "123");
        input.put("type", "test");

        Object result = ControlMessageCompat.normalizeControlMessageKeys(input);

        assertTrue(((Map<?, ?>) result).containsKey("request_id"));
        assertFalse(((Map<?, ?>) result).containsKey("requestId"));
        assertEquals("123", ((Map<?, ?>) result).get("request_id"));
    }

    @Test
    @DisplayName("ControlMessageCompat normalizeControlMessageKeys preserves existing")
    void normalizePreservesExisting() {
        Map<String, Object> input = new HashMap<>();
        input.put("request_id", "123");

        Object result = ControlMessageCompat.normalizeControlMessageKeys(input);

        assertEquals("123", ((Map<?, ?>) result).get("request_id"));
    }

    @Test
    @DisplayName("ControlMessageCompat normalizeControlMessageKeys nested response")
    void normalizeNestedResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", "456");

        Map<String, Object> input = new HashMap<>();
        input.put("response", response);

        ControlMessageCompat.normalizeControlMessageKeys(input);

        assertTrue(response.containsKey("request_id"));
        assertFalse(response.containsKey("requestId"));
    }

    @Test
    @DisplayName("ControlMessageCompat hasRequestId true")
    void hasRequestIdTrue() {
        Map<String, Object> input = new HashMap<>();
        input.put("request_id", "123");
        assertTrue(ControlMessageCompat.hasRequestId(input));
    }

    @Test
    @DisplayName("ControlMessageCompat hasRequestId false")
    void hasRequestIdFalse() {
        Map<String, Object> input = new HashMap<>();
        input.put("other", "value");
        assertFalse(ControlMessageCompat.hasRequestId(input));
    }

    @Test
    @DisplayName("ControlMessageCompat hasRequestId non-map")
    void hasRequestIdNonMap() {
        assertFalse(ControlMessageCompat.hasRequestId("not a map"));
    }

    @Test
    @DisplayName("ControlMessageCompat getRequestId returns value")
    void getRequestIdValue() {
        Map<String, Object> input = new HashMap<>();
        input.put("request_id", "abc123");
        assertEquals("abc123", ControlMessageCompat.getRequestId(input));
    }

    @Test
    @DisplayName("ControlMessageCompat getRequestId null")
    void getRequestIdNull() {
        Map<String, Object> input = new HashMap<>();
        assertNull(ControlMessageCompat.getRequestId(input));
    }

    @Test
    @DisplayName("ControlMessageCompat getRequestId non-map")
    void getRequestIdNonMap() {
        assertNull(ControlMessageCompat.getRequestId("not a map"));
    }

    @Test
    @DisplayName("ControlMessageCompat isControlRequest true")
    void isControlRequestTrue() {
        Map<String, Object> input = new HashMap<>();
        input.put("type", "control_request");
        assertTrue(ControlMessageCompat.isControlRequest(input));
    }

    @Test
    @DisplayName("ControlMessageCompat isControlRequest false")
    void isControlRequestFalse() {
        Map<String, Object> input = new HashMap<>();
        input.put("type", "other");
        assertFalse(ControlMessageCompat.isControlRequest(input));
    }

    @Test
    @DisplayName("ControlMessageCompat isControlRequest non-map")
    void isControlRequestNonMap() {
        assertFalse(ControlMessageCompat.isControlRequest("not a map"));
    }

    @Test
    @DisplayName("ControlMessageCompat isControlResponse true")
    void isControlResponseTrue() {
        Map<String, Object> input = new HashMap<>();
        input.put("type", "control_response");
        assertTrue(ControlMessageCompat.isControlResponse(input));
    }

    @Test
    @DisplayName("ControlMessageCompat isControlResponse false")
    void isControlResponseFalse() {
        Map<String, Object> input = new HashMap<>();
        input.put("type", "other");
        assertFalse(ControlMessageCompat.isControlResponse(input));
    }

    @Test
    @DisplayName("ControlMessageCompat isControlResponse non-map")
    void isControlResponseNonMap() {
        assertFalse(ControlMessageCompat.isControlResponse("not a map"));
    }
}